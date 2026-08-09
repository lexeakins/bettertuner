package com.lexeakins.bettertuner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import com.lexeakins.bettertuner.settings.Settings
import com.lexeakins.bettertuner.settings.ThemeMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.lexeakins.bettertuner.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexeakins.bettertuner.tuner.TuneDirection
import com.lexeakins.bettertuner.tuner.Tuning
import kotlin.math.abs

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

    val launcher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        showRationale = !granted
        if (granted) viewModel.startEngine()
    }

    // Start the engine whenever mic permission is already granted (e.g. fresh launch after a prior grant).
    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.startEngine()
    }

    // Reward bell: edge-triggered ding when a string locks.
    LaunchedEffect(ui.rewardBell) {
        if (viewModel.consumeRewardBell()) playRewardBell(context)
    }

    if (showSettings) {
        SettingsScreen(
            settings = ui.settings,
            onA4Change = { viewModel.updateSettings { copy(a4Hz = Settings.clampA4(it)) } },
            onToleranceChange = { viewModel.updateSettings { copy(toleranceCents = Settings.clampTolerance(it)) } },
            onThemeChange = { viewModel.updateSettings { copy(theme = it) } },
            onBack = { showSettings = false },
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
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Row 2: tuning preset selector + auto toggle.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.widthIn(max = 200.dp)) { TuningSelector(ui.tuning) { viewModel.setTuning(it) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Auto", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                    Switch(checked = ui.autoMode, onCheckedChange = { viewModel.setAutoMode(it) })
                }
            }

            // Main area: left strip + center readout.
            Box(
                Modifier
                    .fillMaxSize()
                    // Swipe anywhere (up/down or left/right) to cycle the selected string. Kept on the outer
                    // Box so it never fights the per-note tap/hold gestures inside the strip.
                    .pointerInput(Unit) {
                        var acc = 0f
                        val commit = { dir: Int -> if (abs(acc) > 40f) viewModel.cycleString(dir) }
                        detectVerticalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { commit(if (acc < 0f) -1 else 1) }, // swipe up = lower, down = higher
                        ) { _, dragAmount -> acc += dragAmount }
                        detectHorizontalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { commit(if (acc < 0f) -1 else 1) },
                        ) { _, dragAmount -> acc += dragAmount }
                    },
            ) {
                StringStrip(
                    tuning = ui.tuning,
                    selectedIndex = ui.selectedTargetIndex,
                    tunedStrings = ui.tunedStrings,
                    lockProgress = ui.lockProgress,
                    onSelect = { viewModel.selectString(it) },
                    onPreviewTone = { viewModel.previewTone(it) },
                    onHoldTone = { viewModel.startToneForPitch(it) },
                    onReleaseTone = { viewModel.stopReferenceTone() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(0.8f)
                        .width(72.dp),
                )

                Column(
                    Modifier.fillMaxSize().padding(start = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val t = ui.tuner
                    Column(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
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
                    }

                    if (!hasPermission) {
                        OutlinedButton(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text("Enable mic")
                        }
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
        val band = w * 0.05f
        drawLine(Color(0xFF2E7D32), Offset(midX - band, y), Offset(midX + band, y), strokeWidth = 6f)
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
    tunedStrings: Set<Int>,
    lockProgress: Float = 0f,
    onSelect: (Int) -> Unit,
    onPreviewTone: (Double) -> Unit,
    onHoldTone: (Double) -> Unit,
    onReleaseTone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Plain Column (NOT LazyColumn) so it doesn't consume drags for scrolling. Swipe-to-cycle lives on the
    // outer Box (TunerScreen) so it never conflicts with the per-note tap/hold gestures here.
    Column(
        modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        tuning.targets.forEachIndexed { i, target ->
            val selected = i == selectedIndex
            val tuned = tunedStrings.contains(i)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    // Grey the note out once it's been detected in-tune (whole-instrument progress).
                    .alpha(if (tuned) 0.35f else 1f)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onPreviewTone(target.frequencyHz) },
                            onPress = {
                                try {
                                    onHoldTone(target.frequencyHz)
                                    awaitRelease()
                                } finally {
                                    onReleaseTone()
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
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
                // Green check at full opacity when this string is tuned (overlays the greyed note).
                if (tuned) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "In tune",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp),
                    )
                }
                // Settling bar: fills across the bottom of the *selected* note as it dwells in tune,
                // so you can see the app is confirming before it advances (not jumping early).
                if (selected && lockProgress > 0.01f && !tuned) {
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(lockProgress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(Color(0xFF2E7D32)),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TuningSelector(current: Tuning, onPick: (Tuning) -> Unit) {
    // Labeled dropdown of presets.
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.menuAnchor().height(40.dp)) {
            Text("Tuning: ${current.name}", fontSize = 13.sp)
        }
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (t in listOf(Tuning.STANDARD, Tuning.DROP_D, Tuning.DADGAD)) {
                DropdownMenuItem(
                    text = { Text(t.name) },
                    onClick = { onPick(t); expanded = false },
                )
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
        Text("ZeroBeat needs the microphone to hear your guitar.", fontSize = 20.sp, textAlign = TextAlign.Center)
        Text("Nothing is recorded or sent anywhere. Audio is processed on your device only.", fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
        OutlinedButton(onClick = onAllow, modifier = Modifier.padding(top = 24.dp)) { Text("Allow microphone") }
        TextButton(onClick = onDenied) { Text("Continue without mic") }
    }
}

@Composable
private fun SettingsScreen(
    settings: Settings,
    onA4Change: (Double) -> Unit,
    onToleranceChange: (Float) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("ZeroBeat is free, offline, and collects no data. These preferences are stored only on this device.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(24.dp))
        Text("Reference pitch (A4)", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("%.0f Hz".format(settings.a4Hz), fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
        Slider(
            value = settings.a4Hz.toFloat(),
            onValueChange = { onA4Change(it.toDouble()) },
            valueRange = 400f..460f,
            steps = 60,
        )
        Text("Concert pitch. 440 Hz is standard; some groups tune to 432 or 442.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))
        Text("In-tune tolerance", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("± %.0f cents".format(settings.toleranceCents), fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
        Slider(
            value = settings.toleranceCents,
            onValueChange = { onToleranceChange(it) },
            valueRange = 1f..20f,
            steps = 19,
        )
        Text("How close a note must be to count as in tune. Tighter = stricter.", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))
        Text("Appearance", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (mode in ThemeMode.values()) {
                val selected = settings.theme == mode
                OutlinedButton(
                    onClick = { onThemeChange(mode) },
                    modifier = Modifier.weight(1f),
                    colors = if (selected) ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(mode.name.replaceFirstChar { it.uppercase() })
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}

/**
 * Play a short, satisfying reward "ding" — fully synthesized on-device, no assets, no network.
 * Uses USAGE_MEDIA + MODE_STREAM (verified to play on the test device); never crashes the UI.
 */
private fun playRewardBell(context: android.content.Context) {
    Thread({
        try {
            val sampleRate = 44100
            val durationMs = 180
            val n = (sampleRate * durationMs) / 1000
            val buf = ShortArray(n)
            for (i in 0 until n) {
                val t = i.toDouble() / sampleRate
                val env = kotlin.math.exp(-t * 18)
                val s = (kotlin.math.sin(2 * Math.PI * 880 * t) + 0.6 * kotlin.math.sin(2 * Math.PI * 1320 * t)) * env
                buf[i] = (s * 0.7 * Short.MAX_VALUE).toInt().toShort()
            }
            val track = android.media.AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    android.media.AudioFormat.Builder()
                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(android.media.AudioTrack.MODE_STREAM)
                .build()
            android.util.Log.d("BetterTuner", "reward bell: state=${track.state}")
            if (track.state == android.media.AudioTrack.STATE_INITIALIZED) {
                var off = 0
                while (off < buf.size) {
                    val written = track.write(buf, off, buf.size - off)
                    if (written <= 0) break
                    off += written
                }
                track.play()
                Thread.sleep((durationMs + 50).toLong())
                track.stop()
            }
            track.release()
        } catch (e: Exception) {
            android.util.Log.w("BetterTuner", "reward bell failed (non-fatal): ${e.message}")
        }
    }, "RewardBell").start()
}
