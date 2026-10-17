#!/usr/bin/env bash
set -euo pipefail

root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
build="$(mktemp -d)"
trap 'rm -rf -- "$build"' EXIT

"${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
    -I"$root/app/jni/src" \
    "$root/app/jni/src/android_ime_queue.c" \
    "$root/tests/native/android_ime_queue_test.c" \
    -o "$build/android-ime-queue-test"
"$build/android-ime-queue-test"
unzip -q "$root/app/libs/SDL3-3.4.18.aar" \
    'prefab/modules/SDL3-Headers/include/SDL3/*' -d "$build"
"${CC:-cc}" -std=c11 -D__ANDROID__ \
    -ffunction-sections -fdata-sections -Wl,--gc-sections \
    -I"$build/prefab/modules/SDL3-Headers/include" -I"$root/app/jni/src" \
    -I"$root/../vendor/xroar" -I"$root/../vendor/xroar/src" -I"$root/../vendor/xroar/portalib" \
    "$root/app/jni/src/android_ime_queue.c" "$root/app/jni/src/android_ime_symbols.c" \
    "$root/../vendor/xroar/src/sdl3/keyboard_sdl3.c" \
    "$root/../vendor/xroar/src/keyboard.c" "$root/../vendor/xroar/src/dkbd.c" \
    "$root/../vendor/xroar/portalib/xmalloc.c" "$root/../vendor/xroar/portalib/pl-estrcpy.c" \
    "$root/../vendor/xroar/portalib/c-strcasecmp.c" \
    "$root/tests/native/android_ime_symbols_test.c" -o "$build/android-ime-symbols-test"
"$build/android-ime-symbols-test"
