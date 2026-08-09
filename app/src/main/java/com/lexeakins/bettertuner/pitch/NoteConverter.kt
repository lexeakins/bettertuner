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
    /**
     * Returns the nearest note to [frequencyHz] and the cents offset from that note, using the given
     * [a4Hz] reference (default 440). Overload keeps the original call sites unchanged.
     */
    fun fromFrequency(frequencyHz: Double, a4Hz: Double = A4_HZ): Pitch {
        require(frequencyHz > 0.0) { "Frequency must be positive, was $frequencyHz" }

        val midiFloat = A4_MIDI + 12 * log2(frequencyHz / a4Hz)
        val nearestMidi = midiFloat.roundToInt()
        val targetHz = a4Hz * 2.0.pow((nearestMidi - A4_MIDI) / 12.0)
        val cents = 1200 * log2(frequencyHz / targetHz)

        val octave = nearestMidi / 12 - 1
        val nameIndex = ((nearestMidi % 12) + 12) % 12
        return Pitch(NAMES[nameIndex], octave, cents, frequencyHz, nearestMidi)
    }

    /** The exact frequency of a given MIDI note number, using the given [a4Hz] reference (default 440). */
    fun midiToFrequency(midi: Int, a4Hz: Double = A4_HZ): Double = a4Hz * 2.0.pow((midi - A4_MIDI) / 12.0)

    /** MIDI number of a note name (e.g. "A#", "Bb") + octave, e.g. midiOf("A", 4) = 69. */
    fun midiOf(name: String, octave: Int): Int {
        val idx = NAMES.indexOfFirst { it.equals(name, ignoreCase = true) }
        require(idx >= 0) { "Unknown note name: $name" }
        return idx + (octave + 1) * 12
    }

    /**
     * Parses a note spec like "E2", "A#3", "Bb4" into a [Pitch]. Returns null if malformed.
     * Accidental may be # or b. Octave is required. Whitespace is tolerated.
     */
    fun parseNote(spec: String, a4Hz: Double = A4_HZ): Pitch? {
        val s = spec.trim()
        if (s.isEmpty()) return null
        val m = Regex("^([A-Ga-g])([#b]?)(\\d+)$").matchEntire(s) ?: return null
        val letter = m.groupValues[1].uppercase()
        val acc = m.groupValues[2]
        val octave = m.groupValues[3].toInt()
        val letterIdx = when (letter) {
            "C" -> 0; "D" -> 2; "E" -> 4; "F" -> 5; "G" -> 7; "A" -> 9; "B" -> 11
            else -> return null
        }
        val accSemis = when (acc) { "#" -> 1; "b" -> -1; else -> 0 }
        val midi = (octave + 1) * 12 + letterIdx + accSemis
        if (midi < 0 || midi > 127) return null
        return fromMidi(midi, a4Hz)
    }

    /**
     * Builds an exact [Pitch] for a MIDI note (no rounding), using the given [a4Hz] reference.
     * Use this for tuning *targets*, where the intended octave must be explicit.
     */
    fun fromMidi(midi: Int, a4Hz: Double = A4_HZ): Pitch {
        val targetHz = midiToFrequency(midi, a4Hz)
        val octave = midi / 12 - 1
        val nameIndex = ((midi % 12) + 12) % 12
        return Pitch(NAMES[nameIndex], octave, 0.0, targetHz, midi)
    }
}
