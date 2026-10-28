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
    fun versionCompare() {
        assertTrue(isNewer("0.1.1", "0.1.0"))
        assertTrue(isNewer("0.2.0", "0.1.9"))
        assertTrue(isNewer("0.1.0.15", "0.1.0"))
        assertFalse(isNewer("0.1.0", "0.1.0"))
        assertFalse(isNewer("0.1.0", "0.1.0-dev"))
    }
}
