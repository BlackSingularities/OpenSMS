package com.blacksingularities.opensms.notify

import com.blacksingularities.opensms.tr

import com.blacksingularities.opensms.schedule.ScheduledSendReceiver
import com.blacksingularities.opensms.schedule.Scheduler
import com.blacksingularities.opensms.data.ScheduledMessage
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.LocusIdCompat
import com.blacksingularities.opensms.MainActivity
import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.canReplyTo
import com.blacksingularities.opensms.data.Message
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.sms.MessageSender
import com.blacksingularities.opensms.sms.async
import com.blacksingularities.opensms.util.OneTimeCode

/**
 * Powiadomienia o wiadomościach: styl rozmowy (MessagingStyle), odpowiedź bez otwierania
 * aplikacji, „Przeczytane” i kopiowanie kodu jednorazowego jednym dotknięciem.
 */
object Notifier {
    const val CHANNEL_MESSAGES = "messages"
    const val CHANNEL_ALERTS = "alerts"
    const val CHANNEL_UPDATES = "updates"
    const val EXTRA_THREAD = "thread_id"

    /** Rozmowa otwarta na ekranie – nie pokazujemy dla niej powiadomień. */
    @Volatile
    var activeThread: Long = -1

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_MESSAGES, tr(R.string.wiadomosci), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = tr(R.string.nowe_sms_i_mms)
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_ALERTS, tr(R.string.bledy_i_zaplanowane), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = tr(R.string.nieudane_wysylanie_mms_do_pobrania)
                },
                NotificationChannel(CHANNEL_UPDATES, tr(R.string.aktualizacje), NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun openThreadIntent(context: Context, threadId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_THREAD, threadId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, threadId.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun showIncoming(context: Context, threadId: Long) {
        val s = Settings.get(context)
        if (!s.notifications || s.isMuted(threadId) || threadId == activeThread || !canNotify(context)) return
        val store = MessageStore(context)
        val unread = store.unreadIn(threadId)
        if (unread.isEmpty()) {
            cancel(context, threadId)
            return
        }
        val addresses = store.threadAddresses(threadId).ifEmpty { listOf(unread.last().address) }
        val title = Contacts.title(context, addresses)
        val group = addresses.size > 1
        val me = Person.Builder().setName(tr(R.string.ty)).setKey("me").build()
        val style = NotificationCompat.MessagingStyle(me)
        if (group) style.setConversationTitle(title).setGroupConversation(true)
        // Każdy nadawca ma swój awatar (zdjęcie albo inicjały) – Android pokazuje go zamiast ikony aplikacji.
        val people = HashMap<String, Person>()
        fun person(address: String): Person = people.getOrPut(address) {
            val face = NotificationAvatars.face(context, address)
            val contact = Contacts.lookup(context, address)
            Person.Builder()
                .setName(face.name)
                .setKey(address)
                .setIcon(runCatching { NotificationAvatars.icon(context, face) }.getOrNull())
                .apply {
                    // Powiązanie z kontaktem: tryb „Nie przeszkadzać” rozpoznaje wtedy ważne osoby.
                    val key = contact?.lookupKey
                    if (contact != null && key != null) {
                        setUri(ContactsContract.Contacts.getLookupUri(contact.contactId, key).toString())
                    }
                }
                .build()
        }
        val texts = unread.map { m -> previewText(s.notificationPreview, m) }
        unread.forEachIndexed { i, m -> style.addMessage(texts[i], m.date, person(m.address)) }
        val last = unread.last()
        val n = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF000000.toInt())
            .setStyle(style)
            .setContentTitle(title)
            .setContentText(texts.last())
            .setLargeIcon(NotificationAvatars.largeIcon(context, addresses))
            .setWhen(last.date)
            .setShowWhen(true)
            .setContentIntent(openThreadIntent(context, threadId))
            .setDeleteIntent(action(context, NotificationActionReceiver.ACTION_DISMISS, threadId))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setNumber(unread.size)
            .setVisibility(
                when {
                    !s.notificationPreview -> NotificationCompat.VISIBILITY_SECRET
                    s.lockScreenContent -> NotificationCompat.VISIBILITY_PUBLIC
                    else -> NotificationCompat.VISIBILITY_PRIVATE
                },
            )
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(0xFF000000.toInt())
                    .setWhen(last.date)
                    .setContentTitle("OpenSMS")
                    .setContentText(if (unread.size == 1) tr(R.string.nowa_wiadomosc) else tr(R.string.nowe_wiadomosci_1, unread.size))
                    .build(),
            )

        // Skrót rozmowy: powiadomienie trafia do sekcji „Rozmowy” i da się je oznaczyć jako priorytetowe.
        ConversationShortcuts.push(context, threadId, addresses)?.let { id ->
            n.setShortcutId(id).setLocusId(LocusIdCompat(id))
        }

        // Nadawcy alfanumeryczni nie przyjmują wiadomości – odpowiedź z powiadomienia byłaby ślepym zaułkiem.
        if (addresses.any { canReplyTo(it) }) {
            val reply = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY).setLabel(tr(R.string.odpowiedz)).build()
            val replyIntent = PendingIntent.getBroadcast(
                context,
                threadId.toInt(),
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_REPLY)
                    .putExtra(EXTRA_THREAD, threadId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            n.addAction(
                NotificationCompat.Action.Builder(R.drawable.ic_action_reply, tr(R.string.odpowiedz_2), replyIntent)
                    .addRemoteInput(reply)
                    .setAllowGeneratedReplies(true)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setShowsUserInterface(false)
                    .build(),
            )
        }
        n.addAction(
            NotificationCompat.Action.Builder(R.drawable.ic_action_read, tr(R.string.przeczytane), action(context, NotificationActionReceiver.ACTION_READ, threadId))
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build(),
        )
        if (s.copyCodeAction) {
            OneTimeCode.find(last.body)?.let { code ->
                val copy = Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_COPY)
                    .putExtra(EXTRA_THREAD, threadId)
                    .putExtra(NotificationActionReceiver.EXTRA_CODE, code)
                n.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_action_copy,
                        tr(R.string.kopiuj_1, code),
                        PendingIntent.getBroadcast(context, threadId.toInt() + 7, copy, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                    ).setShowsUserInterface(false).build(),
                )
            }
        }
        runCatching { NotificationManagerCompat.from(context).notify(threadId.toInt(), n.build()) }
    }

    fun cancel(context: Context, threadId: Long) {
        runCatching { NotificationManagerCompat.from(context).cancel(threadId.toInt()) }
    }

    fun showFailed(context: Context, uri: Uri, reason: String) {
        if (!Settings.get(context).notifyFailures || !canNotify(context)) return
        val threadId = runCatching {
            context.contentResolver.query(uri, arrayOf("thread_id"), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
        }.getOrNull() ?: return
        if (threadId == activeThread) return
        val addresses = MessageStore(context).threadAddresses(threadId)
        val title = Contacts.title(context, addresses)
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF000000.toInt())
            .setLargeIcon(NotificationAvatars.largeIcon(context, addresses))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentTitle(tr(R.string.nie_wyslano_do_1, title))
            .setContentText(tr(R.string.s_1_dotknij_aby_ponowic, reason))
            .setContentIntent(openThreadIntent(context, threadId))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(FAILED_BASE + threadId.toInt(), n) }
    }

    fun showMmsDownloadFailed(context: Context, threadId: Long) {
        if (!canNotify(context)) return
        val addresses = MessageStore(context).threadAddresses(threadId)
        val title = Contacts.title(context, addresses)
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF000000.toInt())
            .setLargeIcon(NotificationAvatars.largeIcon(context, addresses))
            .setContentTitle(tr(R.string.mms_od_1, title))
            .setContentText(tr(R.string.nie_udalo_sie_pobrac_wlacz))
            .setContentIntent(openThreadIntent(context, threadId))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(FAILED_BASE + threadId.toInt(), n) }
    }

    fun showScheduledSent(context: Context, threadId: Long, title: String) {
        if (!canNotify(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF000000.toInt())
            .setLargeIcon(NotificationAvatars.largeIcon(context, MessageStore(context).threadAddresses(threadId)))
            .setContentTitle(tr(R.string.wyslano_zaplanowana_wiadomosc))
            .setContentText(tr(R.string.do_1, title))
            .setContentIntent(openThreadIntent(context, threadId))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(SCHEDULED_BASE + threadId.toInt(), n) }
    }

    /** Pytanie o zaplanowaną wiadomość: „Wyślij” albo „Nie wysyłaj”; bez decyzji nic nie wychodzi. */
    fun showScheduledConfirm(context: Context, m: ScheduledMessage, title: String) {
        if (!canNotify(context)) return
        fun act(action: String, code: Int) = PendingIntent.getBroadcast(
            context,
            m.id.toInt() + code,
            Intent(context, ScheduledSendReceiver::class.java).setAction(action).putExtra("id", m.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, m.id.toInt() + 3, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val n = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF000000.toInt())
            .setLargeIcon(NotificationAvatars.largeIcon(context, m.addresses))
            .setContentTitle(tr(R.string.wyslac_zaplanowana_wiadomosc))
            .setContentText(tr(R.string.do_tresc_1, title, m.text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(tr(R.string.do_tresc_nl_1, title, m.text)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .apply { open?.let { setContentIntent(it) } }
            .addAction(0, tr(R.string.wyslij), act(Scheduler.ACTION_SEND, 1))
            .addAction(0, tr(R.string.nie_wysylaj), act(Scheduler.ACTION_SKIP, 2))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(confirmId(m.id), n) }
    }

    fun cancelScheduledConfirm(context: Context, id: Long) {
        runCatching { NotificationManagerCompat.from(context).cancel(confirmId(id)) }
    }

    private fun confirmId(id: Long) = CONFIRM_BASE + (id % 1_000_000).toInt()

    private fun action(context: Context, action: String, threadId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            threadId.toInt(),
            Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra(EXTRA_THREAD, threadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Treść jednej wiadomości w powiadomieniu; załączniki opisane słowem, gdy brak tekstu. */
    private fun previewText(preview: Boolean, m: Message): String = when {
        !preview -> tr(R.string.nowa_wiadomosc)
        m.pendingUrl != null -> tr(R.string.mms_do_pobrania)
        m.body.isNotBlank() -> m.body.trim()
        m.media.any { it.isImage } -> tr(R.string.zdjecie)
        m.media.any { it.isVideo } -> "🎬 " + tr(R.string.film)
        m.media.any { it.isAudio } -> "🎵 " + tr(R.string.nagranie)
        else -> tr(R.string.zalacznik)
    }

    private const val FAILED_BASE = 1_000_000
    private const val SCHEDULED_BASE = 2_000_000
    private const val CONFIRM_BASE = 3_000_000
}

/** Akcje z powiadomienia: odpowiedź, przeczytane, kopiowanie kodu. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(Notifier.EXTRA_THREAD, -1)
        if (threadId < 0) return
        when (intent.action) {
            ACTION_COPY -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: return
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(tr(R.string.kod), code))
                if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, tr(R.string.skopiowano_kod_1, code), Toast.LENGTH_SHORT).show()
                async {
                    MessageStore(context).markRead(threadId)
                    Notifier.cancel(context, threadId)
                }
            }
            ACTION_READ -> async {
                MessageStore(context).markRead(threadId)
                Notifier.cancel(context, threadId)
            }
            ACTION_DISMISS -> Unit
            ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_REPLY)?.toString()?.trim()
                if (text.isNullOrEmpty()) return
                async {
                    val store = MessageStore(context)
                    val addresses = store.threadAddresses(threadId)
                    if (addresses.isNotEmpty()) {
                        val sub = store.messages(threadId, 1).lastOrNull()?.subId ?: -1
                        runCatching { MessageSender.send(context, addresses, text, subId = sub) }
                    }
                    if (Settings.get(context).markReadOnReply) store.markRead(threadId)
                    Notifier.cancel(context, threadId)
                }
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.blacksingularities.opensms.REPLY"
        const val ACTION_READ = "com.blacksingularities.opensms.READ"
        const val ACTION_COPY = "com.blacksingularities.opensms.COPY_CODE"
        const val ACTION_DISMISS = "com.blacksingularities.opensms.DISMISS"
        const val KEY_REPLY = "reply"
        const val EXTRA_CODE = "code"
    }
}
