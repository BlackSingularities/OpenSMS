package com.blacksingularities.opensms.mms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import com.blacksingularities.opensms.data.BlockedLogEntry
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.data.Sims
import com.blacksingularities.opensms.data.normalizeNumber
import com.blacksingularities.opensms.notify.Notifier
import com.blacksingularities.opensms.sms.async
import com.blacksingularities.opensms.sms.blockReason
import java.io.File

/** Pobieranie MMS od operatora i zapis odebranej wiadomości. */
object MmsDownloader {
    const val ACTION_DOWNLOADED = "com.blacksingularities.opensms.MMS_DOWNLOADED"
    private const val EXTRA_ROW = "row"
    private const val EXTRA_SUB = "sub"
    private const val EXTRA_TID = "tid"

    /** Id wierszy pobieranych teraz (wskaźnik w rozmowie). */
    val downloading = mutableStateOf<Set<Long>>(emptySet())

    /** Powiadomienie WAP push: zapisuje „MMS do pobrania” i (domyślnie) od razu go pobiera. */
    fun onPush(context: Context, data: ByteArray, subId: Int) {
        val pdu = runCatching { Pdu.parse(data) }.getOrNull() ?: return
        if (pdu.messageType != Pdu.NOTIFICATION_IND) return
        val location = pdu.contentLocation ?: return
        val from = pdu.from ?: ""
        val cr = context.contentResolver
        // Operatorzy ponawiają powiadomienia – pomijamy już znane.
        val exists = runCatching {
            cr.query(Telephony.Mms.CONTENT_URI, arrayOf("_id"), "ct_l = ?", arrayOf(location), null)?.use { it.count > 0 }
        }.getOrNull() ?: false
        if (exists) return
        val reason = blockReason(context, from, pdu.subject ?: "")
        if (reason != null) {
            Settings.get(context).addBlockedLog(BlockedLogEntry(from, "MMS" + (pdu.subject?.let { ": $it" } ?: ""), System.currentTimeMillis(), reason))
            return
        }
        val threadId = MessageStore(context).threadIdFor(listOf(from.ifBlank { "unknown" }))
        val uri = MmsPersister.insert(
            context = context,
            box = Telephony.Mms.MESSAGE_BOX_INBOX,
            threadId = threadId,
            from = from,
            to = pdu.to,
            cc = emptyList(),
            subject = pdu.subject,
            date = System.currentTimeMillis() / 1000,
            messageId = null,
            transactionId = pdu.transactionId,
            messageType = MessageStore.M_NOTIFICATION_IND,
            parts = emptyList(),
            read = false,
            subId = subId,
            contentLocation = location,
            size = pdu.messageSize,
            expiry = pdu.expiry,
        ) ?: return
        val row = uri.lastPathSegment?.toLongOrNull() ?: return
        if (Settings.get(context).autoDownloadMms) {
            download(context, row, location, subId, pdu.transactionId)
        } else {
            Notifier.showIncoming(context, threadId)
        }
    }

    fun download(context: Context, row: Long, location: String, subId: Int, transactionId: String?) {
        downloading.value = downloading.value + row
        val dir = File(context.cacheDir, "mms").apply { mkdirs() }
        val file = File(dir, "dl-$row.pdu")
        file.delete()
        file.createNewFile()
        val contentUri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val intent = Intent(context, MmsStatusReceiver::class.java)
            .setAction(ACTION_DOWNLOADED)
            .setData(Uri.parse("content://mms/$row"))
            .putExtra(EXTRA_ROW, row)
            .putExtra(EXTRA_SUB, subId)
            .putExtra(EXTRA_TID, transactionId)
            .putExtra(MmsSender.EXTRA_FILE, file.absolutePath)
        val pending = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        try {
            Sims.smsManager(context, subId).downloadMultimediaMessage(context, location, contentUri, null, pending)
        } catch (e: Exception) {
            downloading.value = downloading.value - row
            file.delete()
            throw e
        }
    }

    /** Ponowne pobranie MMS wskazanego wiersza (przycisk „Pobierz” w rozmowie). */
    fun retry(context: Context, row: Long) {
        var location: String? = null
        var sub = -1
        var tid: String? = null
        context.contentResolver.query(Uri.parse("content://mms/$row"), arrayOf("ct_l", "sub_id", "tr_id"), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                location = c.getString(0)
                sub = c.getInt(1)
                tid = c.getString(2)
            }
        }
        download(context, row, location ?: return, sub, tid)
    }

    internal fun onDownloaded(context: Context, intent: Intent, resultCode: Int) {
        val row = intent.getLongExtra(EXTRA_ROW, -1)
        val subId = intent.getIntExtra(EXTRA_SUB, -1)
        val file = intent.getStringExtra(MmsSender.EXTRA_FILE)?.let(::File)
        downloading.value = downloading.value - row
        val cr = context.contentResolver
        val notifUri = Uri.parse("content://mms/$row")
        val data = file?.takeIf { it.exists() }?.readBytes()
        file?.delete()
        val pdu = if (resultCode == Activity.RESULT_OK && data != null && data.isNotEmpty()) runCatching { Pdu.parse(data) }.getOrNull() else null
        if (pdu == null || pdu.messageType != Pdu.RETRIEVE_CONF) {
            val threadId = runCatching {
                cr.query(notifUri, arrayOf("thread_id"), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
            }.getOrNull()
            if (threadId != null) Notifier.showMmsDownloadFailed(context, threadId)
            return
        }
        val from = pdu.from ?: ""
        val own = Sims.ownNumbers(context)
        val others = (pdu.to + pdu.cc).filter { a -> normalizeNumber(a) !in own && normalizeNumber(a) != normalizeNumber(from) }
        // Rozmowa 1:1, gdy wiadomość była tylko do nas; inaczej grupowa (bez własnego numeru).
        val participants = if (pdu.to.size + pdu.cc.size <= 1) listOf(from) else (listOf(from) + others).distinct()
        val store = MessageStore(context)
        val threadId = store.threadIdFor(participants.filter { it.isNotBlank() }.ifEmpty { listOf("unknown") })
        val saved = MmsPersister.insert(
            context = context,
            box = Telephony.Mms.MESSAGE_BOX_INBOX,
            threadId = threadId,
            from = from,
            to = pdu.to,
            cc = pdu.cc,
            subject = pdu.subject,
            date = System.currentTimeMillis() / 1000,
            messageId = pdu.messageId,
            transactionId = pdu.transactionId ?: intent.getStringExtra(EXTRA_TID),
            messageType = MessageStore.M_RETRIEVE_CONF,
            parts = pdu.parts,
            read = false,
            subId = subId,
        )
        if (saved != null) {
            runCatching {
                if (pdu.date > 0) cr.update(saved, ContentValues().apply { put("date_sent", pdu.date) }, null, null)
            }
            runCatching { cr.delete(notifUri, null, null) }
        }
        acknowledge(context, pdu.transactionId ?: intent.getStringExtra(EXTRA_TID), subId)
        Notifier.showIncoming(context, threadId)
    }

    /** Potwierdzenie dla centrum MMS – bez niego operator może ponawiać powiadomienie. */
    private fun acknowledge(context: Context, transactionId: String?, subId: Int) {
        if (transactionId.isNullOrBlank()) return
        runCatching {
            val dir = File(context.cacheDir, "mms").apply { mkdirs() }
            val file = File(dir, "ack-${System.currentTimeMillis()}.pdu")
            file.writeBytes(Pdu.encodeAcknowledgeInd(transactionId))
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
            Sims.smsManager(context, subId).sendMultimediaMessage(context, uri, null, null, null)
        }
    }

    /** Pliki tymczasowe starsze niż doba (np. po przerwanej wysyłce). */
    fun cleanup(context: Context) {
        val limit = System.currentTimeMillis() - 24 * 3600_000L
        File(context.cacheDir, "mms").listFiles()?.filter { it.lastModified() < limit }?.forEach { it.delete() }
    }
}

/** WAP push z powiadomieniem o MMS (tylko dla domyślnej aplikacji SMS). */
class MmsPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        val data = intent.getByteArrayExtra("data") ?: return
        val subId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", intent.getIntExtra("subscription", -1))
        async { runCatching { MmsDownloader.onPush(context, data, subId) } }
    }
}

/** Wynik wysłania lub pobrania MMS. */
class MmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = resultCode
        async {
            when (intent.action) {
                MmsSender.ACTION_MMS_SENT -> {
                    val uri = intent.data ?: return@async
                    intent.getStringExtra(MmsSender.EXTRA_FILE)?.let { File(it).delete() }
                    val conf = intent.getByteArrayExtra(SmsManager.EXTRA_MMS_DATA)?.let { runCatching { Pdu.parse(it) }.getOrNull() }
                    // Status odpowiedzi 0x80 = OK; brak potwierdzenia przy kodzie OK też traktujemy jako sukces.
                    val ok = code == Activity.RESULT_OK && (conf == null || conf.responseStatus == 0 || conf.responseStatus == 0x80)
                    if (ok) {
                        val v = ContentValues().apply {
                            put("msg_box", Telephony.Mms.MESSAGE_BOX_SENT)
                            conf?.messageId?.let { put("m_id", it) }
                        }
                        runCatching { context.contentResolver.update(uri, v, null, null) }
                        val s = Settings.get(context)
                        uri.lastPathSegment?.let { s.failedMms = s.failedMms - it }
                    } else {
                        MmsSender.markFailed(context, uri)
                        Notifier.showFailed(context, uri, "Nie udało się wysłać MMS (kod $code)")
                    }
                }
                MmsDownloader.ACTION_DOWNLOADED -> MmsDownloader.onDownloaded(context, intent, code)
            }
        }
    }
}
