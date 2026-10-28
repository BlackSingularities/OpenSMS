package com.blacksingularities.opensms.ui

import android.app.Application
import android.app.role.RoleManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blacksingularities.opensms.data.Attachment
import com.blacksingularities.opensms.data.Box
import com.blacksingularities.opensms.data.Contact
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.Conversation
import com.blacksingularities.opensms.data.Message
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.ScheduledMessage
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.data.SimCard
import com.blacksingularities.opensms.data.Sims
import com.blacksingularities.opensms.mms.MmsDownloader
import com.blacksingularities.opensms.mms.MmsSender
import com.blacksingularities.opensms.notify.Notifier
import com.blacksingularities.opensms.schedule.Scheduler
import com.blacksingularities.opensms.sms.MessageSender
import com.blacksingularities.opensms.update.UpdateManager
import com.blacksingularities.opensms.update.UpdateState
import com.blacksingularities.opensms.update.isNewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen

    /** Rozmowa; [threadId] = null – nowa, jeszcze bez wiadomości. */
    data class Thread(
        val threadId: Long?,
        val addresses: List<String>,
        val text: String = "",
        val attachments: List<Attachment> = emptyList(),
        val highlight: String? = null,
    ) : Screen

    data class Compose(val text: String = "", val attachments: List<Attachment> = emptyList()) : Screen
    data class SettingsScreen(val page: SettingsPage = SettingsPage.MAIN) : Screen
    data class Media(val uri: Uri, val mime: String, val name: String?) : Screen
}

enum class SettingsPage { MAIN, BLOCKED, BLOCKED_LOG, TEMPLATES, SCHEDULED, STATS }

enum class Filter(val label: String) { ALL("Wszystkie"), UNREAD("Nieprzeczytane"), PINNED("Przypięte"), ARCHIVED("Archiwum"), TRASH("Kosz") }

/** Rozmowa z gotowymi danymi do wyświetlenia. */
data class ConversationItem(
    val conv: Conversation,
    val title: String,
    val photo: Uri?,
    val known: Boolean,
    val pinned: Boolean,
    val archived: Boolean,
    val trashed: Boolean,
    val muted: Boolean,
)

data class Stats(
    val smsIn: Int,
    val smsOut: Int,
    val mmsIn: Int,
    val mmsOut: Int,
    val threads: Int,
    val top: List<Pair<String, Int>>,
    val busiestHour: Int,
    val avgLength: Int,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val store = MessageStore(ctx)
    val settings = Settings.get(ctx)

    var stack by mutableStateOf(listOf<Screen>(Screen.Home))
        private set
    val screen: Screen get() = stack.last()

    var isDefault by mutableStateOf(false)
    var conversations by mutableStateOf<List<ConversationItem>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var filter by mutableStateOf(Filter.ALL)
    var selection by mutableStateOf<Set<Long>>(emptySet())

    var searching by mutableStateOf(false)
    var query by mutableStateOf("")
    var searchResults by mutableStateOf<List<Message>>(emptyList())
        private set

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set
    private var messageLimit = PAGE
    var canLoadMore by mutableStateOf(false)
        private set
    var openThreadId by mutableStateOf<Long?>(null)
        private set

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var sims by mutableStateOf<List<SimCard>>(emptyList())
        private set

    private var toastState by mutableStateOf<String?>(null)
    /** Licznik komunikatów – ten sam tekst pokazany drugi raz znów odlicza czas. */
    var toastId by mutableStateOf(0)
        private set
    var toast: String?
        get() = toastState
        set(value) {
            toastState = value
            undo = null
            if (value != null) toastId++
        }
    /** Cofnięcie ostatniej operacji – pokazywane jako „Cofnij” przy komunikacie. */
    var undo by mutableStateOf<(() -> Unit)?>(null)
    private var hiddenMessages = emptySet<Long>()
    var update by mutableStateOf<UpdateState>(UpdateState.Idle)
    var updateBannerHidden by mutableStateOf(false)
    var stats by mutableStateOf<Stats?>(null)
        private set
    var busy by mutableStateOf<String?>(null)

    private var reloadJob: Job? = null
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleReload()
    }

    init {
        runCatching { ctx.contentResolver.registerContentObserver(Uri.parse("content://mms-sms/"), true, observer) }
        runCatching { ctx.contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer) }
        runCatching { ctx.contentResolver.registerContentObserver(Telephony.Mms.CONTENT_URI, true, observer) }
        refreshDefault()
        reload()
        if (settings.autoUpdateCheck) checkUpdate(silent = true)
        viewModelScope.launch(Dispatchers.IO) { MmsDownloader.cleanup(ctx) }
        purgeTrash()
    }

    override fun onCleared() {
        ctx.contentResolver.unregisterContentObserver(observer)
    }

    fun refreshDefault() {
        val byPackage = Telephony.Sms.getDefaultSmsPackage(ctx) == ctx.packageName
        val byRole = runCatching {
            ctx.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_SMS) == true
        }.getOrDefault(false)
        isDefault = byPackage || byRole
    }

    // --- Nawigacja ---

    fun push(s: Screen) {
        stack = stack + s
        onScreenChanged()
    }

    fun replace(s: Screen) {
        stack = stack.dropLast(1) + s
        onScreenChanged()
    }

    fun back(): Boolean {
        if (selection.isNotEmpty()) {
            selection = emptySet(); return true
        }
        if (stack.size == 1 && searching) {
            closeSearch(); return true
        }
        if (stack.size == 1 && filter != Filter.ALL) {
            filter = Filter.ALL; return true
        }
        if (stack.size <= 1) return false
        stack = stack.dropLast(1)
        onScreenChanged()
        return true
    }

    fun home() {
        stack = listOf(Screen.Home)
        onScreenChanged()
    }

    fun openThread(threadId: Long, highlight: String? = null) {
        viewModelScope.launch {
            val addresses = withContext(Dispatchers.IO) { store.threadAddresses(threadId) }
            val draft = settings.drafts[threadId].orEmpty()
            val target = Screen.Thread(threadId, addresses, text = draft, highlight = highlight)
            if (screen is Screen.Thread || screen is Screen.Compose) replace(target) else push(target)
        }
    }

    /** Otwiera rozmowę z numerami (istniejącą albo nową). */
    fun openAddresses(addresses: List<String>, text: String = "", attachments: List<Attachment> = emptyList(), replaceTop: Boolean = false) {
        viewModelScope.launch {
            val threadId = withContext(Dispatchers.IO) { existingThread(addresses) }
            val draft = threadId?.let { settings.drafts[it] }.orEmpty()
            val target = Screen.Thread(threadId, addresses, text = text.ifEmpty { draft }, attachments = attachments)
            if (replaceTop) replace(target) else push(target)
        }
    }

    private fun existingThread(addresses: List<String>): Long? {
        val wanted = addresses.map { com.blacksingularities.opensms.data.normalizeNumber(it) }.toSet()
        return conversations.firstOrNull { c -> c.conv.addresses.map { com.blacksingularities.opensms.data.normalizeNumber(it) }.toSet() == wanted }?.conv?.threadId
    }

    private fun onScreenChanged() {
        val s = screen
        if (s is Screen.Thread && s.threadId != null) {
            if (openThreadId != s.threadId) {
                openThreadId = s.threadId
                messageLimit = PAGE
                messages = emptyList()
                loadMessages(markRead = true)
            }
            Notifier.activeThread = s.threadId
        } else if (s !is Screen.Media) {
            openThreadId = null
            messages = emptyList()
            Notifier.activeThread = -1
        }
        if (s is Screen.Compose && contacts.isEmpty()) loadContacts()
        if (s is Screen.Thread || s is Screen.Compose) loadSims()
    }

    /** Aktywność wróciła na pierwszy plan / zeszła z niego. */
    fun onForeground(visible: Boolean) {
        Notifier.activeThread = if (visible) openThreadId ?: -1 else -1
        if (visible) {
            refreshDefault()
            reload()
            openThreadId?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    store.markRead(id)
                    Notifier.cancel(ctx, id)
                }
            }
        }
    }

    // --- Dane ---

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(250)
            reload()
        }
    }

    fun reload() {
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) {
                val trash = settings.trashedAt()
                // Nowa wiadomość w rozmowie z kosza wyciąga ją z powrotem.
                val revived = store.conversations().also { list ->
                    list.filter { c -> trash[c.threadId]?.let { c.date > it } == true }.map { it.threadId }
                        .takeIf { it.isNotEmpty() }?.let { settings.toggleTrashed(it, false) }
                }
                val trashed = settings.trashedAt().keys
                revived.map { c ->
                    val single = c.addresses.size == 1
                    val info = if (single) Contacts.lookup(ctx, c.addresses[0]) else null
                    ConversationItem(
                        conv = c,
                        title = Contacts.title(ctx, c.addresses),
                        photo = info?.photo,
                        known = info != null,
                        pinned = settings.isPinned(c.threadId),
                        archived = settings.isArchived(c.threadId),
                        trashed = c.threadId in trashed,
                        muted = settings.isMuted(c.threadId),
                    )
                }
            }
            conversations = items
            loaded = true
            if (openThreadId != null) loadMessages(markRead = true)
            if (searching && query.isNotBlank()) runSearch()
        }
    }

    /** Lista po filtrze: przypięte na górze, archiwalne tylko w „Archiwum”. */
    fun visibleConversations(): List<ConversationItem> {
        val live = conversations.filter { !it.trashed }
        val list = when (filter) {
            Filter.ALL -> live.filter { !it.archived }
            Filter.UNREAD -> live.filter { it.conv.unread > 0 }
            Filter.PINNED -> live.filter { it.pinned }
            Filter.ARCHIVED -> live.filter { it.archived }
            Filter.TRASH -> conversations.filter { it.trashed }
        }
        return list.sortedWith(compareByDescending<ConversationItem> { it.pinned && filter != Filter.ARCHIVED && filter != Filter.TRASH }.thenByDescending { it.conv.date })
    }

    val unreadCount: Int get() = conversations.filter { !it.archived && !it.trashed }.sumOf { it.conv.unread }

    private fun loadMessages(markRead: Boolean) {
        val id = openThreadId ?: return
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val l = store.messages(id, messageLimit)
                if (markRead && l.any { it.incoming && !it.read }) {
                    store.markRead(id)
                    Notifier.cancel(ctx, id)
                }
                l
            }
            if (openThreadId == id) {
                messages = list.filter { it.id !in hiddenMessages }
                canLoadMore = list.size >= messageLimit
            }
        }
    }

    fun loadMore() {
        if (!canLoadMore) return
        messageLimit += PAGE
        loadMessages(markRead = false)
    }

    fun loadContacts() {
        viewModelScope.launch {
            contacts = withContext(Dispatchers.IO) { Contacts.all(ctx) }
        }
    }

    fun loadSims() {
        viewModelScope.launch { sims = withContext(Dispatchers.IO) { Sims.active(ctx) } }
    }

    fun onPermissionsChanged() {
        Contacts.clearCache()
        reload()
        loadSims()
        if (contacts.isEmpty()) loadContacts()
    }

    // --- Wyszukiwanie ---

    fun openSearch() {
        searching = true
        query = ""
        searchResults = emptyList()
    }

    fun closeSearch() {
        searching = false
        query = ""
        searchResults = emptyList()
    }

    private var searchJob: Job? = null

    fun updateQuery(q: String) {
        query = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            runSearch()
        }
    }

    private suspend fun runSearch() {
        val q = query.trim()
        searchResults = if (q.length < 2) emptyList() else withContext(Dispatchers.IO) { store.search(q) }
    }

    fun titleFor(threadId: Long): String = conversations.firstOrNull { it.conv.threadId == threadId }?.title ?: ""

    // --- Akcje na rozmowach ---

    fun toggleSelect(id: Long) {
        selection = if (id in selection) selection - id else selection + id
    }

    fun pinSelected() {
        val ids = selection
        val allPinned = ids.all { settings.isPinned(it) }
        settings.togglePinned(ids, !allPinned)
        selection = emptySet()
        notify(if (allPinned) "Odpięto" else "Przypięto") { settings.togglePinned(ids, allPinned); reload() }
        reload()
    }

    fun archive(ids: Set<Long>, on: Boolean) {
        val before = ids.filter { settings.isArchived(it) }
        settings.toggleArchived(ids, on)
        selection = emptySet()
        notify(if (on) "Przeniesiono do archiwum" else "Przywrócono z archiwum") {
            settings.toggleArchived(ids, false); settings.toggleArchived(before, true); reload()
        }
        reload()
    }

    fun markRead(ids: Set<Long>, read: Boolean, undoable: Boolean = true) {
        selection = emptySet()
        if (undoable) notify(if (read) "Oznaczono jako przeczytane" else "Oznaczono jako nieprzeczytane") { markRead(ids, !read, undoable = false) }
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach {
                if (read) {
                    store.markRead(it); Notifier.cancel(ctx, it)
                } else {
                    store.markUnread(it)
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch(Dispatchers.IO) {
            store.markAllRead()
            conversations.forEach { Notifier.cancel(ctx, it.conv.threadId) }
        }
        toast = "Wszystko oznaczone jako przeczytane"
    }

    /** Komunikat z opcjonalnym „Cofnij”. */
    fun notify(message: String, onUndo: (() -> Unit)? = null) {
        toast = message
        undo = onUndo
    }

    /** Usuwanie rozmów: najpierw do kosza (z cofnięciem), z kosza – trwale. */
    fun trashThreads(ids: Set<Long>) {
        val inTrash = ids.filter { id -> conversations.any { it.conv.threadId == id && it.trashed } }.toSet()
        if (inTrash.isNotEmpty()) deleteThreads(inTrash)
        val move = ids - inTrash
        if (move.isEmpty()) return
        settings.toggleTrashed(move, true)
        selection = emptySet()
        move.forEach { Notifier.cancel(ctx, it) }
        notify(if (move.size == 1) "Przeniesiono do kosza" else "Przeniesiono do kosza: ${move.size}") { restoreThreads(move) }
        if (openThreadId?.let { it in move } == true) home()
        reload()
    }

    fun restoreThreads(ids: Set<Long>) {
        settings.toggleTrashed(ids, false)
        selection = emptySet()
        notify(if (ids.size == 1) "Przywrócono rozmowę" else "Przywrócono rozmowy: ${ids.size}") { settings.toggleTrashed(ids, true); reload() }
        reload()
    }

    fun emptyTrash() {
        val ids = conversations.filter { it.trashed }.map { it.conv.threadId }.toSet()
        if (ids.isNotEmpty()) deleteThreads(ids)
    }

    /** Rozmowy dłużej niż 30 dni w koszu znikają na dobre. */
    private fun purgeTrash() {
        val old = settings.trashedAt().filterValues { System.currentTimeMillis() - it > TRASH_DAYS * 86_400_000L }.keys
        if (old.isNotEmpty()) viewModelScope.launch(Dispatchers.IO) {
            old.forEach { store.deleteThread(it) }
            settings.forgetThreads(old)
        }
    }

    fun deleteThreads(ids: Set<Long>) {
        selection = emptySet()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ids.forEach { store.deleteThread(it); Notifier.cancel(ctx, it) }
                settings.forgetThreads(ids)
            }
            notify(if (ids.size == 1) "Usunięto rozmowę" else "Usunięto rozmowy: ${ids.size}")
            if (openThreadId?.let { it in ids } == true) home()
            reload()
        }
    }

    fun toggleMute(threadId: Long) {
        val on = !settings.isMuted(threadId)
        settings.toggleMuted(threadId, on)
        notify(if (on) "Powiadomienia wyciszone" else "Powiadomienia włączone") { settings.toggleMuted(threadId, !on); reload() }
        reload()
    }

    fun blockAddresses(addresses: List<String>, threadIds: Set<Long> = emptySet(), alsoDelete: Boolean = false) {
        val added = addresses.toSet() - settings.blockedNumbers
        val wasArchived = threadIds.filter { settings.isArchived(it) }
        settings.blockedNumbers = settings.blockedNumbers + addresses
        selection = emptySet()
        when {
            threadIds.isEmpty() -> Unit
            alsoDelete -> trashThreads(threadIds)
            else -> {
                settings.toggleArchived(threadIds, true)
                reload()
            }
        }
        notify(if (addresses.size == 1) "Zablokowano ${addresses[0]}" else "Zablokowano numery: ${addresses.size}") {
            settings.blockedNumbers = settings.blockedNumbers - added
            if (alsoDelete) settings.toggleTrashed(threadIds, false)
            else { settings.toggleArchived(threadIds, false); settings.toggleArchived(wasArchived, true) }
            reload()
        }
    }

    fun unblock(address: String) {
        settings.blockedNumbers = settings.blockedNumbers - address
    }

    // --- Wiadomości ---

    fun send(target: Screen.Thread, text: String, attachments: List<Attachment>, subId: Int) {
        if (target.addresses.isEmpty() || (text.isBlank() && attachments.isEmpty())) return
        busy = if (attachments.isNotEmpty()) "Przygotowywanie MMS…" else null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { MessageSender.send(ctx, target.addresses, text, attachments, subId) }
            }
            busy = null
            result.onSuccess { r ->
                target.threadId?.let { settings.saveDraft(it, "") }
                if (target.threadId == null || target.threadId != r.threadId) {
                    replace(Screen.Thread(r.threadId, target.addresses))
                } else {
                    replace(target.copy(text = "", attachments = emptyList(), highlight = null))
                    loadMessages(markRead = false)
                }
            }.onFailure { toast = it.message ?: "Nie udało się wysłać" }
        }
    }

    fun schedule(target: Screen.Thread, text: String, time: Long, subId: Int) {
        if (target.addresses.isEmpty() || text.isBlank()) return
        Scheduler.add(ctx, ScheduledMessage(System.currentTimeMillis(), target.addresses, text.trim(), time, subId))
        target.threadId?.let { settings.saveDraft(it, "") }
        replace(target.copy(text = "", attachments = emptyList()))
        toast = "Zaplanowano na " + formatFull(time).substringBeforeLast(':')
    }

    fun saveDraft(threadId: Long?, text: String) {
        if (threadId != null) settings.saveDraft(threadId, text)
    }

    fun retry(m: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (m.isMms) MmsSender.retry(ctx, m.id) else MessageSender.retrySms(ctx, m.uri, m.address, m.body, m.subId)
            }.onFailure { withContext(Dispatchers.Main) { toast = it.message ?: "Nie udało się wysłać" } }
        }
    }

    fun downloadMms(m: Message) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { MmsDownloader.retry(ctx, m.id) }
                .onFailure { withContext(Dispatchers.Main) { toast = "Nie można pobrać MMS: ${it.message}" } }
        }
    }

    /** Wiadomość znika od razu, a z telefonu jest usuwana dopiero po czasie na „Cofnij”. */
    fun deleteMessages(list: List<Message>) {
        val ids = list.map { it.id }.toSet()
        hiddenMessages = hiddenMessages + ids
        messages = messages.filter { it.id !in ids }
        val job = viewModelScope.launch {
            delay(UNDO_MS)
            withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) { list.forEach { store.deleteMessage(it) } }
            hiddenMessages = hiddenMessages - ids
            loadMessages(markRead = false)
        }
        notify(if (list.size == 1) "Usunięto wiadomość" else "Usunięto wiadomości: ${list.size}") {
            job.cancel()
            hiddenMessages = hiddenMessages - ids
            loadMessages(markRead = false)
        }
    }

    fun isFailedOrStuck(m: Message): Boolean =
        m.box == Box.FAILED || (m.box == Box.OUTBOX && System.currentTimeMillis() - m.date > 10 * 60_000L)

    // --- Statystyki ---

    fun loadStats() {
        viewModelScope.launch {
            stats = withContext(Dispatchers.IO) {
                val sms = store.allSms()
                val (mmsIn, mmsOut) = store.allMmsCount()
                val hours = IntArray(24)
                val cal = java.util.Calendar.getInstance()
                sms.forEach { cal.timeInMillis = it.date; hours[cal.get(java.util.Calendar.HOUR_OF_DAY)]++ }
                val top = sms.groupingBy { com.blacksingularities.opensms.data.normalizeNumber(it.address) }.eachCount()
                    .entries.sortedByDescending { it.value }.take(5)
                    .map { (num, n) -> Contacts.displayName(ctx, sms.first { m -> com.blacksingularities.opensms.data.normalizeNumber(m.address) == num }.address) to n }
                Stats(
                    smsIn = sms.count { it.incoming },
                    smsOut = sms.count { it.box == Box.SENT },
                    mmsIn = mmsIn,
                    mmsOut = mmsOut,
                    threads = conversations.size,
                    top = top,
                    busiestHour = hours.indices.maxByOrNull { hours[it] } ?: 0,
                    avgLength = if (sms.isEmpty()) 0 else sms.sumOf { it.body.length } / sms.size,
                )
            }
        }
    }

    // --- Aktualizacje ---

    fun checkUpdate(silent: Boolean) {
        if (update is UpdateState.Checking || update is UpdateState.Downloading || update is UpdateState.Installing) return
        update = UpdateState.Checking
        viewModelScope.launch {
            update = try {
                val info = UpdateManager.check()
                settings.lastUpdateCheck = System.currentTimeMillis()
                if (isNewer(info.version, UpdateManager.currentVersion)) {
                    updateBannerHidden = false
                    UpdateState.Available(info)
                } else {
                    if (!silent) toast = "Masz najnowszą wersję"
                    UpdateState.UpToDate
                }
            } catch (e: Exception) {
                if (!silent) toast = e.message ?: "Nie można sprawdzić aktualizacji"
                if (silent) UpdateState.Idle else UpdateState.Failed(e.message ?: "Błąd")
            }
        }
    }

    fun updateNow() {
        when (val u = update) {
            is UpdateState.Available -> {
                if (!UpdateManager.canInstall(ctx)) {
                    toast = "Zezwól na instalowanie aplikacji z OpenSMS i dotknij ponownie"
                    UpdateManager.openInstallPermissionSettings(ctx)
                    return
                }
                viewModelScope.launch {
                    try {
                        update = UpdateState.Downloading(u.info, 0f)
                        val file = UpdateManager.download(ctx, u.info) { p -> update = UpdateState.Downloading(u.info, p) }
                        update = UpdateState.Installing(u.info)
                        UpdateManager.install(ctx, file)
                        update = UpdateState.Ready(u.info, file)
                    } catch (e: Exception) {
                        update = UpdateState.Available(u.info)
                        toast = e.message ?: "Nie udało się pobrać aktualizacji"
                    }
                }
            }
            is UpdateState.Ready -> viewModelScope.launch {
                runCatching { UpdateManager.install(ctx, u.file) }.onFailure { toast = it.message }
            }
            else -> Unit
        }
    }

    val updatePending: Boolean
        get() = update is UpdateState.Available || update is UpdateState.Downloading || update is UpdateState.Ready || update is UpdateState.Installing

    companion object {
        const val PAGE = 150
        const val UNDO_MS = 5_000L
        const val TRASH_DAYS = 30
    }
}
