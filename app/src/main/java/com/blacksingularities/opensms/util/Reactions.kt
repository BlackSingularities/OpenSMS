package com.blacksingularities.opensms.util

/**
 * Reakcje wysyłane SMS-em przez Google Messages i iPhone'y przychodzą jako zwykły tekst,
 * np. `Reacted 👍 to "Do zobaczenia"`, `Liked “Do zobaczenia”`, `Polubiono „Do zobaczenia”`.
 * Rozpoznajemy je, chowamy jako osobne dymki i pokazujemy emoji pod wiadomością, której dotyczą.
 */
object Reactions {
    data class Reaction(val emoji: String, val quoted: String)

    /** Wynik dopasowania: emoji przypięte do wiadomości (klucz) i reakcje ukryte z listy. */
    data class Attached(val byTarget: Map<String, List<Pick>>, val hidden: Set<String>)

    data class Pick(val emoji: String, val mine: Boolean)

    private const val OPEN = "[\"“„«'‘‚]"
    private const val CLOSE = "[\"”“»'’‘]"

    /** „Reacted 😂 to …”, „Zareagowano 😂 na …”, „Zareagował(a) 😂 na …”. */
    private val generic = Regex(
        "^(?:Reacted|Zareagowano|Zareagowa(?:ł|ła|łem|łam|li|ły))\\s+(\\S+)\\s+(?:to|na)\\s+$OPEN(.+)$CLOSE\\s*$",
        RegexOption.DOT_MATCHES_ALL,
    )

    /** Tapbacki iMessage (angielskie i polskie nazwy). */
    private val verbs: List<Pair<String, String>> = listOf(
        "Laughed at" to "😂",
        "Liked" to "👍",
        "Loved" to "❤️",
        "Disliked" to "👎",
        "Emphasized" to "‼️",
        "Emphasised" to "‼️",
        "Questioned" to "❓",
        "Polubiono" to "👍",
        "Polubiła" to "👍",
        "Polubił" to "👍",
        "Pokochano" to "❤️",
        "Uwielbia" to "❤️",
        "Rozśmieszyło" to "😂",
        "Zaśmiano się z" to "😂",
        "Wyróżniono" to "‼️",
        "Nie lubi" to "👎",
        "Nie podoba się" to "👎",
        "Zapytano o" to "❓",
        "Zapytano" to "❓",
    )
    private val verbRegexes = verbs.map { (verb, emoji) ->
        Regex("^${Regex.escape(verb)}\\s+$OPEN(.+)$CLOSE\\s*$", RegexOption.DOT_MATCHES_ALL) to emoji
    }

    fun parse(body: String): Reaction? {
        val t = body.trim()
        if (t.length < 5 || t.length > 2000) return null
        generic.find(t)?.let { m ->
            val emoji = m.groupValues[1]
            if (emoji.any { it.isLetterOrDigit() }) return null
            return Reaction(emoji, m.groupValues[2].trim())
        }
        for ((re, emoji) in verbRegexes) {
            re.find(t)?.let { return Reaction(emoji, it.groupValues[1].trim()) }
        }
        return null
    }

    /** Czy cytat z reakcji (często przycięty i zakończony „…”) pasuje do treści wiadomości. */
    fun matches(quoted: String, target: String): Boolean {
        val q = quoted.trim().removeSuffix("…").removeSuffix("...").trim()
        val b = target.trim()
        if (q.isEmpty() || b.isEmpty()) return false
        return b == q || (q.length >= 3 && b.startsWith(q)) || normalize(b).startsWith(normalize(q))
    }

    private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { s }

    /**
     * Przypina reakcje do wiadomości. [items] w kolejności chronologicznej (od najstarszej).
     * Reakcja trafia do najnowszej wcześniejszej wiadomości o pasującej treści; bez celu zostaje zwykłym dymkiem.
     */
    fun <T> attach(items: List<T>, body: (T) -> String, key: (T) -> String, mine: (T) -> Boolean, hasMedia: (T) -> Boolean = { false }): Attached {
        val byTarget = LinkedHashMap<String, MutableList<Pick>>()
        val hidden = HashSet<String>()
        items.forEachIndexed { i, m ->
            if (hasMedia(m)) return@forEachIndexed
            val r = parse(body(m)) ?: return@forEachIndexed
            var j = i - 1
            val floor = maxOf(0, i - 300)
            while (j >= floor) {
                val t = items[j]
                if (key(t) !in hidden && matches(r.quoted, body(t))) {
                    byTarget.getOrPut(key(t)) { mutableListOf() } += Pick(r.emoji, mine(m))
                    hidden += key(m)
                    break
                }
                j--
            }
        }
        return Attached(byTarget, hidden)
    }
}
