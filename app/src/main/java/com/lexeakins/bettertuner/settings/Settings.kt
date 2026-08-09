package com.lexeakins.bettertuner.settings

/**
 * App appearance mode. SYSTEM follows the device setting; LIGHT/DARK force one.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * User-configurable preferences. Persisted via [SettingsStore].
 *
 * - [a4Hz]: reference pitch for A4. Standard concert pitch is 440 Hz; some ensembles use 432/442.
 *   Changing it shifts every target note's frequency.
 * - [toleranceCents]: how close (in cents) a reading must be to count as "in tune".
 * - [theme]: appearance mode.
 */
data class Settings(
    val a4Hz: Double = 440.0,
    val toleranceCents: Float = 5f,
    val theme: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        val DEFAULT = Settings()
        /** A4 is meaningful in the 400–460 Hz range; clamp to avoid nonsense. */
        fun clampA4(hz: Double) = hz.coerceIn(400.0, 460.0)
        /** Tolerance is meaningful 1–20 cents. */
        fun clampTolerance(c: Float) = c.coerceIn(1f, 20f)
    }
}
