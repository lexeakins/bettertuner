package com.lexeakins.bettertuner.pitch

import kotlin.math.absoluteValue

/**
 * A detected or target pitch.
 *
 * @param name chromatic note name (e.g. "E", "A#")
 * @param octave scientific pitch notation octave (e.g. E2, A4)
 * @param cents offset from the exact note in cents. 0 = in tune, + = sharp, - = flat.
 * @param frequencyHz the frequency this pitch was derived from
 * @param midi the nearest MIDI note number
 */
data class Pitch(
    val name: String,
    val octave: Int,
    val cents: Double,
    val frequencyHz: Double,
    val midi: Int,
) {
    /** Human-readable label, e.g. "E2". */
    val label: String get() = "$name$octave"

    /** True when within [thresholdCents] of the exact note. */
    fun isInTune(thresholdCents: Double = 5.0): Boolean = cents.absoluteValue <= thresholdCents

    /**
     * Signed cents from this pitch to [other]: positive = this is sharper (higher) than [other],
     * negative = flatter. Computed from the actual frequencies, not the within-note cents fields.
     */
    fun centsTo(other: Pitch): Double = 1200.0 * kotlin.math.log2(frequencyHz / other.frequencyHz)
}
