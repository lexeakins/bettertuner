package com.lexeakins.bettertuner.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence seam for [Settings]. The app uses a [SharedPreferences] backing; tests can supply a fake.
 */
interface SettingsStore {
    fun get(): Settings
    fun set(settings: Settings)
}

private const val KEY_A4 = "a4_hz"
private const val KEY_TOL = "tolerance_cents"
private const val KEY_THEME = "theme"

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
}
