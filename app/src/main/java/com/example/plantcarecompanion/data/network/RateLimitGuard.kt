package com.example.plantcarecompanion.data.network

import android.content.Context
interface RateLimitStore {
    var windowDay: Long
    var usedToday: Int
    var blockedUntil: Long
}

class SharedPrefsRateLimitStore(context: Context) : RateLimitStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("perenual_rate_limit", Context.MODE_PRIVATE)

    override var windowDay: Long
        get() = prefs.getLong("windowDay", -1L)
        set(v) = prefs.edit().putLong("windowDay", v).apply()

    override var usedToday: Int
        get() = prefs.getInt("usedToday", 0)
        set(v) = prefs.edit().putInt("usedToday", v).apply()

    override var blockedUntil: Long
        get() = prefs.getLong("blockedUntil", 0L)
        set(v) = prefs.edit().putLong("blockedUntil", v).apply()
}

class RateLimitGuard(
    private val store: RateLimitStore,
    private val clock: () -> Long = System::currentTimeMillis,
    val dailyLimit: Int = 100
) {

    sealed interface Gate {
        data object Open : Gate
        data class Closed(val reopensAtMillis: Long) : Gate
    }

    @Synchronized
    fun check(keepInReserve: Int = 0): Gate {
        rollWindowIfNeeded()
        val now = clock()
        if (store.blockedUntil > now) return Gate.Closed(store.blockedUntil)
        if (store.usedToday >= dailyLimit - keepInReserve) return Gate.Closed(nextResetMillis(now))
        return Gate.Open
    }

    @Synchronized
    fun recordRequest() {
        rollWindowIfNeeded()
        store.usedToday = store.usedToday + 1
    }

    @Synchronized
    fun recordRateLimited(retryAfterSeconds: Long?) {
        rollWindowIfNeeded()
        val now = clock()
        if (retryAfterSeconds != null && retryAfterSeconds in 1..MAX_TRUSTED_RETRY_AFTER_SECONDS) {
            store.blockedUntil = now + retryAfterSeconds * 1000
        } else {
            store.blockedUntil = nextResetMillis(now)
            store.usedToday = dailyLimit
        }
    }

    @Synchronized
    fun requestsRemaining(): Int {
        rollWindowIfNeeded()
        return (dailyLimit - store.usedToday).coerceAtLeast(0)
    }

    private fun rollWindowIfNeeded() {
        val today = clock() / DAY_MILLIS
        if (store.windowDay != today) {
            store.windowDay = today
            store.usedToday = 0
        }
    }

    private fun nextResetMillis(now: Long): Long = (now / DAY_MILLIS + 1) * DAY_MILLIS

    companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        private const val MAX_TRUSTED_RETRY_AFTER_SECONDS = 3600L
    }
}
