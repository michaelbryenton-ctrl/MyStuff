# XRoar for Android

This project packages the XRoar Dragon, Tandy CoCo, and MC-10 emulator as a native Android app. It builds the current upstream XRoar C emulator with SDL3 and provides a small Android library screen for importing firmware and choosing media.

## Build

Open `android/` in Android Studio, or build from a machine with Android Studio's SDK tools installed:

```sh
cd android
./gradlew assembleDebug
```

Install the debug APK from `android/app/build/outputs/apk/debug/app-debug.apk`. The project is configured for JDK 17 or later, Android SDK 35, NDK 28.2.13676358, CMake 3.22.1, and Android 5.0 or later. Android Studio can install missing SDK and NDK components through SDK Manager.

## First run

XRoar does not include copyrighted machine firmware. On the app's library screen, import the ROM images you are entitled to use. ROM files are copied into the app's private storage. You can also choose a tape, disk, or program image to have it loaded when the emulator starts. Tap **Keyboard** in the emulator to show the Android keyboard; text input is translated to XRoar key presses. Physical keyboards and game controllers are also supported through SDL.

## Source and licenses

- XRoar source: [https://www.6809.org.uk/git/xroar.git](https://www.6809.org.uk/git/xroar.git), upstream commit `53acd7e380aacefaf4ac385282f80532f62827cb` (current upstream at the time this Android port was started). The Android build includes the Dragon, CoCo 3, and MC-10 machine families and SDL3 backend.
- SDL3 Android package: official SDL 3.4.18 Android archive, included as `android/app/libs/SDL3-3.4.18.aar`. SDL's license is in `android/app/libs/SDL3-LICENSE.txt`.
- XRoar is distributed under GPL version 3 or later; see `vendor/xroar/COPYING.GPL`.

The upstream `android` branch is a historical 2014 port for SDL 1.2, so this app uses the maintained upstream source and its SDL3 Android integration instead.
