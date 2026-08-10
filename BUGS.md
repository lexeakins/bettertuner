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

## BT-006 — Instrumented capture test failed on mic-less emulator; stale test APK masked the fix
- **Reported:** 2026-08-09 (rerunning full test suite per user request, Pixel 10 Pro AVD)
- **Symptom:** `AudioRecordSourceTest.realCapture_producesNonSilentBuffers` FAILED with "expected
  non-silent audio buffers". The AVD has no usable microphone, so `AudioRecord` returned silent buffers.
- **Root cause (two parts):**
  1. The test asserted non-silence unconditionally — wrong for an emulator whose virtual mic is silent by
     design. Correct behavior: skip when no real signal path exists.
  2. Gradle served a **stale cached instrumented APK** across runs, so edits to the skip logic didn't take
     effect until the androidTest intermediates were wiped and rebuilt with `--rerun-tasks --no-build-cache`.
- **Fix:** Rewrote the test to `assumeTrue(!isEmulator())` at the top (skip whole test on emulators), and
  assert frame-size/sample-rate unconditionally (verifiable without a mic). Verified: on the AVD the test
  now reports `skipped=1`, BUILD SUCCESSFUL. On a real phone it asserts non-silence.
- **Regression test:** The skip behavior itself (emulator → skipped). Lesson logged in USER_TESTS.md: when
  editing instrumented tests, force a clean rebuild or a stale test APK can hide the change.
- **Status:** FIXED (2026-08-09).

## BT-007 — DADGAD targets mis-octaved (A3/D4 resolved to A2/D3)
- **Reported:** 2026-08-09 (Slice 2 TunerEngine tests)
- **Symptom:** `Tuning.DADGAD.targets` came out `[D2, A2, D3, G3, A2, D3]` — the 5th/6th strings (A3 220 Hz,
  D4 293 Hz) collapsed to A2/D3 because `NoteConverter.fromFrequency` snaps to the nearest *chromatic* note
  and rounds 220 Hz to MIDI 45 (A2) instead of 57 (A3).
- **Root cause:** tuning targets were built from raw frequencies, so octave was implicitly chosen by rounding.
  Targets need an *explicit* octave.
- **Fix:** Added `NoteConverter.fromMidi(midi)` (exact, no rounding) and rebuilt all tunings from explicit
  MIDI numbers. DADGAD now `[D2, A2, D3, G3, A3, D4]`. `fromMidi` is the correct primitive for any target table.
- **Regression test:** `TunerEngineTest.tuningPresets_haveExpectedTargets` asserts exact DADGAD labels.
- **Status:** FIXED (2026-08-09).

## BT-008 — TuneLockDetector long underflow prevented bell from ever firing
- **Reported:** 2026-08-09 (Slice 3 JVM tests)
- **Symptom:** `TuneLockDetector.onState` never returned true on the first lock; `TuneLockDetectorTest`
  (firesOnRisingEdgeIntoTune / rearmsAfterWindow) failed.
- **Root cause:** `lastRewardAt` initialized to `Long.MIN_VALUE`; the rearm check `now - lastRewardAt >=
  rearmMillis` **overflowed** (1 - Long.MIN_VALUE wraps to a negative long), so the condition was always
  false → the reward bell could never sound.
- **Fix:** initialize `lastRewardAt = -rearmMillis` so the first lock is always inside the rearm window.
- **Regression test:** `TuneLockDetectorTest` (4 cases) pins rising-edge, hold, rearm, and out-between-locks.
- **Status:** FIXED (2026-08-09).

## BT-009 — AudioRecordSource emitted short buffers; detection silently failed
- **Reported:** 2026-08-09 (Slice 3 HITL, user's Motorola razr; also caught by instrumented test on phone)
- **Symptom:** After the app's first run, the readout stayed "—" and no note was detected. The JVM pipeline
  test passed because the Fake always emits exactly 4096 samples.
- **Root cause:** `AudioRecordSource` emitted `FloatArray(read)` where `read` is the *actual* samples
  returned by `AudioRecord.read` (often < the requested 4096). YIN needs `n >= 2*tauMax+1`, so short buffers
  returned null → no detection. The instrumented capture test on the real phone failed with
  "every captured buffer should be the requested frame size".
- **Fix:** Emit a full `framesPerBuffer`-sized FloatArray, zero-padding short reads, so the engine always
  gets a consistent detection window.
- **Regression test:** `AudioRecordSourceTest` now passes on the real phone (motorola razr 2024, API 15) —
  non-silent, correctly-sized 4096-sample buffers at 44.1 kHz. (Runs on emulator only as SKIP.)
- **Status:** FIXED (2026-08-09).

## BT-010 — Reward bell / tone AudioTrack could crash the app on lock-in
- **Reported:** 2026-08-09 (Slice 3 HITL: "app closed on first pluck", then silent on relaunch)
- **Symptom:** App closed the moment a string locked in tune (bell fired), and on relaunch made no sound at
  all (bell + center-tap tone both dead).
- **Root cause:** `playRewardBell` built + played an `AudioTrack` on the main/Compose thread with no
  guard; a `STATE_UNINITIALIZED` or config exception threw and killed the process. The center-tone
  `AudioTrackTonePlayer` had the same unguarded path.
- **Fix:** Moved bell synthesis/play to a background thread, wrapped in try/catch (failures logged, never
  thrown), and gated on `AudioTrack.state == STATE_INITIALIZED`. Applied the same defensive pattern to
  `AudioTrackTonePlayer` (explicit attributes, MODE_STREAM, swallow+log on failure). Audio path can no
  longer crash the UI.
- **Regression test:** instrumented capture test confirms the audio subsystem initializes on the phone;
  unit-level coverage pending (AudioTrack is Android-runtime, covered by HITL).
- **Status:** FIXED (2026-08-09).

## BT-011 — Swipes, string-strip tap, and center-tap tone all dead (gesture wiring)
- **Reported:** 2026-08-09 (Slice 3 HITL: user noted "none of the swipe actions work" and center long-press
  tone never played — the long-press clue showed the tone handler was never invoked).
- **Root cause (3 distinct defects in TunerScreen):**
  1. Center tone: the `Box.pointerInput` only registered `detectHorizontal/VerticalDragGestures` with
     EMPTY drag lambdas — there was no tap/long-press handler, so `startReferenceTone()` was never called.
  2. String strip swipes: it used a `LazyColumn`, which consumes vertical drags for its own scrolling, so
     our `detectVerticalDragGestures` never fired; and it treated each *incremental* delta (>40px) as a
     swipe, causing no-op/chaotic cycling instead of one cycle per gesture.
  3. (Audio config, see BT-010 corollary) bell used `USAGE_ASSISTANCE_SONIFICATION` + `MODE_STATIC` which
     is routed away/muted on the test device; switched to `USAGE_MEDIA` + `MODE_STREAM` (proven by probe).
- **Fix:**
  - Center readout wrapped in a `Box` with `detectTapGestures { onPress -> startReferenceTone(); awaitRelease(); finally stopReferenceTone() }`.
  - String strip = plain `Column` (no scroll consumption) with accumulated `accX`/`accY` committed once on drag end (>40px) → single cycle in the dominant axis; tap still selects via `clickable`.
  - Bell + `AudioTrackTonePlayer` now `USAGE_MEDIA`/`MODE_STREAM`; tone player loops a 0.25s buffer while held for a sustained tone.
- **Regression test:** none automated (gesture+audio are HITL/device); verified by building + instrumented
  audio-output probe (passes on real phone). Re-test checks #5/#6 pending user HITL.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-012 — Bell chattered at the in-tune threshold (no hysteresis)
- **Reported:** 2026-08-09 (Slice 3 HITL: "it plays sometimes multiple times fast when ... on the edge ...
  back and forth between in tune and flat/sharp").
- **Root cause:** `TuneLockDetector.onState(tunerState)` fired on *every* re-entry into the ±5¢ band, so a
  reading jittering across the threshold dinged repeatedly.
- **Fix:** Added **hysteresis** — once locked, the string must exceed tolerance + a 4¢ deadband
  (`|cents| > 9¢`) before it can unlock and re-trigger. Signature became `onState(inTune, cents, now)`; the
  ViewModel passes `tunerState.inTune` + `tunerState.cents`.
- **Regression test:** `TuneLockDetectorTest.hysteresisPreventsThresholdChatter` + `hysteresisStaysLockedWithinDeadband`.
- **Status:** FIXED (2026-08-09).

## BT-013 — Tone placement hijacked swipes; harsh timbre
- **Reported:** 2026-08-09 (Slice 3 HITL: check #5 fail — swiping the strip played the long-press tone instead
  of cycling; check #6 tone "unpleasant / aggressive, high attack").
- **Root cause:** (a) center readout's `pointerInput` fired `startReferenceTone()` on any touch, so a swipe
  gesture on the strip was read as a long-press and played the tone, blocking the swipe. (b) The reference
  tone was a raw sine at 0.6 amplitude with an abrupt on/off — harsh.
- **Fix (per user):** moved the tone OFF the center readout. Left-menu notes now do **tap = short preview
  (~350ms)** and **press-and-hold = sustained tone** (onRelease stops). Swipes on the strip cycle cleanly with
  no tone. Tone timbre softened: 0.35 amplitude, a `sin(π·i/n)` attack/release envelope across each loop
  buffer (no clicks), plus a gentle 2nd harmonic for warmth.
- **Regression test:** none automated (gesture/audio HITL). Re-test checks #5/#6 pending user HITL.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-014 — Regression: engine never started when mic permission already granted
- **Reported:** 2026-08-09 (Slice 3 HITL: after BT-012/BT-013 changes, "not detecting sound; audio not playing").
- **Root cause:** `viewModel.startEngine()` was called ONLY inside the permission `launcher` callback. On a
  fresh launch where `RECORD_AUDIO` was already granted (the common case after first grant), `showRationale`
  was false, the tuner rendered, but the engine never started → no capture (readout stuck "—") and no tones.
  The device audio + capture path were fine (instrumented capture test still passed on the phone); the bug was
  the start trigger.
- **Fix:** added `LaunchedEffect(hasPermission) { if (hasPermission) viewModel.startEngine() }` so the engine
  starts whenever permission is present at launch. `startEngine()` is idempotent (`engine != null` guard).
- **Regression test:** none automated (HITL); instrumented `AudioRecordSourceTest`/`AudioOutputProbeTest`
  still green on the real phone, confirming capture+output subsystems are intact. Consider a UI test that
  asserts `TunerEngine` starts when permission is pre-granted.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-015 — Swipe-to-cycle conflicted with per-note tap/hold gestures
- **Reported:** 2026-08-09 (Slice 3 HITL: "still can't swipe through strings").
- **Root cause:** swipe `pointerInput` lived on the `StringStrip` Column while each note `Surface` had its own
  `detectTapGestures` + `clickable`. The nested gesture filters swallowed the drag before the strip handler
  saw it, so swipes never cycled.
- **Fix:** moved swipe-to-cycle to the outer `Box` in `TunerScreen` (accumulate delta, commit one cycle on
  drag-end >40px). `StringStrip` now handles only per-note tap (preview) / press-hold (sustained tone) with no
  `clickable` competing for the gesture.
- **Regression test:** none automated (gesture HITL). Re-test check #5 pending user HITL.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-016 — Swipe direction inverted (down should go to a higher string)
- **Reported:** 2026-08-09 (Slice 3 HITL polish: "if I'm on A2, swipe down I expect to go to B; it goes to E2").
- **Root cause:** swipe-down produced a negative cycle delta (toward low E) instead of positive (toward high e).
- **Fix:** swipe maps `acc < 0 -> -1` (up = lower), `acc > 0 -> +1` (down = higher). `cycleString(+1)` = index+1
  (toward high e). Covered by `TunerViewModelCycleTest.cycleString_directionConvention`.
- **Regression test:** `TunerViewModelCycleTest` (2 cases). JVM suite now 34 tests green.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-017 — Auto mode locked but did not advance the UI to the next string
- **Reported:** 2026-08-09 (Slice 3 HITL polish: "auto mode works... but it does not advance the UI").
- **Root cause:** `TuneLockDetector` fired the bell, but nothing consumed the lock to move the target forward.
- **Fix:** in the engine-state collect, on a lock edge in auto mode the VM marks the string `tunedStrings`,
  advances `selectedTargetIndex` to `(idx+1) % size` (staying in auto), and reflects it into the engine. Manual
  pick still flips to Manual (auto wins only until you interact).
- **Regression test:** auto-advance is HITL (needs live detection); direction/tuned-set logic covered by VM tests.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-018 — Top UI redesign + whole-instrument tuning progress
- **Reported:** 2026-08-09 (Slice 3 HITL polish): top bar was a mess (title cramped, stray settings glyph,
  tunings with no label, Auto toggle randomly placed). Also requested visual progress: grey + green check on
  each tuned string.
- **Fix:**
  - Top: row 1 = "BetterTuner" title (left) + settings gear (right). Row 2 = "Tuning:" dropdown (presets) +
    Auto switch, spaced via SpaceBetween.
  - String strip: each note greys to alpha 0.35 once detected in-tune (`tunedStrings`), with a full-opacity
    green check overlay at the right — whole-guitar progress as you tune.
- **Regression test:** none automated (visual HITL). Re-test checks #4/#5/#6 + new progress visuals pending HITL.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-019 — Settings were a non-functional stub
- **Reported:** 2026-08-09 (Slice 3 polish follow-up: "make the settings do things").
- **Root cause:** `SettingsScreen` only printed static text ("A4 reference: 440 Hz", etc.); nothing was wired to
  engine state or persisted.
- **Fix:**
  - `NoteConverter` A4 made configurable (`fromFrequency`/`midiToFrequency`/`fromMidi` accept `a4Hz`).
  - `Tuning` builders take `a4Hz` (`standard/dropD/dadgad` + `byName`); `STANDARD/DROP_D/DADGAD` kept as 440 defaults.
  - New `settings` package: `Settings` model (a4Hz, toleranceCents, theme) + `SettingsStore` (SharedPreferences
    impl, injectable). `ThemeMode` enum (SYSTEM/LIGHT/DARK).
  - `TunerViewModel.updateSettings { }` persists via the store and rebuilds the engine with the new A4/tolerance
    (reshapes target frequencies; stricter/looser in-tune gate).
  - `MainActivity` applies `theme` via light/dark ColorScheme; `TunerScreen.SettingsScreen` is now real: A4
    slider (400–460 Hz), tolerance slider (1–20¢), theme segmented control. Preferences persist across launches.
- **Regression test:** `TunerViewModelCycleTest.updateSettings_persistsAndReshapesTargets` asserts A4=432 -> E2=80.9 Hz
  and tolerance persists. JVM suite green.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify (sliders feel, theme applies, persistence survives restart).

## BT-020 — Auto-advance fired on a single transient frame (wrong/early string lock)
- **Reported:** 2026-08-09 (HITL: while tuning one string and re-plucking to confirm, auto mode advanced to the
  next string; sometimes it locked the *wrong* string as in-tune, e.g. plucking low E but it decided A was tuned).
- **Root cause:** `collectEngine` advanced + dinged the instant `inTune` became true for one frame. A transient
  lock (re-pluck, harmonic, or a different auto-selected target briefly reading in-tune) advanced immediately.
- **Fix:** replaced the single-frame lock with a **stable-dwell confirmation**. `inTuneSinceMs` accumulates while
  the current target is continuously in tune; a string is only marked tuned + advanced + dinged after `DWELL_MS`
  (600ms) of uninterrupted in-tune. Leaving tune (or the target changing) resets the timer, so a wrong-string or
  one-frame lock can't accumulate the dwell. `lockProgress` (0..1) is exposed and shown as a settling bar on the
  selected note so the user sees confirmation happening before the advance. `TuneLockDetector` is no longer used
  by the VM (kept + still unit-tested).
- **Regression test:** logic is timing/flow-based; covered by HITL (re-pluck to confirm no early advance, wrong
  string no longer falsely locks). Consider a VM test driving the engine with synthetic in-tune frames < DWELL_MS
  to assert no advance.
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-021 — Auto mode advanced the focused string for a *different* string's in-tune lock
- **Reported:** 2026-08-09 (HITL follow-up to BT-020): focused on E2, plucking a tuned A2 (or G2) still marked E2
  as in tune and advanced it. Any in-tune note (not the focused one) confirmed + advanced the focused string.
- **Root cause:** BT-020's dwell ran on `tunerState.inTune` regardless of *which* note was detected. In auto mode the
  engine's `target` is the nearest detected note, so plucking a tuned A while focused on E reported "A in tune" and
  the VM confirmed the focused index (E) for it.
- **Fix:** gate the dwell on the detected note matching the **focused** string — `target.label == focusedTarget.label`.
  The readout still shows the detected note (informative), but only the focused string's own in-tune state can
  confirm + advance. A tuned off-focus string no longer advances the focused one.
- **Regression test:** flow-based; covered by HITL (focus E2, pluck tuned A2/G2 -> E does NOT advance).
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-022 — Readout misreported "IN TUNE" (green) for an off-focus string
- **Reported:** 2026-08-09 (HITL after BT-021): advance no longer fires for a wrong string (good), but the center
  readout still said "IN TUNE" / green when a *different* tuned string was plucked, because the engine snaps its
  target to the nearest note. Evaluating against "any string" is misleading when tuning one string at a time.
- **Fix (display/status only; engine + advance logic untouched):** the center readout now evaluates against the
  **focused** string. `isFocusedNote = detected.label == focusedTarget.label`. Status logic:
  - off-focus note whose cents-from-focused > 200¢ -> "WRONG STRING" (amber), needle deflects vs focused target.
  - focused note: IN TUNE (green) / FLAT (blue) / SHARP (red) as before.
  - off-focus note still near the focused target -> shows the detected note name (neutral), no false IN TUNE.
  - Needle + TARGET Hz now reference the focused target, so a wrong string no longer centers the needle / greens out.
- **Regression test:** visual HITL (focus E2, pluck tuned A2/G2/B3 -> amber "WRONG STRING", no green).
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-023 — Custom tunings + 10 presets; found latent `setTuning` discards custom pitches
- **Requested:** 2026-08-09 — add common tunings + a custom-tuning entry (6 fields) with per-string safety
  warnings and saved presets (save/rename/delete). Two warning severities: >2 semitones above Standard = SOFT
  (amber); >=3 = HARD (red: breakage/neck-warp risk). Allowed but warned.
- **Latent bug found:** `setTuning(Tuning)` rebuilt targets via `Tuning.byName(tuning.name)`, discarding the
  passed object. A custom tuning named "Custom" fell through to `standard()` -> applying any custom/saved tuning
  silently reset to Standard. Fixed by extracting `applyTuningObject(tuning)` that uses the object as-is;
  `setTuning(name)` (presets) delegates via `byName`, custom path calls `applyTuningObject` directly.
- **Implementation:**
  - `NoteConverter.parseNote` (letter+accidental+octave, any octave, flats normalized to sharps).
  - `TuningParser`: validates 6 specs, computes per-string `WarningLevel` vs `STANDARD_REFERENCE` (EADGBE).
  - `Tuning.presets(a4Hz)`: 10 built-ins (Standard, Half-step down, Drop D, Double Drop D, DADGAD, Open D/G/E/C,
    New Standard) — all within safe ranges.
  - `SettingsStore` persists `List<SavedTuning>` (CSV); VM `saveCurrentCustom/renameTuning/deleteTuning/applySavedTuning`.
  - `CustomTuningDialog`: 6 fields, live per-string amber/red warning, Apply (always enabled), Save/Rename/Delete.
  - Dropdown lists presets + saved customs + "Custom…".
  - Variable string count (ukulele, etc.) deferred — noted as future multi-instrument architecture work.
- **Regression tests:** `TuningParserTest` (parse, accidentals, warning thresholds, per-string); `TunerViewModelCustomTest`
  (apply/parse/save/rename/delete/applySaved). JVM suite green.
- **Status:** IMPLEMENTED (2026-08-09) — pending HITL re-verify.
- **HITL re-test (2026-08-09) found 3 gaps — fixed in commit <sha at delivery>:**
  1. **Preset dropdown only applied DADGAD** (others fell back to Standard). Root cause: `Tuning.byName()`
     only mapped Standard/Drop D/DADGAD; every other preset name fell through `else -> standard()`. Fixed by
     mapping all 10 preset names. Unit test `byName_maps_all_presets` locks it.
  2. **Delete was undiscoverable** — it lived only inside the Custom dialog, which resets `currentSavedId` on
     reopen, so the Delete/Rename buttons were unreachable in normal flow. Fixed: saved presets now show a
     **trash + pencil icon row directly in the dropdown** ("Your saved tunings" section). Delete is one tap.
  3. **Soft (+2) warning seemed absent** — code was correct (unit test `warning_tuningUpTwoSemitones_isSoft`
     passes). User had tested a note in the wrong slot (G3 in the high-e field = down-tuning = correctly no
     warning). Fixed by surfacing each field's Standard baseline ("String 6 (low) · std E2") so the comparison
     is obvious. Re-verify via HITL-2026-08-09b.
- **Regression tests added:** `byName_maps_all_presets`, `warning_tuningUpTwoSemitones_soft_is_visible`.

## BT-025 — Custom dialog shows red error on empty/unfinished input
- **Reported:** 2026-08-09 (HITL-2026-08-09b, user image) — opening the Custom dialog (or after cancel/reopen)
  showed a red "Invalid note / enter exactly 6 notes" error immediately, even before typing. Persisted on retry.
- **Root cause:** `TuningParser.parse` treated blank fields as a hard error ("exactly 6 notes"), so the dialog's
  bottom error banner (`if (result.error != null)`) fired on first open / whenever any field was still empty.
- **Fix:** blank fields are now "incomplete, not an error" — `parse` returns `pitches=null` but `error=null` when
  any field is blank. Only a *non-blank but malformed* note (e.g. "X3") sets `error`. Apply stays correctly
  disabled until all 6 are valid; no red text while the user is still entering. `isError` on each field already
  gated on `value.isNotBlank()`, so empty fields never show error state.
- **Regression test:** `parse_blankNote_isIncomplete_notError` (asserts ok=false AND error=null for a blank field).
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-026 — Custom dialog warnings must evaluate per-field, not wait for all 6 filled
- **Reported:** 2026-08-09 (follow-up to BT-025) — entering a note in one field (e.g. E4 in the low-E slot) showed
  no warning while the other 5 were still blank, because `parse()` short-circuited to "incomplete" and returned no
  warnings at all. User: evaluate one field at a time, not the total.
- **Fix:** `TuningParser.parse` now computes a per-field `warnings` list for every field independently: blank ->
  NONE (keeps `ok=false` so Apply stays disabled, but no longer suppresses other fields' warnings); valid ->
  its own WarningLevel vs that string's Standard reference; malformed non-blank -> NONE for that field + sets `error`.
  `ok` is true only when all 6 are valid; `error` only on a malformed non-blank note.
- **Note (verified):** E4 in the low-E slot is +24 semitones above E2 -> correctly HARD (red), not SOFT. The
  user's image showed exactly that; per-field eval now flags it immediately. Use F#2 for a +2 SOFT demo.
- **Regression test:** `parse_warns_perField_even_with_blanks` (F#2 in low slot with others blank -> SOFT on
  field 0, ok=false, error=null).
- **Status:** FIXED (2026-08-09) — pending HITL re-verify.

## BT-027 — Polish: dropdown icon crowding, strip tap-to-select, replace hold-tone with a 3s guitar pluck
- **Reported:** 2026-08-10 (HITL passing) — three polish items:
  1. Saved-tuning rows in the dropdown had always-visible edit/delete icons that ran into the label text.
  2. Tapping a letter in the strip only played a sound; it should ALSO switch to manual mode and change string focus.
  3. Long-press tone was quiet and "built up" inconsistently; replace with a resonant ~3s complete note that
     emulates a plucked guitar string.
- **Fixes:**
  1. Removed the always-visible icon row. Saved rows now respond to **long-press** (via combinedClickable) which
     opens a confirm `AlertDialog` with Rename / Delete (label stays fully legible). Single tap still applies.
  2. Strip note is now `.clickable { onSelect(i); onPreviewTone(...) }` — tap selects that string (manual mode,
     focus changes via `selectString`) AND previews its target tone. Removed the hold-to-sustain gesture entirely.
  3. `TonePlayer` rewritten as a deterministic 3s pluck: a fixed 44.1kHz PCM buffer (MODE_STATIC) = fundamental +
     6 harmonics with 1/n^2 amplitudes under a ~4ms attack + exponential decay, normalized to ~90% FS. Plays once,
     self-terminating; `stop()` cuts early. No streaming/ramp "build-up". The lock-in bell (`playRewardBell`) is
     unchanged (its own 180ms ding).
- **Regression:** interface unchanged (still `start`/`stop`); existing fake implementations in tests unaffected.
  Manual HITL checklist (HITL-2026-08-10) covers the three behaviors.
- **Status:** FIXED (2026-08-10) — pending HITL re-verify.

## BT-028 — No audible tone on tap; long-press did nothing; dropdown interaction rework
- **Reported:** 2026-08-10 — (a) tapping a strip note produced no audible sound; (b) long-pressing a saved
  tuning in the dropdown did nothing.
- **Root causes:**
  - (a) The MODE_STATIC AudioTrack from BT-027 failed to initialize on the user's device (state != INITIALIZED),
    so play() never ran and nothing was audible. The earlier streaming (MODE_STREAM) path worked fine.
  - (b) The long-press handler was wired to a `pendingSavedId` AlertDialog, but the SwipeToDismiss/combinedClickable
    wiring was inconsistent and never surfaced; user also wanted a different interaction model.
- **Fixes:**
  - (a) `TonePlayer` reverted to the proven MODE_STREAM path, keeping the new plucked-string envelope (fundamental
    + 6 harmonics 1/n^2, 4ms attack + exp decay, normalized). The full 3s buffer streams once and the track stops
    on completion. Deterministic, audible, no build-up.
  - (b) Saved-tuning rows reworked per user's preferred model: **swipe left (end-to-start) reveals a red delete
    background** (SwipeToDismissBox) that deletes on dismiss; **long-press = rename** (opens the Custom dialog
    pre-loaded). Single tap still applies the tuning. Removed the dead pendingSavedId AlertDialog.
- **Regression:** interface unchanged; JVM suite green. HITL-2026-08-10b covers tap-sound + swipe-delete + long-press-rename.
- **Status:** FIXED (2026-08-10) — pending HITL re-verify.
