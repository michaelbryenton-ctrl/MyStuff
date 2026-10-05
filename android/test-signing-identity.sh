#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
build="$(mktemp -d)"
trap 'rm -rf -- "$build"' EXIT
# Only a disposable test key. These are the public/default debug passwords,
# not credentials for a user's release signing key.
keytool -genkeypair -keystore "$build/test.keystore" -alias AndroidDebugKey \
    -storepass android -keypass android -dname "CN=Disposable Test Signer" \
    -keyalg RSA -keysize 2048 -validity 1 >"$build/keytool.log" 2>&1
if (cd "$root" && ./gradlew :app:verifyDebugSigningIdentity --no-daemon \
        --init-script "$root/tests/signing-negative.init.gradle" \
        "-Dxroar.test.keystore=$build/test.keystore") >"$build/gradle.log" 2>&1; then
    echo "FAIL: Build accepted a different signing identity." >&2
    exit 1
fi
if ! grep -q "This workspace has a different debug signing key" "$build/gradle.log"; then
    cat "$build/gradle.log"
    echo "FAIL: Validation failed for a reason other than the expected signer mismatch." >&2
    exit 1
fi
echo "PASS: A different signing identity is rejected; original key unchanged"
