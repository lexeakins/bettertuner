# BetterTuner

A free, ad-free, offline guitar tuner for Android. Zero audio collection — the microphone is processed
in-memory and never stored or transmitted.

## Stack
- Kotlin + Jetpack Compose (native Android)
- Gradle / Android Gradle Plugin 8.7.3
- Self-implemented YIN pitch detector (no third-party audio library — MIT-clean)
- minSdk 24, targetSdk / compileSdk 35

## Architecture
The pitch core is pure, testable Kotlin with no Android dependencies:

- `pitch/NoteConverter` — frequency (Hz) → note name + octave + cents
- `pitch/YinPitchDetector` — monophonic fundamental-frequency detection from a sample buffer
- `audio/AudioSource` — capture seam (interface). `AudioRecordSource` is the real mic impl;
  `FakeAudioSource` is the hardware-free stand-in used by JVM tests.

## Build & test
Requires JDK 21 and the Android SDK. Set `sdk.dir` in `local.properties` (gitignored).

```
./gradlew testDebugUnitTest          # JVM unit + pipeline tests (no device)
./gradlew connectedDebugAndroidTest  # instrumented: real mic capture (needs device/emulator)
```

## License
MIT — see LICENSE.
