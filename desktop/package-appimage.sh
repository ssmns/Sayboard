#!/usr/bin/env bash
# Builds a self-contained Linux AppImage of the sayTypist desktop app.
#
#   ./package-appimage.sh
#
# It bundles a Java runtime, so the result runs on any x86_64 Linux distribution
# without Java being installed. Requirements: JDK 17 (for jpackage), curl,
# and a working internet connection the first time (appimagetool is downloaded).
set -euo pipefail

cd "$(dirname "$0")"
HERE="$(pwd)"
GRADLEW="$HERE/../gradlew"
if [ ! -x "$GRADLEW" ]; then
    GRADLEW="$HERE/../gradlew.bat"
fi

if [ -z "${VERSION:-}" ]; then
    VERSION="$(sed -n 's/^versionName=//p' "$HERE/../version.properties")"
fi
APP_IMAGE="build/compose/binaries/main/app/sayTypist"
APPDIR="build/appimage/sayTypist.AppDir"
OUTPUT="build/sayTypist-${VERSION}-x86_64.AppImage"

echo "==> Building the app image with jpackage (this also builds the app)"
"$GRADLEW" --console=plain :desktop:createDistributable

if [ ! -x "$APP_IMAGE/bin/sayTypist" ]; then
    echo "jpackage did not produce $APP_IMAGE/bin/sayTypist" >&2
    exit 1
fi

echo "==> Assembling the AppDir"
rm -rf "$APPDIR" "$OUTPUT"
mkdir -p "$APPDIR"
cp -a "$APP_IMAGE"/. "$APPDIR"/
cp icons/saytypist.png "$APPDIR/sayTypist.png"
cp icons/saytypist.png "$APPDIR/.DirIcon"

cat > "$APPDIR/AppRun" <<'RUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/bin/sayTypist" "$@"
RUN
chmod +x "$APPDIR/AppRun"

cat > "$APPDIR/sayTypist.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=sayTypist
Comment=sayTypist - offline voice typing with Vosk models
Exec=sayTypist
Icon=sayTypist
Categories=Utility;
Terminal=false
DESKTOP

echo "==> Packing the AppImage"
TOOL="${APPIMAGETOOL:-build/appimage/appimagetool-x86_64.AppImage}"
if command -v appimagetool >/dev/null 2>&1; then
    TOOL="$(command -v appimagetool)"
fi
if [ ! -x "$TOOL" ]; then
    mkdir -p "$(dirname "$TOOL")"
    echo "    downloading appimagetool"
    curl -fsSL -o "$TOOL" \
        "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
    chmod +x "$TOOL"
fi

# AppImage tools need FUSE; extract-and-run works everywhere (containers too)
export APPIMAGE_EXTRACT_AND_RUN=1
ARCH=x86_64 "$TOOL" "$APPDIR" "$OUTPUT"

echo
echo "Done: $OUTPUT"
