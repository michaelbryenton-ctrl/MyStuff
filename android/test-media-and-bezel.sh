#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
build="$(mktemp -d)"
trap 'rm -rf -- "$build"' EXIT
javac --release 8 -d "$build" \
    "$root/app/src/main/java/org/xroar/android/MediaArchive.java" \
    "$root/app/src/main/java/org/xroar/android/MonitorGeometry.java" \
    "$root/app/src/main/java/org/xroar/android/MachineProfile.java" \
    "$root/tests/org/xroar/android/MediaArchiveTest.java"
java -cp "$build" org.xroar.android.MediaArchiveTest "$@"
