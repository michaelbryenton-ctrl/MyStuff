# XRoar for Android

Native Android packaging for the XRoar Dragon, Tandy CoCo, and MC-10 emulator, built from the upstream C code with SDL3.

## Run & Operate

- `cd android && ./gradlew assembleDebug` — build the debug APK
- JDK 17+, Android SDK 35, NDK 28.2.13676358, and CMake 3.22.1 are required
- APK output: `android/app/build/outputs/apk/debug/app-debug.apk`

## Stack

- Android Gradle project, Java activity launcher, CMake and NDK
- XRoar C emulator source with SDL3 Android package
- ROM files are imported by the user and stored in app-private storage

## Where things live

- `android/` — Android app and Gradle build
- `android/app/jni/` — CMake build for the XRoar core
- `vendor/xroar/` — upstream XRoar source
- `android/app/libs/SDL3-3.4.18.aar` — official SDL3 Android package

## Architecture decisions

- Use maintained upstream XRoar with SDL3; the upstream Android branch is a 2014 SDL 1.2 experiment.
- Keep proprietary firmware ROM images out of the app; users import their own through Android's document picker.
- Copy imported media and ROMs into app-private storage so emulator paths remain available after the picker closes.

## Product

Users can import machine ROMs, select tape/disk/program media, and launch the XRoar emulator on Android. SDL handles physical keyboards and controllers; a launcher control opens Android's soft keyboard.

## Gotchas

- A ROM-free APK cannot boot a machine; firmware must be imported before launch.
- The Android APK must be built with the SDK/NDK versions listed above; the Replit workspace does not provide a device emulator.

## Pointers

- `README.md` — Android build setup, supported machines, ROM import, and license notes
