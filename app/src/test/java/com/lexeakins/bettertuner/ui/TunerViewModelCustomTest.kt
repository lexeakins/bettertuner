package com.lexeakins.bettertuner.ui

import com.lexeakins.bettertuner.audio.TonePlayer
import com.lexeakins.bettertuner.settings.SavedTuning
import com.lexeakins.bettertuner.settings.Settings
import com.lexeakins.bettertuner.settings.SettingsStore
import com.lexeakins.bettertuner.tuner.TuningParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class TunerViewModelCustomTest {

    private val fakeTone = object : TonePlayer {
        override fun start(frequencyHz: Double) {}
        override fun stop() {}
    }

    private class FakeStore : SettingsStore {
        private val saved = mutableListOf<SavedTuning>()
        override fun get(): Settings = Settings.DEFAULT
        override fun set(settings: Settings) {}
        override fun getSavedTunings(): List<SavedTuning> = saved.toList()
        override fun saveTuning(tuning: SavedTuning) {
            saved.removeAll { it.id == tuning.id }
            saved.add(tuning)
        }
        override fun deleteTuning(id: String) { saved.removeAll { it.id == id } }
        override fun renameTuning(id: String, newName: String) {
            val i = saved.indexOfFirst { it.id == id }
            if (i >= 0) saved[i] = saved[i].copy(name = newName)
        }
    }

    private fun vm() = TunerViewModel(tonePlayer = fakeTone, settingsStore = FakeStore())

    @Test
    fun parseCustom_valid_returnsPitches() {
        val v = vm()
        val r = v.parseCustom(listOf("E2", "A2", "D3", "G3", "B3", "E4"))
        assertTrue(r.ok)
        assertEquals(6, r.pitches!!.size)
    }

    @Test
    fun parseCustom_outOfRangeFlagsHardWarning() {
        val v = vm()
        val r = v.parseCustom(listOf("G2", "A2", "D3", "G3", "B3", "E4")) // E2 -> G2 (+3)
        assertTrue(r.ok)
        assertEquals(TuningParser.WarningLevel.HARD, r.warnings[0])
    }

    @Test
    fun applyCustomTuning_appliesAndSetsSpec() {
        val v = vm()
        val ok = v.applyCustomTuning(listOf("E2", "A2", "D3", "G3", "B3", "E4"))
        assertTrue(ok)
        assertEquals("E2,A2,D3,G3,B3,E4", v.uiState.value.customSpec)
        assertEquals("E2", v.uiState.value.tuning.targets[0].label)
    }

    @Test
    fun applyCustomTuning_invalid_returnsFalse() {
        val v = vm()
        assertFalse(v.applyCustomTuning(listOf("E2", "A2", "Nope", "G3", "B3", "E4")))
        assertNull(v.uiState.value.customSpec)
    }

    @Test
    fun saveRenameDelete_customTuning() {
        val v = vm()
        v.applyCustomTuning(listOf("C2", "G2", "C3", "G3", "C4", "E4"))
        v.saveCurrentCustom("My Open C")
        val saved = v.uiState.value.customTunings
        assertEquals(1, saved.size)
        val id = saved[0].id
        assertEquals("My Open C", saved[0].name)

        v.renameTuning(id, "Renamed")
        assertEquals("Renamed", v.uiState.value.customTunings.first { it.id == id }.name)

        v.deleteTuning(id)
        assertTrue(v.uiState.value.customTunings.isEmpty())
    }

    @Test
    fun applySavedTuning_byId() {
        val v = vm()
        v.applyCustomTuning(listOf("D2", "A2", "D3", "G3", "B3", "D4"))
        v.saveCurrentCustom("Drop D-ish")
        val id = v.uiState.value.customTunings[0].id
        // Apply a different tuning, then apply the saved one.
        v.applyCustomTuning(listOf("E2", "A2", "D3", "G3", "B3", "E4"))
        assertEquals("E2", v.uiState.value.tuning.targets[0].label)
        v.applySavedTuning(id)
        assertEquals("D2", v.uiState.value.tuning.targets[0].label)
    }
}
