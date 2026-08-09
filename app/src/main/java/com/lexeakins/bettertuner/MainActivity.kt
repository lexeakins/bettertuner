package com.lexeakins.bettertuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexeakins.bettertuner.audio.AudioTrackTonePlayer
import com.lexeakins.bettertuner.ui.TunerScreen
import com.lexeakins.bettertuner.ui.TunerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: TunerViewModel = viewModel {
                        TunerViewModel(tonePlayer = AudioTrackTonePlayer())
                    }
                    TunerScreen(viewModel)
                }
            }
        }
    }
}
