#!/usr/bin/env bash
# Publish prebuilt, validated Android artifacts without changing remote main.
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$root"

usage() {
    echo "Usage: bash android/publish-release.sh --check|--publish RELEASE_NOTES.md" >&2
    echo "Requires the matching APK/PDF in generated-artifacts/android, Android SDK, and authenticated gh." >&2
}
if [[ $# != 2 || ( "$1" != "--check" && "$1" != "--publish" ) ]]; then
    usage
    exit 2
fi
mode="$1"
notes="$2"
[[ -s "$notes" ]] || { echo "Release notes are missing or empty: $notes" >&2; exit 1; }
command -v gh >/dev/null || { echo "GitHub CLI (gh) is required." >&2; exit 1; }

origin="$(git remote get-url origin)"
case "$origin" in
    https://github.com/*) repo="${origin#https://github.com/}" ;;
    https://github.replit.internal/*) repo="${origin#https://github.replit.internal/}" ;;
    git@github.com:*) repo="${origin#git@github.com:}" ;;
    *) echo "origin must point to the intended GitHub repository." >&2; exit 1 ;;
esac
repo="${repo%.git}"
[[ "$repo" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] || {
    echo "Cannot safely identify the GitHub repository from origin." >&2; exit 1;
}
version="$(sed -n 's/^[[:space:]]*versionName "\([^"]*\)".*/\1/p' android/app/build.gradle)"
code="$(sed -n 's/^[[:space:]]*versionCode \([0-9]*\).*/\1/p' android/app/build.gradle)"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ && "$code" =~ ^[0-9]+$ ]] || {
    echo "Expected one numeric Android versionName and versionCode." >&2; exit 1;
}
tag="v$version"
apk="$root/generated-artifacts/android/xroar-$version-debug.apk"
guide="$root/generated-artifacts/android/xroar-$version-user-guide.pdf"
[[ -s "$apk" && -s "$guide" ]] || {
    echo "Build the $version APK and generate its guide before publishing." >&2; exit 1;
}
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sed -n 's/^sdk\.dir=//p' android/local.properties)}}"
[[ -x "$sdk/build-tools/35.0.0/aapt2" ]] || {
    echo "Restore the configured Android SDK before release validation." >&2; exit 1;
}
bash android/test-apk-update.sh "$apk"
metadata="$("$sdk/build-tools/35.0.0/aapt2" dump badging "$apk")"
grep -Fq "versionCode='$code' versionName='$version'" <<< "$metadata" || {
    echo "APK version does not match the source; rebuild it." >&2; exit 1;
}
git diff --quiet && git diff --cached --quiet || {
    echo "Commit tracked changes before releasing, so the tag identifies the source." >&2; exit 1;
}
commit="$(git rev-parse HEAD)"
gh api user --jq .login >/dev/null
existing="$(gh api "repos/$repo/releases" --paginate \
    --jq ".[] | select(.tag_name == \"$tag\") | .html_url")"
[[ -z "$existing" ]] || {
    echo "Release already exists; leaving it and its files unchanged: $existing" >&2; exit 1;
}
refs="$(gh api "repos/$repo/git/matching-refs/tags/$tag" \
    --jq ".[] | select(.ref == \"refs/tags/$tag\") | .object.sha")"
[[ -z "$refs" || "$refs" == "$commit" ]] || {
    echo "Tag $tag already points elsewhere; it will not be moved." >&2; exit 1;
}
echo "Validated $repo $tag (versionCode $code), source $commit"
if [[ "$mode" == "--check" ]]; then
    echo "Check only: no tags, branches, or releases changed."
    exit 0
fi

# Push only this release tag, not main or any other branch. Never force-update.
git push origin "$commit:refs/tags/$tag"
# Keep incomplete uploads private. A failure leaves a draft for inspection.
gh release create "$tag" "$apk" "$guide" --repo "$repo" --verify-tag --draft \
    --title "XRoar Android $version" --notes-file "$notes"
verify="$(mktemp -d)"
trap 'rm -rf -- "$verify"' EXIT
gh release download "$tag" --repo "$repo" --dir "$verify" \
    --pattern "$(basename "$apk")" --pattern "$(basename "$guide")"
cmp "$apk" "$verify/$(basename "$apk")"
cmp "$guide" "$verify/$(basename "$guide")"
gh release edit "$tag" --repo "$repo" --draft=false --latest
gh release view "$tag" --repo "$repo" --json url --jq .url
echo "Published: both downloaded assets match the local originals."
