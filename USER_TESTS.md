# BetterTuner — User Test Checklist (HITL)

This is the **human-in-the-loop (HITL) test log**. It complements the automated JUnit suite
(`./gradlew testDebugUnitTest`). Automated tests prove the *logic*; these manual checks prove the app
*behaves* on a real device with a real microphone, a real guitar, and real fingers.

## How the process works
- Built **per slice** — one unit of delivered work gets one section + checklist at handoff.
- Hermes (me) owns this file: I add the section + checklist when a slice is delivered, and I flip the
  boxes when you report results. You don't need to edit anything — just run the checks and tell me.
- Box states: `[ ]` todo · `[x]` done · `⏳` blocked (needs a device/emulator not yet available) · `❌` failed.
- A failed check becomes a `BT-xxx` entry in [BUGS.md](BUGS.md). I fix it, reopen the item, you re-verify.
- Each section names the **commit SHA** it belongs to, so the checklist is traceable in `git log`.

## Slice status
| Slice | What | Commit | Status |
|-------|------|--------|--------|
| 0 | Project scaffold + pitch core + unit tests | `c71e67f` | automated ✓ / HITL n/a |
| 1 | Audio capture seam (`AudioSource` + `AudioRecordSource` + `FakeAudioSource`) | `805619e` | HITL ⏳ blocked (no device) |
| 2 | TunerEngine (`StateFlow<Pitch?>`, wires capture → detector → note) | — | not started |

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

---

## Slice 1 — Audio capture seam (commit `805619e`)
**Goal:** prove the real microphone produces non-silent, correctly-sized buffers at 44.1 kHz, and that
the pipeline recovers a known note from a synthetic tone (already covered by JVM tests — this is the
*device* half).

**Prereq:** complete the 🔧 One-time setup above (at least D — have a runnable device).

- [ ] **1. App installs and launches without crashing**
  Steps: With the project open in Android Studio, select your device in the dropdown and click **▶ Run**.
  Wait for "Launch succeeded" in the build output. Confirm the app opens (blank screen is fine for now).
  ✅ if it launches; ❌ if it crashes or shows a red error.

- [ ] **2. `RECORD_AUDIO` permission prompt appears; granting proceeds without error**
  Steps: (Once the UI requests mic access — wired in a later slice —) on launch the OS dialog "Allow
  BetterTuner to record audio?" appears. Tap **While using the app** / **Allow**. ✅ if no crash after granting.
  (If the dialog hasn't been added yet, mark ⏳ — this check activates with the UI slice.)

- [ ] **3. `connectedDebugAndroidTest` passes — real mic yields non-silent buffers**
  Steps: In Android Studio terminal (bottom panel, "Terminal" tab) run
  `./gradlew connectedDebugAndroidTest`. Wait for "BUILD SUCCESSFUL". ✅ if green; ❌ if any test failed
  (paste the failure to me). Note: on an emulator with no real mic, use the mic-enabled AVD from setup B.

- [ ] **4. Denying the permission does not crash; clear message / app stays usable**
  Steps: Uninstall + re-run the app, tap **Don't allow** on the mic prompt. ✅ if the app stays open and
  shows a message instead of crashing. (Activates with the UI slice; mark ⏳ until then.)

> ⏳ **Blocked:** checks 2 & 4 activate once the UI requests mic permission (Slice 3). Check 3 needs a device
> with audio. HITL checks need a target. Once you have an AVD or a phone with USB debugging, run
> `./gradlew connectedDebugAndroidTest` and report back.

---

<!-- TEMPLATE — copy for the next slice
## Slice N — <title> (commit <sha>)
**Goal:** <one line>
**Prereq:** <setup needed>

- [ ] **1. <check title>**
  Steps: <numbered steps>
-->
