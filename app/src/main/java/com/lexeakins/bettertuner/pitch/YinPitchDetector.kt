package com.lexeakins.bettertuner.pitch

import kotlin.math.abs

/**
 * YIN monophonic pitch detector (Cheveigné & Kawahara, 2002).
 *
 * Implemented from the published algorithm — no third-party library, so the code, license, and tests
 * are entirely our own (MIT/Apache-clean). Self-contained: takes a FloatArray of mono samples and
 * returns the fundamental frequency plus a confidence in [0,1].
 *
 * The public boundary (seam) we test is [detect]: given samples + sample rate, return a [Detection].
 */
object YinPitchDetector {

    /**
     * @param buffer mono PCM samples in [-1, 1]
     * @param sampleRateHz sample rate of [buffer] (e.g. 44100)
     * @param threshold YIN absolute threshold for periodicity (0.1–0.15 typical)
     * @param minFrequencyHz lowest frequency worth detecting; sets the search window (guitar low E ~82 Hz)
     * @param maxFrequencyHz highest frequency worth detecting; detections above this are rejected as noise
     * @return [Detection] with frequency (Hz) and confidence, or null if no periodic signal found in range.
     */
    fun detect(
        buffer: FloatArray,
        sampleRateHz: Int,
        threshold: Double = 0.12,
        minFrequencyHz: Double = 65.0,
        maxFrequencyHz: Double = 1000.0,
    ): Detection? {
        require(minFrequencyHz > 0 && maxFrequencyHz > minFrequencyHz) {
            "Require 0 < minFrequencyHz < maxFrequencyHz"
        }
        val n = buffer.size
        val tauMax = (sampleRateHz / minFrequencyHz).toInt().coerceAtLeast(2)
        if (n < 2 * tauMax + 1) return null

        // Step 1: difference function d(tau)
        val d = DoubleArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var sum = 0.0
            for (i in 0 until n - tau) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            d[tau] = sum
        }

        // Step 2: cumulative mean normalized difference d'(tau)
        val cmnd = DoubleArray(tauMax + 1)
        cmnd[0] = 1.0
        var runningSum = 0.0
        for (tau in 1..tauMax) {
            runningSum += d[tau]
            cmnd[tau] = if (runningSum > 0) d[tau] / (runningSum / tau) else 1.0
        }

        // Step 3 + 4: absolute threshold — first local minimum of cmnd below [threshold].
        var tauEstimate = -1
        for (tau in 2..tauMax) {
            if (cmnd[tau] < threshold) {
                var t = tau
                while (t + 1 <= tauMax && cmnd[t + 1] < cmnd[t]) {
                    t += 1 // descend to the local minimum
                }
                tauEstimate = t
                break
            }
        }
        if (tauEstimate == -1) return null // no periodic signal confident enough

        // Parabolic interpolation around tauEstimate for sub-sample accuracy
        val betterTau = parabolicInterpolation(cmnd, tauEstimate)
        val frequency = sampleRateHz / betterTau
        if (frequency < minFrequencyHz || frequency > maxFrequencyHz) return null

        val confidence = (1.0 - cmnd[tauEstimate]).coerceIn(0.0, 1.0)
        return Detection(frequency, confidence)
    }

    private fun parabolicInterpolation(cmnd: DoubleArray, tau: Int): Double {
        val x0 = if (tau > 0) cmnd[tau - 1] else cmnd[tau]
        val x1 = cmnd[tau]
        val x2 = if (tau + 1 < cmnd.size) cmnd[tau + 1] else cmnd[tau]
        val denom = (x0 + x2 - 2 * x1)
        if (denom == 0.0) return tau.toDouble()
        return tau + 0.5 * (x0 - x2) / denom
    }
}

/**
 * Result of a pitch detection.
 *
 * @param frequencyHz estimated fundamental frequency
 * @param confidence YIN periodicity confidence in [0,1]; higher = more periodic/stable
 */
data class Detection(val frequencyHz: Double, val confidence: Double) {
    val isReliable: Boolean get() = confidence >= 0.9
}
