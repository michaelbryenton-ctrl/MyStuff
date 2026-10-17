#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
build="$(mktemp -d)"
trap 'rm -rf -- "$build"' EXIT
javac --release 8 -d "$build/classes" \
    "$root/app/src/main/java/org/xroar/android/SavedTapeStore.java" \
    "$root/tests/org/xroar/android/SavedTapeStoreTest.java"
mkdir "$build/files"
java -cp "$build/classes" org.xroar.android.SavedTapeStoreTest "$build/files"
unzip -q "$root/app/libs/SDL3-3.4.18.aar" \
    'prefab/modules/SDL3-Headers/include/SDL3/*' -d "$build"
jdk="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
"${CC:-cc}" -std=c11 -Wall -Wextra -Werror \
    -I"$build/prefab/modules/SDL3-Headers/include" \
    -I"$jdk/include" -I"$jdk/include/linux" -I"$root/app/jni/src" \
    -I"$root/../vendor/xroar" -I"$root/../vendor/xroar/src" -I"$root/../vendor/xroar/portalib" \
    "$root/app/jni/src/android_tape_bridge.c" \
    "$root/tests/native/android_tape_bridge_test.c" \
    -o "$build/tape-bridge-test"
"$build/tape-bridge-test"
"${CC:-cc}" -std=c11 -D__ANDROID__ -D_POSIX_C_SOURCE=200112L \
    -ffunction-sections -fdata-sections -Wl,--gc-sections \
    -I"$root/../vendor/xroar" -I"$root/../vendor/xroar/src" -I"$root/../vendor/xroar/portalib" \
    "$root/../vendor/xroar/src/tape_cas.c" "$root/../vendor/xroar/src/fs.c" \
    "$root/../vendor/xroar/src/crc32.c" "$root/../vendor/xroar/portalib/slist.c" \
    "$root/../vendor/xroar/portalib/xmalloc.c" "$root/../vendor/xroar/portalib/pl-estrcpy.c" \
    "$root/../vendor/xroar/portalib/intfuncs.c" \
    "$root/tests/native/cassette_roundtrip_test.c" -lm -o "$build/cassette-roundtrip-test"
"$build/cassette-roundtrip-test" "$build/roundtrip.cas"
