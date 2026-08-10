package com.lexeakins.bettertuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Plays a short, locally-generated tone (no files, no network, nothing recorded or transmitted).
 * Abstraction so the UI depends on this, not on [AudioTrack] directly — testable.
 *
 * Behavior: [start] plays a single ~3s *plucked-string* tone for the given frequency and returns
 * immediately (the sound plays to completion on its own). [stop] cuts it short if needed.
 */
interface TonePlayer {
    /** Play a one-shot plucked tone at [frequencyHz] (~3s, self-terminating). Idempotent: re-calling retriggers. */
    fun start(frequencyHz: Double)

    /** Stop any tone currently playing (e.g. user navigates away). */
    fun stop()
}

/**
 * Real implementation backed by Android [AudioTrack]. Synthesizes a fixed 3s PCM buffer that approximates
 * a plucked guitar string: a fundamental plus several decaying harmonics (1/n^2 amplitudes) under a fast
 * attack + exponential decay envelope. The whole buffer is rendered once and played via MODE_STATIC, so the
 * output is deterministic (no streaming "build-up", no clicks). All audio failures are swallowed (logged) so a
 * missing/blocked output can never crash the tuner UI.
 */
class AudioTrackTonePlayer : TonePlayer {
    @Volatile private var track: AudioTrack? = null

    override fun start(frequencyHz: Double) {
        try {
            stop()
            val sampleRate = 44100
            val durationSec = 3.0
            val n = (sampleRate * durationSec).toInt()
            val buf = ShortArray(n)

            // Plucked-string spectrum: harmonics with 1/n^2 falloff (fundamental dominant, like a real string).
            val partials = listOf(1, 2, 3, 4, 5, 6)
            val amps = partials.map { 1.0 / (it * it) }
            val ampSum = amps.sum()

            // Envelope: ~4ms linear attack, then exponential decay across the 3s tail.
            val attackSamples = (sampleRate * 0.004).toInt()
            val decayRate = 1.9 // exp(-decayRate * t): audible tail that fades by ~3s

            var maxAbs = 1.0
            // First pass: compute raw samples and track peak for normalization.
            val raw = DoubleArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / sampleRate
                val attack = if (i < attackSamples) (i.toDouble() / attackSamples) else 1.0
                val env = attack * kotlin.math.exp(-decayRate * t)
                var s = 0.0
                for (k in partials.indices) {
                    val h = partials[k]
                    s += amps[k] * kotlin.math.sin(2 * Math.PI * h * frequencyHz * t)
                }
                s = (s / ampSum) * env
                raw[i] = s
                val a = kotlin.math.abs(s)
                if (a > maxAbs) maxAbs = a
            }
            // Second pass: normalize to ~90% of full scale to avoid clipping.
            val norm = 0.9 / maxAbs
            for (i in 0 until n) {
                buf[i] = (raw[i] * norm * Short.MAX_VALUE).toInt().toShort()
            }

            val at = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buf.size * 2)
                .build()
            if (at.state != AudioTrack.STATE_INITIALIZED) return
            at.write(buf, 0, buf.size)
            at.play()
            track = at
        } catch (e: Exception) {
            android.util.Log.w("BetterTuner", "tone play failed (non-fatal): ${e.message}")
        }
    }

    override fun stop() {
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {
        }
        track = null
    }
}
