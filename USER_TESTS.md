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

## Slice 1 — Audio capture seam (commit `805619e`)
**Goal:** prove the real microphone produces non-silent, correctly-sized buffers at 44.1 kHz, and that
the pipeline recovers a known note from a synthetic tone (already covered by JVM tests — this is the
*device* half).

- [ ] App installs and launches on a real device/emulator without crashing
- [ ] `RECORD_AUDIO` permission prompt appears on first launch; granting proceeds without error
- [ ] `./gradlew connectedDebugAndroidTest` (`AudioRecordSourceTest`) passes — real mic yields non-silent buffers of the expected frame size
- [ ] Denying the permission does not crash; the app stays usable / shows a clear message

> ⏳ **Blocked:** emulator/device not configured yet. HITL checks need a target. Once you have an AVD or a
> phone with USB debugging, run `./gradlew connectedDebugAndroidTest` and report back. I can also help you
> set up an emulator in Android Studio (Tools → Device Manager → Create Virtual Device).

---

<!-- TEMPLATE — copy for the next slice
## Slice N — <title> (commit <sha>)
**Goal:** <one line>

- [ ] <check>
-->
