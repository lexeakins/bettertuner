package com.lexeakins.bettertuner.pitch

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Converts between a fundamental frequency (Hz) and musical pitch (note name + octave + cents offset).
 *
 * Uses the equal-tempered standard: A4 = 440 Hz, MIDI note 69.
 * The note name is the 12-tone chromatic set repeated across octaves, so one algorithm covers every
 * instrument (guitar, violin, voice) — no per-instrument detection path required.
 */
object NoteConverter {

    /** Chromatic note names, index 0 = C. */
    private val NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** A4 reference frequency in Hz. */
    const val A4_HZ = 440.0

    /** MIDI number of A4. */
    const val A4_MIDI = 69

    /**
     * Returns the nearest note to [frequencyHz] and the cents offset from that note.
     *
     * @param frequencyHz fundamental frequency in Hz (must be > 0)
     * @return [Pitch] describing the nearest note and how sharp (positive) or flat (negative) it is in cents.
     * @throws IllegalArgumentException if [frequencyHz] is not positive.
     */
    fun fromFrequency(frequencyHz: Double): Pitch {
        require(frequencyHz > 0.0) { "Frequency must be positive, was $frequencyHz" }

        val midiFloat = A4_MIDI + 12 * log2(frequencyHz / A4_HZ)
        val nearestMidi = midiFloat.roundToInt()
        val targetHz = A4_HZ * 2.0.pow((nearestMidi - A4_MIDI) / 12.0)
        val cents = 1200 * log2(frequencyHz / targetHz)

        val octave = nearestMidi / 12 - 1
        val nameIndex = ((nearestMidi % 12) + 12) % 12
        return Pitch(NAMES[nameIndex], octave, cents, frequencyHz, nearestMidi)
    }

    /** The exact frequency of a given MIDI note number. */
    fun midiToFrequency(midi: Int): Double = A4_HZ * 2.0.pow((midi - A4_MIDI) / 12.0)

    /**
     * Builds an exact [Pitch] for a MIDI note (no rounding). Use this for tuning *targets*, where the
     * intended octave must be explicit — [fromFrequency] would snap to the nearest chromatic note and
     * could mis-octave a target like A3 (220 Hz) down to A2.
     */
    fun fromMidi(midi: Int): Pitch {
        val targetHz = midiToFrequency(midi)
        val octave = midi / 12 - 1
        val nameIndex = ((midi % 12) + 12) % 12
        return Pitch(NAMES[nameIndex], octave, 0.0, targetHz, midi)
    }
}
