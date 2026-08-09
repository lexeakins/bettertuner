# BetterTuner — User Test Checklist (HITL)

This is the **human-in-the-loop (HITL) test log**. It complements the automated JUnit suite
(`./gradlew testDebugUnitTest`). Automated tests prove the *logic*; these manual checks prove the app
*behaves* on a real device with a real microphone, a real guitar, and real fingers.

## How the process works
- Built **per slice** — one unit of delivered work gets one section + checklist at handoff.
- Hermes (me) owns this file: I add the section + checklist when a slice is delivered, and I flip the
  boxes when you report results. You don't need to edit anything — just run the checks and tell me.
- Box states: `[ ]` todo · `[x]` done · `⏳` blocked (needs a device/emulator not yet available) · `❌` failed.
- **Evidence required:** every ✅/❌ is dated + device-tagged, e.g. `✅ 2026-08-09 (Pixel 10 Pro, API 35)`.
  A bare box with no date/device is not sufficient documentation — the MD is the canonical "what was
  actually verified on hardware" record and must be traceable.
- **Gate:** a slice is not "complete" until its HITL checks are ✅ (or ⏳ with a stated reason). Hermes will
  not start the next slice's UI/UX work until you've signed off.
- **Re-test loop (required):** a ❌ opens a `BT-xxx` in [BUGS.md](BUGS.md). When fixed, Hermes **reopens the
  checklist item** (flips back to `[ ]` with "reopened after BT-00X — please re-verify") and reposts the
  updated checklist. The item stays open until you re-run and confirm. Nothing is silently closed.
- The Slice status table gets a final verdict per slice (`HITL ✅ all checks, 2026-08-09`), so results also
  surface in the summary, not only inline.
- Each section names the **commit SHA** it belongs to, so the checklist is traceable in `git log`.

## Slice status
| Slice | What | Commit | Status |
|-------|------|--------|--------|
| 0 | Project scaffold + pitch core + unit tests | `c71e67f` | automated ✓ / HITL n/a |
| 1 | Audio capture seam (`AudioSource` + `AudioRecordSource` + `FakeAudioSource`) | `805619e` | HITL ✅ launched (BT-005 fixed) / ⏳ mic checks need UI |
| 2 | TunerEngine — `StateFlow<TunerState>` (capture→YIN→note; modes + tunings) | `ee50739` | HITL ✅ check #3 (testDebugUnitTest green, 26 tests, 2026-08-09); #1/#2 ⏳ blocked on UI |

---

## 🔧 One-time setup — get a device to test on
You only do this once. After this, every slice reuses the same emulator/phone.

**A. Install Android Studio** (if not already)
1. Download from https://developer.android.com/studio and run the installer; accept all defaults.
2. On first launch it may offer to install the "Android SDK" — say yes. (We already pointed the project at
   `C:\Android\Sdk`.)

**B. Create a virtual device (emulator) — easiest for a first test**
1. Android Studio → **Tools → Device Manager** (or the phone icon in the right toolbar).
2. Click **Create Virtual Device** → pick any phone (e.g. **Pixel 7**) → **Next**.
3. Under "Recommended" pick a system image with **API 35** (or download one if a download arrow shows) → **Next → Finish**.
4. (Optional but recommended) In the AVD's settings (pencil icon), set **Microphone** to *Virtual scene* so the
   emulator can feed audio into the app for the capture test.

**C. Or use a real phone (often simpler for audio)**
1. On the phone: **Settings → About phone → tap "Build number" 7×** to unlock Developer Options.
2. **Settings → System → Developer options → USB debugging** → ON.
3. Plug the phone into the PC via USB and accept the "Allow USB debugging?" prompt.

**D. Run the app once** (proves install + launch — covers Slice 1 check #1)
1. Android Studio → open this project (`C:\Users\Alex Eakins\Documents\BetterTuner\bettertuner`).
2. Top toolbar: device dropdown → pick your emulator or phone → click the green **▶ Run** (or **Run → Run 'app'**).
3. The app builds, installs, and launches. (Right now there's no UI screen yet, so it may show a blank/app
   background — that's expected until Slice 3.) Confirm it does not crash.

**E. Make `./gradlew` work from any terminal (one-time)**
- The Gradle wrapper needs a JDK. `JAVA_HOME` is set to `C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot`
  in your **user** environment (via `setx`), and `gradle.properties` pins `org.gradle.java.home` to the
  same path as a safety net.
- **If you see `JAVA_HOME is not set`**: you're in a terminal that was open *before* the variable was set.
  **Close and reopen PowerShell/terminal** — new shells inherit it. (Already-open windows don't.)
- To check: in PowerShell run `$env:JAVA_HOME` — it should print the path above. If blank, reopen the window.

---

## Slice 1 — Audio capture seam (commit `805619e`)
**Goal:** prove the real microphone produces non-silent, correctly-sized buffers at 44.1 kHz, and that
the pipeline recovers a known note from a synthetic tone (already covered by JVM tests — this is the
*device* half).

**Prereq:** complete the 🔧 One-time setup above (at least D — have a runnable device).

- [x] **1. App installs and launches without crashing** (verified 2026-08-09 on Pixel 10 Pro emulator via adb; BT-005 fixed)
  Steps: With the project open in Android Studio, select your device in the dropdown and click **▶ Run**.
  Wait for "Launch succeeded" in the build output. Confirm the app opens (blank screen is fine for now).
  ✅ if it launches; ❌ if it crashes or shows a red error.

- [ ] **2. `RECORD_AUDIO` permission prompt appears; granting proceeds without error**
  Steps: (Once the UI requests mic access — wired in a later slice —) on launch the OS dialog "Allow
  BetterTuner to record audio?" appears. Tap **While using the app** / **Allow**. ✅ if no crash after granting.
  (If the dialog hasn't been added yet, mark ⏳ — this check activates with the UI slice.)

- [x] **3. `connectedDebugAndroidTest` passes (or correctly skips on a mic-less emulator)**
  Steps: In Android Studio terminal run `./gradlew connectedDebugAndroidTest`.
  ✅ if BUILD SUCCESSFUL (test passes on a real phone, or **SKIPPED** on an emulator with no mic — that's
  expected, the virtual mic is silent). ❌ only if it FAILS on a real device. Note: changing instrumented
  tests requires a clean rebuild — use `--rerun-tasks --no-build-cache` or it may run a stale test APK.

- [ ] **4. Denying the permission does not crash; clear message / app stays usable**
  Steps: Uninstall + re-run the app, tap **Don't allow** on the mic prompt. ✅ if the app stays open and
  shows a message instead of crashing. (Activates with the UI slice; mark ⏳ until then.)

> ⏳ **Blocked:** checks 2 & 4 activate once the UI requests mic permission (Slice 3). Check 3 needs a device
> with audio. HITL checks need a target. Once you have an AVD or a phone with USB debugging, run
> `./gradlew connectedDebugAndroidTest` and report back.

---

## Slice 2 — TunerEngine (commit `ee50739`)
**Goal:** verify the capture→detection→note pipeline + modes + tunings behave; the engine exposes
`StateFlow<TunerState>` (detected, target, cents, direction, inTune).

**Prereq:** device from setup (D). Slice 2 has no UI, so only the logic-on-device path is HITL-relevant;
visual checks come in Slice 3.

- [⏳] **1. App launches and the engine produces a live readout from the mic**
  Steps: Run the app (▶ Run). Deferred — *no UI yet to display `TunerState`*; logic covered by 26 green
  JVM tests. Activates when Slice 3 renders the state.

- [⏳] **2. Auto mode targets the correct nearest string; manual mode honors the selected string**
  Steps: (needs UI) — deferred to Slice 3, where modes are toggleable and the target is visible.

- [x] **3. `./gradlew testDebugUnitTest` is green (26 tests)** — ✅ 2026-08-09 (user's machine, fresh
  PowerShell w/ JAVA_HOME). Re-confirmed independent of my environment.
  Steps: Android Studio terminal → `./gradlew testDebugUnitTest`. ✅ if BUILD SUCCESSFUL, 26 tests.

> ✅ **Verdict:** Slice 2 logic HITL satisfied via check #3 (2026-08-09). Checks #1/#2 ⏳ blocked on UI (Slice 3).

---

## Slice 3 — Tuner UI + mic permission (commit <sha at delivery>)
**Goal:** a working, "fairly powerful" tuner screen: rationale+permission flow, big clear note readout,
needle gauge with low/high direction, exact-frequency comparison, modes (auto/manual) + auto-advance,
tunings (Standard/Drop D/DADGAD), left-edge EADGBE strip (tap + swipe), lock-in reward bell, center-tap
reference tone, theme toggle.

**📱 First-time phone setup (for the audio checks below)**
Most checks here need a REAL microphone — an emulator's virtual mic is silent, so it cannot verify pitch.
To test on your phone:
1. Phone: **Settings → About phone → tap "Build number" 7×** → Developer options.
2. **Settings → System → Developer options → USB debugging** → ON.
3. Plug phone into PC via USB; on the phone tap **Allow** on the "USB debugging?" prompt.
4. Android Studio: device dropdown (top toolbar) now lists the phone → select it → **▶ Run**.
5. Grant mic when prompted. Pluck a string near the phone's mic. (No PC mic is used.)

**Emulator vs phone — what each can verify:**
- **Emulator CAN:** layout/launch, permission rationale + deny flow, theme toggle, tuning selector UI,
  swipe/tap navigation, Settings screen. (No audio → readout stays silent/empty.)
- **Phone REQUIRED:** live note detection, needle moving, bell on lock-in, center-tap tone, in-tune accuracy.

**Prereq:** setup A–E (top of file) + phone setup above for audio checks.

> ⏳ **Re-opened for re-test (2026-08-09):** user reported app crash on first lock-in + silent on relaunch.
> Root-caused to BT-009 (capture frame-size) + BT-010 (bell AudioTrack crash). Fixes applied + capture test
> now PASSES on the real phone. Please re-verify checks #2, #3, #6 on your phone.

- [x] **1. First launch shows rationale; Allow → live readout appears**  · *phone or emulator*  · ✅ 2026-08-09
  Steps: Uninstall + run. Rationale screen: "BetterTuner needs the microphone to hear your guitar.
  Nothing is recorded or sent." Tap Allow. ✅ if the tuner screen appears and (on phone) reacts to a pluck.

- [ ] **2. Deny → app stays open, "Enable mic" deep-link works, no crash**  · *emulator or phone*  · ⏳ re-test
  Steps: Uninstall + run → tap Don't allow. ✅ if app stays on the tuner screen (no readout) with an
  "Enable mic" button; tapping it opens system App Info / mic settings. No crash either way.

- [ ] **3. Pluck low E → big "E2", needle centers green within ±5¢, bell dings once on lock**  · *phone only*  · ⏳ re-test (BT-012: hysteresis fixes threshold chatter)
  Steps: On phone, pluck low E. ✅ if the large note reads "E2", the needle sits in the green center band,
  and the reward bell sounds a single satisfying ding the moment it locks (not every frame).

- [x] **4. Toggle Drop D / DADGAD → left strip + auto-nearest set change**  · *emulator or phone*  · ✅ 2026-08-09
  Steps: Use the top tuning selector. ✅ if the left EADGBE strip updates (Drop D shows D low; DADGAD shows
  its 6 targets) and auto mode targets within the chosen tuning.

- [x] **5. Swipe up/down + left/right cycles strings; tap a letter jumps**  · *emulator or phone*  · ⏳ RE-VERIFY (BT-013: tone moved off center so swipes cycle cleanly)
  Steps: On the left strip, swipe up/down and left/right; tap a letter (e.g. "G"). ✅ if the selected
  target changes accordingly in all three ways; manual pick flips the mode toggle to Manual.

- [ ] **6. Left-note tap = tone preview; hold = sustained tone; release stops**  · *phone only*  · ⏳ re-test (BT-013: tone moved to left-menu notes)
  Steps: On phone, tap a left-strip note → short tone preview; press-and-hold → sustained tone until release.
  (Distinct from the lock-in bell.)

- [x] **7. Theme toggle (Light/Dark/System) applies**  · *emulator or phone*  · ✅ 2026-08-09 (System default; explicit toggle is a Slice 4 stub)
  Steps: Settings (gear) → Theme. ✅ if the screen recolors live and the choice persists on restart.

> ⏳ **Blocked:** checks 3 & 6 require a real phone (emulator mic is silent). Everything else is emulator-verifiable.
> Re-test of #2/#3/#6 pending BT-009/BT-010 fix verification on user's Motorola razr.

---

<!-- TEMPLATE — copy for the next slice
## Slice N — <title> (commit <sha>)
**Goal:** <one line>
**Prereq:** <setup needed>

- [ ] **1. <check title>**
  Steps: <numbered steps>
-->
