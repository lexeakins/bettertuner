# BetterTuner — UI/UX Design Notes (pre-grill-me)

Working notebook for the tuner UI. Captured from user input on 2026-08-09, to be refined via the
**grill-me / grilling** skill before any UI code is written. NOT a spec yet — a brainstorm to grill.

## Modes
- **Manual mode:** user picks which string/note they're tuning.
- **Automatic mode:** app detects which string is being tuned on its own; auto-advances from top
  (low E) → bottom (high e) as each string is brought into tune.

## Tunings (selectable)
- Multiple presets: Standard (EADGBE), Drop D (DADGBE), DADGAD, others TBD. Pure data table; reuses the
  existing pitch engine. Selecting a tuning changes the set of target notes/strings shown.

## Readout / clarity
- **Note letter must be CLEAR and LARGE** once detected.
- Show **exact detected frequency** vs **exact target frequency** side by side (numeric).
- Show clearly whether the note is **too low or too high** relative to the target (needle / arrow /
  color — direction is a grill decision).
- Visual in-tune indicator when within tolerance.

## Navigation / ergonomics
- Easy, intuitive to reach the tuner; minimal taps to start tuning.
- **Left-edge string selector** (EADGBE targets tappable from the left of the screen) — designed so the
  user can hold the phone in their **left hand** and tap with the left thumb.
- **Swipe** up / left / right to cycle through strings, in addition to tapping a target note.
- Manual selection of which string, but in auto mode advance low→high after each string is tuned.

## Open questions (for grill-me)
- Exact in-tune tolerance (cents) per mode; visual metaphor (needle vs. gauge vs. color band).
- How auto-detect decides "which string" — nearest target in the active tuning? confidence threshold?
- Layout: portrait locked (already set). How the left selector + big readout + freq compare coexist.
- Haptics / sound feedback when in tune? accessibility (TalkBack) for the big note?
- Settings screen scope (tolerance, A4 reference 440/432, theme).

## Slice plan (build order)
- Slice 2 (current): **TunerEngine** — `StateFlow<Pitch?>` pipeline wiring AudioSource → YIN → NoteConverter;
  exposes current note, target, cents, direction. No UI yet, but the data model the UI will observe.
- Slice 3: Tuner UI + mic-permission flow (grill-me first).
- Later: tunings table, modes, swipe/tap navigation, settings.
