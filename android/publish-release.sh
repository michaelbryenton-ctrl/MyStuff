#!/usr/bin/env bash
# Dispatch the remote build/upload workflow; no binaries leave Replit here.
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)"
cd "$root"
if [[ $# != 2 || ( "$1" != "--check" && "$1" != "--publish" ) ]]; then
    echo "Usage: bash android/publish-release.sh --check|--publish docs/releases/VERSION.md" >&2
    exit 2
fi
mode="$1"
version="$(sed -n 's/^[[:space:]]*versionName "\([^"]*\)".*/\1/p' android/app/build.gradle)"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]
[[ "$2" == "docs/releases/$version.md" && -s "$2" ]] || {
    echo "Expected nonempty release notes at docs/releases/$version.md." >&2; exit 1;
}
origin="$(git remote get-url origin)"
case "$origin" in
    https://github.com/*) repo="${origin#https://github.com/}" ;;
    https://github.replit.internal/*) repo="${origin#https://github.replit.internal/}" ;;
    git@github.com:*) repo="${origin#git@github.com:}" ;;
    *) echo "origin must point to the intended GitHub repository." >&2; exit 1 ;;
esac
repo="${repo%.git}"
[[ "$repo" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]]
git diff --quiet && git diff --cached --quiet || {
    echo "Commit and sync your source to GitHub before releasing." >&2; exit 1;
}
remote="$(gh api "repos/$repo/commits/main" --jq .sha)"
[[ "$(git rev-parse HEAD)" == "$remote" ]] || {
    echo "GitHub main does not match this workspace. Sync source before releasing." >&2; exit 1;
}
state="$(gh api "repos/$repo/actions/workflows/android-release.yml" --jq .state)"
[[ "$state" == "active" ]] || {
    echo "Enable the Android release workflow on GitHub first." >&2; exit 1;
}
echo "Validated synced source and active release workflow for $repo v$version."
if [[ "$mode" == "--check" ]]; then
    echo "Check only: no workflow started and no releases changed."
    exit 0
fi
gh workflow run android-release.yml --repo "$repo" --ref main
echo "GitHub will build, verify, and upload the release:"
echo "https://github.com/$repo/actions/workflows/android-release.yml"
