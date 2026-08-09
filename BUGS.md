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

## BT-003 — Wrong `AudioSource` contract assertions in test
- **Reported:** 2026-08-09 (slice #1 audio capture, test run)
- **Symptom:** `AudioSourceContractTest.fake_emits_buffer_of_expected_size_and_rate` and `fake_does_not_loop` failed.
- **Root cause:** Test mis-stated the Fake's contract — `start()` does NOT auto-call `stop()` (correct),
  and the "does not loop" assertion logic was inverted. The `FakeAudioSource` was behaving correctly; the
  tests were wrong, not the code.
- **Regression test:** Corrected `AudioSourceContractTest` (asserts `started`, single emit `calls==1`,
  no auto-stop). Pinned the real contract for the seam.
- **Status:** FIXED (2026-08-09).

## BT-004 — `androidx.test:rules` version line
- **Reported:** 2026-08-09 (instrumented test compile)
- **Symptom:** `Could not find androidx.test:rules:1.2.1` — `GrantPermissionRule` unresolved.
- **Root cause:** `rules` is a distinct artifact versioned with the test runner line (1.6.x), not the
  `androidx.test.ext:junit` line (1.2.1). Needed a separate `testRules` version in the catalog.
- **Regression test:** `AudioRecordSourceTest` compiles (GrantPermissionRule resolves).
- **Status:** FIXED (2026-08-09).

## BT-005 — App crashed on launch: missing `MainActivity`
- **Reported:** 2026-08-09 (Slice 1 HITL, Pixel 10 Pro emulator — "BetterTuner keeps stopping")
- **Symptom:** Launch shows "BetterTuner keeps stopping"; `adb logcat -b crash` shows
  `java.lang.ClassNotFoundException: Didn't find class "com.lexeakins.bettertuner.MainActivity"`.
- **Root cause:** `AndroidManifest.xml` declared a launcher `.MainActivity` but the class was never written
  (the manifest was committed before the UI slice existed). The activity couldn't instantiate.
- **Fix:** Added `MainActivity.kt` — `ComponentActivity` with `setContent`, a `MaterialTheme`/`Surface`
  scaffold and a launch-safe placeholder Compose screen. Build + install + launch verified on the emulator
  via `adb` (FATAL count 0, pid running).
- **Regression test:** JVM suite can't cover this (it's an Android runtime wiring issue). Covered by the
  HITL checklist (Slice 1 check #1). Blocker: a manifest must never declare a component that isn't shipped.
- **Status:** FIXED (2026-08-09).
