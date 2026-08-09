package com.lexeakins.bettertuner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexeakins.bettertuner.settings.SavedTuning
import com.lexeakins.bettertuner.tuner.TuningParser

private val STRING_LABELS = listOf("6 (low)", "5", "4", "3", "2", "1 (high)")

/**
 * Dialog for defining a custom tuning from 6 note specs (low->high). Shows live per-string safety warnings:
 * - >2 semitones above Standard -> SOFT (amber)
 * - >=3 semitones above Standard -> HARD (red: breakage / neck-warp risk). Apply stays enabled either way.
 * "Save" stores the current spec as a named preset; "Delete" removes the applied saved preset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTuningDialog(
    initialSpec: String?,
    savedTunings: List<SavedTuning>,
    currentSavedId: String?,
    parse: (List<String>) -> TuningParser.ParseResult,
    onApply: (List<String>) -> Unit,
    onSave: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialFields = (initialSpec?.split(",")?.map { it.trim() }?.takeIf { it.size == 6 })
        ?: List(6) { "" }
    var fields by remember { mutableStateOf(initialFields) }
    var name by remember { mutableStateOf(savedTunings.firstOrNull { it.id == currentSavedId }?.name ?: "") }

    val result = parse(fields)
    val warnings = result.warnings

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("Custom tuning", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("Enter 6 notes, low string to high (e.g. E2, A2, D3, G3, B3, E4).", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(12.dp))
            STRING_LABELS.forEachIndexed { i, label ->
                val value = fields.getOrElse(i) { "" }
                val warn = warnings.getOrNull(i)
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue -> fields = fields.toMutableList().also { it[i] = newValue.take(4) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    singleLine = true,
                    placeholder = { Text("e.g. E2") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next,
                    ),
                    isError = result.error != null && value.isNotBlank(),
                    trailingIcon = {
                        when (warn) {
                            TuningParser.WarningLevel.HARD ->
                                Text("⚠ 3+ semi", color = Color(0xFFC62828), fontSize = 12.sp)
                            TuningParser.WarningLevel.SOFT ->
                                Text("⚠ 2 semi", color = Color(0xFFF9A825), fontSize = 12.sp)
                            else -> Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    },
                )
            }

            // Per-string warning banner (worst severity wins).
            val worst = warnings.maxOrNull()
            when (worst) {
                TuningParser.WarningLevel.HARD ->
                    Text("⚠ Tuning a string 3+ semitones above standard is high risk of string breakage or neck warping. Proceed only if you know your string gauge.",
                        color = Color(0xFFC62828), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                TuningParser.WarningLevel.SOFT ->
                    Text("⚠ Tuning a string >2 semitones above standard may stress strings/neck. Usually fine, but be mindful.",
                        color = Color(0xFFF9A825), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                else -> Spacer(Modifier.height(8.dp))
            }
            if (result.error != null) {
                Text(result.error ?: "", color = Color(0xFFC62828), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (currentSavedId != null) "Preset name" else "Save as preset (optional)") },
            )

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(
                    enabled = result.ok,
                    onClick = { onApply(fields.map { it.trim() }) },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentSavedId != null) {
                    TextButton(onClick = { onRename(currentSavedId, name.ifBlank { "Custom" }) }) { Text("Rename") }
                    TextButton(onClick = { onDelete(currentSavedId) }) { Text("Delete") }
                } else {
                    TextButton(
                        enabled = result.ok && name.isNotBlank(),
                        onClick = { onSave(name) },
                    ) { Text("Save preset") }
                }
            }
        }
    }
}
