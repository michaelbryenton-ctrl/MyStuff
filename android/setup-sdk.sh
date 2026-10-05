#!/usr/bin/env bash
set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
readonly REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd -P)"
readonly CMDLINE_TOOLS_REVISION="15859902"
readonly CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_REVISION}_latest.zip"
readonly CMDLINE_TOOLS_SHA256="4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583"

usage() {
    cat <<'EOF'
Install the Android SDK packages required by this project and set android/local.properties.

Usage:
  bash android/setup-sdk.sh [SDK_DIRECTORY]

If omitted, SDK_DIRECTORY comes from ANDROID_HOME, then ANDROID_SDK_ROOT,
then defaults to "$HOME/android-sdk". Google SDK licenses are shown
interactively; this script does not auto-accept them.
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    usage
    exit 0
fi
if (( $# > 1 )); then
    usage >&2
    exit 2
fi

if [[ "$(uname -s)" != "Linux" || "$(uname -m)" != "x86_64" ]]; then
    echo "This setup script currently supports Linux x86_64 only." >&2
    exit 1
fi

for command_name in curl sha256sum unzip java awk mktemp realpath; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Required command not found: $command_name" >&2
        exit 1
    fi
done

java_version="$(java -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')"
if [[ -z "$java_version" ]]; then
    echo "Could not determine the installed Java version." >&2
    exit 1
fi
if [[ "$java_version" == 1.* ]]; then
    java_major="${java_version#1.}"
    java_major="${java_major%%.*}"
else
    java_major="${java_version%%.*}"
fi
if ! [[ "$java_major" =~ ^[0-9]+$ ]] || (( java_major < 17 )); then
    echo "JDK 17 or later is required; found Java ${java_version}." >&2
    exit 1
fi

sdk_root="${1:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-${HOME}/android-sdk}}}"
sdk_root="$(realpath -m -- "$sdk_root")"

case "$sdk_root/" in
    "$REPO_ROOT/"*)
        echo "Keep the Android SDK outside the project directory to avoid adding large SDK files to Git." >&2
        echo "Choose a path such as \$HOME/android-sdk." >&2
        exit 1
        ;;
esac
if [[ "$sdk_root" =~ [[:space:]] || "$sdk_root" == *:* || "$sdk_root" == *\\* ]]; then
    echo "Use an SDK path without spaces, colons, or backslashes so Gradle can read local.properties." >&2
    exit 1
fi
mkdir -p "$sdk_root"
sdk_root="$(cd -- "$sdk_root" && pwd -P)"

sdkmanager_dir="${sdk_root}/cmdline-tools/latest"
sdkmanager="${sdkmanager_dir}/bin/sdkmanager"
temporary_dir=""
local_properties_temp=""
cleanup() {
    if [[ -n "$temporary_dir" && -d "$temporary_dir" ]]; then
        rm -rf -- "$temporary_dir"
    fi
    if [[ -n "$local_properties_temp" && -f "$local_properties_temp" ]]; then
        rm -f -- "$local_properties_temp"
    fi
}
trap cleanup EXIT

if [[ ! -x "$sdkmanager" ]]; then
    if [[ -e "$sdkmanager_dir" || -L "$sdkmanager_dir" ]]; then
        echo "Found an incomplete command-line tools directory at $sdkmanager_dir." >&2
        echo "Move it aside or choose a different SDK directory, then retry." >&2
        exit 1
    fi

    echo "Downloading Android command-line tools revision ${CMDLINE_TOOLS_REVISION}..."
    temporary_dir="$(mktemp -d)"
    archive="${temporary_dir}/commandlinetools-linux.zip"
    curl --fail --location --silent --show-error \
        --output "$archive" "$CMDLINE_TOOLS_URL"
    printf '%s  %s\n' "$CMDLINE_TOOLS_SHA256" "$archive" | sha256sum --check

    mkdir -p "${temporary_dir}/unpacked" "${sdk_root}/cmdline-tools"
    unzip -q "$archive" -d "${temporary_dir}/unpacked"
    if [[ ! -x "${temporary_dir}/unpacked/cmdline-tools/bin/sdkmanager" ]]; then
        echo "The downloaded archive did not contain the expected sdkmanager." >&2
        exit 1
    fi
    mv -- "${temporary_dir}/unpacked/cmdline-tools" "$sdkmanager_dir"
fi

export ANDROID_HOME="$sdk_root"
export ANDROID_SDK_ROOT="$sdk_root"

echo "Review and accept the Android SDK licenses if you agree to them."
"$sdkmanager" --sdk_root="$sdk_root" --licenses

echo "Installing SDK Platform 35, Build Tools, NDK, CMake, and Platform Tools..."
"$sdkmanager" --sdk_root="$sdk_root" --install \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "ndk;28.2.13676358" \
    "cmake;3.22.1"

local_properties="${SCRIPT_DIR}/local.properties"
local_properties_temp="$(mktemp "${SCRIPT_DIR}/.local.properties.XXXXXX")"
if [[ -f "$local_properties" ]]; then
    awk -v sdk_line="sdk.dir=${sdk_root}" '
        BEGIN { updated = 0 }
        /^sdk\.dir[[:space:]]*=/ {
            if (!updated) print sdk_line
            updated = 1
            next
        }
        { print }
        END { if (!updated) print sdk_line }
    ' "$local_properties" > "$local_properties_temp"
else
    printf 'sdk.dir=%s\n' "$sdk_root" > "$local_properties_temp"
fi
mv -- "$local_properties_temp" "$local_properties"
local_properties_temp=""

echo "Android SDK setup is complete. Gradle will use ${local_properties}."
echo "Build the debug APK with: cd android && ./gradlew assembleDebug"
