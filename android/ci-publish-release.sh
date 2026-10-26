#!/usr/bin/env bash
# Runs on GitHub's runner, bypassing the Replit binary-upload failure.
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$root"
: "${GITHUB_REPOSITORY:?Run this through the Android release workflow}"
: "${GITHUB_SHA:?Missing workflow source revision}"
: "${GH_TOKEN:?Missing workflow token}"
: "${VERSION:?Missing Android version}"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
tag="v$VERSION"
apk="generated-artifacts/android/xroar-$VERSION-debug.apk"
guide="generated-artifacts/android/xroar-$VERSION-user-guide.pdf"
notes="docs/releases/$VERSION.md"
[[ -s "$apk" && -s "$guide" && -s "$notes" ]]
bash android/test-apk-update.sh "$apk"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
code="$(sed -n 's/^[[:space:]]*versionCode \([0-9]*\).*/\1/p' android/app/build.gradle)"
metadata="$("$sdk/build-tools/35.0.0/aapt2" dump badging "$apk")"
grep -Fq "versionCode='$code' versionName='$VERSION'" <<< "$metadata"

id="$(gh api "repos/$GITHUB_REPOSITORY/releases" --paginate \
    --jq ".[] | select(.tag_name == \"$tag\") | .id")"
if [[ -n "$id" ]]; then
    draft="$(gh api "repos/$GITHUB_REPOSITORY/releases/$id" --jq .draft)"
    [[ "$draft" == "true" ]] || {
        echo "Refusing to change published release $tag." >&2; exit 1;
    }
else
    gh release create "$tag" --repo "$GITHUB_REPOSITORY" --target "$GITHUB_SHA" \
        --draft --title "XRoar Android $VERSION" --notes-file "$notes"
    id="$(gh api "repos/$GITHUB_REPOSITORY/releases" --paginate \
        --jq ".[] | select(.tag_name == \"$tag\") | .id")"
    [[ -n "$id" ]]
fi
# Rebuilds can differ byte-for-byte. Replacing these two *unpublished draft*
# assets makes reruns recoverable; a public release is never overwritten.
gh release upload "$tag" "$apk" "$guide" --repo "$GITHUB_REPOSITORY" --clobber
verify="$(mktemp -d)"
trap 'rm -rf -- "$verify"' EXIT
gh release download "$tag" --repo "$GITHUB_REPOSITORY" --dir "$verify" \
    --pattern "$(basename "$apk")" --pattern "$(basename "$guide")"
cmp "$apk" "$verify/$(basename "$apk")"
cmp "$guide" "$verify/$(basename "$guide")"
(
    cd generated-artifacts/android
    sha256sum "$(basename "$apk")" "$(basename "$guide")"
) > "$verify/SHA256SUMS"
gh release upload "$tag" "$verify/SHA256SUMS" --repo "$GITHUB_REPOSITORY" --clobber
gh release edit "$tag" --repo "$GITHUB_REPOSITORY" \
    --title "XRoar Android $VERSION" --notes-file "$notes" --draft=false --latest
url="$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json url --jq .url)"
echo "Published and download-verified: $url"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
    printf '## Android release published\n\n%s\n\nAPK and PDF downloaded and verified against build outputs. Original signing identity preserved.\n' \
        "$url" >> "$GITHUB_STEP_SUMMARY"
fi
