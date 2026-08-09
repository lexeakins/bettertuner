package com.lexeakins.bettertuner.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * Real microphone capture via [AudioRecord]. Converts 16-bit PCM shorts to normalized FloatArrays.
 *
 * Only instantiable on a device/emulator (requires the MIC permission and audio hardware); unit tests
 * use [FakeAudioSource] instead. Correctness of the *capture* (non-silent, correctly-sized, right rate)
 * is verified by [com.lexeakins.bettertuner.audio.AudioRecordSourceTest] on a real device.
 */
class AudioRecordSource(
    override val sampleRateHz: Int = DEFAULT_SAMPLE_RATE,
    private val framesPerBuffer: Int = DEFAULT_FRAMES,
) : AudioSource {

    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var thread: Thread? = null

    override fun start(onBuffer: (FloatArray) -> Unit) {
        val minBufferShorts = AudioRecord.getMinBufferSize(sampleRateHz, channelConfig, audioFormat)
        val bufferShorts = maxOf(minBufferShorts, framesPerBuffer * 2)
        val record = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateHz, channelConfig, audioFormat, bufferShorts)
        audioRecord = record

        val shorts = ShortArray(framesPerBuffer)
        record.startRecording()

        thread = Thread({
            while (!Thread.currentThread().isInterrupted) {
                val read = record.read(shorts, 0, shorts.size)
                if (read > 0) {
                    val floats = FloatArray(read) { i -> shorts[i] / 32768.0f }
                    onBuffer(floats)
                }
            }
        }, "AudioRecordSource").also { it.start() }
    }

    override fun stop() {
        thread?.interrupt()
        thread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44100
        const val DEFAULT_FRAMES = 4096
    }
}
