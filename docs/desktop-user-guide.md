# sayTypist desktop — user guide

sayTypist turns speech into text on **Windows** and **Linux**. Press a hotkey,
speak, and the words are typed into the application you are working in. Speech
recognition runs on your computer with [Vosk](https://alphacephei.com/vosk)
models — no account, no cloud, no audio leaves the machine.

*نسخهٔ فارسی این راهنما: [desktop-user-guide.fa.md](desktop-user-guide.fa.md)*

---

## 1. Install

| System | How |
| --- | --- |
| Linux, any distribution | `sayTypist-<version>-x86_64.AppImage` — make it executable and run it. Java is **not** needed. |
| Debian / Ubuntu | `sudo apt install ./sayTypist_<version>_amd64.deb` |
| Fedora / openSUSE | `sudo dnf install ./sayTypist-<version>.x86_64.rpm` (or `zypper`) |
| Windows 10/11 | `sayTypist-<version>.msi`, or unzip the portable folder and run `sayTypist.exe` |

Supported: **x86_64** on Windows and on glibc based Linux. Linux ARM and
Alpine/musl are not supported by the official Vosk native library. See
[Which systems work](../desktop/README.md#which-systems-work).

## 2. First start

1. The window opens on the **Models** tab. On the first start a panel titled
   *What this app needs* explains anything that is still missing, with the exact
   command for your distribution.
2. Download a model. Good choices:
   - Persian: `vosk-model-small-fa-0.5` (60 MB, fast) or `vosk-model-fa-0.42`
     (1.6 GB, more accurate)
   - English: `vosk-model-small-en-us-0.15` (40 MB) or `vosk-model-en-us-0.22`
     (1.8 GB)
   - Every model of <https://alphacephei.com/vosk/models> is offered; the list
     shows size, quality class, license and notes. Big models ask for
     confirmation and check the free disk space.
3. Press **Use** on the downloaded model. It becomes the *active* model.
4. Dictate: press **Ctrl+Alt+Space** (or the *Start dictation* button) and speak.
   Press the hotkey again to stop. **Esc** cancels the phrase that is being
   typed.

## 3. The window

- **Models** — left: installed models (use / export as .zip / delete), right: the
  catalog with search and a language filter. Downloads run in the background and
  resume after a broken connection.
- **Transcribe** — turn an audio file into text (see below).
- **Settings** — hotkey, microphone, typing method, live typing, automatic space,
  capitalization, clipboard copying, tray behaviour, and the **models folder**.
- **About** — version, developer, license, credits, folders in use, links, and a
  short quick start.
- Top bar — status, language switch (English / فارسی), log toggle, dictation
  button. The language choice is saved; Persian switches the whole window to
  right-to-left.
- The log at the bottom (toggle *Show log*) explains what the app is doing and
  is the first place to look when something does not work.

## 4. Typing methods (Linux)

The typed text has to be injected into the focused window. Depending on the
session, sayTypist uses:

| Method | When | Notes |
| --- | --- | --- |
| `xdotool` | X11 | `sudo apt install xdotool`; best choice on X11 |
| `wtype` | Wayland without portal (Sway, Hyprland, …) | `sudo apt install wtype` |
| `ydotool` | X11 + Wayland | needs the `ydotoold` daemon; never shows a permission dialog |
| `clipboard` | any | pastes each phrase with Ctrl+V, no live typing |
| `copy` | any | only puts the phrase in the clipboard; you press Ctrl+V |

**GNOME and KDE Wayland** ask for “Remote Desktop” permission whenever an
application simulates keystrokes, and the answer is not remembered. Two reliable
choices there:

1. Install and start `ydotool` once (`sudo ydotoold &`, or
   `sudo systemctl enable --now ydotoold`), then select **ydotool** in the
   Settings tab. Recognition is typed into every application, without dialogs.
2. Select **copy** and press Ctrl+V yourself.

Every phrase is copied to the clipboard by default as well, so nothing is lost
even if an application refuses the paste.

On **Windows** no extra tools exist or are needed: typing uses the native
`SendInput` API (works with Persian, Arabic, CJK), the hotkey uses
`RegisterHotKey`.

## 5. Models folder

Default locations:

| | Linux | Windows |
| --- | --- | --- |
| Models | `~/.local/share/sayTypist/models` | `%LOCALAPPDATA%\sayTypist\models` |
| Downloads (temporary) | `~/.local/share/sayTypist/downloads` | `%LOCALAPPDATA%\sayTypist\downloads` |
| Settings | `~/.config/sayTypist/config.json` | `%APPDATA%\sayTypist\config.json` |

The **Models folder** card in the Settings tab shows the current folder, the free
space, opens it in the file manager, and lets you choose another folder. When
there are already models, the app offers to move them to the new folder.
Folders of the older “Sayboard” desktop build are renamed automatically on the
first start (models and settings are kept).

## 6. Sharing models without internet

- **Local network:** in the Android app, open *Models → share button → Share my
  models on this network*; on the other phone use *Scan the network*. The model is
  copied directly into the models folder.
- **No network at all:** on the phone, the share icon next to a model creates a
  `.zip`; send it with Bluetooth / Nearby Share / cable and import it here with
  **Import .zip…** (or the other way round: **Export .zip** here, import on the
  phone).

## 7. Troubleshooting

| Symptom | What to do |
| --- | --- |
| Nothing is typed | Press **What do I need to install?** in the banner, or Settings → *Details and install commands*. On GNOME/KDE Wayland choose `ydotool` (after `sudo ydotoold &`) or `copy`. |
| Nothing is in the clipboard | Check *Also copy every phrase to the clipboard* in the Settings tab; the **Copy last phrase** button next to the preview always works. |
| Hotkey does nothing | The combination is taken or reserved by the desktop; choose another (e.g. `f9`) and press *Save hotkey*. On Wayland the global hotkey works through XWayland; the button and the tray menu always work. |
| No microphone | Restart the app after plugging it in, and pick the device in the Settings tab. |
| Recognition is slow | Use a small model, and enable *Keep the model in memory between sessions*. |
| Big model uses too much RAM | Settings → *Unload model from RAM*. |
| Black window / app does not start | Start it with software rendering: `SKIKO_RENDER_API=SOFTWARE ./sayTypist-…AppImage`. |
| Persian text is shown as boxes | Not expected (the font is bundled); if it happens in another app, install `fonts-noto-core` / `noto-fonts`. |
| Window disappears on close | It keeps running in the tray; use the tray menu to show it again or quit. |

## 8. Privacy

- Audio is only captured while dictation is running; it is never written to disk.
- Models are downloaded only when you press *Download* — from
  `alphacephei.com`. Model sharing between devices happens on your own network.
- No analytics, no telemetry, no account.

## 9. Reporting a problem

Include: the version (About tab), your operating system and session type (the
*Desktop session* line in the requirements panel), the typing method, and the
last lines of the log.
