package com.lexeakins.bettertuner.tuner

import com.lexeakins.bettertuner.pitch.NoteConverter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneLockDetectorTest {

    private fun state(inTune: Boolean) = TunerState(inTune = inTune)

    @Test
    fun firesOnRisingEdgeIntoTune() {
        val d = TuneLockDetector(rearmMillis = 0)
        assertFalse(d.onState(state(false), now = 0))   // not in tune -> no
        assertTrue(d.onState(state(true), now = 1))      // edge into tune -> reward
    }

    @Test
    fun doesNotFireWhileRemainingInTune() {
        val d = TuneLockDetector(rearmMillis = 0)
        d.onState(state(true), now = 0)
        assertFalse(d.onState(state(true), now = 1))     // still in tune, no new edge
        assertFalse(d.onState(state(true), now = 2))
    }

    @Test
    fun rearmsAfterWindow() {
        val d = TuneLockDetector(rearmMillis = 100)
        assertTrue(d.onState(state(true), now = 1000))   // first lock
        assertFalse(d.onState(state(false), now = 1001)) // out
        assertFalse(d.onState(state(true), now = 1050))  // back in, but <100ms since last reward
        assertFalse(d.onState(state(false), now = 1090)) // out again
        assertTrue(d.onState(state(true), now = 1150))   // re-armed (>=100ms since last reward)
    }

    @Test
    fun requiresOutOfTuneBetweenLocks() {
        val d = TuneLockDetector(rearmMillis = 0)
        assertTrue(d.onState(state(true), now = 0))
        assertFalse(d.onState(state(true), now = 999))   // never left tune -> no second reward
    }
}

class TunerEngineMiscTest {
    @Test
    fun noteConverter_fromMidi_isExact() {
        // Guards the tuning-target primitive used by Slice 2/3 against regression.
        val e2 = NoteConverter.fromMidi(40)
        assertTrue(e2.label == "E2")
        assertTrue(kotlin.math.abs(e2.frequencyHz - 82.41) < 0.1)
    }
}
