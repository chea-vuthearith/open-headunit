package com.andrerinas.openheadunit.connection.wifi

/**
 * Whether a detected hibernate wake should rebuild the Native AA wireless stack.
 *
 * On a unit whose P2P framework restarts during a deep sleep, the `WifiDirectManager`'s manager and
 * Channel survive as a non-null shell the platform no longer answers: every later createGroup comes
 * back BUSY, the wedge ladder spends its whole budget, and the unit refuses to host a group until the
 * process is restarted. Swinging the launcher back through a forced re-arm rebuilds both exactly like
 * a fresh boot does, which is the one thing measured to clear it.
 *
 * Boot must not be doubled. `BootCompleteReceiver` and the ACC/OEM intents both land in the same
 * debounced wake path, so a re-arm that fresh belongs to a bring-up that just ran and is refused.
 * The same window keeps a unit that merely woke its screen on an existing drive from tearing down a
 * stack that is still healthy.
 */
object HibernateWakeRearmPolicy {

    /**
     * A car sleep is minutes long, so anything re-armed inside this window is not the bring-up this
     * wake is answering. Above [NativeBringUpReentryPolicy.DUPLICATE_WINDOW_MS] by design: that
     * policy is the last-milliseconds guard, this is the boot-vs-sleep one.
     */
    const val FRESH_REARM_WINDOW_MS = 60_000L

    enum class Decision {
        /** Stop the armed Native launcher and arm it again, rebuilding the manager and Channel. */
        REARM,

        /** Leave the stack exactly as it is. */
        SKIP,
    }

    fun decide(
        mode: WifiLauncherMode,
        activeMode: WifiLauncherMode?,
        activeIsStarted: Boolean,
        msSinceLastNativeRearm: Long,
        busy: Boolean,
    ): Decision = when {
        // A live session made its own repair; its wake is not this one.
        busy -> Decision.SKIP

        // This repair is for Native AA, whose quiet-host group is the one that stays wrong.
        mode != WifiLauncherMode.NATIVE -> Decision.SKIP

        // Nothing Native is armed (the pill's X, a setting, a USB session), so there is no stale
        // stack to replace and no reason to jog the radio.
        activeMode != WifiLauncherMode.NATIVE -> Decision.SKIP
        !activeIsStarted -> Decision.SKIP

        // The bring-up just ran - this wake is boot, not a sleep. A negative age is a clock that
        // went backwards and is left alone rather than read as ancient.
        msSinceLastNativeRearm < FRESH_REARM_WINDOW_MS -> Decision.SKIP

        else -> Decision.REARM
    }
}
