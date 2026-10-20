# XRoar for Android

This project packages the XRoar Dragon, Tandy CoCo, and MC-10 emulator as a native Android app. It builds the current upstream XRoar C emulator with SDL3 and provides a small Android library screen for importing firmware and choosing media.

## Build

Open `android/` in Android Studio, or build from a machine with Android Studio's SDK tools installed:

```sh
cd android
./gradlew assembleDebug
```

Install the debug APK from `android/app/build/outputs/apk/debug/app-debug.apk`. The project is configured for JDK 17 or later, Android SDK 35, NDK 28.2.13676358, CMake 3.22.1, and Android 5.0 or later. Android Studio can install missing SDK and NDK components through SDK Manager.

Debug updates must use the original signing key. The build checks its public certificate against `android/app/debug-signing-certificate.sha256` and refuses to package an APK with a different signer. Keep the original workspace's debug keystore safe; do not regenerate it or replace the pinned fingerprint to bypass this check. An isolated build environment with a newly generated debug key cannot produce an update-compatible APK. Private signing keys must never be committed to the repository.

To check a packaged update against a previous APK, run `bash android/test-apk-update.sh NEW_APK PREVIOUS_APK`. It verifies signature identity, application ID, increasing version code and the emulator's separate-process/same-task manifest. This does not replace an installation test on an Android device. Install updates over the existing app; uninstalling deletes its imported ROMs and media.

To test that the build rejects a different signing key, run `bash android/test-signing-identity.sh`. It generates a disposable test key without changing the real keystore or packaging an APK.

### Set up the SDK in Replit

The Replit workspace already has JDK 19. To install the Android SDK components needed by Gradle, run:

```sh
bash android/setup-sdk.sh
```

The script downloads the official Android command-line tools, verifies their SHA-256 checksum, prompts you to review and accept the SDK licenses, installs the required SDK/NDK/CMake packages, and writes the local SDK path to the ignored `android/local.properties` file. It does not auto-accept licenses. To use another SDK location, pass it as an argument, for example `bash android/setup-sdk.sh "$HOME/android-sdk"`.

Launch-configuration regression checks can run with just the JDK: `bash android/test-launch-config.sh`. These check firmware requirements, machine selection and cartridge arguments, not device playback or APK packaging.

Keyboard timing regression checks can run with a C compiler: `bash android/test-keyboard-input.sh`. These check virtual key holds, repeated characters, Shift, cancellation, and queue/tick wraparound without requiring an Android SDK.

Touch-joystick regression checks run with a JDK and C compiler: `bash android/test-touch-joystick.sh`. These check absolute axis ranges, non-centering release, fire timing, and native port selection. They do not replace testing Android touch gestures on a device.

## First run

For step-by-step usage, the Direct/CoCo position-hold gamepad methods, the 1.0.9 quote button, and cassette saving/export/reloading, see the printable [Android 1.0.9 user guide](docs/android-user-guide.html).

XRoar does not include copyrighted machine firmware. On the app's library screen, select the computer to emulate and import the ROM images you are entitled to use. The launcher reports missing firmware instead of allowing XRoar to fall back to a different machine. ROM files are copied into the app's private storage with their original filenames preserved.

For **Tandy CoCo (NTSC)**, normally import both **Color BASIC** (`bas12.rom`, or another supported `bas10`–`bas13` image) and **Extended Color BASIC** (`extbas11.rom` or `extbas10.rom`). `extbas11.rom` alone is normally only the Extended BASIC half, not the complete firmware. A 16K combined firmware image named `coco.rom` is also supported. Firmware must retain a name recognised by the selected machine; the launcher shows a suggested filename when it is missing.

Use **Choose cartridge, tape, disk, or program** for games. A `.ccc` file, such as Space Assault, is a **cartridge**, not machine firmware. The cartridge is passed to XRoar with autorun enabled. Other tape, disk, or program images can also be selected to load at startup.

The emulator display is centered within the top half of the window. **Keyboard**, **Enter**, **Space**, and **Library** are in separate rows below it, with space reserved around Android system bars, display cutouts and the phone keyboard. Tap **Keyboard** to show the Android keyboard, or use **Enter** and **Space** directly for game prompts. Phone-keyboard taps are held for 50 milliseconds of emulated time with a 30-millisecond gap so a cartridge's keyboard scan can detect them. Physical keyboards retain normal held-key behaviour.

From 1.0.9, the **"** button next to **Space** enters a straight double quotation mark, useful for `CSAVE "MYPROG"` and `PRINT "HELLO"`. Android's synthetic ASCII punctuation is translated to each emulated machine's actual character mapping rather than treated as matching physical key positions. Physical keyboard mapping remains unchanged. If your phone keyboard offers curly “smart quotes”, use the dedicated **"** button for BASIC's straight quotes.

Touch or drag on the emulator display or the lower joystick pad for absolute analog position. The position stays when you lift your finger, like a non-centering CoCo joystick. Double-tap to press its single fire button; hold the second tap for continuous fire. Quick taps have a minimum 120-millisecond fire pulse. **Joystick: Right** switches between right and left ports; the inactive port remains neutral. Touch joystick input is mapped by default, rather than a physical game controller.

## Source and licenses

**USB gamepads:** Connect an Android-recognized gamepad using your phone's USB host/OTG connection. On the emulator screen, tap **Input: Touch** to select **Input: Gamepad**. The lower pad shows the connected controller name and live stick/fire state, or asks you to connect a controller. The left stick or D-pad moves; **A** fires (D-pad center is also accepted on controllers that report it). **Port: Right/Left** selects the CoCo joystick port. Only the first available controller is used. Unplugging, leaving the app, switching source/port, or losing window focus releases gamepad input. USB recognition and button layouts depend on the Android device/controller; there is no raw-USB driver or custom button remapping in this version.

Touch remains the default. Switching back to Touch restores its previous non-centering position. In Gamepad input, the separate **Gamepad: Direct** button below the picture toggles between **Direct** (the original spring-centering mapping) and **CoCo position-hold**. Position-hold uses the left stick to adjust a stored analog position at proportional speed; releasing it keeps that position. The D-pad makes slower, fine adjustments; **A** fires and **B** centres (holding B keeps it centred). This is cursor-like positioning, not an absolute replica of a physical non-centering CoCo stick. Mode changes, disconnection and focus/source/port changes reset the gamepad to neutral. Phone Keyboard/Enter/Space controls remain available in either mode. The Input and Port selectors sit below the picture, alongside Exit app; the mode button appears only for Gamepad input.

Run `bash android/test-gamepad.sh` for axis normalization, dead zones, both D-pad forms, fire timing and state reset tests. Run `bash android/test-touch-joystick.sh` for touch behavior and native port isolation. These host checks do not replace a real Android controller test.

**Exit app** is available in both the library and emulator controls. Confirming it stops the current session and removes the app's task from Recents; **Cancel** leaves the app open. Imported ROMs/media and library preferences are retained. The library's Exit button is temporarily disabled during file imports so a copy is not interrupted.

The CoCo display has a decorative bezel inspired by the TRS-80 13-inch Color Video Receiver. Its CRT opening contains the live SDL display; it does not add scanlines or tint. Dragon and MC-10 displays keep their plain frame.

Use the media picker to import a cassette ZIP. A ZIP with one CAS and a same-named WAV is unpacked and the CAS is selected. Archives with multiple different programs must be extracted manually so you can select the intended one. ZIP paths, expanded size and checksums are validated. XRoar's native autorun reads tape headers: machine-code tapes use `CLOADM` (and `EXEC` where appropriate); BASIC tapes use `CLOAD` and `RUN`. Previously imported ZIPs need to be selected again to extract them.

Run `bash android/test-media-and-bezel.sh [CASSETTE_ZIP]` for archive safety and bezel sizing checks, optionally using the Canyon Climber example. Games and ROMs supplied for testing are not bundled into the APK.

**Library** stops the current emulation session and returns to the existing ROM library. The emulator runs in a private Android process but shares the library's navigation task. This protects the library from upstream XRoar's process-wide `exit()` on quit and starts each emulation session with fresh native state.

## Saving BASIC programs to cassette (1.0.8)

1. To boot into BASIC without starting the previously selected game, use **Clear selected media / Start in BASIC** in the library, then **Start XRoar**.
2. Enter your BASIC program. In the emulator, tap **Files → Record new cassette**, enter a file label, and tap **Start recording**.
3. Wait for “Recording ready”, then type `CSAVE "MYPROG"` and press Enter. The quoted BASIC cassette name is separate from the Android file label. Use a short name supported by your emulated BASIC.
4. Wait until the save command completes and BASIC's prompt returns. Tap **Files → Finish recording** and confirm. Finishing too early produces an incomplete cassette.
5. Use **Files → Saved cassettes / Export**, select the file, and tap **Export copy**. Android's document picker lets you choose an external destination. Cancelling or failing an export does not delete the local original.
6. To reload, return to **Library → Saved cassettes / Export**, select a cassette, tap **Select to load**, then **Start XRoar**. You can also import an exported `.cas` through the usual media picker.

This records XRoar's emulated cassette output, not microphone audio. Each recording gets a unique filename in private app storage; existing recordings and imported media are not overwritten. Normal Library/Exit/Back navigation requires finishing an active recording first. The tape is closed/flushed on the emulator thread before a successful Finish acknowledgement; an active recording cannot be exported. Incomplete files are retained for possible recovery, but their presence or nonzero size does not prove that `CSAVE` completed. Test-load a saved program before relying on it.

Exported copies survive outside the app; local copies are deleted by uninstalling or clearing app storage. Keep backups. Cassette saving does not implement RAM/save-state snapshots, disk-image export, or automatic preservation of a running game. The new Android recording/document-picker journey has not yet been exercised on physical hardware.

Run `bash android/test-cassette-saving.sh` for cassette storage/export safety, emulator-thread command handling, real CAS waveform encode/decode round-trip, and failed-write reporting checks.

- XRoar source: [https://www.6809.org.uk/git/xroar.git](https://www.6809.org.uk/git/xroar.git), upstream commit `53acd7e380aacefaf4ac385282f80532f62827cb` (current upstream at the time this Android port was started). The Android build includes the Dragon, CoCo 3, and MC-10 machine families and SDL3 backend.
- SDL3 Android package: official SDL 3.4.18 Android archive, included as `android/app/libs/SDL3-3.4.18.aar`. SDL's license is in `android/app/libs/SDL3-LICENSE.txt`.
- XRoar is distributed under GPL version 3 or later; see `vendor/xroar/COPYING.GPL`.

The upstream `android` branch is a historical 2014 port for SDL 1.2, so this app uses the maintained upstream source and its SDL3 Android integration instead.
