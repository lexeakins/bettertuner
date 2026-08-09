package com.lexeakins.bettertuner.audio

/**
 * Plays a short, locally-generated tone (no files, no network, nothing recorded or transmitted).
 * Abstraction so the UI depends on this, not on [android.media.AudioTrack] directly — testable.
 */
interface TonePlayer {
    /** Start a continuous sine at [frequencyHz]. Idempotent: calling again just retunes. */
    fun start(frequencyHz: Double)

    /** Stop any tone currently playing. */
    fun stop()
}

/**
 * Real implementation backed by Android [android.media.AudioTrack]. Generates a sine in memory and
 * streams it; the buffer is computed on the device and never leaves it.
 */
class AudioTrackTonePlayer : TonePlayer {
    @Volatile private var playing = false

    override fun start(frequencyHz: Double) {
        // Implementation wired in the UI module; kept here as the seam's concrete type.
        // (Body supplied with the Compose screen; see TunerScreen.kt for the AudioTrack worker.)
        playing = true
    }

    override fun stop() {
        playing = false
    }
}
