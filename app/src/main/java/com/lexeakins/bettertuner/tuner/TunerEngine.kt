package com.lexeakins.bettertuner.tuner

import com.lexeakins.bettertuner.audio.AudioSource
import com.lexeakins.bettertuner.pitch.NoteConverter
import com.lexeakins.bettertuner.pitch.Pitch
import com.lexeakins.bettertuner.pitch.YinPitchDetector
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Direction of the detected pitch relative to the target note.
 */
enum class TuneDirection { LOW, IN_TUNE, HIGH }

/**
 * A named tuning: an ordered list of target notes (low → high), e.g. Standard EADGBE.
 * Pure data — reuses the pitch engine; no Android dependency.
 */
data class Tuning(
    val name: String,
    val targets: List<Pitch>,
) {
    companion object {
        /** Standard guitar tuning, E2 A2 D3 G3 B3 E4 (low E → high e). */
        val STANDARD = Tuning(
            "Standard",
            listOf(
                NoteConverter.fromMidi(40),  // E2
                NoteConverter.fromMidi(45),  // A2
                NoteConverter.fromMidi(50),  // D3
                NoteConverter.fromMidi(55),  // G3
                NoteConverter.fromMidi(59),  // B3
                NoteConverter.fromMidi(64),  // E4
            ),
        )

        /** Drop D: D2 A2 D3 G3 B3 E4. */
        val DROP_D = Tuning(
            "Drop D",
            listOf(
                NoteConverter.fromMidi(38),  // D2
                NoteConverter.fromMidi(45),  // A2
                NoteConverter.fromMidi(50),  // D3
                NoteConverter.fromMidi(55),  // G3
                NoteConverter.fromMidi(59),  // B3
                NoteConverter.fromMidi(64),  // E4
            ),
        )

        /** DADGAD. */
        val DADGAD = Tuning(
            "DADGAD",
            listOf(
                NoteConverter.fromMidi(38),  // D2
                NoteConverter.fromMidi(45),  // A2
                NoteConverter.fromMidi(50),  // D3
                NoteConverter.fromMidi(55),  // G3
                NoteConverter.fromMidi(57),  // A3
                NoteConverter.fromMidi(62),  // D4
            ),
        )
    }
}

/**
 * Snapshot the UI observes. `target` is the note being tuned to (manual selection or auto-nearest);
 * `detected` is the live pitch from the mic; `cents` is detected − target in cents (negative = low).
 */
data class TunerState(
    val detected: Pitch? = null,
    val target: Pitch? = null,
    val cents: Double = 0.0,
    val direction: TuneDirection = TuneDirection.IN_TUNE,
    val inTune: Boolean = false,
)

/**
 * Wires the capture seam → YIN detector → note converter and exposes a [StateFlow] of [TunerState].
 *
 * - **Auto mode:** the target is the nearest target note in the active [Tuning] to the detected pitch.
 * - **Manual mode:** the target is fixed to [selectedTargetIndex] until the caller changes it.
 *
 * The engine owns the audio lifecycle; call [start] to begin capturing and [stop] to release.
 */
class TunerEngine(
    private val audioSource: AudioSource,
    private val tuning: Tuning = Tuning.STANDARD,
    private val inTuneCents: Float = 5f,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    private val _state = MutableStateFlow(TunerState())
    val state: StateFlow<TunerState> = _state.asStateFlow()

    /** Manual mode: index into [Tuning.targets] of the currently selected string. */
    var selectedTargetIndex: Int = 0
        set(value) {
            require(value in tuning.targets.indices) { "target index out of range" }
            field = value
            recomputeTarget()
        }

    var autoMode: Boolean = true
        set(value) {
            field = value
            recomputeTarget()
        }

    private fun recomputeTarget() {
        val cur = _state.value
        val target = if (autoMode) cur.detected?.let { nearestTarget(it) } else tuning.targets[selectedTargetIndex]
        val inTune = target != null && cur.detected != null &&
            cur.detected!!.centsTo(target).absoluteValue <= inTuneCents
        _state.value = cur.copy(target = target, inTune = inTune)
    }

    fun start() {
        coroutineScope.launch {
            audioSource.start { buffer ->
                val detection = YinPitchDetector.detect(buffer, audioSource.sampleRateHz) ?: return@start
                val pitch = NoteConverter.fromFrequency(detection.frequencyHz)
                val target = if (autoMode) nearestTarget(pitch) else tuning.targets[selectedTargetIndex]
                val cents = pitch.centsTo(target)
                val direction = when {
                    cents < -inTuneCents -> TuneDirection.LOW
                    cents > inTuneCents -> TuneDirection.HIGH
                    else -> TuneDirection.IN_TUNE
                }
                _state.value = TunerState(
                    detected = pitch,
                    target = target,
                    cents = cents,
                    direction = direction,
                    inTune = direction == TuneDirection.IN_TUNE,
                )
            }
        }
    }

    fun stop() = audioSource.stop()

    /** Nearest target note in the active tuning to [pitch] by absolute cents distance. */
    private fun nearestTarget(pitch: Pitch): Pitch =
        tuning.targets.minByOrNull { kotlin.math.abs(pitch.centsTo(it)) } ?: tuning.targets.first()
}
