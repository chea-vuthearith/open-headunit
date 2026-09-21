package com.andrerinas.openheadunit.connection.wifi

import com.andrerinas.openheadunit.connection.wifi.HibernateWakeRearmPolicy.Decision
import com.andrerinas.openheadunit.connection.wifi.HibernateWakeRearmPolicy.FRESH_REARM_WINDOW_MS
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The shape this exists for: a head unit whose P2P framework restarted under a deep sleep, so the
 * Native launcher is up but its manager and Channel are shells, and only a rebuild forms a group
 * again.
 */
class HibernateWakeRearmPolicyTest {

    private fun decide(
        mode: WifiLauncherMode = WifiLauncherMode.NATIVE,
        activeMode: WifiLauncherMode? = WifiLauncherMode.NATIVE,
        activeIsStarted: Boolean = true,
        age: Long = FRESH_REARM_WINDOW_MS,
        busy: Boolean = false,
    ) = HibernateWakeRearmPolicy.decide(
        mode = mode,
        activeMode = activeMode,
        activeIsStarted = activeIsStarted,
        msSinceLastNativeRearm = age,
        busy = busy,
    )

    @Test
    fun `a sleep-old Native stack is rebuilt`() {
        assertEquals(Decision.REARM, decide(age = FRESH_REARM_WINDOW_MS))
        assertEquals(Decision.REARM, decide(age = 30 * 60_000L))
    }

    @Test
    fun `a wake landing on a bring-up that just ran is boot, and is left alone`() {
        assertEquals(Decision.SKIP, decide(age = 0L))
        assertEquals(Decision.SKIP, decide(age = FRESH_REARM_WINDOW_MS - 1))
    }

    @Test
    fun `a live session owns its own repair`() {
        assertEquals(Decision.SKIP, decide(age = 30 * 60_000L, busy = true))
    }

    @Test
    fun `only Native is rebuilt here`() {
        assertEquals(Decision.SKIP, decide(mode = WifiLauncherMode.HELPER))
        assertEquals(Decision.SKIP, decide(mode = WifiLauncherMode.MANUAL))
    }

    @Test
    fun `a stack that is not Native, or not up, is nothing this wake repairs`() {
        assertEquals(Decision.SKIP, decide(activeMode = WifiLauncherMode.HELPER))
        assertEquals(Decision.SKIP, decide(activeMode = null))
        assertEquals(Decision.SKIP, decide(activeIsStarted = false))
    }

    @Test
    fun `a clock that went backwards is not read as a stale stack`() {
        assertEquals(Decision.SKIP, decide(age = -5_000L))
    }
}
