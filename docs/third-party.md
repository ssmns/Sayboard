# Third party components, logos and licenses

sayTypist itself is licensed under the **GNU General Public License v3.0**
([LICENSE.txt](../LICENSE.txt)), the same license as the project it was forked
from. It is a fork of [Sayboard](https://github.com/ElishaAz/Sayboard) by
Elisha Azaria, which in turn is based on
[Felicis/vosk-android-demo](https://github.com/Felicis/vosk-android-demo).

## Libraries

| Component | License | Used for |
| --- | --- | --- |
| [Vosk](https://alphacephei.com/vosk) / vosk-api (Java + Android) | Apache-2.0 | offline speech recognition engine |
| [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform), Compose Desktop, Skiko, Kotlin, kotlinx-coroutines, kotlinx-serialization | Apache-2.0 | user interface, concurrency, JSON |
| [JNA](https://github.com/java-native-access/jna) / jna-platform | Apache-2.0 / LGPL-2.1 (dual) | native calls: `SendInput`, `RegisterHotKey`, `XGrabKey` |
| [AndroidX](https://developer.android.com/jetpack) (appcompat, core, lifecycle, compose) | Apache-2.0 | Android app |
| [JetPref](https://github.com/Chainfire/jetpref) | Apache-2.0 | Android settings |
| [EventBus](https://github.com/greenrobot/EventBus) | Apache-2.0 | Android download events |
| [Material Icons](https://fonts.google.com/icons) | Apache-2.0 | icons in both apps |
| [aboutlibraries](https://github.com/mikepenz/AboutLibraries) | Apache-2.0 | Android license screen |
| [Vazirmatn](https://github.com/rastikerdar/vazirmatn) font | SIL Open Font License 1.1 | bundled UI font (Persian + Latin) |

The full text of the Vazirmatn license is bundled with the desktop app in
`desktop/src/main/resources/fonts/OFL-Vazirmatn.txt` and, in packaged builds,
under `licenses/`.

## Speech models

The app itself ships **no** models. They are downloaded from
<https://alphacephei.com/vosk/models> on request, or imported from another
device. Every model has its own license — the model list shows it for each entry
(Apache-2.0 for most of them, but also AGPL-3.0, GPL-3.0, LGPL-3.0, MIT and
CC-BY-NC-SA). Check the license shown in the model list (or on the Vosk page)
before using a model, especially in a commercial or redistributed product.

## Logo and artwork

The logo in [`logo/`](../logo) (`Page 3.svg`, `saytypist.png`,
`saytypist-mark.png`) belongs to the maintainer of this fork. The legacy artwork
of the upstream Sayboard project is not used by sayTypist any more; the icon
files in `app/` and `desktop/` are generated from `logo/Page 3.svg` by
`logo/make-icons.py`.

If you publish your own fork, replace the logo files and run
`python3 logo/make-icons.py` to regenerate every icon.
