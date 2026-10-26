# sayTypist for Windows and Linux (desktop)

**sayTypist** is a voice typing app for **Windows** and **Linux**, written in
Kotlin with Compose Desktop and Vosk. Press a global hotkey, speak, and the text is typed into the
window that has the focus — offline, without accounts and without a browser.

It shares the model catalog with the sayTypist keyboard for Android in this
repository (`catalog/vosk_models.json`), so both platforms offer the same models, including
the big multi-gigabyte ones.

```
desktop/
├── build.gradle.kts          Compose Desktop app + jpackage packaging
├── package-appimage.sh       builds a self-contained sayTypist-x86_64.AppImage
├── icons/                    app icon (Linux/Windows packaging)
└── src/main/kotlin/…         the app
    src/main/resources/       vosk_models.json (generated) + tray icon
```

## Requirements

Only a **JDK 17** is needed to build and run from source; the Gradle wrapper
downloads the rest. The packaged AppImage/installer bundles its own Java runtime,
so **end users do not need Java at all**.

## Which systems work?

| System | State | Typing | Global hotkey |
| --- | --- | --- | --- |
| **Windows 10/11 (x64)** | supported | native `SendInput` — nothing to install | `RegisterHotKey` |
| **Linux, glibc, x86_64** (Debian/Ubuntu, Fedora, Arch, openSUSE, …) | supported | `xdotool` (X11), `ydotool`/`wtype` (Wayland), clipboard, copy | `XGrabKey` (X11 / XWayland) |
| **Linux ARM** (Raspberry Pi, ARM laptops) | **not supported** | – | – |
| **Alpine / musl** | **not supported** | – | – |
| macOS | builds, but untested and not packaged | – | – |

The reason for the limits is the official Vosk JVM artifact: it contains the
native engine only for `linux-x86-64`, `win32-x86-64` and macOS, and those Linux
libraries need glibc. Everything else in the app (models, downloads, sharing,
translations) is pure Java/Kotlin and works wherever those two do.

On **Windows** there is nothing else to install: typing, the hotkey and the
clipboard all use the operating system directly. The requirements panel says so
explicitly instead of showing Linux commands.

On **Linux** the same window works on every distribution; only the installation
hint changes. The app reads `/etc/os-release` (including `ID_LIKE`) and shows the
command for apt, pacman, dnf, zypper, eopkg, nix, apk, xbps or emerge — and a
generic sentence for anything unknown. The *Models*, *Settings* and *About* tabs,
the two UI languages (English/Persian) and the requirements panel are identical
on every platform; only the checks and hints are platform-specific.

## Run from source

```bash
./gradlew :desktop:run            # Linux / macOS
gradlew.bat :desktop:run          # Windows
```

## Build the installers

### Linux AppImage (recommended, no Java needed on the target machine)

```bash
cd desktop
./package-appimage.sh
# -> desktop/build/sayTypist-1.0.0-x86_64.AppImage
```

The script runs `jpackage` (through the Compose Desktop plugin) to create a
self-contained app image, wraps it into an AppDir and packs it with
`appimagetool` (downloaded automatically the first time; set `APPIMAGETOOL` to
use your own copy). The AppImage is about 120 MB because it bundles a Java
runtime plus the Vosk native library; it is a single executable file:

```bash
chmod +x sayTypist-1.0.0-x86_64.AppImage
./sayTypist-1.0.0-x86_64.AppImage
```

### Linux .deb

```bash
./gradlew :desktop:packageDeb
# -> desktop/build/compose/binaries/main/deb/

./gradlew :desktop:packageRpm
# -> desktop/build/compose/binaries/main/rpm/   (needs rpmbuild)
```

`.deb` needs `dpkg-deb` (part of `dpkg`), `.rpm` needs `rpmbuild` (part of
`rpm-build`/`rpmdevtools`) on the build machine. The AppImage needs neither.

### Windows

```bat
gradlew.bat :desktop:packageMsi
:: needs WiX Toolset 3.x on the build machine
:: or, for a portable folder without an installer:
gradlew.bat :desktop:createDistributable
:: -> desktop\build\compose\binaries\main\app\sayTypist\sayTypist.exe
```

The portable folder can be zipped and copied to any 64-bit Windows machine.

## Using it

1. In the **Models** tab pick a model and press **Download**. Models bigger than
   a "small" one ask for confirmation first and show the download size, the RAM
   they need and the free disk space.
   - Persian: `vosk-model-small-fa-0.5` (60 MB, fast) or `vosk-model-fa-0.42`
     (1.6 GB, more accurate)
   - English: `vosk-model-small-en-us-0.15` (40 MB) or `vosk-model-en-us-0.22`
     (1.8 GB)
2. Select the model and press **Use**.
3. Press the hotkey (**Ctrl+Alt+Space** by default) to start listening and again
   to stop. The text is typed into the focused window; **Esc** cancels the phrase
   that is being typed.

The **Settings** tab has the hotkey, microphone, typing method, typing while
speaking, automatic space, capitalization, whether the model stays in memory and
whether the app starts in the system tray — plus a **System requirements**
section that re-runs the checks on demand.

Downloads are resumable: a broken connection continues where it stopped instead
of restarting a 2 GB download.

### Typing methods on Linux

| Typing method | Session | Notes |
| --- | --- | --- |
| `xdotool` | X11 | best choice: `sudo apt install xdotool` |
| `wtype` | Wayland without portal (Sway, Hyprland, …) | `sudo apt install wtype` |
| `ydotool` | X11 + Wayland | needs the `ydotoold` service; never shows a permission dialog |
| `clipboard` | any | pastes each phrase with Ctrl+V, no typing while speaking |
| `copy` | any | only copies the phrase to the clipboard, sends no keystrokes at all |

On **GNOME and KDE Wayland** simulated keystrokes can raise a “Remote Desktop”
permission window, which is easy to miss and is not remembered between
pastes — that is why the app does not pick `wtype` there. Two reliable choices
on those desktops:

1. **Automatic typing with ydotool** (recommended): start the daemon once
   (`sudo ydotoold &`, or `sudo systemctl enable --now ydotoold` if your
   distribution ships the service) and choose `ydotool` in the Settings tab.
   It types in every Wayland and X11 application and never asks for permission.
   The app also picks it automatically the next time it starts.
2. **Copy method**: choose `copy` in the Settings tab. Every phrase is written to
   the clipboard and you press Ctrl+V where you need it; no keystrokes, so no
   dialogs at all.

Every phrase is also copied to the clipboard by default anyway, so nothing is
lost even if an application refuses the paste. Settings are applied immediately
(there is a *Save hotkey* button for the hotkey field only), and the
requirements panel shows where the settings file lives.

On Windows the app uses the Win32 `SendInput` API with unicode key events, so
Persian, Arabic and CJK text is typed exactly and no extra tool is needed.

**The app tells you what is missing.** It checks these requirements on every
start: the keyboard injection tools, the global hotkey, the microphone and the
models. Anything that is not available is shown as a banner in the window, and
the **“What do I need to install?”** button opens a panel with the exact
installation command for your distribution (apt/pacman/dnf/zypper/…), ready to
copy. Nothing has to be diagnosed from a terminal.

The log at the bottom of the window always shows which method is in use, and
the *Desktop session* line of the requirements panel shows how your session was
detected.

## Models without an internet connection

The Android app shares its models offline (see “Sharing models without the
internet” in the main README): next to each installed model on the phone there is
a share icon that packs the model into a `.zip`. Copy that file to the computer
(cable, Bluetooth, memory card) and install it here with **Import .zip…**.
In the other direction, **Export .zip** in the Models tab creates an archive the
phone can import.

The **Models folder** card in the Settings tab shows the exact path, the free
space and lets you pick another folder (existing models can be moved there in
one go). Folders of the old *Sayboard* desktop build (`…/sayboard`) are renamed
automatically on the first start, so models and settings are kept.

## Where things are stored

| | Linux | Windows |
| --- | --- | --- |
| Models | `~/.local/share/sayTypist/models` | `%LOCALAPPDATA%\sayTypist\models` |
| Downloads | `~/.local/share/sayTypist/downloads` | `%LOCALAPPDATA%\sayTypist\downloads` |
| Settings | `~/.config/sayTypist/config.json` | `%APPDATA%\sayTypist\config.json` |

## Adding a model

All models from <https://alphacephei.com/vosk/models> can be installed, including
the big "server" models. Either download a `.zip` from that site and use
**Import .zip…**, or add it to `catalog/vosk_models.json` and run:

```bash
./gradlew :desktop:generateCatalog
```

which regenerates this app's catalog (and the Android `ModelLink.kt`) from that
single JSON file.

## Language

The window is available in **English** and **Persian** — switch it with the
language button in the top bar (Persian switches the whole layout to right to
left). The choice is saved.

The *About* tab shows the version, the developer, the license (GPL-3.0), the
credits and the folders in use. Put your own name and link there by editing
`desktop/src/main/kotlin/com/elishaazaria/sayboard/desktop/AppInfo.kt`
(`MAINTAINER`, `MAINTAINER_URL`).

## Version

The version of both apps lives in [`version.properties`](../version.properties).
Bump `versionName`/`versionCode` there and everything (About tab, window title,
installers, AppImage name, Android APK) follows.

## Links

- This project: <https://github.com/ssmns/Sayboard>
- Upstream Sayboard: <https://github.com/ElishaAz/Sayboard>
- User guide: [English](../docs/desktop-user-guide.md) ·
  [فارسی](../docs/desktop-user-guide.fa.md)
- Building: [docs/building.md](../docs/building.md)
- Third party and licenses: [docs/third-party.md](../docs/third-party.md)

## Tests

```bash
./gradlew :desktop:test
```

The tests are offline: they check the model catalog and the typing logic
(live corrections with backspaces, capitalization, cancelling a phrase, Persian
text).

## Troubleshooting

- **Nothing is typed** — press **“What do I need to install?”** in the yellow
  banner (or Settings → *Show details*): the app lists the missing tools with the
  exact command for your distribution. You can also try Settings → **Test
  typing**; the log line `Typing method: …` shows what is used.
- **`Typing: none`** — no keyboard injection tool was found (see the table
  above); the clipboard method needs a desktop session with Java's AWT support.
- **No microphone** — the device list is read at start-up; restart the app after
  plugging in a microphone.
- **Hotkey does nothing** — some desktops reserve the combination; choose
  another one (e.g. `f9`) and press **Save settings**. On Wayland the global
  hotkey only works through XWayland; the **Start dictation** button and the tray
  menu always work.
- **Recognition is slow** — small models are the fast ones; enable
  *Keep the model in memory between sessions*.
- **Free the RAM of a big model** — **Unload model from RAM** in the Settings tab.
- **The window is black / the app does not start on an old graphics stack** —
  start it with software rendering: `SKIKO_RENDER_API=SOFTWARE ./sayTypist-…AppImage`
  (the same setting as `-Dskiko.renderApi=SOFTWARE`).
- **Persian text shows as boxes** — the system has no Persian font installed;
  install e.g. `fonts-noto-core` (Debian/Ubuntu) or `noto-fonts` (Arch).
- **The window disappears when I close it** — it keeps running in the system
  tray (dictation still works). Use the tray menu to show the window again or to
  quit. Desktops without a tray (some GNOME setups) need the AppIndicator
  extension, otherwise start the app again.
