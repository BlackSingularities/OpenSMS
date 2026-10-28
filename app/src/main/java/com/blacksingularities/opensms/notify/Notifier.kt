package com.blacksingularities.opensms.notify

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
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.blacksingularities.opensms.MainActivity
import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.data.Contacts
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
                NotificationChannel(CHANNEL_MESSAGES, "Wiadomości", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Nowe SMS i MMS"
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_ALERTS, "Błędy i zaplanowane", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Nieudane wysyłanie, MMS do pobrania, wysłane wiadomości zaplanowane"
                },
                NotificationChannel(CHANNEL_UPDATES, "Aktualizacje", NotificationManager.IMPORTANCE_LOW),
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
        val me = Person.Builder().setName("Ty").build()
        val style = NotificationCompat.MessagingStyle(me)
        if (group) style.setConversationTitle(title).setGroupConversation(true)
        val avatar = Contacts.lookup(context, addresses.first())?.photo?.let { loadIcon(context, it) }
        for (m in unread) {
            val info = Contacts.lookup(context, m.address)
            val person = Person.Builder()
                .setName(info?.name ?: m.address)
                .setKey(m.address)
                .apply { if (!group && avatar != null) setIcon(avatar) }
                .build()
            val text = when {
                !s.notificationPreview -> "Nowa wiadomość"
                m.pendingUrl != null -> "MMS do pobrania"
                m.body.isNotBlank() -> m.body
                m.media.any { it.isImage } -> "📷 Zdjęcie"
                m.media.any { it.isVideo } -> "🎬 Film"
                m.media.any { it.isAudio } -> "🎵 Nagranie"
                else -> "Załącznik"
            }
            style.addMessage(text, m.date, person)
        }
        val n = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF000000.toInt())
            .setStyle(style)
            .setContentTitle(title)
            .setContentText(if (s.notificationPreview) unread.last().body else "Nowa wiadomość")
            .setContentIntent(openThreadIntent(context, threadId))
            .setDeleteIntent(action(context, NotificationActionReceiver.ACTION_DISMISS, threadId))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setNumber(unread.size)
            .setVisibility(if (s.notificationPreview) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_MESSAGES)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("OpenSMS")
                    .setContentText(if (unread.size == 1) "Nowa wiadomość" else "Nowe wiadomości: ${unread.size}")
                    .build(),
            )

        val reply = RemoteInput.Builder(NotificationActionReceiver.KEY_REPLY).setLabel("Odpowiedz…").build()
        val replyIntent = PendingIntent.getBroadcast(
            context,
            threadId.toInt(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_REPLY)
                .putExtra(EXTRA_THREAD, threadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        n.addAction(
            NotificationCompat.Action.Builder(R.drawable.ic_notification, "Odpowiedz", replyIntent)
                .addRemoteInput(reply)
                .setAllowGeneratedReplies(true)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false)
                .build(),
        )
        n.addAction(
            NotificationCompat.Action.Builder(R.drawable.ic_notification, "Przeczytane", action(context, NotificationActionReceiver.ACTION_READ, threadId))
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false)
                .build(),
        )
        if (s.copyCodeAction) {
            OneTimeCode.find(unread.last().body)?.let { code ->
                val copy = Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_COPY)
                    .putExtra(EXTRA_THREAD, threadId)
                    .putExtra(NotificationActionReceiver.EXTRA_CODE, code)
                n.addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_notification,
                        "Kopiuj $code",
                        PendingIntent.getBroadcast(context, threadId.toInt() + 7, copy, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
                    ).build(),
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
        val title = Contacts.title(context, MessageStore(context).threadAddresses(threadId))
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nie wysłano do: $title")
            .setContentText("$reason. Dotknij, aby ponowić.")
            .setContentIntent(openThreadIntent(context, threadId))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(FAILED_BASE + threadId.toInt(), n) }
    }

    fun showMmsDownloadFailed(context: Context, threadId: Long) {
        if (!canNotify(context)) return
        val title = Contacts.title(context, MessageStore(context).threadAddresses(threadId))
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("MMS od: $title")
            .setContentText("Nie udało się pobrać. Włącz dane komórkowe i dotknij „Pobierz” w rozmowie.")
            .setContentIntent(openThreadIntent(context, threadId))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(FAILED_BASE + threadId.toInt(), n) }
    }

    fun showScheduledSent(context: Context, threadId: Long, title: String) {
        if (!canNotify(context)) return
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Wysłano zaplanowaną wiadomość")
            .setContentText("Do: $title")
            .setContentIntent(openThreadIntent(context, threadId))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(SCHEDULED_BASE + threadId.toInt(), n) }
    }

    private fun action(context: Context, action: String, threadId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            threadId.toInt(),
            Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra(EXTRA_THREAD, threadId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun loadIcon(context: Context, uri: Uri): IconCompat? = runCatching {
        val bmp: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
            d.setTargetSize(128, 128)
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        IconCompat.createWithAdaptiveBitmap(bmp)
    }.getOrNull()

    private const val FAILED_BASE = 1_000_000
    private const val SCHEDULED_BASE = 2_000_000
}

/** Akcje z powiadomienia: odpowiedź, przeczytane, kopiowanie kodu. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(Notifier.EXTRA_THREAD, -1)
        if (threadId < 0) return
        when (intent.action) {
            ACTION_COPY -> {
                val code = intent.getStringExtra(EXTRA_CODE) ?: return
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Kod", code))
                if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Skopiowano kod $code", Toast.LENGTH_SHORT).show()
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
