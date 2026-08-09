package com.lexeakins.bettertuner.tuner

import kotlin.math.absoluteValue

/**
 * Tracks the in-tune *transition* so a reward (bell) fires once per lock, not every frame — and not on
 * boundary jitter. Uses hysteresis: once locked, the string must go clearly out of tune (past the in-tune
 * band + a deadband) before the next lock can trigger, so a reading that flickers around the threshold
 * does not ding repeatedly.
 *
 * Pure state machine — fully unit-testable without Android.
 *
 * @param rearmMillis minimum time between allowed rewards (guards against rapid re-locks).
 * @param deadbandCents extra cents past the in-tune tolerance required to "unlock" (hysteresis width).
 */
class TuneLockDetector(
    private val rearmMillis: Long = 1500,
    private val deadbandCents: Double = 4.0,
) {
    private var locked = false
    private var lastRewardAt = -rearmMillis

    /**
     * @param inTune whether the current reading is within the in-tune tolerance.
     * @param cents signed cents from target (used for hysteresis exit).
     * @param now current monotonic time in ms (inject a clock in tests).
     * @return true exactly once on the rising edge into a stable lock.
     */
    fun onState(inTune: Boolean, cents: Double, now: Long = System.currentTimeMillis()): Boolean {
        if (locked) {
            // Stay locked until clearly out of tune (hysteresis): must exceed tolerance + deadband.
            if (cents.absoluteValue > DEFAULT_TOLERANCE + deadbandCents) locked = false
            return false
        }
        // Not locked: fire on the rising edge into tune, respecting the rearm window.
        val fire = inTune && (now - lastRewardAt >= rearmMillis)
        if (fire) {
            locked = true
            lastRewardAt = now
        }
        return fire
    }

    companion object {
        // Mirrors TunerEngine's default in-tune tolerance (±5¢). Kept here so hysteresis exit is independent
        // of the engine's instantaneous `inTune` flag.
        const val DEFAULT_TOLERANCE = 5.0
    }
}
