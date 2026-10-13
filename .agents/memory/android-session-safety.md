---
name: Android session safety
description: Tradeoffs protecting the ROM library across native emulator shutdown and APK updates.
---

## Native sessions

Prefer a separate emulator process, sharing the library's navigation task, over turning upstream process termination into a repeatable in-process emulator session.

**Why:** Upstream assumes a single native run and process-wide termination. Making quit return is not sufficient: shutdown guards and native globals also need safe reinitialization. Process isolation preserves upstream assumptions without risking the library or relying on unverified repeated native initialization.

**How to apply:** Do not merge the emulator into the library's process merely to simplify navigation. An in-process alternative needs comprehensive cleanup/reinitialization and repeated launch/quit verification, not just a changed quit function or activity intent.

## App exit

For app exit, close the shared Android task through the library activity rather than abruptly killing the library process. Keep this distinct from returning to the library, which leaves the app open.

**Why:** The native emulator has process-wide shutdown behavior, while the library owns imported files and import work. Task-based closure preserves normal lifecycle cleanup without treating exit as data deletion. The user confirmed the Exit app behavior works on Android.

**How to apply:** Retain emulator process isolation, release held input before exit, and avoid interrupting an active file import.

## Update signing

Having the same SDK and source in a task copy does not mean having the same APK signing identity.

**Why:** Debug keystores belong to the build environment, not the source checkout. A different signing certificate makes Android reject an update. The user previously had to uninstall to install an APK; the exact original installation error was not available, so a certificate mismatch was not conclusively established.

**How to apply:** Compare updates against the previously distributed APK's public certificate and version. Do not regenerate a key or replace the pinned public identity to bypass an update check. If the original key is unavailable, report that blocker and use secure key restoration rather than recommending uninstall, which deletes imported files. Never store private keys in source control or memory.

A missing SDK directory or empty `~/.android` does not prove the signing key is lost. Check Android's effective configuration home before drawing that conclusion.

**Why:** After a build-environment reset, the SDK was absent but the original signing key survived in the workspace's XDG configuration storage. Its public fingerprint still matched the distributed APK.

**How to apply:** Locate the existing key and verify only its public certificate; restore the already-licensed SDK tools rather than generating another signing identity.

Keep delivered APK copies and comparison APKs outside Gradle's build-output directory, in an ignored generated-artifact archive.

**Why:** Gradle can remove extra APK copies from its output directory when packaging the next version, leaving no previous binary for a side-by-side update comparison.

**How to apply:** Preserve the previous APK outside the build tree before assembling an update. Verify the new APK against the pinned public certificate even if no prior binary remains available.
