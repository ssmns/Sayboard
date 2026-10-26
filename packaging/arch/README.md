# Arch Linux package

`PKGBUILD` builds `saytypist-<version>-1-x86_64.pkg.tar.zst`, a self-contained
package (bundled Java runtime + Vosk native library) that installs:

| Path | Content |
| --- | --- |
| `/opt/saytypist` | the app and its runtime |
| `/usr/bin/saytypist` | launcher on `PATH` |
| `/usr/share/applications/saytypist.desktop` | menu entry |
| `/usr/share/icons/hicolor/512x512/apps/saytypist.png` | icon |
| `/usr/share/doc/saytypist/` | user guide (English + Persian) |
| `/usr/share/licenses/saytypist/` | GPL-3.0 and third party notices |

## Build

```bash
./gradlew :desktop:createDistributable     # from the repository root
cd packaging/arch
makepkg -f
```

Install with `sudo pacman -U saytypist-1.0.0-1-x86_64.pkg.tar.zst`.

To publish on the AUR, replace the local `appdir` with a `source=()` entry that
points at a release tarball of `desktop/build/compose/binaries/main/app/sayTypist`
and add the matching checksum.
