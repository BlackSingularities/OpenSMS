package com.blacksingularities.opensms

import com.blacksingularities.opensms.data.canReplyTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanReplyToTest {
    @Test
    fun `zwykle numery przyjmuja odpowiedz`() {
        assertTrue(canReplyTo("+48600100200"))
        assertTrue(canReplyTo("600 100 200"))
        assertTrue(canReplyTo("(22) 123-45-67"))
    }

    @Test
    fun `numery skrocone przyjmuja odpowiedz`() {
        assertTrue(canReplyTo("8080"))
        assertTrue(canReplyTo("112"))
    }

    @Test
    fun `nadawcy alfanumeryczni nie przyjmuja odpowiedzi`() {
        assertFalse(canReplyTo("Google"))
        assertFalse(canReplyTo("InPost"))
        assertFalse(canReplyTo("Info"))
        assertFalse(canReplyTo("mBank24"))
    }

    @Test
    fun `pusty adres i zbyt krotki numer nie przyjmuja odpowiedzi`() {
        assertFalse(canReplyTo(""))
        assertFalse(canReplyTo("   "))
        assertFalse(canReplyTo("12"))
    }

    @Test
    fun `adres e-mail przyjmuje wiadomosc MMS`() {
        assertTrue(canReplyTo("ktos@example.com"))
    }
}
