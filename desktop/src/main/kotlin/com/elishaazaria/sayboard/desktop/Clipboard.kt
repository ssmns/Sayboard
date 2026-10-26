package com.elishaazaria.sayboard.desktop

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Clipboard access that also works on Linux sessions where the AWT clipboard is
 * not visible to other applications (Wayland) by falling back to the usual
 * command line helpers (wl-copy, xclip, xsel).
 */
object ClipboardTools {

    fun read(): String? {
        val fromAwt = try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val contents = clipboard.getContents(null)
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                contents.getTransferData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        if (!fromAwt.isNullOrEmpty()) return fromAwt
        return readWithTool()
    }

    fun set(text: String): Boolean {
        if (setWithTool(text)) return true
        return try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            true
        } catch (e: Exception) {
            System.err.println("Could not write to the clipboard: ${e.message}")
            false
        }
    }

    /**
     * On Wayland the AWT clipboard lives inside XWayland, which some native
     * applications cannot see, so the Wayland helper is preferred there.
     */
    private fun setWithTool(text: String): Boolean {
        val wayland = !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()
        val hasX11 = !System.getenv("DISPLAY").isNullOrBlank()
        // only stop when a helper actually succeeded, otherwise fall back to AWT
        if (wayland && inPath("wl-copy") && runTool(listOf("wl-copy"), text.toByteArray())) {
            return true
        }
        if (hasX11) {
            if (inPath("xclip") &&
                runTool(listOf("xclip", "-selection", "clipboard"), text.toByteArray())
            ) {
                return true
            }
            if (inPath("xsel") &&
                runTool(listOf("xsel", "--clipboard", "--input"), text.toByteArray())
            ) {
                return true
            }
        }
        if (!wayland && inPath("wl-copy") && runTool(listOf("wl-copy"), text.toByteArray())) {
            return true
        }
        return false
    }

    private fun readWithTool(): String? {
        val wayland = !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()
        val candidates = buildList {
            if (wayland && inPath("wl-paste")) add(listOf("wl-paste", "--no-newline"))
            if (!System.getenv("DISPLAY").isNullOrBlank()) {
                if (inPath("xclip")) add(listOf("xclip", "-selection", "clipboard", "-o"))
                if (inPath("xsel")) add(listOf("xsel", "--clipboard", "--output"))
            }
        }
        for (command in candidates) {
            val output = runToolForOutput(command) ?: continue
            if (output.isNotEmpty()) return output
        }
        return null
    }

    /** Sends Ctrl+V with the tool that matches the session. */
    fun sendPaste(): Boolean {
        // wtype creates a virtual keyboard, which GNOME/KDE only allow after a
        // "Remote Desktop" permission dialog - avoid it on those desktops.
        if (Session.isWayland && !Session.usesPortal && inPath("wtype")) {
            return runTool(listOf("wtype", "-M", "ctrl", "-k", "v", "-m", "ctrl"), null)
        }
        if (!System.getenv("DISPLAY").isNullOrBlank() && inPath("xdotool")) {
            return runTool(listOf("xdotool", "key", "--clearmodifiers", "ctrl+v"), null)
        }
        if (inPath("ydotool")) {
            // 29 = LEFTCTRL, 47 = V
            return runTool(
                listOf("ydotool", "key", "29:1", "47:1", "47:0", "29:0"), null
            )
        }
        return false
    }

    fun sendBackspace(count: Int): Boolean {
        if (count <= 0) return false
        if (Session.isWayland && !Session.usesPortal && inPath("wtype")) {
            val command = mutableListOf("wtype")
            repeat(count) {
                command.add("-k")
                command.add("BackSpace")
            }
            return runTool(command, null)
        }
        if (!System.getenv("DISPLAY").isNullOrBlank() && inPath("xdotool")) {
            return runTool(
                listOf(
                    "xdotool", "key", "--clearmodifiers", "--repeat", count.toString(),
                    "--delay", "0", "BackSpace"
                ),
                null
            )
        }
        return false
    }

    private fun runTool(command: List<String>, input: ByteArray?): Boolean {
        return try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            if (input != null) {
                process.outputStream.use { it.write(input) }
            } else {
                process.outputStream.close()
            }
            process.inputStream.readBytes()
            process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun runToolForOutput(command: List<String>): String? {
        return try {
            val process = ProcessBuilder(command).redirectErrorStream(false).start()
            process.outputStream.close()
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) output else null
        } catch (e: Exception) {
            null
        }
    }

    fun inPath(tool: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        return path.split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .any { File(it, tool).let { file -> file.isFile && file.canExecute() } }
    }
}
