package com.blacksingularities.opensms.util

/**
 * Zamiana emotikon tekstowych na emoji przy wyświetlaniu wiadomości: `:)` staje się 🙂,
 * `<3` sercem i tak dalej. Treść wiadomości zostaje nietknięta – podmieniany jest tylko napis
 * pokazywany w dymku i na liście rozmów.
 *
 * Emotikona z dwukropkiem („dobrze:)”) łapie się też wprost po literze, a wzory z liter
 * („XD”, „T_T”) i ze znakiem ukośnika („:/”) wymagają przerwy przed sobą – inaczej psułyby
 * zwykłe wyrazy i adresy `http://`. Po emotikonie nie może stać litera ani cyfra, więc
 * zamiana działa w środku zdania: „dobrze :) jutro”.
 */
object Emoticons {
    private val map: List<Pair<String, String>> = listOf(
        ":)" to "🙂", ":-)" to "🙂", "=)" to "🙂", "(:" to "🙂",
        ":D" to "😃", ":-D" to "😃", "=D" to "😃", ":d" to "😃",
        "XD" to "😆", "xD" to "😆", "xd" to "😆",
        ";)" to "😉", ";-)" to "😉",
        ":(" to "🙁", ":-(" to "🙁", "=(" to "🙁", "):" to "🙁",
        ":'(" to "😢", ":,(" to "😢", "T_T" to "😭", ";(" to "😭",
        ":P" to "😛", ":-P" to "😛", ":p" to "😛", ":-p" to "😛", ";P" to "😜", ";p" to "😜",
        ":O" to "😮", ":-O" to "😮", ":o" to "😮", ":-o" to "😮",
        ":/" to "😕", ":-/" to "😕", ":\\" to "😕",
        ":|" to "😐", ":-|" to "😐", "-_-" to "😑",
        ":*" to "😘", ":-*" to "😘",
        ":3" to "😊", "^^" to "😄", "^_^" to "😄",
        ">:(" to "😠", ":@" to "😠",
        "8)" to "😎", "B)" to "😎", "8-)" to "😎",
        ":S" to "😖", ":s" to "😖", ":$" to "😳",
        "O_o" to "😳", "o_O" to "😳",
        "<3" to "❤️", "</3" to "💔",
        ":+1" to "👍", ":thumbsup:" to "👍",
    ).sortedByDescending { it.first.length }

    private fun Char.wordish() = isLetterOrDigit()

    /** Wzory z liter i z ukośnikiem wymagają przerwy przed sobą, żeby nie psuć wyrazów i adresów. */
    private fun strict(pattern: String) = pattern[0].isLetterOrDigit() || pattern.contains('/') || pattern.contains('\\')

    /** Zwraca tekst z emotikonami zamienionymi na emoji (albo ten sam tekst, gdy nie ma czego zmieniać). */
    fun apply(text: String): String {
        if (text.isEmpty()) return text
        var out: StringBuilder? = null
        var i = 0
        while (i < text.length) {
            val before = if (i == 0) null else text[i - 1]
            val free = before == null || (!before.wordish() && before != '/' && before != '\\' && before != ':')
            var hit: Pair<String, String>? = null
            for (e in map) {
                if (!text.startsWith(e.first, i)) continue
                if (!free && strict(e.first)) continue
                if (before == ':' || before == '/' || before == '\\') continue
                val after = text.getOrNull(i + e.first.length)
                if (after != null && after.wordish()) continue
                hit = e
                break
            }
            if (hit != null) {
                val b = out ?: StringBuilder(text.substring(0, i)).also { out = it }
                b.append(hit.second)
                i += hit.first.length
            } else {
                out?.append(text[i])
                i++
            }
        }
        return out?.toString() ?: text
    }
}
