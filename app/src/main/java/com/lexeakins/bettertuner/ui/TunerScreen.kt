package com.lexeakins.bettertuner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexeakins.bettertuner.tuner.TuneDirection
import com.lexeakins.bettertuner.tuner.Tuning
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(viewModel: TunerViewModel) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var showRationale by remember { mutableStateOf(!hasPermission) }
    var showSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        showRationale = !granted
        if (granted) viewModel.startEngine()
    }

    // Reward bell: edge-triggered ding when a string locks.
    LaunchedEffect(ui.rewardBell) {
        if (viewModel.consumeRewardBell()) playRewardBell(context)
    }

    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false },
            onTheme = { /* theme persists via DataStore/Settings; v1: restart not required */ },
        )
        return
    }

    if (showRationale && !hasPermission) {
        RationaleScreen(
            onAllow = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            onDenied = { showRationale = false }, // stay usable, no readout
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BetterTuner") },
                actions = {
                    TuningSelector(ui.tuning) { viewModel.setTuning(it) }
                    TextButton(onClick = { showSettings = true }) { Text("⚙") }
                },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Center tap plays the target reference tone while held.
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { viewModel.stopReferenceTone() },
                    ) { _, _ -> }
                    detectVerticalDragGestures(
                        onDragEnd = { viewModel.stopReferenceTone() },
                    ) { _, _ -> }
                },
        ) {
            // LEFT EDGE: EADGBE strip (tappable + swipeable), for left-hand thumb.
            StringStrip(
                tuning = ui.tuning,
                selectedIndex = ui.selectedTargetIndex,
                onSelect = { viewModel.selectString(it) },
                onCycle = { viewModel.cycleString(it) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(0.8f)
                    .width(72.dp),
            )

            // CENTER: big note, needle, frequency compare.
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val t = ui.tuner
                Text(
                    t.detected?.label ?: "—",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (t.inTune) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onBackground,
                )
                val cents = t.cents.toFloat()
                val needleX = (cents / 50f).coerceIn(-1f, 1f) // ±50¢ full sweep
                NeedleGauge(deflection = needleX, inTune = t.inTune)
                Text(
                    t.directionLabel,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (t.direction) {
                        TuneDirection.LOW -> Color(0xFF1565C0)
                        TuneDirection.HIGH -> Color(0xFFC62828)
                        TuneDirection.IN_TUNE -> Color(0xFF2E7D32)
                    },
                )
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DETECTED", fontSize = 12.sp)
                        Text("%.1f Hz".format(t.detected?.frequencyHz ?: 0.0), fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TARGET", fontSize = 12.sp)
                        Text("%.1f Hz".format(t.target?.frequencyHz ?: 0.0), fontSize = 18.sp)
                    }
                }
                Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto", fontSize = 16.sp)
                    Switch(checked = ui.autoMode, onCheckedChange = { viewModel.setAutoMode(it) })
                }
                if (!hasPermission) {
                    Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Enable mic")
                    }
                }
            }
        }
    }
}

@Composable
private fun NeedleGauge(deflection: Float, inTune: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 32.dp)) {
        val w = size.width
        val midX = w / 2
        val y = size.height / 2
        // center green band for +/-5% of sweep
        val band = w * 0.05f
        drawLine(Color(0xFF2E7D32), Offset(midX - band, y), Offset(midX + band, y), strokeWidth = 6f)
        // needle
        val nx = midX + deflection * (w / 2 - 8f)
        drawLine(
            if (inTune) Color(0xFF2E7D32) else Color(0xFF9E9E9E),
            Offset(midX, y),
            Offset(nx, y),
            strokeWidth = 4f,
        )
        drawCircle(if (inTune) Color(0xFF2E7D32) else Color(0xFF9E9E9E), 6f, Offset(nx, y))
    }
}

@Composable
private fun StringStrip(
    tuning: Tuning,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onCycle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, drag ->
                    if (abs(drag) > 40) onCycle(if (drag < 0) 1 else -1)
                }
                detectHorizontalDragGestures { _, drag ->
                    if (abs(drag) > 40) onCycle(if (drag < 0) 1 else -1)
                }
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(tuning.targets) { i, target ->
            val selected = i == selectedIndex
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(i) },
            ) {
                Text(
                    target.label,
                    fontSize = 22.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TuningSelector(current: Tuning, onPick: (Tuning) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // Simplified: a row of three buttons (Standard / Drop D / DADGAD).
    Row {
        for (t in listOf(Tuning.STANDARD, Tuning.DROP_D, Tuning.DADGAD)) {
            val active = t.name == current.name
            OutlinedButton(
                onClick = { onPick(t) },
                modifier = Modifier.padding(horizontal = 2.dp).height(36.dp),
            ) {
                Text(t.name, fontSize = 12.sp, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun RationaleScreen(onAllow: () -> Unit, onDenied: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("BetterTuner needs the microphone to hear your guitar.", fontSize = 20.sp, textAlign = TextAlign.Center)
        Text("Nothing is recorded or sent anywhere. Audio is processed on your device only.", fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = onAllow, modifier = Modifier.padding(top = 24.dp)) { Text("Allow microphone") }
        TextButton(onClick = onDenied) { Text("Continue without mic") }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit, onTheme: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        // v1: theme note + A4 reference + tolerance placeholders (full controls in a later slice).
        Text("Theme: System (Light/Dark toggle — coming in Settings v2)", fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp))
        Text("A4 reference: 440 Hz", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text("In-tune tolerance: ±5 cents", fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Back") }
    }
}

/** Play a short, satisfying reward "ding" — fully synthesized on-device, no assets, no network. */
private fun playRewardBell(context: android.content.Context) {
    val sampleRate = 44100
    val durationMs = 180
    val n = (sampleRate * durationMs) / 1000
    val buf = ShortArray(n)
    // Two-tone pleasant bell: 880 Hz + 1320 Hz with a fast decay.
    for (i in 0 until n) {
        val t = i.toDouble() / sampleRate
        val env = kotlin.math.exp(-t * 18)
        val s = (kotlin.math.sin(2 * Math.PI * 880 * t) + 0.6 * kotlin.math.sin(2 * Math.PI * 1320 * t)) * env
        buf[i] = (s * 0.7 * Short.MAX_VALUE).toInt().toShort()
    }
    val audioTrack = android.media.AudioTrack.Builder()
        .setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setAudioFormat(
            android.media.AudioFormat.Builder()
                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(android.media.AudioTrack.MODE_STATIC)
        .build()
    audioTrack.write(buf, 0, buf.size)
    audioTrack.play()
}
