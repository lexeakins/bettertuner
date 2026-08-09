package com.lexeakins.bettertuner.audio

import com.lexeakins.bettertuner.pitch.NoteConverter
import com.lexeakins.bettertuner.pitch.Pitch
import com.lexeakins.bettertuner.pitch.SignalGenerator
import com.lexeakins.bettertuner.pitch.YinPitchDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Vertical-slice integration test: the capture seam feeding the pitch engine, with no hardware.
 * The [FakeAudioSource] plays a known synthetic tone; we assert the full path
 * capture → [YinPitchDetector] → [NoteConverter] recovers the right note. This pins the contract
 * between the audio layer and the detector so a future real source can be dropped in unchanged.
 */
class AudioToPitchPipelineTest {

    private val sampleRate = 44100

    private fun detectNote(source: AudioSource): Pitch? {
        var pitch: Pitch? = null
        source.start { buf ->
            val det = YinPitchDetector.detect(buf, sampleRate)
            if (det != null) pitch = NoteConverter.fromFrequency(det.frequencyHz)
        }
        return pitch
    }

    @Test
    fun a4_tone_resolves_to_A4() {
        val source = FakeAudioSource(sampleRate, SignalGenerator.sine(440.0, sampleRate, 4096))
        val pitch = detectNote(source)
        assertNotNull(pitch)
        assertEquals("A4", pitch!!.label)
        assertEquals(0.0, pitch.cents, 1.0)
    }

    @Test
    fun low_e_string_resolves_to_E2() {
        val source = FakeAudioSource(sampleRate, SignalGenerator.sine(82.41, sampleRate, 4096))
        val pitch = detectNote(source)
        assertNotNull(pitch)
        assertEquals("E2", pitch!!.label)
    }

    @Test
    fun a2_tone_resolves_to_A2() {
        val source = FakeAudioSource(sampleRate, SignalGenerator.sine(110.0, sampleRate, 4096))
        val pitch = detectNote(source)
        assertNotNull(pitch)
        assertEquals("A2", pitch!!.label)
    }
}
