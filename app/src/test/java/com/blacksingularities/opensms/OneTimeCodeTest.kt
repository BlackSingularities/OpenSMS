package com.blacksingularities.opensms

import com.blacksingularities.opensms.update.isNewer
import com.blacksingularities.opensms.util.OneTimeCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OneTimeCodeTest {
    @Test
    fun findsBankCodes() {
        assertEquals("482913", OneTimeCode.find("Twój kod weryfikacyjny to 482913. Nie podawaj go nikomu."))
        assertEquals("7731", OneTimeCode.find("Kod BLIK: 7731"))
        assertEquals("123456", OneTimeCode.find("Your verification code is 123-456"))
    }

    @Test
    fun ignoresAmountsAndPlainMessages() {
        assertNull(OneTimeCode.find("Spotkajmy się o 18:30 pod kinem"))
        assertEquals("889201", OneTimeCode.find("Kod autoryzacji transakcji na 2500 zł: 889201"))
    }

    @Test
    fun ignoresBankMarketing() {
        assertNull(OneTimeCode.find("Wlacz BLIKA zblizeniowego, zrob nim 10 transakcji do 5.12.2026 r. i zgarnij 100 zl w formie czeku BLIK. Regulamin na stronie Alior Banku."))
        assertNull(OneTimeCode.find("Alior Bank: Twoj numer telefonu zarejestrowany w BLIK zostal przypisany do rachunku w innym banku. Jezeli nie wykonywales tej operacji skontaktuj sie z infolinia."))
        assertNull(OneTimeCode.find("Kazdy plan jest wazny, bo jest Twoj. Sprawdz pozyczke w Alior Mobile/Online. RRSO 13,13%"))
        assertNull(OneTimeCode.find("Potwierdzamy transakcje na kwote 2500 zl"))
        assertNull(OneTimeCode.find("Twoja rata wynosi 1499 zl miesiecznie"))
    }

    @Test
    fun ignoresPhoneNumbersAndLinks() {
        assertNull(OneTimeCode.find("W sprawie autoryzacji zadzwon na infolinie 801 123 456"))
        assertNull(OneTimeCode.find("Potwierdz transakcje: +48 123 456 789"))
        assertNull(OneTimeCode.find("Potwierdz logowanie na https://bank.pl/promo2024"))
    }

    @Test
    fun ignoresDatesAndIdentifiers() {
        assertNull(OneTimeCode.find("Kod rabatowy wazny do 12122026"))
        assertNull(OneTimeCode.find("Autoryzacja dotyczy umowy nr 445566"))
    }

    @Test
    fun versionCompare() {
        assertTrue(isNewer("0.1.1", "0.1.0"))
        assertTrue(isNewer("0.2.0", "0.1.9"))
        assertTrue(isNewer("0.1.0.15", "0.1.0"))
        assertFalse(isNewer("0.1.0", "0.1.0"))
        assertFalse(isNewer("0.1.0", "0.1.0-dev"))
    }
}
