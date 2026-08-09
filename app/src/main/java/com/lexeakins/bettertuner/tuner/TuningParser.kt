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
    /**
     * Parses [fields] (low string -> high string). Evaluates each field INDEPENDENTLY:
     * - A blank field is "not entered yet" -> contributes WarningLevel.NONE, keeps [ok] false (Apply disabled)
     *   but does NOT suppress warnings on the fields the user HAS filled.
     * - A non-blank but malformed note (e.g. "X3") sets [error] and that field's warning to NONE.
     * - A valid note contributes its own safety [WarningLevel] vs that string's Standard reference.
     * So typing "E4" (or E4 in the low slot) flags a warning immediately, even with the other 5 still empty.
     */
    fun parse(fields: List<String>, a4Hz: Double = 440.0): ParseResult {
        if (fields.size != 6) {
            return ParseResult(null, "Enter exactly 6 notes (low E to high e).", List(6) { WarningLevel.NONE })
        }
        val pitches = mutableListOf<Pitch?>()
        var malformed: String? = null
        for (raw in fields) {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) {
                pitches.add(null) // not entered yet
                continue
            }
            val p = NoteConverter.parseNote(trimmed, a4Hz)
            if (p == null) {
                malformed = trimmed
                pitches.add(null)
            } else {
                pitches.add(p)
            }
        }
        val warnings = pitches.mapIndexed { i, p ->
            if (p == null) WarningLevel.NONE else warningFor(STANDARD_REFERENCE[i], p)
        }
        // ok only when every field is a valid note; error only when a non-blank entry is malformed.
        val allValid = pitches.all { it != null }
        return ParseResult(
            pitches = if (allValid) pitches.filterNotNull() else null,
            error = malformed?.let { "Invalid note: \"$it\". Use e.g. E2, A#3, Bb4." },
            warnings = warnings,
        )
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
