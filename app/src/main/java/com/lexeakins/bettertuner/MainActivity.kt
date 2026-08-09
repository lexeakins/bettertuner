package com.lexeakins.bettertuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexeakins.bettertuner.audio.AudioTrackTonePlayer
import com.lexeakins.bettertuner.ui.SplashScreen
import com.lexeakins.bettertuner.ui.TunerScreen
import com.lexeakins.bettertuner.ui.TunerViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: TunerViewModel = viewModel {
                        TunerViewModel(tonePlayer = AudioTrackTonePlayer())
                    }
                    // Branded splash on cold start, then the tuner. Keeps the launch feeling intentional.
                    var showSplash by remember { mutableStateOf(true) }
                    LaunchedEffect(Unit) {
                        delay(900)
                        showSplash = false
                    }
                    if (showSplash) SplashScreen() else TunerScreen(viewModel)
                }
            }
        }
    }
}
