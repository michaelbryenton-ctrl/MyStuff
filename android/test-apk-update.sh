#!/usr/bin/env bash
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
apk="${1:-$root/app/build/outputs/apk/debug/app-debug.apk}"
previous="${2:-}"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sed -n 's/^sdk\.dir=//p' "$root/local.properties")}}"
signer="$sdk/build-tools/35.0.0/apksigner"
aapt="$sdk/build-tools/35.0.0/aapt2"
expected="$(tr -d '\r\n ' < "$root/app/debug-signing-certificate.sha256")"
fingerprint() {
    "$signer" verify --print-certs "$1" |
        sed -n 's/^Signer #1 certificate SHA-256 digest: //p'
}
if [[ "$(fingerprint "$apk")" != "$expected" ]]; then
    echo "FAIL: APK does not have the pinned update signing certificate." >&2
    exit 1
fi
metadata="$("$aapt" dump badging "$apk")"
package_name="$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<< "$metadata")"
version="$(sed -n "s/^package: .*versionCode='\([^']*\)'.*/\1/p" <<< "$metadata")"
[[ "$package_name" == "org.xroar.android" && "$version" =~ ^[0-9]+$ ]]
if [[ -n "$previous" ]]; then
    old_metadata="$("$aapt" dump badging "$previous")"
    old_package="$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<< "$old_metadata")"
    old_version="$(sed -n "s/^package: .*versionCode='\([^']*\)'.*/\1/p" <<< "$old_metadata")"
    [[ "$old_version" =~ ^[0-9]+$ ]]
    if [[ "$package_name" != "$old_package" || "$(fingerprint "$previous")" != "$expected" ]] ||
            (( version <= old_version )); then
        echo "FAIL: Update package/signer must match and versionCode must increase." >&2
        exit 1
    fi
fi
# Inspect the packaged manifest, not just the source manifest.
activity="$("$aapt" dump xmltree --file AndroidManifest.xml "$apk" | awk '
    /E: activity \(/ { if (found) exit; block = "" }
    { block = block $0 "\n" }
    /android:name.*org\.xroar\.android\.XRoarActivity/ { found = 1 }
    END { if (found) printf "%s", block }
')"
grep -Eq 'android:process.*":emulator"' <<< "$activity"
grep -Eq 'android:launchMode.*=0$' <<< "$activity"
echo "PASS: APK signer, package, version and isolated same-task emulator manifest"
