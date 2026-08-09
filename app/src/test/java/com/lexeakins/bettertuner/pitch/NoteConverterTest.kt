package com.lexeakins.bettertuner.pitch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [NoteConverter]. Pure function: frequency -> note name + octave + cents.
 * Expected values are computed from the equal-tempered definition (independent source of truth),
 * not from the implementation, so these can never pass "by construction".
 */
class NoteConverterTest {

    @Test
    fun a4_maps_to_A4_zero_cents() {
        val pitch = NoteConverter.fromFrequency(440.0)
        assertEquals("A", pitch.name)
        assertEquals(4, pitch.octave)
        assertEquals("A4", pitch.label)
        assertEquals(0.0, pitch.cents, 1e-9)
    }

    @Test
    fun low_e_string_maps_to_E2() {
        // Guitar low E (6th string) is ~82.41 Hz.
        val pitch = NoteConverter.fromFrequency(82.41)
        assertEquals("E2", pitch.label)
        assertEquals(82.41, pitch.frequencyHz, 1e-9)
    }

    @Test
    fun sharp_note_reports_positive_cents() {
        // 446 Hz is slightly above A4 (440) -> positive cents (~23.5).
        val pitch = NoteConverter.fromFrequency(446.0)
        assertEquals("A", pitch.name)
        assertTrue("expected positive cents for a sharp A, got ${pitch.cents}", pitch.cents > 0)
        assertEquals(23.5, pitch.cents, 1.0)
    }

    @Test
    fun flat_note_reports_negative_cents() {
        // 435 Hz is slightly below A4 -> negative cents (~-19.8).
        val pitch = NoteConverter.fromFrequency(435.0)
        assertEquals("A", pitch.name)
        assertTrue("expected negative cents for a flat A, got ${pitch.cents}", pitch.cents < 0)
        assertEquals(-19.8, pitch.cents, 1.0)
    }

    @Test
    fun different_octaves_same_letter() {
        assertEquals("A3", NoteConverter.fromFrequency(220.0).label)
        assertEquals("A5", NoteConverter.fromFrequency(880.0).label)
    }

    @Test
    fun isInTune_threshold() {
        assertTrue(NoteConverter.fromFrequency(440.0).isInTune())
        assertFalse(NoteConverter.fromFrequency(451.0).isInTune()) // ~43 cents off
    }

    @Test(expected = IllegalArgumentException::class)
    fun negative_frequency_rejected() {
        NoteConverter.fromFrequency(-1.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zero_frequency_rejected() {
        NoteConverter.fromFrequency(0.0)
    }
}
