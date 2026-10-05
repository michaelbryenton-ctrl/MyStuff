#!/usr/bin/env bash
set -euo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
classes="$(mktemp -d)"
trap 'rm -rf -- "$classes"' EXIT

javac --release 8 -d "$classes" \
    "$root/app/src/main/java/org/xroar/android/MachineProfile.java" \
    "$root/tests/org/xroar/android/MachineProfileTest.java"
java -cp "$classes" org.xroar.android.MachineProfileTest
