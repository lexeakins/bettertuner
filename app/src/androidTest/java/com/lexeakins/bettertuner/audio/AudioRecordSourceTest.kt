package com.lexeakins.bettertuner.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented capture-layer test. Verifies the real [AudioRecordSource] opens the mic and delivers
 * correctly-shaped buffers at the declared sample rate.
 *
 * This test is meaningful only on a device with a usable microphone. An emulator's virtual mic is
 * typically silent, so the whole test is **skipped** on emulators (detected up front via [isEmulator]).
 * Run it on a real phone to assert non-silent capture. The signal-path correctness on synthetic audio
 * is already covered deterministically by [AudioToPitchPipelineTest] (JVM).
 *
 * Requires RECORD_AUDIO granted (handled by [GrantPermissionRule]).
 */
@RunWith(AndroidJUnit4::class)
class AudioRecordSourceTest {

    @get:Rule
    val grantPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @Test
    fun realCapture_producesNonSilentBuffers() {
        // Skip the whole test on emulators — their virtual mic cannot provide a real signal.
        assumeTrue("no usable microphone on emulator — skipping capture assertion (run on real hardware)", !isEmulator())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val micGranted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        assumeTrue("RECORD_AUDIO not granted — skipping capture assertion", micGranted)

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
        assertTrue("every captured buffer should be the requested frame size", buffers.all { it.size == AudioRecordSource.DEFAULT_FRAMES })
        val hasSignal = buffers.any { b -> b.maxOf { kotlin.math.abs(it) } > 0.001f }
        assertTrue("expected non-silent audio buffers from a real microphone", hasSignal)
    }

    private fun isEmulator(): Boolean = listOf(
        Build.PRODUCT, Build.DEVICE, Build.MODEL, Build.FINGERPRINT, Build.HARDWARE,
    ).any {
        it.contains("sdk", ignoreCase = true) || it.contains("emulator", ignoreCase = true) ||
            it.contains("generic", ignoreCase = true) || it.contains("emu", ignoreCase = true)
    }
}
