package com.lexeakins.bettertuner.pitch

import kotlin.math.PI
import kotlin.math.sin

/**
 * Deterministic synthetic signal generators for tests. No audio hardware required.
 * A pure sine at [frequencyHz] with a known period is the ground truth our detector must recover.
 */
object SignalGenerator {

    /**
     * Generates [sampleCount] samples of a sine wave at [frequencyHz] sampled at [sampleRateHz].
     * Amplitude is 1.0 (full scale) to maximize signal-to-noise for the detector under test.
     */
    fun sine(frequencyHz: Double, sampleRateHz: Int, sampleCount: Int, amplitude: Double = 1.0): FloatArray {
        val out = FloatArray(sampleCount)
        val omega = 2 * PI * frequencyHz / sampleRateHz
        for (i in 0 until sampleCount) {
            out[i] = (amplitude * sin(omega * i)).toFloat()
        }
        return out
    }
}
