package com.lexeakins.bettertuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexeakins.bettertuner.audio.AudioTrackTonePlayer
import com.lexeakins.bettertuner.settings.SettingsStore
import com.lexeakins.bettertuner.settings.SharedPreferencesSettingsStore
import com.lexeakins.bettertuner.settings.ThemeMode
import com.lexeakins.bettertuner.ui.SplashScreen
import com.lexeakins.bettertuner.ui.TunerScreen
import com.lexeakins.bettertuner.ui.TunerViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsStore = remember { SharedPreferencesSettingsStore(this) }
            val viewModel: TunerViewModel = viewModel {
                TunerViewModel(
                    tonePlayer = AudioTrackTonePlayer(),
                    settingsStore = settingsStore,
                )
            }
            // Branded splash on cold start, then the tuner. Keeps the launch feeling intentional.
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(900)
                showSplash = false
            }
            val theme = viewModel.uiState.value.settings.theme
            val darkTheme = when (theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showSplash) SplashScreen() else TunerScreen(viewModel)
                }
            }
        }
    }
}
