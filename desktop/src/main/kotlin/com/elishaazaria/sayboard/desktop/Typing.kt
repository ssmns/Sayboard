package com.elishaazaria.sayboard.desktop

import java.awt.Toolkit
import java.awt.Robot
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Types text into whatever window has the focus.
 */
interface Typer {
    val name: String

    /** false when the method can only paste a finished phrase. */
    val supportsLive: Boolean
        get() = true

    fun typeText(text: String)

    fun backspace(count: Int)
}

class TypingException(message: String) : Exception(message)

// --------------------------------------------------------------------- Windows

/**
 * Win32 SendInput with unicode key events: works with every keyboard layout and
 * every script (Persian, Arabic, CJK, ...).
 */
private class WindowsTyper : Typer {
    override val name: String = "windows"

    override fun typeText(text: String) {
        if (text.isEmpty()) return
        val inputs = mutableListOf<com.sun.jna.platform.win32.WinUser.INPUT>()
        inputs.addAll(modifierReleases())
        for (char in text) {
            for (unit in utf16Units(char)) {
                inputs.add(unicodeEvent(unit, false))
                inputs.add(unicodeEvent(unit, true))
            }
        }
        send(inputs)
    }

    override fun backspace(count: Int) {
        if (count <= 0) return
        val inputs = mutableListOf<com.sun.jna.platform.win32.WinUser.INPUT>()
        inputs.addAll(modifierReleases())
        repeat(count) {
            inputs.add(virtualEvent(VK_BACK, false))
            inputs.add(virtualEvent(VK_BACK, true))
        }
        send(inputs)
    }

    private fun utf16Units(char: Char): List<Int> {
        val encoded = char.toString().toByteArray(Charsets.UTF_16LE)
        return encoded.toList().chunked(2).map { bytes ->
            (bytes[0].toInt() and 0xFF) or ((bytes.getOrElse(1) { 0 }.toInt() and 0xFF) shl 8)
        }
    }

    private fun send(inputs: List<com.sun.jna.platform.win32.WinUser.INPUT>) {
        if (inputs.isEmpty()) return
        val array = inputs.toTypedArray()
        val sent = com.sun.jna.platform.win32.User32.INSTANCE.SendInput(
            com.sun.jna.platform.win32.WinDef.DWORD(array.size.toLong()),
            array,
            com.sun.jna.platform.win32.WinUser.INPUT().size(),
        )
        if (sent == null || sent.toLong() != array.size.toLong()) {
            throw TypingException("SendInput failed")
        }
    }

    /**
     * The dictation hotkey is usually still held down when the first words
     * arrive; drop the modifiers so the text is not turned into shortcuts.
     */
    private fun modifierReleases(): List<com.sun.jna.platform.win32.WinUser.INPUT> {
        return listOf(VK_CONTROL, VK_MENU, VK_SHIFT, VK_LWIN, VK_RWIN).flatMap { key ->
            listOf(virtualEvent(key, true))
        }
    }

    private fun unicodeEvent(unit: Int, keyUp: Boolean): com.sun.jna.platform.win32.WinUser.INPUT {
        val input = com.sun.jna.platform.win32.WinUser.INPUT()
        input.type = com.sun.jna.platform.win32.WinDef.DWORD(
            com.sun.jna.platform.win32.WinUser.INPUT.INPUT_KEYBOARD.toLong()
        )
        input.input.setType(com.sun.jna.platform.win32.WinUser.KEYBDINPUT::class.java)
        input.input.ki.wVk = com.sun.jna.platform.win32.WinDef.WORD(0)
        input.input.ki.wScan = com.sun.jna.platform.win32.WinDef.WORD(unit.toLong())
        var flags = com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_UNICODE
        if (keyUp) flags = flags or com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_KEYUP
        input.input.ki.dwFlags = com.sun.jna.platform.win32.WinDef.DWORD(flags.toLong())
        input.input.ki.time = com.sun.jna.platform.win32.WinDef.DWORD(0)
        input.input.ki.dwExtraInfo = com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0)
        return input
    }

    private fun virtualEvent(key: Int, keyUp: Boolean): com.sun.jna.platform.win32.WinUser.INPUT {
        val input = com.sun.jna.platform.win32.WinUser.INPUT()
        input.type = com.sun.jna.platform.win32.WinDef.DWORD(
            com.sun.jna.platform.win32.WinUser.INPUT.INPUT_KEYBOARD.toLong()
        )
        input.input.setType(com.sun.jna.platform.win32.WinUser.KEYBDINPUT::class.java)
        input.input.ki.wVk = com.sun.jna.platform.win32.WinDef.WORD(key.toLong())
        input.input.ki.wScan = com.sun.jna.platform.win32.WinDef.WORD(0)
        var flags = 0
        if (keyUp) flags = flags or com.sun.jna.platform.win32.WinUser.KEYBDINPUT.KEYEVENTF_KEYUP
        input.input.ki.dwFlags = com.sun.jna.platform.win32.WinDef.DWORD(flags.toLong())
        input.input.ki.time = com.sun.jna.platform.win32.WinDef.DWORD(0)
        input.input.ki.dwExtraInfo = com.sun.jna.platform.win32.BaseTSD.ULONG_PTR(0)
        return input
    }

    companion object {
        private const val VK_BACK = 0x08
        private const val VK_SHIFT = 0x10
        private const val VK_CONTROL = 0x11
        private const val VK_MENU = 0x12
        private const val VK_LWIN = 0x5B
        private const val VK_RWIN = 0x5C
    }
}

// ----------------------------------------------------------------------- Linux

private fun runCommand(command: List<String>, input: ByteArray? = null) {
    val process = ProcessBuilder(command).redirectErrorStream(true).start()
    if (input != null) {
        process.outputStream.use { it.write(input) }
    } else {
        process.outputStream.close()
    }
    val output = process.inputStream.bufferedReader().readText()
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        throw TypingException("${command.first()} timed out")
    }
    if (process.exitValue() != 0) {
        throw TypingException("${command.first()} failed: ${output.trim()}")
    }
}

private class XdotoolTyper : Typer {
    override val name: String = "xdotool"

    override fun typeText(text: String) {
        if (text.isEmpty()) return
        runCommand(listOf("xdotool", "type", "--clearmodifiers", "--delay", "0", "--", text))
    }

    override fun backspace(count: Int) {
        if (count <= 0) return
        runCommand(
            listOf(
                "xdotool", "key", "--clearmodifiers", "--repeat", count.toString(),
                "--delay", "0", "BackSpace"
            )
        )
    }
}

private class WtypeTyper : Typer {
    override val name: String = "wtype"

    override fun typeText(text: String) {
        if (text.isEmpty()) return
        runCommand(listOf("wtype", "-"), text.toByteArray())
    }

    override fun backspace(count: Int) {
        if (count <= 0) return
        val command = mutableListOf("wtype")
        repeat(count) {
            command.add("-k")
            command.add("BackSpace")
        }
        runCommand(command)
    }
}

private class YdotoolTyper : Typer {
    override val name: String = "ydotool"

    override fun typeText(text: String) {
        if (text.isEmpty()) return
        runCommand(listOf("ydotool", "type", "--delay", "0", "--", text))
    }

    override fun backspace(count: Int) {
        if (count <= 0) return
        runCommand(
            listOf("ydotool", "key", "--key-delay", "0", "--repeat", count.toString(), "Backspace")
        )
    }
}

/** Last resort: put the text in the clipboard and press Ctrl+V. */
/**
 * Last resort on Linux: put the whole phrase in the clipboard and press Ctrl+V.
 *
 * The whole phrase (including the trailing space) is always pasted in one go:
 * a second paste for the space used to overwrite the clipboard before the target
 * application had read the text, so only the space ended up in the text field.
 */
class ClipboardTyper : Typer {
    override val name: String = "clipboard"
    override val supportsLive: Boolean = false

    override fun typeText(text: String) {
        if (text.isEmpty()) return
        if (!ClipboardTools.set(text)) {
            throw TypingException("could not write to the clipboard")
        }
        val check = ClipboardTools.read()
        if (check != null && check != text) {
            throw TypingException("the clipboard kept the old content")
        }
        Thread.sleep(PASTE_SETTLE_MS)
        if (!ClipboardTools.sendPaste()) {
            // Fall back to AWT Robot when no helper tool is installed.
            pressCtrlV()
        }
        // The text stays in the clipboard on purpose: if the application did
        // not accept the paste, the user can still press Ctrl+V by hand.
        Thread.sleep(PASTE_CONSUME_MS)
    }

    override fun backspace(count: Int) {
        if (count <= 0) return
        if (ClipboardTools.sendBackspace(count)) return
        val robot = Robot()
        repeat(count) {
            robot.keyPress(KeyEvent.VK_BACK_SPACE)
            robot.keyRelease(KeyEvent.VK_BACK_SPACE)
        }
    }

    private fun pressCtrlV() {
        val robot = Robot()
        try {
            robot.keyPress(KeyEvent.VK_CONTROL)
            robot.keyPress(KeyEvent.VK_V)
            robot.keyRelease(KeyEvent.VK_V)
        } finally {
            robot.keyRelease(KeyEvent.VK_CONTROL)
        }
    }

    companion object {
        private const val PASTE_SETTLE_MS = 150L
        private const val PASTE_CONSUME_MS = 400L
    }
}

/**
 * Safest mode for Wayland desktops that ask for a "Remote Desktop" permission
 * before any keystroke: the phrase is only put in the clipboard and the user
 * pastes it with Ctrl+V.
 */
class CopyOnlyTyper : Typer {
    override val name: String = "copy"
    override val supportsLive: Boolean = false

    override fun typeText(text: String) {
        if (text.isEmpty()) return
        if (!ClipboardTools.set(text)) {
            throw TypingException("could not write to the clipboard")
        }
    }

    override fun backspace(count: Int) = Unit
}

class NullTyper : Typer {
    override val name: String = "none"
    override val supportsLive: Boolean = false
    override fun typeText(text: String) = Unit
    override fun backspace(count: Int) = Unit
}

// -------------------------------------------------------------------- detection

object Typers {
    private fun inPath(tool: String): Boolean = ClipboardTools.inPath(tool)

    private fun isWindows(): Boolean = AppPaths.isWindows

    fun available(): List<String> {
        val names = mutableListOf("auto")
        if (isWindows()) {
            names.add("windows")
            names.add("clipboard")
            names.add("copy")
            return names
        }
        if (inPath("xdotool") && !System.getenv("DISPLAY").isNullOrBlank()) names.add("xdotool")
        if (inPath("wtype") && !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()) names.add("wtype")
        if (inPath("ydotool")) names.add("ydotool")
        names.add("clipboard")
        names.add("copy")
        return names
    }

    fun create(choice: String): Typer {
        if (isWindows()) {
            return when (choice) {
                "clipboard" -> ClipboardTyper()
                "copy" -> CopyOnlyTyper()
                else -> WindowsTyper()
            }
        }
        return when (choice) {
            "xdotool" -> if (inPath("xdotool")) XdotoolTyper() else NullTyper()
            "wtype" -> if (inPath("wtype")) WtypeTyper() else NullTyper()
            "ydotool" -> if (inPath("ydotool")) YdotoolTyper() else NullTyper()
            "clipboard" -> ClipboardTyper()
            "copy" -> CopyOnlyTyper()
            "windows" -> NullTyper()
            else -> auto()
        }
    }

    private fun auto(): Typer {
        val hasDisplay = !System.getenv("DISPLAY").isNullOrBlank()
        val wayland = !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()
        if (!wayland && hasDisplay && inPath("xdotool")) return XdotoolTyper()
        // ydotool (uinput) never needs a permission dialog, so it is preferred
        // on GNOME/KDE where synthetic keystrokes go through a portal.
        if (inPath("ydotool") && ydotoolDaemonRunning()) return YdotoolTyper()
        if (wayland && !Session.usesPortal && inPath("wtype")) return WtypeTyper()
        // A portal based Wayland desktop either shows a "Remote Desktop"
        // permission dialog for keystrokes or ignores them completely. Paste
        // through the clipboard instead (no dialog), so the phrase still lands
        // in any X11 application and always stays in the clipboard.
        if (wayland && Session.usesPortal) return ClipboardTyper()
        if (hasDisplay) return ClipboardTyper()
        if (wayland) return CopyOnlyTyper()
        return NullTyper()
    }

    /** ydotool talks to the 'ydotoold' daemon through a unix socket. */
    fun ydotoolDaemonRunning(): Boolean {
        val custom = System.getenv("YDOTOOL_SOCKET")
        val candidates = buildList {
            if (!custom.isNullOrBlank()) add(custom)
            add("/tmp/.ydotool_socket")
            System.getenv("XDG_RUNTIME_DIR")?.takeIf { it.isNotBlank() }?.let {
                add("$it/.ydotool_socket")
            }
        }
        return candidates.any { java.io.File(it).exists() }
    }
}

/**
 * Information about the desktop session, used to pick methods that do not ask
 * the user for permissions over and over again.
 */
object Session {
    val isWayland: Boolean
        get() = !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()

    val hasX11: Boolean
        get() = !System.getenv("DISPLAY").isNullOrBlank()

    private val desktop: String
        get() = listOf("XDG_CURRENT_DESKTOP", "XDG_SESSION_DESKTOP", "DESKTOP_SESSION")
            .mapNotNull { System.getenv(it)?.lowercase() }
            .joinToString(":")

    /**
     * GNOME, KDE and friends route synthetic input through xdg-desktop-portal,
     * which shows a "Remote Desktop" confirmation dialog.
     */
    val usesPortal: Boolean
        get() = listOf(
            "gnome", "ubuntu", "kde", "plasma", "cosmic", "unity", "budgie", "deepin",
            "pantheon", "elementary", "pop",
        ).any { desktop.contains(it) }

    val summary: String
        get() = buildString {
            append(if (isWayland) "Wayland" else if (hasX11) "X11" else "no display")
            if (desktop.isNotBlank()) append(" · ").append(desktop)
            if (usesPortal) append(" (portal based input)")
        }
}

// --------------------------------------------------------------------- renderer

/**
 * Turns recognition results into keystrokes. In live mode the partial results
 * are typed while speaking and corrected with backspaces.
 */
class TextRenderer(
    @Volatile var typer: Typer,
    live: Boolean = false,
    @Volatile var trailingSpace: Boolean = true,
    @Volatile var capitalize: Boolean = true,
    private val onTyped: (String) -> Unit = {},
    private val onError: (String) -> Unit = {},
) {
    private val lock = Any()
    private var typed = ""

    @Volatile
    var live: Boolean = live && typer.supportsLive

    fun partial(text: String) {
        if (!live) return
        synchronized(lock) { replace(format(text)) }
    }

    fun final(text: String) {
        val formatted = format(text)
        if (formatted.isEmpty()) return
        var delivered: Boolean
        synchronized(lock) {
            delivered = if (!typer.supportsLive) {
                // Paste the phrase and the trailing space in one go: a second
                // paste would overwrite the clipboard before the target
                // application has read the text.
                val insertion = if (trailingSpace) "$formatted " else formatted
                try {
                    typer.typeText(insertion)
                    true
                } catch (e: Exception) {
                    fail(formatted, e)
                    false
                }
            } else {
                val typedOk = replace(formatted, force = true)
                typed = ""
                if (typedOk && trailingSpace) {
                    try {
                        type(" ")
                        true
                    } catch (e: Exception) {
                        fail(formatted, e)
                        false
                    }
                } else {
                    typedOk
                }
            }
        }
        if (delivered) onTyped(formatted)
    }

    fun cancel() {
        synchronized(lock) { replace("") }
    }

    fun reset() {
        synchronized(lock) { typed = "" }
    }

    private fun format(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        if (!capitalize) return trimmed
        return trimmed[0].uppercaseChar() + trimmed.substring(1)
    }

    private fun replace(text: String, force: Boolean = false): Boolean {
        if (!force && text == typed) return true
        if (!typer.supportsLive && !force) return false
        val common = commonPrefix(typed, text)
        val remove = typed.length - common
        var ok = true
        try {
            if (remove > 0) typer.backspace(remove)
            type(text.substring(common))
        } catch (e: Exception) {
            ok = false
            if (force) {
                fail(text, e)
            } else {
                onError("Could not type the text (${typer.name}): ${e.message}")
            }
        }
        typed = text
        return ok
    }

    private fun type(text: String) {
        if (text.isNotEmpty()) typer.typeText(text)
    }

    private fun fail(phrase: String, error: Exception) {
        onError("Could not type the text (${typer.name}): ${error.message}")
        // never lose the phrase: keep it in the clipboard
        if (ClipboardTools.set(phrase)) {
            onError("The text was copied to the clipboard instead - press Ctrl+V.")
        }
    }

    companion object {
        fun commonPrefix(left: String, right: String): Int {
            val limit = minOf(left.length, right.length)
            var index = 0
            while (index < limit && left[index] == right[index]) index++
            return index
        }
    }
}
