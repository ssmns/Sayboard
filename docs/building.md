# Building sayTypist

Everything is built from this repository with the Gradle wrapper; no external
build scripts are needed. Two modules live here:

| Module | What it is | Output |
| --- | --- | --- |
| `app` | the Android keyboard (IME) | APKs |
| `desktop` | the Windows/Linux app (Kotlin + Compose Desktop) | AppImage, .deb, .rpm, .msi, portable folder |

## Requirements

- **JDK 17** (`java -version`). Android Studio's JDK works too.
- **Android SDK** for the `app` module: platform 34 and build-tools 34
  (`sdkmanager "platforms;android-34" "build-tools;34.0.0"`), and a
  `local.properties` with `sdk.dir=/path/to/Android/Sdk` (or `ANDROID_HOME`).
- Linux packaging: `dpkg-deb` for `.deb`, `rpmbuild` for `.rpm`; the AppImage
  additionally needs `appimagetool` (downloaded automatically by the script) and
  `curl`.
- Windows packaging: [WiX Toolset 3.x](https://github.com/wixtoolset/wix3) for `.msi`.

## Android

```bash
./gradlew assembleDebug                 # debug APKs (all ABIs + universal)
./gradlew assembleRelease               # needs a signing config
# -> app/build/outputs/apk/<variant>/sayTypist_<abi>.apk
```

The version comes from [`version.properties`](../version.properties)
(`versionName`, `versionCode`).

## Desktop

```bash
./gradlew :desktop:run                  # run from source
./gradlew :desktop:test                 # unit tests (offline)
cd desktop && ./package-appimage.sh     # -> desktop/build/sayTypist-<version>-x86_64.AppImage
./gradlew :desktop:packageDeb           # -> desktop/build/compose/binaries/main/deb
./gradlew :desktop:packageRpm           # -> desktop/build/compose/binaries/main/rpm
./gradlew :desktop:createDistributable  # portable folder (Linux or Windows)
cd packaging/arch && makepkg -f        # Arch Linux package (.pkg.tar.zst)
gradlew.bat :desktop:packageMsi         # Windows installer (needs WiX)
```

The AppImage bundles a Java runtime, the Vosk native library and the UI fonts, so
the target machine needs neither Java nor extra packages (except the input tools
for the chosen typing method on Linux).

### Versioning

One file controls the version of both apps:

```properties
# version.properties
versionName=1.0.0
versionCode=500000
```

- `versionName` is shown in the About tab, in the window title, used by the
  installers and in the AppImage file name.
- `versionCode` is the Android upgrade counter and must only grow.
- The desktop app gets the value through a generated `Version.kt`
  (`desktop/build/generated/version/…`), the Android app reads
  `version.properties` in `app/build.gradle`.

Bump both before a release; nothing else has to be edited.

### Regenerating the model catalog and the icons

```bash
./gradlew :desktop:generateCatalog      # catalog/vosk_models.json -> ModelLink.kt + desktop resource
python3 logo/make-icons.py              # logo/Page 3.svg -> all icons (needs rsvg-convert)
```

## Optional runtime tools

The desktop app starts with nothing else installed. These are used when present:

| Tool | Used for |
| --- | --- |
| `xdotool`, `wtype`, `ydotool` | typing into other applications (per session, see the user guide) |
| `ffmpeg` / `ffprobe` | reading MP3, OGG, FLAC, M4A, … in the **Transcribe** tab (WAV needs nothing) |

## Tests

```bash
./gradlew :desktop:test                                        # UI logic, i18n, hints, catalog
SAYBOARD_CLIPBOARD_TESTS=1 xvfb-run -a ./gradlew :desktop:test # also the real clipboard
```

## Project layout

```
app/                     Android keyboard (Kotlin, Compose)
desktop/                 Windows/Linux app (Kotlin, Compose Desktop)
  package-appimage.sh    builds the AppImage
catalog/vosk_models.json the model list shared by both apps
logo/                    the logo (SVG) and make-icons.py
docs/                    user guide (EN/FA), build and third party notes
version.properties       version of the whole project
```

## Common build problems

| Problem | Reason |
| --- | --- |
| `[CXX1103] Requested NDK version 'r25c' could not be parsed` | harmless warning of the old NDK setting in `app/build.gradle`; the build succeeds. |
| `SDK location not found` | create `local.properties` with `sdk.dir=…` or set `ANDROID_HOME`. |
| AppImage shows a black window | start it with `SKIKO_RENDER_API=SOFTWARE`; the machine lacks working OpenGL. |
| `:desktop:packageRpm` fails | install `rpmbuild` (`rpm-build`, `rpmdevtools`). |
| Arch package is old | `cd packaging/arch && makepkg -f` after `:desktop:createDistributable`. |
