package com.lexeakins.bettertuner.tuner

import com.lexeakins.bettertuner.audio.FakeAudioSource
import com.lexeakins.bettertuner.pitch.NoteConverter
import com.lexeakins.bettertuner.pitch.SignalGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerEngineTest {

    private val sampleRate = 44100

    /** Build an engine fed by a fake source playing [freqHz]; run its coroutine to completion. */
    private fun runEngine(freqHz: Double, tuning: Tuning = Tuning.STANDARD, block: TunerEngine.() -> Unit = {}): TunerEngine {
        val source = FakeAudioSource(sampleRate, SignalGenerator.sine(freqHz, sampleRate, 4096))
        // Unconfined makes the engine's launch{} run synchronously as FakeAudioSource emits.
        val engine = TunerEngine(source, tuning, coroutineScope = CoroutineScope(Dispatchers.Unconfined))
        engine.block()
        engine.start() // consumes the fake's single buffer inline
        return engine
    }

    @Test
    fun autoMode_picksNearestStandardTarget_forLowE() {
        val engine = runEngine(82.41) { autoMode = true }
        val s = engine.state.value
        assertNotNull(s.detected)
        assertEquals("E2", s.detected!!.label)
        assertEquals("E2", s.target!!.label)
    }

    @Test
    fun autoMode_reportsSharpDirection_whenSlightlyHigh() {
        // 85 Hz is closer to E2 (82.41) than A2 (110): target E2, detected sharp -> HIGH.
        val engine = runEngine(85.0) { autoMode = true }
        val s = engine.state.value
        assertEquals("E2", s.target!!.label)
        assertEquals(TuneDirection.HIGH, s.direction)
        assertFalse(s.inTune)
    }

    @Test
    fun manualMode_usesSelectedTargetIndex_regardlessOfDetected() {
        val engine = runEngine(82.41) {
            autoMode = false
            selectedTargetIndex = 3 // G3
        }
        val s = engine.state.value
        assertEquals("G3", s.target!!.label)
        assertEquals("E2", s.detected!!.label)
        assertTrue("detected (E2) is far below target (G3) -> negative cents", s.cents < 0)
    }

    @Test
    fun inTune_whenDetectedMatchesTarget() {
        val engine = runEngine(82.41) {
            autoMode = false
            selectedTargetIndex = 0 // E2
        }
        val s = engine.state.value
        assertEquals("E2", s.target!!.label)
        assertEquals(TuneDirection.IN_TUNE, s.direction)
        assertTrue(s.inTune)
    }

    @Test
    fun tuningPresets_haveExpectedTargets() {
        assertEquals("D2", Tuning.DROP_D.targets.first().label)
        assertEquals("E2", Tuning.STANDARD.targets.first().label)
        assertEquals(6, Tuning.STANDARD.targets.size)
        assertEquals(6, Tuning.DROP_D.targets.size)
        assertEquals(listOf("D2", "A2", "D3", "G3", "A3", "D4"), Tuning.DADGAD.targets.map { it.label })
    }
}
