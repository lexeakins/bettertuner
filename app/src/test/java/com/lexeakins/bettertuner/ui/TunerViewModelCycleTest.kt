package com.lexeakins.bettertuner.ui

import com.lexeakins.bettertuner.audio.TonePlayer
import com.lexeakins.bettertuner.settings.Settings
import com.lexeakins.bettertuner.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Test

class TunerViewModelCycleTest {

    // Fake tone player + settings store: no Android deps, just records nothing.
    private val fakeTone = object : TonePlayer {
        override fun start(frequencyHz: Double) {}
        override fun stop() {}
    }
    private val fakeSettings = object : SettingsStore {
        override fun get(): Settings = Settings.DEFAULT
        override fun set(settings: Settings) {}
    }

    private fun vm() = TunerViewModel(tonePlayer = fakeTone, settingsStore = fakeSettings)

    @Test
    fun cycleString_directionConvention() {
        // +1 = toward the high e string (index increases); -1 = toward low E.
        val v = vm()
        // Standard targets: [E2(0), A2(1), D3(2), G3(3), B3(4), E4(5)]
        v.selectString(1) // A2
        assertEquals(1, v.uiState.value.selectedTargetIndex)
        v.cycleString(+1) // swipe down -> higher (next)
        assertEquals(2, v.uiState.value.selectedTargetIndex) // D3
        v.cycleString(-1) // swipe up -> lower
        assertEquals(1, v.uiState.value.selectedTargetIndex) // back to A2
        v.cycleString(-1) // wrap to low E
        assertEquals(0, v.uiState.value.selectedTargetIndex)
        // And +1 from last wraps to first.
        v.selectString(5) // E4
        v.cycleString(+1)
        assertEquals(0, v.uiState.value.selectedTargetIndex) // wraps to E2
    }

    @Test
    fun selectString_dropsAutoMode() {
        val v = vm()
        v.setAutoMode(true)
        assertEquals(true, v.uiState.value.autoMode)
        v.selectString(3)
        assertEquals(false, v.uiState.value.autoMode) // manual pick exits auto
        assertEquals(3, v.uiState.value.selectedTargetIndex)
    }

    @Test
    fun updateSettings_persistsAndReshapesTargets() {
        val v = vm()
        // Change A4 to 432 Hz -> E2 target frequency should drop from ~82.4 to ~80.9 Hz.
        v.updateSettings { copy(a4Hz = 432.0) }
        val e2 = v.uiState.value.tuning.targets.first()
        assertEquals(432.0, v.uiState.value.settings.a4Hz, 0.001)
        assertEquals(80.9, e2.frequencyHz, 0.2)
        // Tolerance change is stored.
        v.updateSettings { copy(toleranceCents = 10f) }
        assertEquals(10f, v.uiState.value.settings.toleranceCents)
    }
}
