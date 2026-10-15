package com.example.plantcarecompanion

import com.example.plantcarecompanion.data.network.RateLimitGuard
import com.example.plantcarecompanion.data.network.RateLimitStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class GuardFakeStore : RateLimitStore {
    override var windowDay: Long = -1
    override var usedToday: Int = 0
    override var blockedUntil: Long = 0
}

class RateLimitGuardTest {

    private var now = 10 * RateLimitGuard.DAY_MILLIS + 1_000   // some moment on "day 10"
    private val store = GuardFakeStore()
    private val guard = RateLimitGuard(store, clock = { now }, dailyLimit = 10)

    @Test
    fun opensUntilBudgetIsSpent() {
        repeat(10) {
            assertEquals(RateLimitGuard.Gate.Open, guard.check())
            guard.recordRequest()
        }
        assertTrue(guard.check() is RateLimitGuard.Gate.Closed)
        assertEquals(0, guard.requestsRemaining())
    }

    @Test
    fun reserveKeepsRequestsBackForOtherCalls() {
        repeat(8) { guard.recordRequest() }
        assertTrue(guard.check(keepInReserve = 3) is RateLimitGuard.Gate.Closed)   // searches blocked at 7
        assertEquals(RateLimitGuard.Gate.Open, guard.check(keepInReserve = 0))     // details still allowed
    }

    @Test
    fun budgetResetsOnNewDay() {
        repeat(10) { guard.recordRequest() }
        now += RateLimitGuard.DAY_MILLIS
        assertEquals(RateLimitGuard.Gate.Open, guard.check())
        assertEquals(10, guard.requestsRemaining())
    }

    @Test
    fun retryAfterHeaderBlocksOnlyBriefly() {
        guard.recordRateLimited(retryAfterSeconds = 30)
        val gate = guard.check() as RateLimitGuard.Gate.Closed
        assertEquals(now + 30_000, gate.reopensAtMillis)
        now += 31_000
        assertEquals(RateLimitGuard.Gate.Open, guard.check())
    }

    @Test
    fun rateLimitWithoutHintBlocksUntilTomorrow() {
        guard.recordRateLimited(retryAfterSeconds = null)
        val gate = guard.check() as RateLimitGuard.Gate.Closed
        assertEquals(11 * RateLimitGuard.DAY_MILLIS, gate.reopensAtMillis)
    }
}
