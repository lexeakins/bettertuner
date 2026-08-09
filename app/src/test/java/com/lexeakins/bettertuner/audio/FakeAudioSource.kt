package com.lexeakins.bettertuner.audio

/**
 * Deterministic, hardware-free stand-in for [AudioSource]. Emits a single captured [buffer] on [start],
 * then stops. Lets JVM unit tests exercise the capture→detect pipeline without a microphone.
 */
class FakeAudioSource(
    override val sampleRateHz: Int,
    private val buffer: FloatArray,
) : AudioSource {

    var started: Boolean = false
        private set
    var stopped: Boolean = false
        private set
    var lastEmitted: FloatArray? = null
        private set

    override fun start(onBuffer: (FloatArray) -> Unit) {
        started = true
        val copy = buffer.copyOf()
        lastEmitted = copy
        onBuffer(copy)
    }

    override fun stop() {
        stopped = true
    }
}
