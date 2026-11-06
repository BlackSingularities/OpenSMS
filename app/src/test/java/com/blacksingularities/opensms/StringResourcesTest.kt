package com.blacksingularities.opensms

import com.blacksingularities.opensms.ui.plural
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tłumaczenia: angielski w `values/` jest wzorcem. Każdy katalog `values-xx` z `strings.xml`
 * to kolejny język – nie może mieć kluczy spoza wzorca, a argumenty (%1$s…) muszą się zgadzać.
 * Brakujące klucze są dozwolone (pokaże się angielski), ale polski musi być kompletny.
 */
class StringResourcesTest {
    private val entry = Regex("<string name=\"(\\w+)\"[^>]*>(.*?)</string>")
    private val placeholder = Regex("%(\\d)\\\$s")

    private fun load(path: String): Map<String, String> =
        entry.findAll(File(path).readText()).associate { it.groupValues[1] to it.groupValues[2] }

    private val en by lazy { load("src/main/res/values/strings.xml") }
    private val pl by lazy { load("src/main/res/values-pl/strings.xml") }

    /** Wszystkie tłumaczenia: nazwa katalogu → napisy. */
    private val translations by lazy {
        File("src/main/res").listFiles().orEmpty()
            .filter { it.name.startsWith("values-") && File(it, "strings.xml").exists() }
            .associate { it.name to load(File(it, "strings.xml").path) }
    }

    @Test
    fun `polski jest kompletny`() {
        assertTrue(en.size > 400)
        assertEquals(en.keys, pl.keys)
    }

    @Test
    fun `tlumaczenia nie maja obcych kluczy`() {
        for ((dir, map) in translations) {
            assertEquals("Klucze spoza values/strings.xml w $dir", emptySet<String>(), map.keys - en.keys)
        }
    }

    @Test
    fun `argumenty sa zgodne we wszystkich jezykach`() {
        for ((dir, map) in translations) {
            for ((key, value) in map) {
                val a = placeholder.findAll(en.getValue(key)).map { it.value }.toSortedSet()
                val b = placeholder.findAll(value).map { it.value }.toSortedSet()
                assertEquals("Argumenty w $dir/$key", a, b)
            }
        }
    }

    @Test
    fun `kazdy napis uzyty w kodzie istnieje`() {
        val used = File("src/main/java").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { Regex("R\\.string\\.(\\w+)").findAll(it.readText()).map { m -> m.groupValues[1] } }
            .toSet()
        assertTrue(used.isNotEmpty())
        assertEquals(emptySet<String>(), used - en.keys)
    }

    @Test
    fun `odmiana liczebnikow`() {
        assertEquals("1 wiadomość", plural(1, "wiadomość", "wiadomości", "wiadomości"))
        assertEquals("3 części", plural(3, "część", "części", "części"))
        assertEquals("5 SMS-ów", plural(5, "SMS", "SMS-y", "SMS-ów"))
        assertEquals("12 SMS-ów", plural(12, "SMS", "SMS-y", "SMS-ów"))
        assertEquals("22 SMS-y", plural(22, "SMS", "SMS-y", "SMS-ów"))
    }
}
