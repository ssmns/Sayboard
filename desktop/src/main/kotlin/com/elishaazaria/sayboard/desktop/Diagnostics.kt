package com.elishaazaria.sayboard.desktop

import java.io.File

enum class CheckState { OK, WARNING, ERROR }

/**
 * One line of the "what do I still need to install" list.
 */
data class DiagnosticCheck(
    val title: String,
    val state: CheckState,
    val detail: String,
    val hint: String? = null,
)

/**
 * Checks the optional system requirements (keyboard injection tools, X11 for the
 * global hotkey, headphones/microphone...) so that the app can explain them in
 * its window instead of failing silently in a terminal.
 */
object Diagnostics {

    fun run(
        config: AppConfig,
        hotkeyError: String?,
        typerName: String,
        strings: Strings = Strings.english,
    ): List<DiagnosticCheck> {
        val s = strings
        val checks = mutableListOf<DiagnosticCheck>()
        checks.add(typingCheck(typerName, s))
        checks.add(hotkeyCheck(hotkeyError, config.hotkey, s))
        checks.add(audioCheck(s))
        checks.add(modelCheck(config.model, s))
        if (!AppPaths.isWindows) {
            checks.add(sessionCheck(typerName, s))
            checks.add(ydotoolCheck(typerName, s))
            if (Session.isWayland) {
                checks.add(waylandCheck(typerName, hotkeyError, s))
            }
        }
        // show the problems first so nothing important is below the fold
        return checks.sortedBy {
            when (it.state) {
                CheckState.ERROR -> 0
                CheckState.WARNING -> 1
                CheckState.OK -> 2
            }
        }
    }

    private fun typingCheck(typerName: String, s: Strings): DiagnosticCheck {
        return when (typerName) {
            "none" -> DiagnosticCheck(
                title = s["diag.typing"],
                state = CheckState.ERROR,
                detail = s["diag.typingNone"],
                hint = installHint(s),
            )

            "clipboard" -> DiagnosticCheck(
                title = s["diag.typing"],
                state = CheckState.WARNING,
                detail = s["diag.typingClipboard"],
                hint = installHint(s),
            )

            "copy" -> DiagnosticCheck(
                title = s["diag.typing"],
                state = CheckState.OK,
                detail = s["diag.typingCopy"],
            )

            else -> DiagnosticCheck(
                title = s["diag.typing"],
                state = CheckState.OK,
                detail = s.f("diag.typingUsing", typerName),
            )
        }
    }

    private fun hotkeyCheck(hotkeyError: String?, hotkey: String, s: Strings): DiagnosticCheck {
        return if (hotkeyError == null) {
            DiagnosticCheck(
                title = s["diag.hotkey"],
                state = CheckState.OK,
                detail = s.f("diag.hotkeyOk", hotkey),
            )
        } else {
            DiagnosticCheck(
                title = s["diag.hotkey"],
                state = CheckState.WARNING,
                detail = s.f("diag.hotkeyFail", hotkey, hotkeyError),
            )
        }
    }

    private fun audioCheck(s: Strings): DiagnosticCheck {
        val devices = try {
            RecognitionEngine.inputDevices()
        } catch (e: Exception) {
            emptyList()
        }
        val usable = devices.count { it.mixerInfo != null }
        return when {
            devices.isEmpty() -> DiagnosticCheck(
                title = s["diag.mic"],
                state = CheckState.ERROR,
                detail = s["diag.micNone"],
                hint = s["diag.micNoneHint"],
            )

            usable == 0 -> DiagnosticCheck(
                title = s["diag.mic"],
                state = CheckState.WARNING,
                detail = s["diag.micDefault"],
                hint = s["diag.micDefaultHint"],
            )

            else -> DiagnosticCheck(
                title = s["diag.mic"],
                state = CheckState.OK,
                detail = s.f("diag.micOk", usable),
            )
        }
    }

    private fun modelCheck(activeModel: String, s: Strings): DiagnosticCheck {
        return if (activeModel.isNotBlank() && ModelStore.installed(activeModel) != null) {
            DiagnosticCheck(
                title = s["diag.model"],
                state = CheckState.OK,
                detail = s.f("diag.modelOk", activeModel),
            )
        } else {
            DiagnosticCheck(
                title = s["diag.model"],
                state = CheckState.WARNING,
                detail = s["diag.modelNone"],
                hint = s["diag.modelHint"],
            )
        }
    }

    /**
     * ydotool is the only method that types normally on GNOME/KDE Wayland, but
     * it only works while its daemon is running.
     */
    private fun ydotoolCheck(typerName: String, s: Strings): DiagnosticCheck {
        val installed = ClipboardTools.inPath("ydotool")
        val running = Typers.ydotoolDaemonRunning()
        return when {
            typerName == "ydotool" && !running -> DiagnosticCheck(
                title = s["diag.ydotool"],
                state = CheckState.ERROR,
                detail = s["diag.ydotoolMissing"],
                hint = s["diag.ydotoolMissingHint"],
            )

            installed && !running && Session.usesPortal -> DiagnosticCheck(
                title = s["diag.ydotool"],
                state = CheckState.WARNING,
                detail = s["diag.ydotoolStopped"],
                hint = s["diag.ydotoolStoppedHint"],
            )

            else -> DiagnosticCheck(
                title = s["diag.ydotool"],
                state = CheckState.OK,
                detail = if (running) s["diag.ydotoolOk"] else s["diag.ydotoolNotNeeded"],
            )
        }
    }

    /**
     * GNOME and KDE only allow synthetic input through a portal, which shows a
     * "Remote Desktop" dialog that is easy to miss and is not remembered.
     */
    private fun sessionCheck(typerName: String, s: Strings): DiagnosticCheck {
        val session = Session.summary
        if (!Session.usesPortal || typerName == "copy") {
            return DiagnosticCheck(
                title = s["diag.session"],
                state = CheckState.OK,
                detail = s.f("diag.sessionOk", session),
            )
        }
        if (typerName == "ydotool") {
            return DiagnosticCheck(
                title = s["diag.session"],
                state = CheckState.OK,
                detail = s.f("diag.sessionYdotool", session),
            )
        }
        return DiagnosticCheck(
            title = s["diag.session"],
            state = CheckState.WARNING,
            detail = s.f("diag.sessionPortal", session),
            hint = s.f("diag.sessionHint", installHint(s)),
        )
    }

    private fun waylandCheck(
        typerName: String,
        hotkeyError: String?,
        s: Strings,
    ): DiagnosticCheck {
        val needsTools = typerName != "ydotool" && typerName != "wtype"
        return if (needsTools || hotkeyError != null) {
            DiagnosticCheck(
                title = s["diag.wayland"],
                state = CheckState.WARNING,
                detail = s["diag.waylandPortal"],
                hint = installHint(s),
            )
        } else {
            DiagnosticCheck(
                title = s["diag.wayland"],
                state = CheckState.OK,
                detail = s["diag.waylandOk"],
            )
        }
    }

    fun isWayland(): Boolean = !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()

    /**
     * Installation commands for the keyboard injection tools, matching the
     * distribution of the running system.
     */
    fun installHint(strings: Strings = Strings.english): String = installHint(strings, distroFamilies())

    /** Also used by the tests with a made up distribution. */
    internal fun installHint(strings: Strings, families: List<String>): String {
        if (AppPaths.isWindows) return strings["diag.windowsNoTools"]
        val packages = "xdotool wtype ydotool"
        val known = families.map { it.lowercase() }
        val commands = when {
            known.any { it in APT } -> "sudo apt install $packages"
            known.any { it in PACMAN } -> "sudo pacman -S $packages"
            known.any { it in DNF } -> "sudo dnf install $packages"
            known.any { it in ZYPPER } -> "sudo zypper install $packages"
            known.any { it in EOPKG } -> "sudo eopkg install $packages"
            known.any { it in NIX } -> "nix-shell -p $packages   # or add them to configuration.nix"
            known.any { it == "alpine" } -> "sudo apk add $packages"
            known.any { it == "void" } -> "sudo xbps-install $packages"
            known.any { it == "gentoo" } -> "sudo emerge x11-misc/xdotool wtype ydotool"
            known.any { it == "clear-linux-os" } -> "sudo swupd bundle-add desktop-autostart"
            else -> strings["diag.installGeneric"]
        }
        return strings.f("diag.installHint", commands)
    }

    private val APT = setOf(
        "debian", "ubuntu", "linuxmint", "pop", "raspbian", "kali", "zorin", "neon",
        "elementary", "mx", "devuan", "parrot",
    )
    private val PACMAN = setOf(
        "arch", "manjaro", "endeavouros", "garuda", "cachyos", "parch", "artix",
        "arcolinux", "blackarch", "archcraft",
    )
    private val DNF = setOf(
        "fedora", "rhel", "centos", "rocky", "almalinux", "nobara", "bazzite", "ultramarine",
    )
    private val ZYPPER = setOf("opensuse", "opensuse-leap", "opensuse-tumbleweed", "sles", "suse")
    private val EOPKG = setOf("solus")
    private val NIX = setOf("nixos", "nix")

    /** `ID` first, then every `ID_LIKE` value, so derivatives are recognised. */
    private fun distroFamilies(): List<String> = distroId().map { it.lowercase() }

    private fun distroId(): List<String> {
        val release = File("/etc/os-release")
        if (!release.isFile) return emptyList()
        val values = try {
            release.readLines()
                .mapNotNull { line ->
                    val separator = line.indexOf('=')
                    if (separator <= 0) null
                    else line.substring(0, separator) to line.substring(separator + 1).trim('"')
                }
                .toMap()
        } catch (e: Exception) {
            return emptyList()
        }
        val candidates = ArrayList<String>()
        values["ID"]?.lowercase()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
        values["ID_LIKE"]?.lowercase()
            ?.split(' ')
            ?.filter { it.isNotBlank() }
            ?.forEach { candidates.add(it) }
        return candidates
    }
}
