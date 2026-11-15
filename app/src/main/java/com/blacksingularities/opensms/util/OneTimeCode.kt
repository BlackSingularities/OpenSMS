package com.blacksingularities.opensms.util

/**
 * Wykrywanie kodów jednorazowych (SMS z banku, logowanie) – do przycisku „Kopiuj kod”.
 *
 * Zasada: sam ciąg cyfr nie wystarczy. Potrzebna jest zapowiedź kodu w pobliżu – albo zwrot
 * jednoznaczny („kod weryfikacyjny”, „otp”, „3D Secure”), albo słabsze słowo („kod”, „blik”,
 * „transakcja”) wraz z wyraźnym przypisaniem („kod: 123456”, „kod to 123456”). Dzięki temu
 * marketing bankowy, w którym słowa „blik” czy „transakcja” występują na co dzień, nie zapala
 * znaczka kopiowania.
 */
object OneTimeCode {
    private const val MAX_LEN = 400

    /** Jak daleko od zapowiedzi może stać kod (w znakach). */
    private const val STRONG_DISTANCE = 48
    private const val WEAK_DISTANCE = 26

    /** Zwroty jednoznacznie zapowiadające kod – wystarczą same. */
    private val strongKeywords = listOf(
        "kod jednorazow", "kod weryfikacyjn", "kod weryfikacji", "kod autoryz", "kod sms",
        "kod blik", "kod potwierdz", "kod aktywacyjn", "kod dostepu", "kod dostępu",
        "kod zabezpieczaj", "kod logowan", "twoj kod", "twój kod", "kod to", "kod:",
        "jednorazowe haslo", "jednorazowe hasło", "haslo jednorazowe", "hasło jednorazowe",
        "otp", "3d secure", "3d-secure", "3dsecure",
        "verification code", "security code", "access code", "login code", "auth code",
        "one-time", "one time password", "your code", "code:", "code is",
    )

    /** Słowa, które same o niczym nie przesądzają – wymagają przypisania tuż przed kodem. */
    private val weakKeywords = listOf(
        "kod", "code", "hasło", "haslo", "password", "pin", "weryfik", "verif", "autoryz",
        "potwierdz", "potwierdź", "token", "login", "logowan", "jednorazow", "blik", "transakcj",
    )

    private val codeRegex = Regex("""(?<![\d.,:/-])(\d{4,8}|\d{3}[- ]\d{3})(?![\d.,:/-]*\d)""")
    private val alnumRegex = Regex("""(?<![A-Za-z0-9])([A-Z0-9]{6,8})(?![A-Za-z0-9])""")

    /** Numer telefonu – kod nigdy nie leży w jego środku. */
    private val phoneRegex = Regex("""\+?\d[\d\s\-()]{5,}\d""")
    private val urlRegex = Regex("""(?:https?://|www\.)\S+""", RegexOption.IGNORE_CASE)

    /** Przypisanie kodu: „kod: 1234”, „kod to 1234”, „code is 1234”, „kod – 1234”. */
    private val assignRegex = Regex("""(?::|=|\bto\b|\bis\b|\bwynosi\b|[-–—])[\s]*$""", RegexOption.IGNORE_CASE)

    private val currencyRegex = Regex(
        """^\s*(?:zł|zl|pln|eur|usd|gbp|chf|czk|€|\$|%|proc\.|procent)""",
        RegexOption.IGNORE_CASE,
    )

    /** Słowa, po których cyfry są kwotą, numerem albo oprocentowaniem – nie kodem. */
    private val amountWords = listOf(
        "kwot", "saldo", "salda", "limit", "rrso", "oprocentowan", "rata", "raty", "cena",
        "nr ", "numer", "rachun", "konto", "konta", "umow", "faktur", "zaplac", "zapłać",
        "do zaplaty", "do zapłaty", "prowizj", "odsetk", "infolini",
    )

    fun find(text: String?): String? {
        if (text.isNullOrBlank() || text.length > MAX_LEN) return null
        val lower = text.lowercase()

        val strong = occurrences(lower, strongKeywords)
        val weak = occurrences(lower, weakKeywords)
        if (strong.isEmpty() && weak.isEmpty()) return null
        val anchors = strong.ifEmpty { weak }
        val maxDistance = if (strong.isNotEmpty()) STRONG_DISTANCE else WEAK_DISTANCE
        val requireAssign = strong.isEmpty()

        val blocked = buildList {
            phoneRegex.findAll(text).forEach { if (it.value.count(Char::isDigit) >= 9) add(it.range) }
            urlRegex.findAll(text).forEach { add(it.range) }
        }

        val numeric = codeRegex.findAll(text)
            .filter { accept(text, lower, it.range, blocked, anchors, maxDistance, requireAssign) }
            .filterNot { looksLikeDate(it.groupValues[1]) }
            .minByOrNull { distance(anchors, it.range.first) }
        if (numeric != null) return numeric.groupValues[1].replace(" ", "").replace("-", "")

        return alnumRegex.findAll(text)
            .filter { m -> m.groupValues[1].any(Char::isDigit) && m.groupValues[1].any(Char::isLetter) }
            .filter { accept(text, lower, it.range, blocked, anchors, maxDistance, requireAssign) }
            .minByOrNull { distance(anchors, it.range.first) }
            ?.groupValues?.get(1)
    }

    private fun accept(
        text: String,
        lower: String,
        range: IntRange,
        blocked: List<IntRange>,
        anchors: List<IntRange>,
        maxDistance: Int,
        requireAssign: Boolean,
    ): Boolean {
        if (blocked.any { range.first <= it.last && range.last >= it.first }) return false
        if (distance(anchors, range.first) > maxDistance) return false
        val prefix = lower.substring(maxOf(0, range.first - 24), range.first)
        val assigned = assignRegex.containsMatchIn(prefix)
        if (requireAssign && !assigned) return false
        val value = text.substring(range.first, range.last + 1)
        if (!assigned && looksLikeYear(value)) return false
        if (looksLikeAmount(text, lower, range)) return false
        return true
    }

    private fun occurrences(lower: String, keywords: List<String>): List<IntRange> = buildList {
        for (k in keywords) {
            var i = lower.indexOf(k)
            while (i >= 0) {
                add(i until i + k.length)
                i = lower.indexOf(k, i + 1)
            }
        }
    }

    /** Odległość do najbliższego wystąpienia zapowiedzi – nie do pierwszego w wiadomości. */
    private fun distance(anchors: List<IntRange>, pos: Int): Int = anchors.minOf { a ->
        when {
            pos in a -> 0
            pos < a.first -> a.first - pos
            else -> pos - a.last
        }
    }

    private fun looksLikeYear(s: String) = s.length == 4 && s.toIntOrNull()?.let { it in 1990..2099 } == true

    /** „12122026”, „20261205” – data zapisana jednym ciągiem. */
    private fun looksLikeDate(s: String): Boolean {
        if (s.length != 8 || s.any { !it.isDigit() }) return false
        val asYmd = s.take(4).toInt() in 1990..2099 && s.substring(4, 6).toInt() in 1..12 && s.substring(6).toInt() in 1..31
        val asDmy = s.takeLast(4).toInt() in 1990..2099 && s.take(2).toInt() in 1..31 && s.substring(2, 4).toInt() in 1..12
        return asYmd || asDmy
    }

    /** Kwota, procent albo numer rachunku: waluta po cyfrach lub słowo-kwota tuż przed nimi. */
    private fun looksLikeAmount(text: String, lower: String, range: IntRange): Boolean {
        val after = text.substring(range.last + 1)
        if (currencyRegex.containsMatchIn(after)) return true
        val before = lower.substring(maxOf(0, range.first - 16), range.first)
        return amountWords.any { before.contains(it) }
    }
}
