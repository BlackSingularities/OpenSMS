package com.blacksingularities.opensms.util

/** Wykrywanie kodów jednorazowych (SMS z banku, logowanie) – do przycisku „Kopiuj kod”. */
object OneTimeCode {
    private val keywords = listOf(
        "kod", "code", "hasło", "haslo", "password", "pin", "otp", "weryfik", "verif", "autoryz",
        "potwierdz", "potwierdź", "token", "login", "logowan", "jednorazow", "blik", "3d secure", "transakcj",
    )
    private val codeRegex = Regex("""(?<![\d.,:/-])(\d{4,8}|\d{3}[- ]\d{3})(?![\d.,:/-]*\d)""")
    private val alnumRegex = Regex("""(?<![A-Za-z0-9])([A-Z0-9]{6,8})(?![A-Za-z0-9])""")

    fun find(text: String?): String? {
        if (text.isNullOrBlank() || text.length > 400) return null
        val lower = text.lowercase()
        if (keywords.none { lower.contains(it) }) return null
        // Najpierw liczba najbliżej słowa kluczowego.
        val anchor = keywords.mapNotNull { k -> lower.indexOf(k).takeIf { it >= 0 } }.minOrNull() ?: 0
        val numeric = codeRegex.findAll(text)
            .map { it.groupValues[1] }
            .filterNot { looksLikeYear(it) || looksLikeAmount(text, it) }
            .sortedBy { kotlin.math.abs(text.indexOf(it) - anchor) }
            .firstOrNull()
        if (numeric != null) return numeric.replace(" ", "").replace("-", "")
        return alnumRegex.findAll(text).map { it.groupValues[1] }.firstOrNull { c -> c.any { it.isDigit() } && c.any { it.isLetter() } }
    }

    private fun looksLikeYear(s: String) = s.length == 4 && s.toIntOrNull()?.let { it in 1990..2099 } == true

    private fun looksLikeAmount(text: String, code: String): Boolean {
        val i = text.indexOf(code)
        if (i < 0) return false
        val after = text.substring(i + code.length).trimStart().lowercase()
        return after.startsWith("zł") || after.startsWith("pln") || after.startsWith("eur") || after.startsWith("usd")
    }
}
