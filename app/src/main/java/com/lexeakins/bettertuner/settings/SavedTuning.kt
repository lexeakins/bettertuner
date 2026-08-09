package com.lexeakins.bettertuner.settings

/**
 * A user-defined tuning preset. [spec] is the 6 note specs (low->high), e.g. "E2,A2,D3,G3,B3,E4".
 * Stored in [SettingsStore] so custom presets survive restarts and appear in the tuning dropdown.
 */
data class SavedTuning(
    val id: String,
    val name: String,
    val spec: String,
)
