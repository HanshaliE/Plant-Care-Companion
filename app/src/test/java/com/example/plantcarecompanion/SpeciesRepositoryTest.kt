package com.example.plantcarecompanion

import com.example.plantcarecompanion.data.local.dao.SpeciesDao
import com.example.plantcarecompanion.data.local.entity.CachedSpecies
import com.example.plantcarecompanion.data.network.ConnectivityChecker
import com.example.plantcarecompanion.data.network.RateLimitGuard
import com.example.plantcarecompanion.data.network.RateLimitStore
import com.example.plantcarecompanion.data.remote.PerenualApi
import com.example.plantcarecompanion.data.remote.dto.SpeciesDto
import com.example.plantcarecompanion.data.remote.dto.SpeciesListResponse
import com.example.plantcarecompanion.data.repository.SpeciesRepository
import com.example.plantcarecompanion.domain.LookupError
import com.example.plantcarecompanion.domain.SpeciesSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

// ---------- fakes ----------

private class RepoFakeStore : RateLimitStore {
    override var windowDay: Long = -1
    override var usedToday: Int = 0
    override var blockedUntil: Long = 0
}

private class FakeDao : SpeciesDao {
    val rows = mutableMapOf<Int, CachedSpecies>()
    override suspend fun upsertAll(species: List<CachedSpecies>) { species.forEach { rows[it.id] = it } }
    override suspend fun upsert(species: CachedSpecies) { rows[species.id] = species }
    override suspend fun getById(id: Int) = rows[id]
    override suspend fun search(query: String, limit: Int) =
        rows.values.filter { it.commonName.lowercase().contains(query) }.take(limit)
}

/** Plays back a script of responses/exceptions, one per call. */
private class FakeApi(private val script: MutableList<() -> Response<SpeciesListResponse>>) : PerenualApi {
    var calls = 0
    override suspend fun searchSpecies(query: String, page: Int): Response<SpeciesListResponse> {
        calls++
        return script.removeAt(0).invoke()
    }
    override suspend fun getSpeciesDetails(id: Int): Response<SpeciesDto> = throw IOException("not used")
}

/** Search is never used; the details endpoint answers 404 (plant has no details page). */
private class DetailsNotFoundApi : PerenualApi {
    override suspend fun searchSpecies(query: String, page: Int): Response<SpeciesListResponse> =
        throw IOException("not used")
    override suspend fun getSpeciesDetails(id: Int): Response<SpeciesDto> {
        val raw = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("https://perenual.com/").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(404).message("Not Found")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
        return Response.error(raw.body!!, raw)
    }
}

private fun ok(vararg names: String) = {
    Response.success(
        SpeciesListResponse(
            data = names.mapIndexed { i, n ->
                SpeciesDto(id = 100 + i, commonName = JsonPrimitive(n), watering = JsonPrimitive("Average"))
            }
        )
    )
}

private fun httpError(code: Int, retryAfter: String? = null): () -> Response<SpeciesListResponse> = {
    val raw = okhttp3.Response.Builder()
        .request(okhttp3.Request.Builder().url("https://perenual.com/").build())
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(code).message("err")
        .apply { if (retryAfter != null) header("Retry-After", retryAfter) }
        .body("{}".toResponseBody("application/json".toMediaType()))
        .build()
    Response.error(raw.body!!, raw)
}

private fun throwing(e: IOException): () -> Response<SpeciesListResponse> = { throw e }

class SpeciesRepositoryTest {

    private val now = 5 * RateLimitGuard.DAY_MILLIS + 5_000
    private val dao = FakeDao()
    private var online = true

    private fun repo(api: FakeApi, hasKey: Boolean = true, limit: Int = 100) = SpeciesRepository(
        api = api,
        speciesDao = dao,
        guard = RateLimitGuard(RepoFakeStore(), clock = { now }, dailyLimit = limit),
        connectivity = ConnectivityChecker { online },
        hasApiKey = hasKey,
        clock = { now }
    )

    @Test
    fun liveSuccessIsReturnedAndCached() = runTest {
        val api = FakeApi(mutableListOf(ok("monstera", "pothos")))
        val outcome = repo(api).search("mon")

        assertNull(outcome.error)
        assertEquals(listOf("Monstera", "Pothos"), outcome.species.map { it.commonName })
        assertEquals(2, dao.rows.size)                        // cached for offline use
    }

    @Test
    fun repeatingASearchDoesNotSpendAnotherRequest() = runTest {
        val api = FakeApi(mutableListOf(ok("monstera")))
        val r = repo(api)
        r.search("mon")
        r.search("MON ")                                      // same query after normalising
        assertEquals(1, api.calls)
    }

    @Test
    fun timeoutIsRetriedOnceThenSucceeds() = runTest {
        val api = FakeApi(mutableListOf(throwing(SocketTimeoutException("slow")), ok("fern")))
        val outcome = repo(api).search("fern")

        assertEquals(2, api.calls)
        assertNull(outcome.error)
        assertEquals("Fern", outcome.species.single().commonName)
    }

    @Test
    fun persistentTimeoutFallsBackToCacheThenBuiltIn() = runTest {
        dao.upsert(CachedSpecies(id = 1, commonName = "Pothos Golden", wateringIntervalDays = 7))
        val api = FakeApi(mutableListOf(throwing(SocketTimeoutException()), throwing(SocketTimeoutException())))

        val outcome = repo(api).search("pothos")

        assertEquals(LookupError.Timeout, outcome.error)
        assertEquals(SpeciesSource.CACHED, outcome.species.first().source)       // saved lookup first
        assertTrue(outcome.species.any { it.source == SpeciesSource.BUILT_IN })  // built-in "Pothos" too
        assertEquals(2, api.calls)                                               // 1 try + 1 retry, no more
    }

    @Test
    fun offlineSkipsTheNetworkEntirely() = runTest {
        online = false
        val api = FakeApi(mutableListOf())
        val outcome = repo(api).search("monstera")

        assertEquals(LookupError.Offline, outcome.error)
        assertEquals(0, api.calls)
        assertTrue(outcome.species.isNotEmpty())                                 // built-in Monstera
    }

    @Test
    fun missingKeyUsesFallbackWithoutCallingApi() = runTest {
        val api = FakeApi(mutableListOf())
        val outcome = repo(api, hasKey = false).search("aloe")

        assertEquals(LookupError.MissingKey, outcome.error)
        assertEquals(0, api.calls)
        assertEquals("Aloe Vera", outcome.species.first().commonName)
    }

    @Test
    fun http429FallsBackAndStopsFurtherRequests() = runTest {
        val api = FakeApi(mutableListOf(httpError(429)))
        val r = repo(api)

        val first = r.search("cactus")
        assertTrue(first.error is LookupError.RateLimited)
        assertTrue(first.species.isNotEmpty())               // built-in cactus

        val second = r.search("jade")                        // different query, but we're cooling down
        assertTrue(second.error is LookupError.RateLimited)
        assertEquals(1, api.calls)                           // the second search never hit the network
    }

    @Test
    fun serverErrorIsRetriedAndReportedIfItPersists() = runTest {
        val api = FakeApi(mutableListOf(httpError(503), httpError(503)))
        val outcome = repo(api).search("ivy")

        assertEquals(LookupError.Server(503), outcome.error)
        assertEquals(2, api.calls)
    }

    @Test
    fun badKeyIsNotRetried() = runTest {
        val api = FakeApi(mutableListOf(httpError(401)))
        val outcome = repo(api).search("ivy")

        assertEquals(LookupError.InvalidKey, outcome.error)
        assertEquals(1, api.calls)
    }

    @Test
    fun localBudgetStopsSearchesBeforeTheServerHasTo() = runTest {
        // limit 100 with a 10-request reserve -> searches stop after 90; use a tiny limit to test it.
        val api = FakeApi(mutableListOf(ok("a1"), ok("a2")))
        val r = repo(api, limit = 11)                        // 11 - reserve 10 = 1 search allowed

        assertNull(r.search("aa").error)
        val blocked = r.search("bb")
        assertTrue(blocked.error is LookupError.RateLimited)
        assertEquals(1, api.calls)
    }

    @Test
    fun noResultsAnywhereReturnsEmptyListWithError() = runTest {
        online = false
        val outcome = repo(FakeApi(mutableListOf())).search("zzzzqq")
        assertTrue(outcome.species.isEmpty())
        assertEquals(LookupError.Offline, outcome.error)
    }

    @Test
    fun missingDetailsPageIsNotReportedAsAServerError() = runTest {
        val species = com.example.plantcarecompanion.domain.Species(id = 8759, commonName = "Tomato")
        val repo = SpeciesRepository(
            api = DetailsNotFoundApi(),
            speciesDao = dao,
            guard = RateLimitGuard(RepoFakeStore(), clock = { now }, dailyLimit = 100),
            connectivity = ConnectivityChecker { true },
            hasApiKey = true,
            clock = { now }
        )

        val outcome = repo.enrichCareInfo(species)

        assertEquals(LookupError.NotFound, outcome.error)      // friendly notice, not "error 404"
        assertEquals("Tomato", outcome.species.commonName)     // the plant the user picked is kept
    }
}
