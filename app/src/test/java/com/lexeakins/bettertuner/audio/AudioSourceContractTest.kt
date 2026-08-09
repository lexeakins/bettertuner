package com.lexeakins.bettertuner.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the [AudioSource] seam, exercised via [FakeAudioSource] (no hardware).
 * Verifies the buffer shape and normalization the rest of the pipeline relies on.
 */
class AudioSourceContractTest {

    @Test
    fun fake_emits_buffer_of_expected_size_and_rate() {
        val sig = FloatArray(4096)
        val source = FakeAudioSource(44100, sig)

        var received: FloatArray? = null
        source.start { received = it }

        assertEquals(44100, source.sampleRateHz)
        assertTrue("start() was not called", source.started)
        assertEquals(4096, received!!.size)
    }

    @Test
    fun fake_emits_values_normalized_to_unit_range() {
        // Full-scale sine (amplitude 1.0) must map to ~[-1, 1] with peaks near 1.0.
        val sig = FloatArray(4096) { i -> kotlin.math.sin(2 * kotlin.math.PI * 440.0 * i / 44100.0).toFloat() }
        val source = FakeAudioSource(44100, sig)

        var received: FloatArray? = null
        source.start { received = it }

        val maxAbs = received!!.maxOf { kotlin.math.abs(it) }
        assertTrue("samples must stay within [-1,1], got $maxAbs", maxAbs <= 1.0)
        assertTrue("samples should reach near full scale, got $maxAbs", maxAbs > 0.9)
    }

    @Test
    fun fake_does_not_loop() {
        // The fake emits exactly once, so a second onBuffer call must not happen.
        var calls = 0
        val source = FakeAudioSource(44100, FloatArray(1024))
        source.start { calls++ }
        assertEquals(1, calls)
        assertFalse("fake should not auto-stop on start()", source.stopped)
    }
}
