package com.lexeakins.bettertuner.tuner

import com.lexeakins.bettertuner.pitch.NoteConverter
import com.lexeakins.bettertuner.pitch.Pitch

/**
 * Parses user-entered custom tunings and computes per-string safety warnings.
 *
 * Safety model (from luthier/community consensus — Stringjoy break tests, Music.SE):
 * tuning a string UP raises tension and risks breakage / neck warp. There is no safe *lower* bound worth
 * warning about (detuning is routine). We therefore warn only when a string is tuned ABOVE its Standard
 * reference pitch:
 * - > 2 semitones above Standard -> SOFT (amber) advisory.
 * - >= 3 semitones above Standard -> HARD (red): high risk of breakage or neck warping.
 */
object TuningParser {

    /** Standard EADGBE reference pitches, used as the per-string safety baseline. */
    val STANDARD_REFERENCE: List<Pitch> = listOf(
        NoteConverter.fromMidi(40), // E2
        NoteConverter.fromMidi(45), // A2
        NoteConverter.fromMidi(50), // D3
        NoteConverter.fromMidi(55), // G3
        NoteConverter.fromMidi(59), // B3
        NoteConverter.fromMidi(64), // E4
    )

    enum class WarningLevel { NONE, SOFT, HARD }

    data class ParseResult(
        val pitches: List<Pitch>?,
        val error: String?,
        /** Per-string warning, parallel to a successful [pitches] list. Empty list when parse failed. */
        val warnings: List<WarningLevel> = emptyList(),
    ) {
        val ok get() = pitches != null
    }

    /**
     * Parses [fields] (low string -> high string). Expects exactly 6 non-empty note specs (e.g. "E2",
     * "A#3", "Bb4"). Blank or malformed entries, or a wrong count, fail with [ParseResult.error].
     * On success, [warnings] holds the safety level for each string vs its Standard reference.
     */
    fun parse(fields: List<String>, a4Hz: Double = 440.0): ParseResult {
        if (fields.size != 6) {
            return ParseResult(null, "Enter exactly 6 notes (low E to high e).", emptyList())
        }
        // Blank fields mean "not entered yet" — incomplete, not an error. Caller disables Apply but shows no red text.
        if (fields.any { it.isBlank() }) {
            return ParseResult(null, null, emptyList())
        }
        val pitches = mutableListOf<Pitch>()
        for (raw in fields) {
            val p = NoteConverter.parseNote(raw, a4Hz)
            if (p == null) {
                return ParseResult(null, "Invalid note: \"${raw.trim()}\". Use e.g. E2, A#3, Bb4.", emptyList())
            }
            pitches.add(p)
        }
        val warnings = pitches.mapIndexed { i, p ->
            warningFor(STANDARD_REFERENCE[i], p)
        }
        return ParseResult(pitches, null, warnings)
    }

    /** Semitone distance of [custom] above [standard]; negative means tuned down (safe). */
    fun semisAbove(standard: Pitch, custom: Pitch): Int =
        (custom.midi - standard.midi)

    fun warningFor(standard: Pitch, custom: Pitch): WarningLevel {
        val above = semisAbove(standard, custom)
        return when {
            above >= 3 -> WarningLevel.HARD
            above > 2 -> WarningLevel.SOFT // strictly >2 and <3 can't happen for integer semitones; kept for clarity
            above == 2 -> WarningLevel.SOFT
            else -> WarningLevel.NONE
        }
    }
}
