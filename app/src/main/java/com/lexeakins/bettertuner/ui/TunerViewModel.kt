package com.lexeakins.bettertuner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexeakins.bettertuner.audio.AudioRecordSource
import com.lexeakins.bettertuner.audio.TonePlayer
import com.lexeakins.bettertuner.settings.SavedTuning
import com.lexeakins.bettertuner.settings.Settings
import com.lexeakins.bettertuner.settings.SettingsStore
import com.lexeakins.bettertuner.tuner.TuneDirection
import com.lexeakins.bettertuner.tuner.TuneLockDetector
import com.lexeakins.bettertuner.tuner.TunerEngine
import com.lexeakins.bettertuner.tuner.TunerState
import com.lexeakins.bettertuner.tuner.Tuning
import com.lexeakins.bettertuner.tuner.TuningParser
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
 * - Settings (A4 reference, in-tune tolerance, theme) persist via [SettingsStore] and reshape the engine
 *   targets/tolerance on change.
 */
class TunerViewModel(
    private val audioSourceFactory: () -> com.lexeakins.bettertuner.audio.AudioSource = { AudioRecordSource() },
    private val tonePlayer: TonePlayer,
    private val settingsStore: SettingsStore,
    private val inTuneCents: Float = 5f,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var engine: TunerEngine? = null

    /**
     * Continuous in-tune timestamp (ms) for the *current* target. A string counts as confirmed only after it
     * stays in tune uninterrupted for [DWELL_MS] — this prevents a single-frame or wrong-string transient from
     * advancing auto mode. Reset whenever the target changes or we leave tune.
     */
    private var inTuneSinceMs: Long? = null
    private val DWELL_MS = 600L

    init {
        // Load persisted settings so the engine is built with the user's A4/tolerance from the first launch.
        val loaded = settingsStore.get()
        _uiState.value = _uiState.value.copy(
            settings = loaded,
            tuning = Tuning.byName(_uiState.value.tuning.name, loaded.a4Hz),
            customTunings = settingsStore.getSavedTunings(),
        )
    }

    /** Call once microphone permission is granted. Builds + starts the engine. */
    fun startEngine() {
        if (engine != null) return
        inTuneSinceMs = null
        val s = _uiState.value.settings
        val source = audioSourceFactory()
        engine = TunerEngine(source, Tuning.byName(_uiState.value.tuning.name, s.a4Hz), s.toleranceCents, viewModelScope)
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
        // Preset path: rebuild by name so a changed A4 reference reshapes targets. For non-preset names this
        // falls back to Standard — custom tunings must use [applyCustomTuning] (which keeps the exact pitches).
        val s = _uiState.value.settings
        applyTuningObject(Tuning.byName(tuning.name, s.a4Hz))
    }

    /** Applies a tuning object exactly as given (used for custom tunings whose name isn't a preset). */
    private fun applyTuningObject(tuning: Tuning) {
        val s = _uiState.value.settings
        inTuneSinceMs = null
        _uiState.update { it.copy(tuning = tuning, selectedTargetIndex = 0, autoMode = true, tunedStrings = emptySet()) }
        engine?.let { eng ->
            eng.stop()
            val src = audioSourceFactory()
            val newEngine = TunerEngine(src, tuning, s.toleranceCents, viewModelScope)
            newEngine.autoMode = true
            newEngine.selectedTargetIndex = 0
            collectEngine(newEngine)
            newEngine.start()
            engine = newEngine
        }
    }

    // region Custom tunings

    /** Parses 6 note specs (low->high) using [TuningParser]. Returns the safe/unsafe [TuningParser.ParseResult]. */
    fun parseCustom(fields: List<String>): TuningParser.ParseResult {
        val a4 = _uiState.value.settings.a4Hz
        return TuningParser.parse(fields, a4)
    }

    /** Applies a custom tuning from 6 specs if valid. Returns true if applied. */
    fun applyCustomTuning(fields: List<String>): Boolean {
        val result = parseCustom(fields)
        if (!result.ok || result.pitches == null) return false
        val spec = fields.map { it.trim() }.joinToString(",")
        applyTuningObject(Tuning("Custom", result.pitches))
        _uiState.update { it.copy(customSpec = spec) }
        return true
    }

    /** Applies an already-saved custom tuning by id. */
    fun applySavedTuning(id: String) {
        val saved = _uiState.value.customTunings.firstOrNull { it.id == id } ?: return
        val fields = saved.spec.split(",")
        if (applyCustomTuning(fields)) {
            _uiState.update { it.copy(customSpec = saved.spec, currentSavedId = id) }
        }
    }

    /** Saves the current custom spec as a new preset (or overwrites if name collides). */
    fun saveCurrentCustom(name: String) {
        val spec = _uiState.value.customSpec ?: return
        val id = "custom_${spec.hashCode()}"
        settingsStore.saveTuning(SavedTuning(id, name.ifBlank { "Custom" }, spec))
        _uiState.update {
            it.copy(customTunings = settingsStore.getSavedTunings(), currentSavedId = id)
        }
    }

    fun renameTuning(id: String, newName: String) {
        settingsStore.renameTuning(id, newName)
        _uiState.update { it.copy(customTunings = settingsStore.getSavedTunings()) }
    }

    fun deleteTuning(id: String) {
        settingsStore.deleteTuning(id)
        _uiState.update { st ->
            val clearedId = if (st.currentSavedId == id) null else st.currentSavedId
            st.copy(customTunings = settingsStore.getSavedTunings(), currentSavedId = clearedId)
        }
    }

    // endregion

    private fun collectEngine(eng: TunerEngine) {
        viewModelScope.launch {
            eng.state.collect { tunerState ->
                val now = System.currentTimeMillis()
                val idx = _uiState.value.selectedTargetIndex
                val auto = _uiState.value.autoMode
                val focusedTarget = _uiState.value.tuning.targets.getOrNull(idx)

                // Confirm only when the *focused* string is the one detected in tune. In auto mode the engine
                // reports whichever string is nearest (e.g. plucking a tuned A while focused on E), but we must
                // not advance E for a different string's lock. The readout still shows the detected note.
                val detectedIsFocused = tunerState.inTune &&
                    tunerState.target != null &&
                    tunerState.target.label == focusedTarget?.label
                if (detectedIsFocused) {
                    if (inTuneSinceMs == null) inTuneSinceMs = now
                } else {
                    inTuneSinceMs = null
                }
                val elapsed = inTuneSinceMs?.let { now - it } ?: 0L
                val lockProgress = (elapsed.toFloat() / DWELL_MS).coerceIn(0f, 1f)
                val confirmed = auto && detectedIsFocused && elapsed >= DWELL_MS

                _uiState.update { st ->
                    val tuned = if (confirmed) st.tunedStrings + idx else st.tunedStrings
                    var nextIdx = idx
                    var nextAuto = auto
                    if (confirmed) {
                        val size = st.tuning.targets.size
                        nextIdx = (idx + 1) % size
                        nextAuto = true
                        inTuneSinceMs = null
                    }
                    st.copy(
                        tuner = tunerState,
                        rewardBell = confirmed,
                        tunedStrings = tuned,
                        selectedTargetIndex = nextIdx,
                        autoMode = nextAuto,
                        lockProgress = lockProgress,
                    )
                }
                if (confirmed) {
                    eng.selectedTargetIndex = _uiState.value.selectedTargetIndex
                    eng.autoMode = true
                }
            }
        }
    }

    fun setAutoMode(auto: Boolean) {
        inTuneSinceMs = null
        _uiState.update { it.copy(autoMode = auto) }
        engine?.autoMode = auto
    }

    /** Manual string pick: selects the target and drops out of auto mode. */
    fun selectString(index: Int) {
        inTuneSinceMs = null
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

    /** Persist + apply new settings (A4 reference and/or tolerance). Reshapes engine targets on change. */
    fun updateSettings(block: Settings.() -> Settings) {
        val next = _uiState.value.settings.block()
        settingsStore.set(next)
        val s = _uiState.value.copy(settings = next)
        _uiState.value = s
        // Rebuild the active tuning with the (possibly new) A4 so target frequencies update.
        val tuned = Tuning.byName(_uiState.value.tuning.name, next.a4Hz)
        _uiState.update { it.copy(tuning = tuned) }
        engine?.let { eng ->
            eng.stop()
            val src = audioSourceFactory()
            val newEngine = TunerEngine(src, tuned, next.toleranceCents, viewModelScope)
            newEngine.autoMode = _uiState.value.autoMode
            newEngine.selectedTargetIndex = _uiState.value.selectedTargetIndex
            collectEngine(newEngine)
            newEngine.start()
            engine = newEngine
        }
    }

    /** Press-and-hold center: play the current target's tone. */
    fun startReferenceTone() {
        val target = _uiState.value.tuner.target ?: _uiState.value.tuning.targets
            .getOrNull(_uiState.value.selectedTargetIndex) ?: return
        tonePlayer.start(target.frequencyHz)
    }

    fun stopReferenceTone() = tonePlayer.stop()

    /** Preview the target tone for a pitch: a ~3s plucked-string sound (self-terminating). */
    fun previewTone(frequencyHz: Double) {
        tonePlayer.start(frequencyHz)
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
    val settings: Settings = Settings.DEFAULT,
    /** 0..1 progress toward confirming the current target in tune (auto mode dwell). For a "settling" cue. */
    val lockProgress: Float = 0f,
    /** User-saved custom tuning presets, shown alongside built-ins in the tuning dropdown. */
    val customTunings: List<SavedTuning> = emptyList(),
    /** The 6-spec string of the currently active custom tuning (e.g. "E2,A2,..."), null if not custom. */
    val customSpec: String? = null,
    /** Id of the saved preset currently applied, if any (so we can rename/delete it). */
    val currentSavedId: String? = null,
)

val TunerState.directionLabel: String
    get() = when (direction) {
        TuneDirection.LOW -> "FLAT"
        TuneDirection.HIGH -> "SHARP"
        TuneDirection.IN_TUNE -> "IN TUNE"
    }
