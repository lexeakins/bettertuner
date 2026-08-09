package com.lexeakins.bettertuner.pitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [YinPitchDetector]. This is the core algorithm seam.
 * We feed synthetic sines at known frequencies and assert the detector recovers them within a small
 * tolerance. Frequencies chosen to span the guitar's range (low E2 ~82 Hz to high E4 ~330 Hz).
 */
class YinPitchDetectorTest {

    private val sampleRate = 44100

    /** Enough samples to resolve the lowest frequency (>~3 periods of 82 Hz ~ 0.036s -> ~1600 samples; use 4096). */
    private val sampleCount = 4096

    private fun assertFrequency(expectedHz: Double, actual: Detection?, toleranceHz: Double = 0.5) {
        assertNotNull("detector returned null for ${expectedHz}Hz", actual)
        assertEquals("frequency mismatch for ${expectedHz}Hz", expectedHz, actual!!.frequencyHz, toleranceHz)
    }

    @Test
    fun detects_a4_440() {
        val sig = SignalGenerator.sine(440.0, sampleRate, sampleCount)
        assertFrequency(440.0, YinPitchDetector.detect(sig, sampleRate))
    }

    @Test
    fun detects_low_e2_82_41() {
        val sig = SignalGenerator.sine(82.41, sampleRate, sampleCount)
        assertFrequency(82.41, YinPitchDetector.detect(sig, sampleRate), toleranceHz = 0.5)
    }

    @Test
    fun detects_high_e4_329_63() {
        val sig = SignalGenerator.sine(329.63, sampleRate, sampleCount)
        assertFrequency(329.63, YinPitchDetector.detect(sig, sampleRate), toleranceHz = 0.5)
    }

    @Test
    fun detects_a2_110_for_drop_d_low_string() {
        val sig = SignalGenerator.sine(110.0, sampleRate, sampleCount)
        assertFrequency(110.0, YinPitchDetector.detect(sig, sampleRate))
    }

    @Test
    fun confidence_is_high_for_clean_sine() {
        val sig = SignalGenerator.sine(220.0, sampleRate, sampleCount)
        val det = YinPitchDetector.detect(sig, sampleRate)
        assertNotNull(det)
        assertTrue("expected high confidence for clean sine, got ${det!!.confidence}", det.confidence >= 0.9)
    }

    @Test
    fun near_silence_returns_low_or_null() {
        // Very low amplitude noise should not produce a confident periodic detection.
        val sig = FloatArray(sampleCount) { (it % 3 - 1) * 0.0001f } // tiny deterministic non-periodic
        val det = YinPitchDetector.detect(sig, sampleRate)
        if (det != null) {
            assertTrue("silence should not be reliable", !det.isReliable)
        }
    }

    @Test
    fun buffer_too_short_returns_null() {
        val tiny = FloatArray(10) { 0.5f }
        assertEquals(null, YinPitchDetector.detect(tiny, sampleRate))
    }
}
