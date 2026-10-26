package com.elishaazaria.sayboard.desktop

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser

class HotkeyException(message: String) : Exception(message)

internal data class KeyMapping(val name: String, val windowsVk: Int, val x11Keysym: String)

private val NAMED_KEYS: Map<String, KeyMapping> = buildMap {
    fun put(name: String, vk: Int, keysym: String) {
        put(name, KeyMapping(name, vk, keysym))
    }
    put("space", 0x20, "space")
    put("enter", 0x0D, "Return")
    put("return", 0x0D, "Return")
    put("tab", 0x09, "Tab")
    put("esc", 0x1B, "Escape")
    put("escape", 0x1B, "Escape")
    put("backspace", 0x08, "BackSpace")
    put("delete", 0x2E, "Delete")
    put("insert", 0x2D, "Insert")
    put("home", 0x24, "Home")
    put("end", 0x23, "End")
    put("pageup", 0x21, "Prior")
    put("pagedown", 0x22, "Next")
    put("up", 0x26, "Up")
    put("down", 0x28, "Down")
    put("left", 0x25, "Left")
    put("right", 0x27, "Right")
    put("pause", 0x13, "Pause")
    put("printscreen", 0x2C, "Print")
    put("minus", 0xBD, "minus")
    put("equals", 0xBB, "equal")
    put("comma", 0xBC, "comma")
    put("period", 0xBE, "period")
    put("slash", 0xBF, "slash")
    put("semicolon", 0xBA, "semicolon")
    put("quote", 0xDE, "apostrophe")
    put("bracketleft", 0xDB, "bracketleft")
    put("bracketright", 0xDD, "bracketright")
    put("backslash", 0xDC, "backslash")
    put("grave", 0xC0, "grave")
    for (letter in 'a'..'z') {
        put(letter.toString(), 0x41 + (letter - 'a'), letter.toString())
    }
    for (digit in 0..9) {
        put(digit.toString(), 0x30 + digit, digit.toString())
    }
    for (index in 1..24) {
        put("f$index", 0x6F + index, "F$index")
    }
}

internal data class Hotkey(
    val key: KeyMapping,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false,
) {
    val windowsModifiers: Int
        get() = (if (alt) MOD_ALT else 0) or (if (ctrl) MOD_CONTROL else 0) or
                (if (shift) MOD_SHIFT else 0) or (if (meta) MOD_WIN else 0)

    val x11Modifiers: Int
        get() = (if (shift) X11_SHIFT else 0) or (if (ctrl) X11_CONTROL else 0) or
                (if (alt) X11_MOD1 else 0) or (if (meta) X11_MOD4 else 0)

    companion object {
        private const val MOD_ALT = 0x0001
        private const val MOD_CONTROL = 0x0002
        private const val MOD_SHIFT = 0x0004
        private const val MOD_WIN = 0x0008

        const val X11_SHIFT = 1
        const val X11_CONTROL = 4
        const val X11_MOD1 = 8
        const val X11_MOD4 = 64
    }
}

internal object HotkeyParser {
    fun parse(combination: String): Hotkey {
        val parts = combination.lowercase()
            .split("+")
            .map { it.trim().removePrefix("<").removeSuffix(">") }
            .filter { it.isNotEmpty() }
        if (parts.isEmpty()) throw HotkeyException("The hotkey is empty")
        val keyName = parts.last()
        val key = NAMED_KEYS[keyName] ?: throw HotkeyException("Unknown key: $keyName")
        var hotkey = Hotkey(key)
        for (modifier in parts.dropLast(1)) {
            hotkey = when (modifier) {
                "ctrl", "control" -> hotkey.copy(ctrl = true)
                "alt", "option" -> hotkey.copy(alt = true)
                "shift" -> hotkey.copy(shift = true)
                "cmd", "win", "super", "meta" -> hotkey.copy(meta = true)
                else -> throw HotkeyException("Unknown modifier: $modifier")
            }
        }
        return hotkey
    }
}

/**
 * A global hotkey that is registered with the operating system itself:
 * `RegisterHotKey` on Windows and `XGrabKey` on X11. Nothing has to be extracted
 * at run time, which keeps packaged (read-only) applications such as the
 * AppImage working.
 */
class GlobalHotkeys {
    @Volatile
    var onToggle: (() -> Unit)? = null

    @Volatile
    var onCancel: (() -> Unit)? = null

    @Volatile
    var isListening: () -> Boolean = { false }

    @Volatile
    var error: String? = null

    private var backend: HotkeyBackend? = null
    private var hotkey: Hotkey? = null
    private var thread: Thread? = null

    @Volatile
    private var running = false

    fun start(combination: String) {
        val parsed = HotkeyParser.parse(combination)
        val newBackend = backend ?: createBackend()
        newBackend.release()
        newBackend.grab(parsed)
        hotkey = parsed
        backend = newBackend
        error = null
        if (thread?.isAlive != true) {
            running = true
            thread = Thread({ loop() }, "saytypist-hotkey").apply {
                isDaemon = true
                start()
            }
        }
    }

    private fun createBackend(): HotkeyBackend {
        return try {
            if (AppPaths.isWindows) WindowsHotkeyBackend() else X11HotkeyBackend()
        } catch (e: HotkeyException) {
            throw e
        } catch (e: Throwable) {
            throw HotkeyException("Global hotkey is not available: ${e.message ?: e}")
        }
    }

    private fun loop() {
        var escapeWasDown = false
        while (running) {
            try {
                val current = backend
                if (current != null) {
                    if (current.poll()) onToggle?.invoke()
                    if (isListening()) {
                        val down = current.escapeDown()
                        if (down && !escapeWasDown) onCancel?.invoke()
                        escapeWasDown = down
                    } else {
                        escapeWasDown = false
                    }
                }
            } catch (e: Throwable) {
                error = e.message
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (interrupted: InterruptedException) {
                return
            }
        }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
        backend?.release()
        backend = null
    }

    fun shutdown() {
        stop()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30L
    }
}

private interface HotkeyBackend {
    fun grab(hotkey: Hotkey)

    /** True when the hotkey was pressed since the last call. */
    fun poll(): Boolean

    fun escapeDown(): Boolean = false

    /** Frees the grab; used before registering another combination. */
    fun release()
}

// --------------------------------------------------------------------- Windows

private class WindowsHotkeyBackend : HotkeyBackend {
    private var registered = false
    private val message = WinUser.MSG()

    override fun grab(hotkey: Hotkey) {
        release()
        if (!User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID, hotkey.windowsModifiers, hotkey.key.windowsVk)) {
            throw HotkeyException("the system refused the hotkey (it may already be taken)")
        }
        registered = true
    }

    override fun poll(): Boolean {
        var triggered = false
        while (User32.INSTANCE.PeekMessage(message, null, 0, 0, PM_REMOVE)) {
            if (message.message == WM_HOTKEY && message.wParam.toInt() == HOTKEY_ID) {
                triggered = true
            }
        }
        return triggered
    }

    override fun escapeDown(): Boolean {
        return (User32.INSTANCE.GetAsyncKeyState(VK_ESCAPE).toInt() and 0x8000) != 0
    }

    override fun release() {
        if (registered) {
            User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID)
            registered = false
        }
    }

    companion object {
        private const val HOTKEY_ID = 1
        private const val WM_HOTKEY = 0x0312
        private const val PM_REMOVE = 0x0001
        private const val VK_ESCAPE = 0x1B
    }
}

// ----------------------------------------------------------------------- Linux

private interface X11Library : Library {
    fun XOpenDisplay(displayName: String?): Pointer?
    fun XCloseDisplay(display: Pointer?): Int
    fun XDefaultScreen(display: Pointer?): Int
    fun XRootWindow(display: Pointer?, screenNumber: Int): Long
    fun XStringToKeysym(string: String?): Long
    fun XKeysymToKeycode(display: Pointer?, keysym: Long): Byte
    fun XGrabKey(
        display: Pointer?,
        keycode: Int,
        modifiers: Int,
        grabWindow: Long,
        ownerEvents: Int,
        pointerMode: Int,
        keyboardMode: Int,
    ): Int

    fun XUngrabKey(display: Pointer?, keycode: Int, modifiers: Int, grabWindow: Long): Int
    fun XNextEvent(display: Pointer?, event: Pointer?): Int
    fun XPending(display: Pointer?): Int
    fun XSync(display: Pointer?, discard: Int): Int
    fun XQueryKeymap(display: Pointer?, keysReturn: ByteArray): Int
}

private class X11HotkeyBackend : HotkeyBackend {
    private val x11: X11Library
    private val display: Pointer
    private val rootWindow: Long
    private val eventMemory = Memory(EVENT_SIZE)
    private var grabbedKeyCode = -1
    private var grabbedModifiers = 0

    init {
        if (System.getenv("DISPLAY").isNullOrBlank()) {
            throw HotkeyException("no X11 display (Wayland without XWayland?)")
        }
        x11 = Native.load("X11", X11Library::class.java)
        display = x11.XOpenDisplay(null)
            ?: throw HotkeyException("could not open the X11 display")
        rootWindow = x11.XRootWindow(display, x11.XDefaultScreen(display))
    }

    override fun grab(hotkey: Hotkey) {
        release()
        val keysym = x11.XStringToKeysym(hotkey.key.x11Keysym)
        if (keysym == 0L) throw HotkeyException("unknown keysym: ${hotkey.key.x11Keysym}")
        val keyCode = x11.XKeysymToKeycode(display, keysym).toInt() and 0xFF
        if (keyCode == 0) throw HotkeyException("cannot map the key ${hotkey.key.name}")
        val modifiers = hotkey.x11Modifiers
        // NumLock and CapsLock change the reported modifier state, so grab every
        // combination that can physically produce the shortcut.
        val lockVariants = intArrayOf(0, X11_LOCK, X11_MOD2, X11_LOCK or X11_MOD2)
        var grabbed = false
        for (mask in lockVariants) {
            val result = x11.XGrabKey(
                display, keyCode, modifiers or mask, rootWindow,
                OWNER_EVENTS, GRAB_MODE_ASYNC, GRAB_MODE_ASYNC
            )
            if (result != 0) grabbed = true
        }
        if (!grabbed) throw HotkeyException("the system refused the hotkey (it may already be taken)")
        x11.XSync(display, 0)
        grabbedKeyCode = keyCode
        grabbedModifiers = modifiers
    }

    override fun poll(): Boolean {
        var triggered = false
        while (x11.XPending(display) > 0) {
            x11.XNextEvent(display, eventMemory)
            val type = eventMemory.getInt(0)
            if (type != KEY_PRESS) continue
            val state = eventMemory.getInt(KEY_STATE_OFFSET) and RELEVANT_MASK
            val keyCode = eventMemory.getInt(KEY_CODE_OFFSET) and 0xFF
            if (keyCode == grabbedKeyCode && state == (grabbedModifiers and RELEVANT_MASK)) {
                triggered = true
            }
        }
        return triggered
    }

    override fun escapeDown(): Boolean {
        val keys = ByteArray(32)
        if (x11.XQueryKeymap(display, keys) == 0) return false
        val escapeCode = x11.XKeysymToKeycode(display, x11.XStringToKeysym("Escape")).toInt() and 0xFF
        if (escapeCode <= 0 || escapeCode >= keys.size * 8) return false
        return (keys[escapeCode / 8].toInt() and (1 shl (escapeCode % 8))) != 0
    }

    override fun release() {
        if (grabbedKeyCode >= 0 && display != null) {
            val lockVariants = intArrayOf(0, X11_LOCK, X11_MOD2, X11_LOCK or X11_MOD2)
            for (mask in lockVariants) {
                x11.XUngrabKey(display, grabbedKeyCode, grabbedModifiers or mask, rootWindow)
            }
            x11.XSync(display, 0)
        }
        grabbedKeyCode = -1
    }

    companion object {
        private const val EVENT_SIZE = 256L
        private const val KEY_PRESS = 2
        private const val KEY_CODE_OFFSET = 84L
        private const val KEY_STATE_OFFSET = 80L
        private const val X11_LOCK = 2
        private const val X11_MOD2 = 16
        private const val RELEVANT_MASK = 0xFF
        private const val OWNER_EVENTS = 1
        private const val GRAB_MODE_ASYNC = 1
    }
}
