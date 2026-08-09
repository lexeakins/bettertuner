package com.lexeakins.bettertuner.tuner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneLockDetectorTest {

    @Test
    fun firesOnRisingEdgeIntoTune() {
        val d = TuneLockDetector(rearmMillis = 0)
        assertFalse(d.onState(false, 0.0, now = 0))   // not in tune -> no
        assertTrue(d.onState(true, 0.0, now = 1))       // edge into tune -> reward
    }

    @Test
    fun doesNotFireWhileRemainingInTune() {
        val d = TuneLockDetector(rearmMillis = 0)
        d.onState(true, 0.0, now = 0)
        assertFalse(d.onState(true, 0.0, now = 1))     // still in tune, no new edge
        assertFalse(d.onState(true, 0.0, now = 2))
    }

    @Test
    fun rearmsAfterWindow() {
        val d = TuneLockDetector(rearmMillis = 100)
        assertTrue(d.onState(true, 0.0, now = 1000))    // first lock
        assertFalse(d.onState(false, 10.0, now = 1001)) // out
        assertFalse(d.onState(true, 0.0, now = 1050))   // back in, but <100ms since last reward
        assertFalse(d.onState(false, 10.0, now = 1090)) // out again
        assertTrue(d.onState(true, 0.0, now = 1150))    // re-armed
    }

    @Test
    fun requiresOutOfTuneBetweenLocks() {
        val d = TuneLockDetector(rearmMillis = 0)
        assertTrue(d.onState(true, 0.0, now = 0))
        assertFalse(d.onState(true, 0.0, now = 999))    // never left tune -> no second reward
    }

    @Test
    fun hysteresisPreventsThresholdChatter() {
        // With deadband 4c past the ±5c tolerance, flickering within ±4c must NOT re-ding.
        val d = TuneLockDetector(rearmMillis = 0, deadbandCents = 4.0)
        assertTrue(d.onState(true, 0.0, now = 0))       // lock at 0c
        // Flicker around the threshold (+4c, -4c, +3c) — all within tolerance+deadband(=9c) so still locked.
        assertFalse(d.onState(true, 4.0, now = 1))
        assertFalse(d.onState(true, -4.0, now = 2))
        assertFalse(d.onState(true, 3.0, now = 3))
        // A clear excursion past 9c unlocks; returning to tune then re-dings.
        assertFalse(d.onState(false, 12.0, now = 4))     // clearly out (past deadband)
        assertTrue(d.onState(true, 0.0, now = 5))        // re-lock -> reward
    }

    @Test
    fun hysteresisStaysLockedWithinDeadband() {
        val d = TuneLockDetector(rearmMillis = 0, deadbandCents = 4.0)
        d.onState(true, 0.0, now = 0)
        // 8c is inside tolerance(5)+deadband(4)=9, so the detector considers it still locked.
        assertFalse(d.onState(true, 8.0, now = 1))
        assertFalse(d.onState(true, -8.0, now = 2))
    }
}
