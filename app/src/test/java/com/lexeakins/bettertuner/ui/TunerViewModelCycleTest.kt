package com.lexeakins.bettertuner.ui

import com.lexeakins.bettertuner.audio.TonePlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class TunerViewModelCycleTest {

    // Fake tone player: records nothing, never plays — lets us unit-test UI state logic without Android.
    private val fakeTone = object : TonePlayer {
        override fun start(frequencyHz: Double) {}
        override fun stop() {}
    }

    @Test
    fun cycleString_directionConvention() {
        // +1 = toward the high e string (index increases); -1 = toward low E.
        val vm = TunerViewModel(tonePlayer = fakeTone)
        // Standard targets: [E2(0), A2(1), D3(2), G3(3), B3(4), E4(5)]
        vm.selectString(1) // A2
        assertEquals(1, vm.uiState.value.selectedTargetIndex)
        vm.cycleString(+1) // swipe down -> higher (B region / next)
        assertEquals(2, vm.uiState.value.selectedTargetIndex) // D3
        vm.cycleString(-1) // swipe up -> lower
        assertEquals(1, vm.uiState.value.selectedTargetIndex) // back to A2
        vm.cycleString(-1) // wrap to low E
        assertEquals(0, vm.uiState.value.selectedTargetIndex)
        // And +1 from last wraps to first.
        vm.selectString(5) // E4
        vm.cycleString(+1)
        assertEquals(0, vm.uiState.value.selectedTargetIndex) // wraps to E2
    }

    @Test
    fun selectString_dropsAutoMode() {
        val vm = TunerViewModel(tonePlayer = fakeTone)
        vm.setAutoMode(true)
        assertEquals(true, vm.uiState.value.autoMode)
        vm.selectString(3)
        assertEquals(false, vm.uiState.value.autoMode) // manual pick exits auto
        assertEquals(3, vm.uiState.value.selectedTargetIndex)
    }
}
