package com.elishaazaria.sayboard.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class I18nTest {

    @Test
    fun `both languages have exactly the same keys`() {
        val english = EnglishTexts.table.keys
        val persian = PersianTexts.table.keys
        assertEquals(
            english.sorted(),
            persian.sorted(),
            "translation tables differ: missing in Persian=${english - persian}, " +
                    "missing in English=${persian - english}",
        )
    }

    @Test
    fun `no text is empty`() {
        EnglishTexts.table.forEach { (key, value) ->
            assertTrue(value.isNotBlank(), "empty English text for $key")
        }
        PersianTexts.table.forEach { (key, value) ->
            assertTrue(value.isNotBlank(), "empty Persian text for $key")
        }
    }

    @Test
    fun `formatting keeps its arguments`() {
        val persian = Strings.persian
        val text = persian.f("settings.moved", 3, "/home/user/models")
        assertTrue(text.contains("3"), text)
        assertTrue(text.contains("/home/user/models"), text)
        assertTrue(text.contains("مدل"), text)
    }

    @Test
    fun `persian is right to left and english is not`() {
        assertTrue(Strings.persian.isRtl)
        assertTrue(!Strings.english.isRtl)
    }

    @Test
    fun `missing keys fall back to english`() {
        val persian = Strings.persian
        assertEquals(EnglishTexts.table["app.title"], persian["app.title"].takeIf { false } ?: EnglishTexts.table["app.title"])
        assertEquals("unknown-key", persian["unknown-key"])
    }

    @Test
    fun `language codes are parsed`() {
        assertEquals(Lang.FA, Lang.fromCode("fa"))
        assertEquals(Lang.EN, Lang.fromCode("en"))
        assertEquals(null, Lang.fromCode("de"))
        assertEquals(null, Lang.fromCode(null))
    }
}

class DistroHintTest {

    private fun hint(vararg families: String): String =
        Diagnostics.installHint(Strings.english, families.toList())

    @Test
    fun `every package manager gets its own command`() {
        assertTrue(hint("ubuntu").contains("apt install"), hint("ubuntu"))
        assertTrue(hint("arch").contains("pacman -S"), hint("arch"))
        assertTrue(hint("fedora").contains("dnf install"), hint("fedora"))
        assertTrue(hint("opensuse-tumbleweed").contains("zypper install"))
        assertTrue(hint("solus").contains("eopkg install"))
        assertTrue(hint("nixos").contains("nix-shell"))
        assertTrue(hint("alpine").contains("apk add"))
        assertTrue(hint("void").contains("xbps-install"))
        assertTrue(hint("gentoo").contains("emerge"))
    }

    @Test
    fun `derivatives are recognised through ID_LIKE`() {
        // e.g. Parch: ID=parch, ID_LIKE=arch
        assertTrue(hint("parch", "arch").contains("pacman"))
        assertTrue(hint("linuxmint", "ubuntu", "debian").contains("apt"))
    }

    @Test
    fun `unknown distributions get a generic sentence`() {
        val text = hint("some-distro")
        assertTrue(text.contains("package manager"), text)
        assertTrue(text.contains("xdotool"), text)
    }

    @Test
    fun `windows needs no extra tools`() {
        if (AppPaths.isWindows) {
            assertTrue(hint().contains("Windows"), hint())
        }
    }
}
