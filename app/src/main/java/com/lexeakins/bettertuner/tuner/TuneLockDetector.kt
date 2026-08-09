package com.lexeakins.bettertuner.tuner

/**
 * Tracks the in-tune *transition* so a reward (bell) fires once per lock, not every frame.
 *
 * Pure state machine — fully unit-testable without Android. Feed it the current [TunerState] on every
 * update; it tells you whether a reward should sound *now* (the rising edge into IN_TUNE).
 *
 * @param rearmMillis minimum time after a reward before another can fire (debounce / spam guard).
 */
class TuneLockDetector(private val rearmMillis: Long = 1500) {
    private var wasInTune = false
    private var lastRewardAt = -rearmMillis

    /**
     * @param now current monotonic time in ms (inject a clock in tests).
     * @return true exactly on the rising edge into IN_TUNE, after the rearm window has elapsed.
     */
    fun onState(state: TunerState, now: Long = System.currentTimeMillis()): Boolean {
        val inTune = state.inTune
        val fire = !wasInTune && inTune && (now - lastRewardAt) >= rearmMillis
        if (fire) lastRewardAt = now
        wasInTune = inTune
        return fire
    }
}
