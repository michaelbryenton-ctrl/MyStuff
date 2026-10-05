#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
build="$(mktemp -d)"
trap 'rm -rf -- "$build"' EXIT
javac --release 8 -d "$build/classes" \
    "$root/app/src/main/java/org/xroar/android/TouchJoystickState.java" \
    "$root/tests/org/xroar/android/TouchJoystickStateTest.java"
java -cp "$build/classes" org.xroar.android.TouchJoystickStateTest
unzip -q "$root/app/libs/SDL3-3.4.18.aar" \
    'prefab/modules/SDL3-Headers/include/SDL3/*' -d "$build"
jdk="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
"${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
    -I"$build/prefab/modules/SDL3-Headers/include" \
    -I"$jdk/include" -I"$jdk/include/linux" \
    -I"$root/../vendor/xroar" -I"$root/../vendor/xroar/src" \
    -I"$root/../vendor/xroar/portalib" \
    "$root/app/jni/src/android_touch_joystick.c" \
    "$root/tests/native/android_touch_joystick_test.c" \
    -o "$build/touch-joystick-test"
"$build/touch-joystick-test"
