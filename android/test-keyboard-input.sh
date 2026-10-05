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
