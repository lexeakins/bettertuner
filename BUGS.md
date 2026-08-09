# BetterTuner — Bug Log

Bug reports follow a tight, reproducible harness. Each entry: symptom, root cause, the regression test that pins it, status.

---

## BT-001 — YIN detector returned sampleRate/2 (or null) for all guitar frequencies
- **Reported:** 2026-08-09 (first unit-test run, all 6 YinPitchDetectorTest cases red)
- **Symptom:** `detect()` returned 22050.0 Hz (= sampleRate/2) regardless of input; every guitar string (82–330 Hz) failed.
- **Root cause:** Two defects in `YinPitchDetector`:
  1. Search window was bounded by `maxFrequencyHz` (default 1000) so `tauMax = sampleRate/maxFreq = 44`, which excludes everything below ~1000 Hz. The parameter named the *upper* bound but was used as the *lower* window edge. Fix: search window should be driven by the *minimum* detectable frequency (`tauMax = sampleRate/minFreq`).
  2. The local-minimum descent `while` loop reassigned the `for`-loop `val tau` (compile error) and, as originally written, broke immediately instead of descending — so it grabbed the first tau under threshold (tau=2) rather than the true period. Fix: use a mutable `var t` and actually walk to the local minimum.
- **Regression test:** `YinPitchDetectorTest` (`detects_low_e2_82_41`, `detects_a4_440`, `detects_a2_110_for_drop_d_low_string`, `detects_high_e4_329_63`, `confidence_is_high_for_clean_sine`). All assert the recovered frequency within ±0.5 Hz and `confidence >= 0.9` for clean sines.
- **Status:** FIXED (2026-08-09). Green on commit.

## BT-002 — `Pitch.isInTune` used unimported `absoluteValue`
- **Reported:** 2026-08-09 (compile error during first test run)
- **Symptom:** `Unresolved reference 'absoluteValue'` at Pitch.kt:23.
- **Root cause:** `kotlin.math.absoluteValue` not imported.
- **Regression test:** `NoteConverterTest.isInTune_threshold` (plus compile of `Pitch`).
- **Status:** FIXED (2026-08-09).
