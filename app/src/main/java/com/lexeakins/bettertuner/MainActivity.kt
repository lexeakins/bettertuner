package com.lexeakins.bettertuner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * App entry point. The tuner UI will live here in a later slice; for now it shows a launch-safe
 * placeholder so the activity instantiates and the app does not crash on open.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BetterTunerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Placeholder()
                }
            }
        }
    }
}

@Composable
private fun BetterTunerTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
private fun Placeholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("BetterTuner", style = MaterialTheme.typography.headlineMedium)
        Text("Launching… (tuner UI coming next)", style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview
@Composable
private fun PlaceholderPreview() {
    BetterTunerTheme { Surface { Placeholder() } }
}
