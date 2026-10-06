#!/usr/bin/env bash
# Preserve the supplied pixel artwork and produce Android launcher densities.
set -euo pipefail
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
source="${1:-$root/artwork/xroar-icon.png}"
resources="$root/app/src/main/res"
for spec in "mdpi 48 108 64" "hdpi 72 162 96" "xhdpi 96 216 128" \
            "xxhdpi 144 324 192" "xxxhdpi 192 432 256"; do
    read -r density legacy canvas art <<< "$spec"
    magick "$source" -filter point -resize "${legacy}x${legacy}" \
        "$resources/mipmap-$density/ic_launcher.png"
    magick "$source" -filter point -resize "${art}x${art}" \
        -background none -gravity center -extent "${canvas}x${canvas}" \
        "$resources/mipmap-$density/ic_launcher_foreground.png"
done
mkdir -p "$root/build/previews"
magick "$source" -filter point -resize 320x320 -background black \
    -gravity center -extent 512x512 "$root/build/previews/xroar-launcher-icon.png"
