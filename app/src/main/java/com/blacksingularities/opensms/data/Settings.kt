package com.blacksingularities.opensms.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray
import org.json.JSONObject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Rozmiar tekstu wiadomości. */
enum class TextSize(val label: String, val scale: Float) {
    SMALL("Mały", 0.88f),
    NORMAL("Zwykły", 1f),
    LARGE("Duży", 1.15f),
    HUGE("Bardzo duży", 1.32f),
}

/** Limit rozmiaru MMS – zdjęcia są zmniejszane, żeby zmieścić się w limicie operatora. */
enum class MmsLimit(val label: String, val bytes: Int) {
    KB300("300 KB", 300 * 1024),
    KB600("600 KB", 600 * 1024),
    MB1("1 MB", 1024 * 1024),
}

/** Akcja przesunięcia rozmowy w bok na liście. */
enum class SwipeAction(val label: String) {
    NONE("Brak"),
    ARCHIVE("Archiwizuj"),
    READ("Przeczytane"),
    PIN("Przypnij"),
    DELETE("Usuń"),
}

data class Template(val id: Long, val text: String)

data class ScheduledMessage(
    val id: Long,
    val addresses: List<String>,
    val text: String,
    val time: Long,
    val subId: Int,
)

/** Wiadomość zatrzymana przez filtr (do przejrzenia w ustawieniach). */
data class BlockedLogEntry(val address: String, val body: String, val date: Long, val reason: String)

/**
 * Ustawienia aplikacji w SharedPreferences. Każde pole jest też stanem Compose,
 * więc zmiana od razu odświeża interfejs; odbiorniki w tle czytają te same wartości.
 */
class Settings private constructor(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("opensms", Context.MODE_PRIVATE)

    // Wygląd
    var textSize by enumPref("textSize", TextSize.NORMAL)
    var colorAvatars by boolPref("colorAvatars", true)
    var showSimBadge by boolPref("showSimBadge", true)
    var swipeLeft by enumPref("swipeLeft", SwipeAction.ARCHIVE)
    var swipeRight by enumPref("swipeRight", SwipeAction.READ)
    var haptics by boolPref("haptics", true)
    var relativeTimes by boolPref("relativeTimes", true)

    // Wysyłanie
    var deliveryReports by boolPref("deliveryReports", false)
    var signature by stringPref("signature", "")
    var enterSends by boolPref("enterSends", false)
    var longAsMms by boolPref("longAsMms", false)
    var longAsMmsSegments by intPref("longAsMmsSegments", 4)
    var groupAsMms by boolPref("groupAsMms", true)
    var mmsLimit by enumPref("mmsLimit", MmsLimit.KB600)
    var stripAccents by boolPref("stripAccents", false)
    var confirmDelete by boolPref("confirmDelete", true)
    var defaultSubId by intPref("defaultSubId", -1)
    var autoDownloadMms by boolPref("autoDownloadMms", true)

    // Powiadomienia
    var notifications by boolPref("notifications", true)
    var notificationPreview by boolPref("notificationPreview", true)
    var copyCodeAction by boolPref("copyCodeAction", true)
    var markReadOnReply by boolPref("markReadOnReply", true)
    var notifyFailures by boolPref("notifyFailures", true)

    // Prywatność i filtry
    var blockUnknown by boolPref("blockUnknown", false)
    var keepBlockedLog by boolPref("keepBlockedLog", true)
    var blockedNumbers by stringSetPref("blockedNumbers")
    var blockedKeywords by stringSetPref("blockedKeywords")

    // Rozmowy
    var pinned by stringSetPref("pinned")
    var archived by stringSetPref("archived")
    var muted by stringSetPref("muted")
    /** Kosz: wpisy „threadId:czas przeniesienia”. */
    var trash by stringSetPref("trash")
    var failedMms by stringSetPref("failedMms")

    // Aktualizacje
    var autoUpdateCheck by boolPref("autoUpdateCheck", true)
    var lastUpdateCheck by longPref("lastUpdateCheck", 0L)
    var onboardingDone by boolPref("onboardingDone", false)

    private val templatesState = mutableStateOf(readTemplates())
    private val draftsState = mutableStateOf(readDrafts())
    private val scheduledState = mutableStateOf(readScheduled())
    private val blockedLogState = mutableStateOf(readBlockedLog())

    val templates: List<Template> get() = templatesState.value
    val drafts: Map<Long, String> get() = draftsState.value
    val scheduled: List<ScheduledMessage> get() = scheduledState.value
    val blockedLog: List<BlockedLogEntry> get() = blockedLogState.value

    fun isPinned(threadId: Long) = threadId.toString() in pinned
    fun isArchived(threadId: Long) = threadId.toString() in archived
    fun isMuted(threadId: Long) = threadId.toString() in muted

    fun trashedAt(): Map<Long, Long> = trash.mapNotNull { e ->
        val (id, t) = e.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
        (id.toLongOrNull() ?: return@mapNotNull null) to (t.toLongOrNull() ?: 0L)
    }.toMap()

    fun toggleTrashed(ids: Collection<Long>, on: Boolean) {
        val keep = trash.filterNot { e -> ids.any { e.startsWith("$it:") } }.toSet()
        val now = System.currentTimeMillis()
        trash = if (on) keep + ids.map { "$it:$now" } else keep
    }

    fun togglePinned(ids: Collection<Long>, on: Boolean) {
        pinned = if (on) pinned + ids.map { it.toString() } else pinned - ids.map { it.toString() }.toSet()
    }

    fun toggleArchived(ids: Collection<Long>, on: Boolean) {
        archived = if (on) archived + ids.map { it.toString() } else archived - ids.map { it.toString() }.toSet()
    }

    fun toggleMuted(id: Long, on: Boolean) {
        muted = if (on) muted + id.toString() else muted - id.toString()
    }

    /** Usunięte rozmowy nie zostawiają śladów w przypięciach, archiwum i szkicach. */
    fun forgetThreads(ids: Collection<Long>) {
        val keys = ids.map { it.toString() }.toSet()
        pinned = pinned - keys
        archived = archived - keys
        muted = muted - keys
        toggleTrashed(ids, false)
        val d = drafts.toMutableMap()
        ids.forEach { d.remove(it) }
        writeDrafts(d)
    }

    // --- Numery zablokowane ---

    fun isBlockedNumber(address: String): Boolean {
        val n = normalizeNumber(address)
        return blockedNumbers.any { normalizeNumber(it) == n || (n.length >= 7 && it.length >= 7 && n.endsWith(normalizeNumber(it).takeLast(9))) }
    }

    fun blockedKeywordIn(text: String): String? {
        val t = text.lowercase()
        return blockedKeywords.firstOrNull { it.isNotBlank() && t.contains(it.lowercase()) }
    }

    fun addBlockedLog(entry: BlockedLogEntry) {
        if (!keepBlockedLog) return
        val list = (listOf(entry) + blockedLog).take(200)
        blockedLogState.value = list
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("a", it.address).put("b", it.body).put("d", it.date).put("r", it.reason))
        }
        sp.edit().putString("blockedLog", arr.toString()).apply()
    }

    fun clearBlockedLog() {
        blockedLogState.value = emptyList()
        sp.edit().remove("blockedLog").apply()
    }

    private fun readBlockedLog(): List<BlockedLogEntry> = runCatching {
        val arr = JSONArray(sp.getString("blockedLog", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            BlockedLogEntry(o.optString("a"), o.optString("b"), o.optLong("d"), o.optString("r"))
        }
    }.getOrDefault(emptyList())

    // --- Szablony szybkich odpowiedzi ---

    fun saveTemplates(list: List<Template>) {
        templatesState.value = list
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("text", it.text)) }
        sp.edit().putString("templates", arr.toString()).apply()
    }

    private fun readTemplates(): List<Template> {
        val raw = sp.getString("templates", null) ?: return DEFAULT_TEMPLATES.mapIndexed { i, t -> Template(i.toLong() + 1, t) }
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { val o = arr.getJSONObject(it); Template(o.getLong("id"), o.getString("text")) }
        }.getOrDefault(emptyList())
    }

    // --- Szkice ---

    fun saveDraft(threadId: Long, text: String) {
        val d = drafts.toMutableMap()
        if (text.isBlank()) d.remove(threadId) else d[threadId] = text
        if (d != drafts) writeDrafts(d)
    }

    private fun writeDrafts(d: Map<Long, String>) {
        draftsState.value = d
        val o = JSONObject()
        d.forEach { (k, v) -> o.put(k.toString(), v) }
        sp.edit().putString("drafts", o.toString()).apply()
    }

    private fun readDrafts(): Map<Long, String> = runCatching {
        val o = JSONObject(sp.getString("drafts", "{}") ?: "{}")
        o.keys().asSequence().associate { it.toLong() to o.getString(it) }
    }.getOrDefault(emptyMap())

    // --- Wiadomości zaplanowane ---

    fun saveScheduled(list: List<ScheduledMessage>) {
        scheduledState.value = list.sortedBy { it.time }
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject().put("id", m.id).put("text", m.text).put("time", m.time).put("sub", m.subId)
                    .put("to", JSONArray(m.addresses)),
            )
        }
        sp.edit().putString("scheduled", arr.toString()).commit()
    }

    /** Odczyt z dysku – odbiornik alarmu może działać w innym procesie niż interfejs. */
    fun reloadScheduled(): List<ScheduledMessage> {
        scheduledState.value = readScheduled()
        return scheduled
    }

    private fun readScheduled(): List<ScheduledMessage> = runCatching {
        val arr = JSONArray(sp.getString("scheduled", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val to = o.getJSONArray("to")
            ScheduledMessage(
                o.getLong("id"),
                (0 until to.length()).map { i -> to.getString(i) },
                o.getString("text"),
                o.getLong("time"),
                o.optInt("sub", -1),
            )
        }.sortedBy { it.time }
    }.getOrDefault(emptyList())

    // --- Delegaty ---

    private fun boolPref(key: String, def: Boolean) = Pref(key, def, { sp.getBoolean(key, def) }, { putBoolean(key, it) })
    private fun intPref(key: String, def: Int) = Pref(key, def, { sp.getInt(key, def) }, { putInt(key, it) })
    private fun longPref(key: String, def: Long) = Pref(key, def, { sp.getLong(key, def) }, { putLong(key, it) })
    private fun stringPref(key: String, def: String) = Pref(key, def, { sp.getString(key, def) ?: def }, { putString(key, it) })
    private fun stringSetPref(key: String) = Pref<Set<String>>(key, emptySet(), { sp.getStringSet(key, emptySet())?.toSet() ?: emptySet() }, { putStringSet(key, it) })
    private inline fun <reified E : Enum<E>> enumPref(key: String, def: E) = Pref(
        key,
        def,
        { runCatching { enumValueOf<E>(sp.getString(key, def.name)!!) }.getOrDefault(def) },
        { putString(key, it.name) },
    )

    private inner class Pref<T>(
        key: String,
        def: T,
        read: () -> T,
        private val write: SharedPreferences.Editor.(T) -> Unit,
    ) : ReadWriteProperty<Any?, T> {
        private val state: MutableState<T> = mutableStateOf(runCatching(read).getOrDefault(def))

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state.value = value
            sp.edit().apply { write(value) }.apply()
        }
    }

    companion object {
        @Volatile
        private var instance: Settings? = null

        fun get(context: Context): Settings =
            instance ?: synchronized(this) { instance ?: Settings(context.applicationContext).also { instance = it } }

        val DEFAULT_TEMPLATES = listOf(
            "Oddzwonię za chwilę.",
            "Jestem w drodze.",
            "Nie mogę teraz rozmawiać, napiszę później.",
            "Dzięki!",
            "OK 👍",
        )
    }
}

/**
 * Czy na ten adres da się odpowiedzieć. Nadawcy alfanumeryczni („Google”, „InPost”, „Info”)
 * to jednokierunkowe identyfikatory – operator nie przyjmie wiadomości wysłanej na nie.
 * Numery skrócone (np. „8080”) i adresy e-mail (MMS) są w porządku.
 */
fun canReplyTo(address: String): Boolean {
    val a = address.trim()
    if (a.isEmpty()) return false
    if ('@' in a) return true
    if (a.any { it.isLetter() }) return false
    return a.count { it.isDigit() } >= 3
}

/** Numer bez spacji, myślników i nawiasów – do porównań. */
fun normalizeNumber(address: String): String {
    val digits = address.filter { it.isDigit() || it == '+' }
    return if (digits.length >= 3) digits else address.trim().lowercase()
}
