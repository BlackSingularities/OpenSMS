package com.blacksingularities.opensms

import com.blacksingularities.opensms.util.Reactions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactionsTest {
    @Test
    fun `reakcje Google Messages po angielsku i po polsku`() {
        assertEquals(Reactions.Reaction("👍", "Do zobaczenia"), Reactions.parse("Reacted 👍 to \"Do zobaczenia\""))
        assertEquals(Reactions.Reaction("❤️", "Kocham"), Reactions.parse("Zareagowano ❤️ na „Kocham”"))
        assertEquals(Reactions.Reaction("😂", "Ale akcja"), Reactions.parse("Reacted 😂 to “Ale akcja”"))
    }

    @Test
    fun `tapbacki iMessage`() {
        assertEquals(Reactions.Reaction("👍", "Jutro o 8?"), Reactions.parse("Liked “Jutro o 8?”"))
        assertEquals(Reactions.Reaction("😂", "xD"), Reactions.parse("Laughed at “xD”"))
        assertEquals(Reactions.Reaction("👍", "Kupię chleb"), Reactions.parse("Polubiono „Kupię chleb”"))
    }

    @Test
    fun `zwykle wiadomosci nie sa reakcjami`() {
        assertNull(Reactions.parse("Liked the movie a lot"))
        assertNull(Reactions.parse("Reacted badly to \"news\""))
        assertNull(Reactions.parse("Hej, co słychać?"))
        assertNull(Reactions.parse(""))
    }

    @Test
    fun `przyciety cytat pasuje do poczatku wiadomosci`() {
        assertTrue(Reactions.matches("Do zobaczenia", "Do zobaczenia"))
        assertTrue(Reactions.matches("To jest bardzo długa wiado…", "To jest bardzo długa wiadomość o niczym"))
        assertFalse(Reactions.matches("Coś innego", "Do zobaczenia"))
        assertFalse(Reactions.matches("", "Do zobaczenia"))
    }

    @Test
    fun `reakcja trafia pod wiadomosc i znika z listy`() {
        val items = listOf(
            "Hej, jutro kino?" to false,
            "Reacted 👍 to “Hej, jutro kino?”" to true,
            "Liked “Nie ma takiej wiadomości”" to false,
        )
        val r = Reactions.attach(items, { it.first }, { items.indexOf(it).toString() }, { it.second })
        assertEquals(listOf(Reactions.Pick("👍", true)), r.byTarget["0"])
        assertEquals(setOf("1"), r.hidden)
    }
}
