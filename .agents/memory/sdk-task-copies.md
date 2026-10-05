---
name: SDKs in task copies
description: Distinguishing an isolated task's missing SDK from the user's working main workspace, and avoiding large-toolchain home quotas.
---

The user confirmed: “we have a working sdk. you built me an apk earlier after I ran the script and accepted all the licenses.”

Do not treat an SDK search in an assigned task's isolated environment as evidence that the user's main workspace lacks its SDK or accepted licenses.

**Why:** Replit tasks use isolated project copies. SDK installations outside the repository and ignored local configuration may not be present there, even after a successful main-workspace APK build.

**How to apply:** Distinguish the task environment from the main workspace before reporting missing setup. For the same already-licensed packages, restore the necessary official binaries in the task copy rather than asking the user to repeat license acceptance. Do not manufacture acceptance receipts or automatically answer new license prompts.

Large NDKs may hit a home-directory quota even when the filesystem reports ample free space.

**Why:** Restoring a full NDK into the task's home directory failed with a disk-quota error; scratch storage worked.

**How to apply:** Prefer scratch storage outside the repository for disposable task-build SDKs when home quota blocks installation. Keep SDK location configuration local and uncommitted.
