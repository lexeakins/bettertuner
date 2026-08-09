package com.lexeakins.bettertuner.audio

/**
 * Abstraction over a mono PCM audio input. This is the capture **seam**: the rest of the app depends
 * on this interface, never on [android.media.AudioRecord] directly, so the real mic source can be
 * swapped for a [com.lexeakins.bettertuner.audio.FakeAudioSource] in JVM tests.
 *
 * Implementations emit normalized FloatArrays in [-1, 1] at [sampleRateHz].
 */
interface AudioSource {
    /** Sample rate of emitted buffers, in Hz. */
    val sampleRateHz: Int

    /**
     * Begin capturing. Invokes [onBuffer] with each captured frame of normalized samples.
     * Blocks until [stop] is called (real sources run on a background thread; the fake emits synchronously).
     */
    fun start(onBuffer: (FloatArray) -> Unit)

    /** Stop capturing and release underlying resources. */
    fun stop()
}
