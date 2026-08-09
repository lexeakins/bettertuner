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
        /** Standard guitar tuning, E2 A2 D3 G3 B3 E4 (low E → high e). [a4Hz] sets the reference pitch. */
        fun standard(a4Hz: Double = 440.0) = Tuning(
            "Standard",
            listOf(
                NoteConverter.fromMidi(40, a4Hz),  // E2
                NoteConverter.fromMidi(45, a4Hz),  // A2
                NoteConverter.fromMidi(50, a4Hz),  // D3
                NoteConverter.fromMidi(55, a4Hz),  // G3
                NoteConverter.fromMidi(59, a4Hz),  // B3
                NoteConverter.fromMidi(64, a4Hz),  // E4
            ),
        )

        /** Drop D: D2 A2 D3 G3 B3 E4. */
        fun dropD(a4Hz: Double = 440.0) = Tuning(
            "Drop D",
            listOf(
                NoteConverter.fromMidi(38, a4Hz),  // D2
                NoteConverter.fromMidi(45, a4Hz),  // A2
                NoteConverter.fromMidi(50, a4Hz),  // D3
                NoteConverter.fromMidi(55, a4Hz),  // G3
                NoteConverter.fromMidi(59, a4Hz),  // B3
                NoteConverter.fromMidi(64, a4Hz),  // E4
            ),
        )

        /** DADGAD. */
        fun dadgad(a4Hz: Double = 440.0) = Tuning(
            "DADGAD",
            listOf(
                NoteConverter.fromMidi(38, a4Hz),  // D2
                NoteConverter.fromMidi(45, a4Hz),  // A2
                NoteConverter.fromMidi(50, a4Hz),  // D3
                NoteConverter.fromMidi(55, a4Hz),  // G3
                NoteConverter.fromMidi(57, a4Hz),  // A3
                NoteConverter.fromMidi(62, a4Hz),  // D4
            ),
        )

        /** Convenience defaults at A4 = 440 Hz (kept for existing call sites). */
        val STANDARD: Tuning get() = standard()
        val DROP_D: Tuning get() = dropD()
        val DADGAD: Tuning get() = dadgad()

        /** Build a tuning by its display name with the given A4 reference (default 440). */
        fun byName(name: String, a4Hz: Double = 440.0): Tuning = when (name) {
            "Standard" -> standard(a4Hz)
            "Drop D" -> dropD(a4Hz)
            "DADGAD" -> dadgad(a4Hz)
            else -> standard(a4Hz)
        }

        /** The built-in preset list shown in the tuning dropdown (all within safe tuning ranges). */
        fun presets(a4Hz: Double = 440.0): List<Tuning> = listOf(
            standard(a4Hz),
            halfStepDown(a4Hz),
            dropD(a4Hz),
            doubleDropD(a4Hz),
            dadgad(a4Hz),
            openD(a4Hz),
            openG(a4Hz),
            openE(a4Hz),
            openC(a4Hz),
            newStandard(a4Hz),
        )

        fun halfStepDown(a4Hz: Double = 440.0) = Tuning("Half-step down", listOf(
            NoteConverter.fromMidi(39, a4Hz), NoteConverter.fromMidi(44, a4Hz), NoteConverter.fromMidi(49, a4Hz),
            NoteConverter.fromMidi(54, a4Hz), NoteConverter.fromMidi(58, a4Hz), NoteConverter.fromMidi(63, a4Hz),
        ))
        fun doubleDropD(a4Hz: Double = 440.0) = Tuning("Double Drop D", listOf(
            NoteConverter.fromMidi(38, a4Hz), NoteConverter.fromMidi(45, a4Hz), NoteConverter.fromMidi(50, a4Hz),
            NoteConverter.fromMidi(55, a4Hz), NoteConverter.fromMidi(59, a4Hz), NoteConverter.fromMidi(62, a4Hz),
        ))
        fun openD(a4Hz: Double = 440.0) = Tuning("Open D", listOf(
            NoteConverter.fromMidi(38, a4Hz), NoteConverter.fromMidi(45, a4Hz), NoteConverter.fromMidi(50, a4Hz),
            NoteConverter.fromMidi(54, a4Hz), NoteConverter.fromMidi(57, a4Hz), NoteConverter.fromMidi(62, a4Hz),
        ))
        fun openG(a4Hz: Double = 440.0) = Tuning("Open G", listOf(
            NoteConverter.fromMidi(38, a4Hz), NoteConverter.fromMidi(43, a4Hz), NoteConverter.fromMidi(50, a4Hz),
            NoteConverter.fromMidi(55, a4Hz), NoteConverter.fromMidi(59, a4Hz), NoteConverter.fromMidi(62, a4Hz),
        ))
        fun openE(a4Hz: Double = 440.0) = Tuning("Open E", listOf(
            NoteConverter.fromMidi(40, a4Hz), NoteConverter.fromMidi(47, a4Hz), NoteConverter.fromMidi(52, a4Hz),
            NoteConverter.fromMidi(56, a4Hz), NoteConverter.fromMidi(59, a4Hz), NoteConverter.fromMidi(64, a4Hz),
        ))
        fun openC(a4Hz: Double = 440.0) = Tuning("Open C", listOf(
            NoteConverter.fromMidi(36, a4Hz), NoteConverter.fromMidi(43, a4Hz), NoteConverter.fromMidi(48, a4Hz),
            NoteConverter.fromMidi(55, a4Hz), NoteConverter.fromMidi(60, a4Hz), NoteConverter.fromMidi(64, a4Hz),
        ))
        fun newStandard(a4Hz: Double = 440.0) = Tuning("New Standard", listOf(
            NoteConverter.fromMidi(36, a4Hz), NoteConverter.fromMidi(43, a4Hz), NoteConverter.fromMidi(50, a4Hz),
            NoteConverter.fromMidi(57, a4Hz), NoteConverter.fromMidi(64, a4Hz), NoteConverter.fromMidi(67, a4Hz),
        ))
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
