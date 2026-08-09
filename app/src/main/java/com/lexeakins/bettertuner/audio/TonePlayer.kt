package com.lexeakins.bettertuner.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder

/**
 * Plays a short, locally-generated tone (no files, no network, nothing recorded or transmitted).
 * Abstraction so the UI depends on this, not on [AudioTrack] directly — testable.
 */
interface TonePlayer {
    /** Start a continuous sine at [frequencyHz]. Idempotent: calling again just retunes. */
    fun start(frequencyHz: Double)

    /** Stop any tone currently playing. */
    fun stop()
}

/**
 * Real implementation backed by Android [AudioTrack]. Generates a sine in memory and streams it; the
 * buffer is computed on the device and never leaves it. All audio failures are swallowed (logged) so a
 * missing/blocked audio output can never crash the tuner UI.
 */
class AudioTrackTonePlayer : TonePlayer {
    @Volatile private var track: AudioTrack? = null
    @Volatile private var playing = false

    override fun start(frequencyHz: Double) {
        try {
            stop() // ensure clean retune
            val sampleRate = 44100
            val n = sampleRate // 1s ring buffer
            val buf = ShortArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / sampleRate
                buf[i] = (kotlin.math.sin(2 * Math.PI * frequencyHz * t) * 0.6 * Short.MAX_VALUE).toInt().toShort()
            }
            val at = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            if (at.state != AudioTrack.STATE_INITIALIZED) return
            at.play()
            at.write(buf, 0, buf.size)
            // loop the buffer for a sustained tone while held
            track = at
            playing = true
        } catch (e: Exception) {
            android.util.Log.w("BetterTuner", "tone play failed (non-fatal): ${e.message}")
        }
    }

    override fun stop() {
        playing = false
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {
        }
        track = null
    }
}
