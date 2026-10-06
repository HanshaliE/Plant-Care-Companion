package com.example.plantcarecompanion.data.repository

import com.example.plantcarecompanion.data.local.dao.SpeciesDao
import com.example.plantcarecompanion.data.network.ConnectivityChecker
import com.example.plantcarecompanion.data.network.RateLimitGuard
import com.example.plantcarecompanion.data.remote.PerenualApi
import com.example.plantcarecompanion.domain.CareInfoOutcome
import com.example.plantcarecompanion.domain.LocalPlantCatalog
import com.example.plantcarecompanion.domain.LookupError
import com.example.plantcarecompanion.domain.SearchOutcome
import com.example.plantcarecompanion.domain.Species
import com.example.plantcarecompanion.domain.SpeciesSource
import com.example.plantcarecompanion.domain.isTransient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.UnknownHostException
import kotlin.random.Random


class SpeciesRepository(
    private val api: PerenualApi,
    private val speciesDao: SpeciesDao,
    private val guard: RateLimitGuard,
    private val connectivity: ConnectivityChecker,
    private val hasApiKey: Boolean,
    private val clock: () -> Long = System::currentTimeMillis
) {

    fun normalize(raw: String): String =
        raw.trim().replace(Regex("[%_\\\\]"), " ").replace(Regex("\\s+"), " ").trim().lowercase()

    suspend fun search(rawQuery: String): SearchOutcome {
        val query = normalize(rawQuery)
        if (query.length < MIN_QUERY_LENGTH) return SearchOutcome(emptyList())

        memo[query]?.takeIf { clock() - it.at < MEMO_TTL_MILLIS }?.let { return SearchOutcome(it.species) }

        return when (val live = fetchLive(query)) {
            is Live.Ok -> {
                speciesDao.upsertAll(live.value.map { it.toEntity(clock()) })   // grow the offline cache
                rememberQuery(query, live.value)
                SearchOutcome(live.value)
            }
            is Live.Failed -> SearchOutcome(fallback(query), live.error)
        }
    }

    suspend fun getSpecies(id: Int): Species? =
        if (id < 0) LocalPlantCatalog.byId(id) else speciesDao.getById(id)?.toSpecies()

    suspend fun enrichCareInfo(species: Species): CareInfoOutcome {
        if (species.id <= 0) return CareInfoOutcome(species)             // built-in/manual: nothing to fetch

        val cached = speciesDao.getById(species.id)
        val fresh = cached != null && clock() - cached.cachedAt < DETAILS_TTL_MILLIS
        if (fresh && cached!!.wateringIntervalDays != null) return CareInfoOutcome(cached.toSpecies())

        if (!hasApiKey) return CareInfoOutcome(species, LookupError.MissingKey)
        if (!connectivity.isOnline()) return CareInfoOutcome(species, LookupError.Offline)
        gateOrNull(keepInReserve = 0)?.let { return CareInfoOutcome(species, it) }

        guard.recordRequest()
        return when (val result = callDetails(species.id)) {
            is Live.Ok -> {
                val merged = result.value.mergedOnto(species)
                speciesDao.upsert(merged.toEntity(clock()))
                CareInfoOutcome(merged.copy(source = SpeciesSource.LIVE))
            }
            is Live.Failed -> CareInfoOutcome(species, result.error)        // single attempt: non-critical
        }
    }

    private sealed interface Live<out T> {
        data class Ok<T>(val value: T) : Live<T>
        data class Failed(val error: LookupError) : Live<Nothing>
    }

    private suspend fun fetchLive(query: String): Live<List<Species>> {
        if (!hasApiKey) return Live.Failed(LookupError.MissingKey)
        if (!connectivity.isOnline()) return Live.Failed(LookupError.Offline)

        var attempt = 0
        while (true) {
            attempt++
            gateOrNull(keepInReserve = SEARCH_RESERVE)?.let { return Live.Failed(it) }
            guard.recordRequest()

            val result = callSearch(query)
            val error = (result as? Live.Failed)?.error
            if (error != null && error.isTransient && attempt < MAX_ATTEMPTS) {
                delay(backoffMillis(attempt))             // 0.6s, then 1.2s... plus jitter
                continue
            }
            return result
        }
    }

    private suspend fun callSearch(query: String): Live<List<Species>> = safely {
        val response = api.searchSpecies(query)
        if (response.isSuccessful) {
            val body = response.body() ?: return@safely Live.Failed(LookupError.BadResponse)
            Live.Ok(body.data.mapNotNull { it.toSpecies() })
        } else {
            Live.Failed(errorFor(response.code(), response.headers()["Retry-After"]))
        }
    }

    private suspend fun callDetails(id: Int): Live<Species> = safely {
        val response = api.getSpeciesDetails(id)
        if (response.isSuccessful) {
            val dto = response.body()?.toSpecies() ?: return@safely Live.Failed(LookupError.BadResponse)
            Live.Ok(dto)
        } else {
            Live.Failed(errorFor(response.code(), response.headers()["Retry-After"]))
        }
    }

    private suspend fun <T> safely(block: suspend () -> Live<T>): Live<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e                                              // never swallow coroutine cancellation
    } catch (e: UnknownHostException) {
        Live.Failed(LookupError.Offline)
    } catch (e: InterruptedIOException) {                    // includes SocketTimeoutException and OkHttp's call timeout
        Live.Failed(LookupError.Timeout)
    } catch (e: IOException) {
        Live.Failed(LookupError.Network)
    } catch (e: SerializationException) {
        Live.Failed(LookupError.BadResponse)
    }

    private fun errorFor(code: Int, retryAfterHeader: String?): LookupError = when (code) {
        429 -> {
            guard.recordRateLimited(retryAfterHeader?.trim()?.toLongOrNull())
            val gate = guard.check()
            LookupError.RateLimited((gate as? RateLimitGuard.Gate.Closed)?.reopensAtMillis ?: clock())
        }
        401, 403 -> LookupError.InvalidKey
        else -> LookupError.Server(code)
    }

    private fun gateOrNull(keepInReserve: Int): LookupError? =
        when (val gate = guard.check(keepInReserve)) {
            RateLimitGuard.Gate.Open -> null
            is RateLimitGuard.Gate.Closed -> LookupError.RateLimited(gate.reopensAtMillis)
        }

    private fun backoffMillis(attempt: Int): Long = 600L * (1L shl (attempt - 1)) + Random.nextLong(0, 250)

    private suspend fun fallback(query: String): List<Species> {
        val cached = speciesDao.search(query, FALLBACK_LIMIT).map { it.toSpecies() }
        val seen = cached.map { it.commonName.lowercase() }.toSet()
        val builtIn = LocalPlantCatalog.search(query).filter { it.commonName.lowercase() !in seen }
        return (cached + builtIn).take(FALLBACK_LIMIT)
    }

    private class MemoEntry(val species: List<Species>, val at: Long)

    private val memo = object : LinkedHashMap<String, MemoEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MemoEntry>?) = size > MEMO_SIZE
    }

    private fun rememberQuery(query: String, species: List<Species>) {
        memo[query] = MemoEntry(species, clock())
    }

    private fun Species.mergedOnto(base: Species): Species = copy(
        commonName = base.commonName,                        // keep the name the user already saw
        scientificName = scientificName ?: base.scientificName,
        wateringIntervalDays = wateringIntervalDays ?: base.wateringIntervalDays,
        wateringLabel = wateringLabel ?: base.wateringLabel,
        lightRequirement = lightRequirement ?: base.lightRequirement,
        imageUrl = imageUrl ?: base.imageUrl
    )

    companion object {
        const val MIN_QUERY_LENGTH = 2
        private const val MAX_ATTEMPTS = 2
        private const val SEARCH_RESERVE = 10
        private const val FALLBACK_LIMIT = 25
        private const val MEMO_SIZE = 20
        private const val MEMO_TTL_MILLIS = 10 * 60 * 1000L
        private const val DETAILS_TTL_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
