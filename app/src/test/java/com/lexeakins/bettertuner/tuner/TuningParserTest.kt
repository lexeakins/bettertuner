package com.lexeakins.bettertuner.tuner

import com.lexeakins.bettertuner.pitch.NoteConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningParserTest {

    @Test
    fun parse_validStandard_succeeds() {
        val r = TuningParser.parse(listOf("E2", "A2", "D3", "G3", "B3", "E4"))
        assertTrue(r.ok)
        assertEquals(6, r.pitches!!.size)
        assertEquals("E2", r.pitches!![0].label)
        assertEquals("E4", r.pitches!![5].label)
    }

    @Test
    fun parse_accidentals_and_case() {
        val r = TuningParser.parse(listOf("Eb2", "a#2", "D3", "G3", "B3", "E4"))
        assertTrue(r.ok)
        assertEquals("D#2", r.pitches!![0].label) // Eb normalizes to D#
        assertEquals("A#2", r.pitches!![1].label)
    }

    @Test
    fun parse_wrongCount_fails() {
        val r = TuningParser.parse(listOf("E2", "A2", "D3"))
        assertFalse(r.ok)
        assertEquals("Enter exactly 6 notes (low E to high e).", r.error)
    }

    @Test
    fun parse_malformedNote_fails() {
        val r = TuningParser.parse(listOf("E2", "A2", "X3", "G3", "B3", "E4"))
        assertFalse(r.ok)
        assertTrue(r.error!!.contains("Invalid note"))
    }

    @Test
    fun parse_warns_perField_even_with_blanks() {
        // Only the low-E field is filled (F#2 = +2 above E2). Others blank. Warning must still show on field 0,
        // and Apply must stay disabled (ok=false) without a red error.
        val r = TuningParser.parse(listOf("F#2", "", "", "", "", ""))
        assertFalse(r.ok) // not all 6 entered -> Apply disabled
        assertNull(r.error) // no red error for blanks
        assertEquals(TuningParser.WarningLevel.SOFT, r.warnings[0]) // F#2 in low slot -> +2 -> SOFT
        assertEquals(TuningParser.WarningLevel.NONE, r.warnings[1]) // blank -> NONE
    }

    @Test
    fun parse_blankNote_isIncomplete_notError() {
        // Blank fields mean "not entered yet": parse fails (ok=false) but must NOT set a red error message.
        val r = TuningParser.parse(listOf("E2", "A2", "", "G3", "B3", "E4"))
        assertFalse(r.ok)
        assertNull(r.error) // no scary red text while the user is still filling in
    }

    @Test
    fun warning_tuningUpTwoSemitones_isSoft() {
        // Standard E2 = MIDI 40. E2 -> F#2 (+2) should be SOFT.
        val std = NoteConverter.fromMidi(40)
        val up2 = NoteConverter.fromMidi(42)
        assertEquals(TuningParser.WarningLevel.SOFT, TuningParser.warningFor(std, up2))
    }

    @Test
    fun warning_tuningUpThreeSemitones_isHard() {
        // E2 -> G2 (+3) should be HARD.
        val std = NoteConverter.fromMidi(40)
        val up3 = NoteConverter.fromMidi(43)
        assertEquals(TuningParser.WarningLevel.HARD, TuningParser.warningFor(std, up3))
    }

    @Test
    fun warning_tuningDown_isNone() {
        val std = NoteConverter.fromMidi(40) // E2
        val down = NoteConverter.fromMidi(38) // D2 (-2)
        assertEquals(TuningParser.WarningLevel.NONE, TuningParser.warningFor(std, down))
    }

    @Test
    fun byName_maps_all_presets() {
        // Every preset name must resolve to a non-Standard tuning (no fallthrough to standard()).
        val names = listOf("Standard","Half-step down","Drop D","Double Drop D","DADGAD","Open D","Open G","Open E","Open C","New Standard")
        for (n in names) {
            val t = Tuning.byName(n)
            if (n == "Standard") {
                assertEquals("E2", t.targets[0].label)
            } else {
                // Must not silently resolve to Standard.
                assertNotEquals("Standard", t.name)
                assertNotEquals(listOf("E2","A2","D3","G3","B3","E4").map { it }, t.targets.map { it.label })
            }
        }
    }

    @Test
    fun warning_tuningUpTwoSemitones_soft_is_visible() {
        // Custom tuning: low-E bumped to F#2 (+2) -> SOFT warning must be present on that string.
        val r = TuningParser.parse(listOf("F#2", "A2", "D3", "G3", "B3", "E4"))
        assertTrue(r.ok)
        assertEquals(TuningParser.WarningLevel.SOFT, r.warnings[0])
    }

    @Test
    fun warning_exactlyStandard_isNone() {
        val std = NoteConverter.fromMidi(40)
        assertEquals(TuningParser.WarningLevel.NONE, TuningParser.warningFor(std, std))
    }

    @Test
    fun parse_reportsWarningPerString() {
        // Tune string 1 (E2) up to G2 (+3, HARD); rest standard.
        val r = TuningParser.parse(listOf("G2", "A2", "D3", "G3", "B3", "E4"))
        assertTrue(r.ok)
        assertEquals(TuningParser.WarningLevel.HARD, r.warnings[0])
        assertEquals(TuningParser.WarningLevel.NONE, r.warnings[1])
    }

    @Test
    fun noteConverter_parseNote_basic() {
        assertEquals("A4", NoteConverter.parseNote("A4")!!.label)
        assertEquals("C#4", NoteConverter.parseNote("C#4")!!.label)
        assertEquals("C#4", NoteConverter.parseNote("Db4")!!.label) // Db = C#
        assertNull(NoteConverter.parseNote(""))
        assertNull(NoteConverter.parseNote("Hello"))
    }
}
