# Go Strix Booster

Android (Jetpack Compose) prototype that applies **best-effort global DSP without root** using Android `AudioEffect` APIs (global session `audioSessionId = 0`) and provides a simple **spectrum analyzer**.

## Goals

- Apply audio effects globally (when supported by the device/output path):
  - `Equalizer` (multi-band EQ)
  - `BassBoost`
  - `DynamicsProcessing` (simple limiting)
  - `LoudnessEnhancer` / `Virtualizer` (device-dependent)
- Display a spectrum via `Visualizer` (FFT)
- Provide a Compose UI to control DSP parameters and validate the active audio output

## Important limitations (no root)

- Android does **not** reliably allow re-processing **all audio from all apps**.
- Some apps use audio paths that **bypass** the system mixer/effects.
  - Typical examples: “Hi-Res”, “Direct USB”, and some custom audio engines.
- With a **USB dongle / gaming headset**, `AudioEffect` may have **no audible impact** depending on ROM, device, and output route.

This is a **best-effort prototype**. A fully global/stable solution is usually system-level (often root / audio modules).

## Package / App ID

- `applicationId`: `fr.maythayus.gostrixbooster`

## Requirements

- Recent Android Studio
- JDK 17 (required by AGP/Gradle in Android Studio)

### JDK configuration (Windows)

Gradle is pinned to Android Studio’s bundled JDK via `gradle.properties`:

- `org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr`

If your Android Studio path is different, update it.

Verify:

```powershell
.\gradlew.bat -version
```

You should see a JVM 17.

## Build

```powershell
.\gradlew.bat :app:assembleDebug
```

## Run

- Install from Android Studio (**Run** ▶)
- Or build the APK with Gradle and install it to a device/emulator

## Usage

- Open the app
- Enable/disable **Global DSP**
- Adjust EQ bands / BassBoost (and other effects if available)
- The spectrum analyzer may request `RECORD_AUDIO` permission (required by `Visualizer` on many devices)

## Testing tips (YouTube Music / SoundCloud / Poweramp)

- Try strong EQ changes to confirm the effect is audible.
- Some players can bypass Android effects in “direct/hi-res” modes.
  - Disable “direct/hi-res” output options when testing.

## Key source files

- `app/src/main/java/com/example/k60gostrix/audio/GlobalAudioEffectsController.kt`
  - Initializes and controls global `AudioEffect` instances (`Equalizer`, `BassBoost`, etc.)
- `app/src/main/java/com/example/k60gostrix/audio/SpectrumAnalyzer.kt`
  - `Visualizer` + FFT-to-magnitudes conversion
- `app/src/main/java/com/example/k60gostrix/AudioTuningScreen.kt`
  - Compose UI (sliders, toggles, spectrum view)

## Notes

- EQ band count and level ranges depend on the device implementation.
- Some effects are deprecated on newer Android versions; behavior depends on the device.
