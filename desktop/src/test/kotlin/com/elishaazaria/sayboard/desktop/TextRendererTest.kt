package com.elishaazaria.sayboard.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeTyper : Typer {
    override val name = "fake"
    val ops = mutableListOf<String>()

    override fun typeText(text: String) {
        if (text.isNotEmpty()) ops.add("type:$text")
    }

    override fun backspace(count: Int) {
        if (count > 0) ops.add("bs:$count")
    }
}

/** Behaves like the clipboard backend: only whole phrases, one paste each. */
private class PasteTyper : Typer {
    override val name = "paste"
    override val supportsLive = false
    val ops = mutableListOf<String>()

    override fun typeText(text: String) {
        if (text.isNotEmpty()) ops.add("paste:$text")
    }

    override fun backspace(count: Int) {
        if (count > 0) ops.add("bs:$count")
    }
}

class TextRendererTest {

    @Test
    fun `live typing corrects the partial result with backspaces`() {
        val typer = FakeTyper()
        val renderer = TextRenderer(typer, live = true, trailingSpace = true, capitalize = false)
        renderer.partial("hel")
        renderer.partial("hello")
        renderer.partial("hello wor")
        renderer.final("hello world")

        assertEquals(
            listOf("type:hel", "type:lo", "type: wor", "type:ld", "type: "),
            typer.ops,
        )
    }

    @Test
    fun `without live mode only finished phrases are typed`() {
        val typer = FakeTyper()
        val renderer = TextRenderer(typer, live = false, trailingSpace = false, capitalize = false)
        renderer.partial("ignored")
        renderer.final("hello")

        assertEquals(listOf("type:hello"), typer.ops)
    }

    @Test
    fun `the first letter is capitalized`() {
        val typer = FakeTyper()
        val renderer = TextRenderer(typer, live = false, trailingSpace = true, capitalize = true)
        renderer.final("hello there")

        assertEquals(listOf("type:Hello there", "type: "), typer.ops)
    }

    @Test
    fun `a paste based typer inserts the phrase and the space in one go`() {
        val typer = PasteTyper()
        val renderer = TextRenderer(typer, live = true, trailingSpace = true, capitalize = true)
        renderer.partial("hello")
        renderer.final("hello there")

        // One single paste: a second one (for the space) would overwrite the
        // clipboard before the target application has read the text.
        assertEquals(listOf("paste:Hello there "), typer.ops)
    }

    @Test
    fun `a failing typer keeps the phrase in the clipboard`() {
        val previous = ClipboardTools.read()
        val messages = mutableListOf<String>()
        val typer = object : Typer {
            override val name = "broken"
            override fun typeText(text: String) = throw TypingException("no display")
            override fun backspace(count: Int) = Unit
        }
        val renderer = TextRenderer(typer, live = false, onError = { messages.add(it) })
        renderer.final("سلام دنیا")

        assertTrue(messages.any { it.contains("Could not type") }, messages.toString())
        if (!java.awt.GraphicsEnvironment.isHeadless() && previous != null) {
            assertEquals("سلام دنیا", ClipboardTools.read())
        }
        previous?.let { ClipboardTools.set(it) }
    }

    @Test
    fun `cancelling a phrase removes what was typed`() {
        val typer = FakeTyper()
        val renderer = TextRenderer(typer, live = true, trailingSpace = true, capitalize = false)
        renderer.partial("hell")
        renderer.cancel()

        assertEquals(listOf("type:hell", "bs:4"), typer.ops)
    }

    @Test
    fun `persian text is not broken`() {
        val typer = FakeTyper()
        val renderer = TextRenderer(typer, live = true, trailingSpace = false, capitalize = true)
        renderer.partial("سلام دنیا")
        renderer.final("سلام دنیا")

        assertEquals(listOf("type:سلام دنیا"), typer.ops)
    }

    @Test
    fun `common prefix is computed`() {
        assertEquals(2, TextRenderer.commonPrefix("abc", "abd"))
        assertEquals(0, TextRenderer.commonPrefix("", "x"))
    }
}

class ModelCatalogTest {

    @Test
    fun `the catalog is loaded and has heavy models`() {
        val models = ModelCatalog.models
        assertTrue(models.size > 50, "expected the full Vosk catalog, got ${models.size}")

        val persian = models.filter { it.localeTag == "fa" }
        assertTrue(persian.isNotEmpty(), "no Persian models")
        assertTrue(persian.any { it.isHeavy }, "no heavy Persian model")

        val heavy = persian.first { it.isHeavy }
        assertTrue(heavy.url.startsWith("https://alphacephei.com/vosk/models/"))
        assertTrue(heavy.tier.ramMb >= 1500)
    }

    @Test
    fun `every model has a positive size and a language`() {
        for (model in ModelCatalog.models) {
            assertTrue(model.sizeMb > 0, model.id)
            assertTrue(model.lang.isNotBlank(), model.id)
        }
    }
}

class HotkeyTest {

    @Test
    fun `parses a combination`() {
        val hotkey = HotkeyParser.parse("ctrl+alt+space")
        assertTrue(hotkey.ctrl)
        assertTrue(hotkey.alt)
        assertTrue(!hotkey.shift)
        assertEquals(0x20, hotkey.key.windowsVk)
        assertEquals("space", hotkey.key.x11Keysym)
    }

    @Test
    fun `parses function keys and angle bracket notation`() {
        assertEquals(0x78, HotkeyParser.parse("f9").key.windowsVk)
        assertEquals(0x78, HotkeyParser.parse("<F9>").key.windowsVk)
        assertTrue(HotkeyParser.parse("ctrl+shift+d").meta.not())
    }

    @Test
    fun `rejects unknown keys`() {
        assertFailsWith<HotkeyException> { HotkeyParser.parse("ctrl+nope") }
        assertFailsWith<HotkeyException> { HotkeyParser.parse("hyper+space") }
    }
}

class DiagnosticsTest {

    @Test
    fun `checks cover the important requirements`() {
        val checks = Diagnostics.run(AppConfig(model = ""), hotkeyError = null, typerName = "clipboard")
        val titles = checks.map { it.title }
        assertTrue(titles.contains("Typing text into other applications"), titles.toString())
        assertTrue(titles.contains("Global hotkey"), titles.toString())
        assertTrue(titles.contains("Microphone"), titles.toString())
        assertTrue(titles.contains("Speech model"), titles.toString())

        val typing = checks.first { it.title.startsWith("Typing") }
        assertEquals(CheckState.WARNING, typing.state)
        assertTrue(checks.any { it.title == "Desktop session" }, titles.toString())
        val model = checks.first { it.title == "Speech model" }
        assertEquals(CheckState.WARNING, model.state)
    }

    @Test
    fun `missing typing tools are an error with an install hint`() {
        val check = Diagnostics.run(AppConfig(), hotkeyError = null, typerName = "none")
            .first { it.title.startsWith("Typing") }
        assertEquals(CheckState.ERROR, check.state)
        assertTrue(check.hint.orEmpty().contains("xdotool"), check.hint ?: "no hint")
    }

    @Test
    fun `copy mode is a clean state and mentions the clipboard`() {
        val check = Diagnostics.run(AppConfig(), hotkeyError = null, typerName = "copy")
            .first { it.title.startsWith("Typing") }
        assertEquals(CheckState.OK, check.state)
        assertTrue(check.detail.contains("Ctrl+V"), check.detail)
    }

    @Test
    fun `a failing hotkey is reported but not fatal`() {
        val check = Diagnostics.run(AppConfig(), hotkeyError = "already taken", typerName = "xdotool")
            .first { it.title == "Global hotkey" }
        assertEquals(CheckState.WARNING, check.state)
        assertTrue(check.detail.contains("already taken"))
    }
}

class ClipboardTest {

    @Test
    fun `the clipboard keeps the phrase`() {
        // only runs when explicitly asked for (it touches the real clipboard):
        //   SAYBOARD_CLIPBOARD_TESTS=1 xvfb-run -a ./gradlew :desktop:test --tests '*ClipboardTest*'
        org.junit.jupiter.api.Assumptions.assumeTrue(
            System.getenv("SAYBOARD_CLIPBOARD_TESTS") == "1",
            "set SAYBOARD_CLIPBOARD_TESTS=1 to test the clipboard",
        )
        val previous = ClipboardTools.read()
        try {
            assertTrue(ClipboardTools.set("سلام دنیا hello 123"), "set() failed")
            assertEquals("سلام دنیا hello 123", ClipboardTools.read())
        } finally {
            previous?.let { ClipboardTools.set(it) }
        }
    }

    @Test
    fun `copy mode only fills the clipboard`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            System.getenv("SAYBOARD_CLIPBOARD_TESTS") == "1",
            "set SAYBOARD_CLIPBOARD_TESTS=1 to test the clipboard",
        )
        val previous = ClipboardTools.read()
        try {
            CopyOnlyTyper().typeText("متن آزمایشی")
            assertEquals("متن آزمایشی", ClipboardTools.read())
        } finally {
            previous?.let { ClipboardTools.set(it) }
        }
    }
}
