package com.blacksingularities.opensms.util

/**
 * Prosty, lokalny filtr spamu – bez sieci i bez wysyłania treści gdziekolwiek.
 * Punktuje typowe cechy oszustw SMS (dopłata do paczki, blokada konta, skrócone linki).
 * Sprawdzane są tylko wiadomości od nadawców spoza kontaktów; zatrzymane da się przywrócić.
 */
object SpamFilter {
    const val THRESHOLD = 4

    private val url = Regex("(?i)\\b((?:https?://|www\\.)\\S+|[a-z0-9-]+\\.(?:[a-z]{2,6})/\\S*)")
    private val shorteners = Regex(
        "(?i)\\b(bit\\.ly|tinyurl\\.com|t\\.co|is\\.gd|cutt\\.ly|goo\\.gl|ow\\.ly|rb\\.gy|shorturl\\.at|tiny\\.cc|s\\.id|t\\.ly|rebrand\\.ly|bitly\\.com)/",
    )
    private val ipUrl = Regex("(?i)https?://\\d{1,3}(?:\\.\\d{1,3}){3}")
    private val shadyTld = Regex("(?i)\\.(xyz|top|click|icu|buzz|cfd|sbs|rest|bond|online|site|live|shop|info|monster|lol|cyou|vip)(?:/|\\b)")

    /** Frazy typowe dla oszustw (PL i EN); małe litery. */
    private val phrases = listOf(
        // Paczki i dopłaty
        "dopłat", "doplat", "niedopłat", "paczka zosta", "paczka jest wstrzymana", "przesyłka zosta", "przesylka zosta",
        "wstrzyman", "adres dostawy", "uzupełnij adres", "potwierdź adres", "opłata celna", "oplata celna",
        "your package", "parcel", "delivery fee", "customs fee", "redelivery",
        // Konto, bank, blokady
        "zostanie zablokowan", "zostało zablokowan", "zostalo zablokowan", "konto zablokowane", "zweryfikuj konto",
        "weryfikacja konta", "potwierdź dane", "aktualizacja danych", "nieautoryzowan", "podejrzane logowanie",
        "account suspended", "account has been", "verify your account", "unusual activity", "confirm your identity",
        // Energia, podatki, mandaty
        "odłączenie prądu", "odlaczenie pradu", "zaległ", "zalegl", "nadpłat", "zwrot podatku", "mandat", "e-toll",
        "tax refund", "unpaid", "outstanding balance",
        // Wygrane, kredyty, oferty
        "wygrał", "wygral", "wygrana", "nagroda", "gratulacje", "darmow", "pożyczk", "pozyczk", "kredyt bez",
        "inwestuj", "kryptowalut", "zarabiaj", "winner", "you have won", "prize", "lottery", "free gift", "claim now",
        "loan", "crypto", "investment",
        // Presja i wezwanie do kliknięcia
        "kliknij", "wejdź w link", "wejdz w link", "pilne", "natychmiast", "w ciągu 24", "w ciagu 24", "ostatnie ostrzeżenie",
        "click here", "click the link", "urgent", "immediately", "within 24", "final notice", "act now",
    )

    fun score(body: String): Int {
        val t = body.lowercase()
        var s = 0
        val hasUrl = url.containsMatchIn(body)
        if (hasUrl) s += 1
        if (shorteners.containsMatchIn(body)) s += 2
        if (ipUrl.containsMatchIn(body)) s += 2
        if (hasUrl && shadyTld.containsMatchIn(body)) s += 1
        val hits = phrases.count { it in t }
        s += minOf(hits, 3)
        // Wezwanie do działania połączone z linkiem to najsilniejszy sygnał.
        if (hasUrl && hits > 0) s += 1
        val letters = body.filter { it.isLetter() }
        if (letters.length >= 20 && letters.count { it.isUpperCase() } * 10 >= letters.length * 7) s += 1
        return s
    }

    fun isSpam(body: String, threshold: Int = THRESHOLD): Boolean = score(body) >= threshold
}
