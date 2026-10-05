---
name: Android native validation
description: Why simulated Android host syntax checks are not enough to establish native Android compatibility.
---

Host C syntax checks, even with Android platform defines and SDL Android headers, do not validate the Android minimum API level because they still use host libc headers.

**Why:** A real NDK build targeting API 21 exposed unavailable complex-number accessors that the host syntax checks had accepted.

**How to apply:** Compile and link with the actual NDK sysroot at the chosen minimum API level, across the supported ABIs, before asserting that the project produces a compatible Android APK.
