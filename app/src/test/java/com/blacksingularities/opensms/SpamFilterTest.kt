package com.blacksingularities.opensms

import com.blacksingularities.opensms.util.SpamFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamFilterTest {
    @Test
    fun `oszustwa z doplata do paczki i blokada konta`() {
        assertTrue(SpamFilter.isSpam("Twoja paczka zostala wstrzymana. Dopłata 1,20 zł: https://bit.ly/abc123"))
        assertTrue(SpamFilter.isSpam("Twoje konto zostanie zablokowane. Zweryfikuj konto: http://192.168.10.4/login"))
        assertTrue(SpamFilter.isSpam("URGENT: your package is on hold, pay the delivery fee now: https://parcel-help.top/pay"))
    }

    @Test
    fun `zwykle wiadomosci przechodza`() {
        assertFalse(SpamFilter.isSpam("Hej, będę o 18. Kupić coś po drodze?"))
        assertFalse(SpamFilter.isSpam("Twój kod logowania do Allegro: 482913. Nie podawaj go nikomu."))
        assertFalse(SpamFilter.isSpam("Twoja paczka czeka w Paczkomacie KRA01M. Kod odbioru: 123456. Szczegóły: https://inpost.pl/sledzenie"))
        assertFalse(SpamFilter.isSpam("Zobacz zdjęcia z wyjazdu: https://photos.app.goo.gl/xyz"))
    }
}
