package com.lexeakins.bettertuner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexeakins.bettertuner.audio.AudioRecordSource
import com.lexeakins.bettertuner.audio.TonePlayer
import com.lexeakins.bettertuner.tuner.TuneDirection
import com.lexeakins.bettertuner.tuner.TuneLockDetector
import com.lexeakins.bettertuner.tuner.TunerEngine
import com.lexeakins.bettertuner.tuner.TunerState
import com.lexeakins.bettertuner.tuner.Tuning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Bridges [TunerEngine] to the Compose screen. Owns UI-only state (selected tuning, mode, permission
 * gate, settings, per-string tuning progress) and translates engine events (lock-in) into one-shot side
 * effects (reward bell, tone, auto-advance).
 *
 * Design decisions from the grill-me session:
 * - Manual target selection flips autoMode off (manual wins); re-toggling Auto resumes it.
 * - Bell fires once per lock edge (via [TuneLockDetector]); on lock the string is marked tuned and, in auto
 *   mode, the UI advances to the next string (low E -> high e).
 * - Left-menu notes: tap = short preview, press-and-hold = sustained tone; release stops.
 */
class TunerViewModel(
    private val audioSourceFactory: () -> com.lexeakins.bettertuner.audio.AudioSource = { AudioRecordSource() },
    private val tonePlayer: TonePlayer,
    private val inTuneCents: Float = 5f,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var engine: TunerEngine? = null
    private val lockDetector = TuneLockDetector()

    /** Call once microphone permission is granted. Builds + starts the engine. */
    fun startEngine() {
        if (engine != null) return
        val source = audioSourceFactory()
        engine = TunerEngine(source, _uiState.value.tuning, inTuneCents, viewModelScope)
        engine!!.autoMode = _uiState.value.autoMode
        engine!!.selectedTargetIndex = _uiState.value.selectedTargetIndex
        collectEngine(engine!!)
        engine!!.start()
    }

    fun stopEngine() {
        engine?.stop()
        engine = null
    }

    fun setTuning(tuning: Tuning) {
        _uiState.update { it.copy(tuning = tuning, selectedTargetIndex = 0, autoMode = true, tunedStrings = emptySet()) }
        engine?.let { eng ->
            eng.stop()
            val src = audioSourceFactory()
            val newEngine = TunerEngine(src, tuning, inTuneCents, viewModelScope)
            newEngine.autoMode = true
            newEngine.selectedTargetIndex = 0
            collectEngine(newEngine)
            newEngine.start()
            engine = newEngine
        }
    }

    private fun collectEngine(eng: TunerEngine) {
        viewModelScope.launch {
            eng.state.collect { tunerState ->
                val bell = lockDetector.onState(tunerState.inTune, tunerState.cents, System.currentTimeMillis())
                val idx = _uiState.value.selectedTargetIndex
                val auto = _uiState.value.autoMode
                _uiState.update { st ->
                    var nextIdx = idx
                    var nextAuto = auto
                    // On a lock edge: mark this string tuned; in auto mode, advance to the next string.
                    val tuned = if (bell) st.tunedStrings + idx else st.tunedStrings
                    if (bell && auto) {
                        val size = st.tuning.targets.size
                        nextIdx = (idx + 1) % size
                        nextAuto = true // stay in auto; advancing is the auto behavior
                    }
                    st.copy(tuner = tunerState, rewardBell = bell, tunedStrings = tuned,
                        selectedTargetIndex = nextIdx, autoMode = nextAuto)
                }
                // Reflect the advance into the engine so its target tracks the UI.
                if (bell && auto) {
                    eng.selectedTargetIndex = _uiState.value.selectedTargetIndex
                    eng.autoMode = true
                }
            }
        }
    }

    fun setAutoMode(auto: Boolean) {
        _uiState.update { it.copy(autoMode = auto) }
        engine?.autoMode = auto
    }

    /** Manual string pick: selects the target and drops out of auto mode. */
    fun selectString(index: Int) {
        val max = _uiState.value.tuning.targets.lastIndex
        val idx = index.coerceIn(0, max)
        _uiState.update { it.copy(selectedTargetIndex = idx, autoMode = false) }
        engine?.apply {
            autoMode = false
            selectedTargetIndex = idx
        }
    }

    /**
     * Cycle the selected string by [delta]. Convention: +1 = toward the high e string (index increases),
     * -1 = toward low E. A downward swipe maps to +1 (matches "swipe down -> higher string").
     */
    fun cycleString(delta: Int) {
        val size = _uiState.value.tuning.targets.size
        val next = (_uiState.value.selectedTargetIndex + delta).mod(size)
        selectString(next)
    }

    /** Press-and-hold center: play the current target's tone. */
    fun startReferenceTone() {
        val target = _uiState.value.tuner.target ?: _uiState.value.tuning.targets
            .getOrNull(_uiState.value.selectedTargetIndex) ?: return
        tonePlayer.start(target.frequencyHz)
    }

    fun stopReferenceTone() = tonePlayer.stop()

    /** Preview/sustain a specific pitch's tone from the left-menu note (tap = short, hold = sustained). */
    fun startToneForPitch(frequencyHz: Double) = tonePlayer.start(frequencyHz)

    /** Tap preview: play the pitch briefly (~350ms) then stop, so a tap doesn't leave a dangling tone. */
    fun previewTone(frequencyHz: Double) {
        tonePlayer.start(frequencyHz)
        viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            tonePlayer.stop()
        }
    }

    fun consumeRewardBell(): Boolean {
        val fire = _uiState.value.rewardBell
        if (fire) _uiState.update { it.copy(rewardBell = false) }
        return fire
    }

    override fun onCleared() {
        stopEngine()
        tonePlayer.stop()
        super.onCleared()
    }
}

/**
 * UI-facing snapshot. [tuner] is the live engine state; the rest is UI-only state.
 * [tunedStrings] holds the indices already detected in-tune, for whole-instrument progress display.
 */
data class TunerUiState(
    val tuner: TunerState = TunerState(),
    val tuning: Tuning = Tuning.STANDARD,
    val autoMode: Boolean = true,
    val selectedTargetIndex: Int = 0,
    val rewardBell: Boolean = false,
    val tunedStrings: Set<Int> = emptySet(),
)

val TunerState.directionLabel: String
    get() = when (direction) {
        TuneDirection.LOW -> "FLAT"
        TuneDirection.HIGH -> "SHARP"
        TuneDirection.IN_TUNE -> "IN TUNE"
    }
