package com.lexeakins.bettertuner.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence seam for [Settings]. The app uses a [SharedPreferences] backing; tests can supply a fake.
 */
interface SettingsStore {
    fun get(): Settings
    fun set(settings: Settings)
    /** User-saved custom tuning presets. */
    fun getSavedTunings(): List<SavedTuning>
    fun saveTuning(tuning: SavedTuning)
    fun deleteTuning(id: String)
    fun renameTuning(id: String, newName: String)
}

private const val KEY_A4 = "a4_hz"
private const val KEY_TOL = "tolerance_cents"
private const val KEY_THEME = "theme"
private const val KEY_CUSTOM = "custom_tunings" // JSON-ish CSV: id|name|spec,;;;


class SharedPreferencesSettingsStore(
    private val prefs: SharedPreferences,
) : SettingsStore {

    constructor(context: Context) : this(
        context.getSharedPreferences("zerobeat_settings", Context.MODE_PRIVATE),
    )

    override fun get(): Settings {
        val theme = when (prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        return Settings(
            a4Hz = prefs.getFloat(KEY_A4, 440f).toDouble(),
            toleranceCents = prefs.getFloat(KEY_TOL, 5f),
            theme = theme,
        )
    }

    override fun set(settings: Settings) {
        prefs.edit().apply {
            putFloat(KEY_A4, settings.a4Hz.toFloat())
            putFloat(KEY_TOL, settings.toleranceCents)
            putString(KEY_THEME, settings.theme.name)
        }.apply()
    }

    override fun getSavedTunings(): List<SavedTuning> {
        val raw = prefs.getString(KEY_CUSTOM, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(";;;").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size < 3) null else SavedTuning(parts[0], parts[1], parts[2])
        }
    }

    override fun saveTuning(tuning: SavedTuning) {
        val current = getSavedTunings().filter { it.id != tuning.id }.toMutableList()
        current.add(tuning)
        writeCustom(current)
    }

    override fun deleteTuning(id: String) {
        writeCustom(getSavedTunings().filter { it.id != id })
    }

    override fun renameTuning(id: String, newName: String) {
        writeCustom(getSavedTunings().map { if (it.id == id) it.copy(name = newName) else it })
    }

    private fun writeCustom(list: List<SavedTuning>) {
        val raw = list.joinToString(";;;") { "${it.id}|${it.name}|${it.spec}" }
        prefs.edit().putString(KEY_CUSTOM, raw).apply()
    }
}
