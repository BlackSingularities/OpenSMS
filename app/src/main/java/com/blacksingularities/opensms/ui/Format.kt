package com.blacksingularities.opensms.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val PL = Locale("pl", "PL")

private fun cal(t: Long) = Calendar.getInstance().apply { timeInMillis = t }

private fun sameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

fun isSameDay(a: Long, b: Long) = sameDay(cal(a), cal(b))

fun formatTime(t: Long): String = SimpleDateFormat("HH:mm", PL).format(Date(t))

/** Czas na liście rozmów: „teraz”, „5 min”, godzina, dzień tygodnia albo data. */
fun formatListTime(t: Long, relative: Boolean): String {
    val now = System.currentTimeMillis()
    val diff = now - t
    if (relative && diff in 0 until 60_000) return "teraz"
    if (relative && diff in 60_000 until 3_600_000) return "${diff / 60_000} min"
    val c = cal(t)
    val n = cal(now)
    return when {
        sameDay(c, n) -> formatTime(t)
        diff < 6 * 24 * 3_600_000L -> SimpleDateFormat("EEE", PL).format(Date(t)).trimEnd('.')
        c.get(Calendar.YEAR) == n.get(Calendar.YEAR) -> SimpleDateFormat("d MMM", PL).format(Date(t))
        else -> SimpleDateFormat("d.MM.yyyy", PL).format(Date(t))
    }
}

/** Separator dnia w rozmowie: „Dziś”, „Wczoraj”, „poniedziałek, 3 marca”. */
fun formatDayHeader(t: Long): String {
    val c = cal(t)
    val n = cal(System.currentTimeMillis())
    if (sameDay(c, n)) return "Dziś"
    n.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(c, n)) return "Wczoraj"
    val now = cal(System.currentTimeMillis())
    val pattern = if (c.get(Calendar.YEAR) == now.get(Calendar.YEAR)) "EEEE, d MMMM" else "d MMMM yyyy"
    return SimpleDateFormat(pattern, PL).format(Date(t)).replaceFirstChar { it.uppercase() }
}

fun formatFull(t: Long): String = SimpleDateFormat("d MMMM yyyy, HH:mm:ss", PL).format(Date(t))

fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(PL, "%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

/** Liczba w odmianie: 1 wiadomość, 2 wiadomości, 5 wiadomości. */
fun plural(n: Int, one: String, few: String, many: String): String {
    val form = when {
        n == 1 -> one
        n % 10 in 2..4 && n % 100 !in 12..14 -> few
        else -> many
    }
    return "$n $form"
}
