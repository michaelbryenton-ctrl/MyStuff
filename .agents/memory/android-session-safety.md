---
name: Android session safety
description: Tradeoffs protecting the ROM library across native emulator shutdown and APK updates.
---

## Native sessions

Prefer a separate emulator process, sharing the library's navigation task, over turning upstream process termination into a repeatable in-process emulator session.

**Why:** Upstream assumes a single native run and process-wide termination. Making quit return is not sufficient: shutdown guards and native globals also need safe reinitialization. Process isolation preserves upstream assumptions without risking the library or relying on unverified repeated native initialization.

**How to apply:** Do not merge the emulator into the library's process merely to simplify navigation. An in-process alternative needs comprehensive cleanup/reinitialization and repeated launch/quit verification, not just a changed quit function or activity intent.

## Update signing

Having the same SDK and source in a task copy does not mean having the same APK signing identity.

**Why:** Debug keystores belong to the build environment, not the source checkout. A different signing certificate makes Android reject an update. The user previously had to uninstall to install an APK; the exact original installation error was not available, so a certificate mismatch was not conclusively established.

**How to apply:** Compare updates against the previously distributed APK's public certificate and version. Do not regenerate a key or replace the pinned public identity to bypass an update check. If the original key is unavailable, report that blocker and use secure key restoration rather than recommending uninstall, which deletes imported files. Never store private keys in source control or memory.
