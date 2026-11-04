package com.blacksingularities.opensms

import com.blacksingularities.opensms.util.Emoticons
import org.junit.Assert.assertEquals
import org.junit.Test

class EmoticonsTest {
    @Test
    fun `sama emotikona staje sie emoji`() {
        assertEquals("🙂", Emoticons.apply(":)"))
        assertEquals("❤️", Emoticons.apply("<3"))
    }

    @Test
    fun `emotikona w zdaniu i po wyrazie`() {
        assertEquals("dobrze 🙂 jutro", Emoticons.apply("dobrze :) jutro"))
        assertEquals("ok😉", Emoticons.apply("ok;)"))
        assertEquals("no 😆 ale", Emoticons.apply("no XD ale"))
    }

    @Test
    fun `adresy i wyrazy zostaja bez zmian`() {
        assertEquals("http://example.com", Emoticons.apply("http://example.com"))
        assertEquals("waxd", Emoticons.apply("waxd"))
        assertEquals("c:\dane", Emoticons.apply("c:\dane"))
        assertEquals("zwykły tekst", Emoticons.apply("zwykły tekst"))
    }
}
