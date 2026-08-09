package com.lexeakins.bettertuner.audio

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented capture-layer test. Runs ONLY on a device/emulator with a microphone — it verifies the
 * real [AudioRecordSource] produces non-silent, correctly-sized buffers at the declared sample rate.
 * It does NOT judge pitch (that is covered deterministically by [AudioToPitchPipelineTest]).
 *
 * Requires a build with recording permission granted (handled by [GrantPermissionRule]).
 */
@RunWith(AndroidJUnit4::class)
class AudioRecordSourceTest {

    @get:Rule
    val grantPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun realCapture_producesNonSilentBuffers() {
        val source = AudioRecordSource()
        val latch = CountDownLatch(3)
        val buffers = Collections.synchronizedList(mutableListOf<FloatArray>())

        source.start { buf ->
            buffers.add(buf.copyOf())
            latch.countDown()
        }

        assertTrue("timed out waiting for audio buffers", latch.await(5, TimeUnit.SECONDS))
        source.stop()

        assertEquals(44100, source.sampleRateHz)
        val hasSignal = buffers.any { b -> b.maxOf { kotlin.math.abs(it) } > 0.001f }
        assertTrue("expected non-silent audio buffers from the microphone", hasSignal)
        assertTrue("every captured buffer should be the requested frame size", buffers.all { it.size == AudioRecordSource.DEFAULT_FRAMES })
    }
}
