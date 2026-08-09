# InTune — Google Play Store Listing

> Working brand: **InTune**. Display name on device = "InTune". Package stays
> `com.lexeakins.bettertuner` (allowed by Google; only the visible name changed).
> If you prefer a different name, swap it in `app/src/main/res/values/strings.xml`
> (`app_name`) and this doc.

## Branding assets (in `app/src/main/playstore/`)
- `icon_foreground_h.png` — the app emblem (horizontal tuner gauge + green check, transparent bg).
- `ic_launcher_foreground.png` — same, wired into the adaptive icon (`res/drawable`).
- `feature_graphic.png` — 1024×500 Play Store feature graphic (emblem + negative space).
- `phone_screenshot.png` — live capture from the test phone (re-shoot after final UI pass).

## Listing copy (paste into Play Console)

**App name:** InTune

**Short description (80 chars):**
Free guitar tuner. Offline, no ads, no tracking. Chromatic YIN pitch detection.

**Full description:**
InTune is a free, private guitar tuner that does one thing exceptionally well: it tells you, fast and clearly, whether you're in tune.

No accounts. No ads. No internet connection. No audio is ever recorded, stored, or sent anywhere — every note is analyzed on your device, in memory, and forgotten the instant it's read.

Features:
• Chromatic tuner — any note, any instrument, not just guitar.
• Three tunings built in: Standard, Drop D, and DADGAD.
• Auto mode — pluck a string and InTune finds the nearest target and locks on; it even advances to the next string as each one tunes.
• Manual mode — pick a string from the left edge (swipe or tap) and tune to it.
• A clear, large note readout with a needle gauge, exact frequency compare (detected vs. target), and a flat/sharp direction.
• Tap or hold any string on the left to hear its reference tone — handy when you're away from an amp.
• Whole-instrument progress: each tuned string greys out and gets a green check, so you can see the guitar come into tune as a whole.
• A satisfying, subtle "ding" the moment a string locks in tune.

Whether you're dropping to D mid-song or exploring DADGAD for the first time, InTune gets you there and back without the mental friction — and without asking for a single permission it doesn't need.

Free forever. Open source. Made for players who just want to tune and play.

**Category:** Music & Audio
**Tags:** guitar, tuner, chromatic, tuning, offline, free, DADGAD, drop D
**Content rating:** Everyone
**Privacy:** No data collected. (Data safety form: "No data collected" across all categories.)

## Notes for Play Console
- Upload `feature_graphic.png` as the 1024×500 feature graphic.
- Phone screenshots: take 2–3 from the test device (tuner in-tune, tuning dropdown open, DADGAD selected). Re-shoot after the final UI pass.
- Icon is an adaptive icon (foreground + green background) — Play will render it correctly; no separate legacy PNGs needed.
- "BetterTuner" still appears in some internal strings/code comments; the user-visible name is InTune.
