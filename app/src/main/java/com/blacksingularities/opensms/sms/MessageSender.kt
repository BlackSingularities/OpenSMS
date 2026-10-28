package com.blacksingularities.opensms.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsMessage
import com.blacksingularities.opensms.data.Attachment
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.data.Sims
import com.blacksingularities.opensms.mms.MmsSender
import java.text.Normalizer

/** Wynik wysyłki: id rozmowy, do której trafiła wiadomość. */
data class SendResult(val threadId: Long, val asMms: Boolean)

/**
 * Wysyłanie: decyduje, czy wiadomość idzie jako SMS czy MMS (załączniki, rozmowa grupowa,
 * bardzo długi tekst), zapisuje ją w bazie i śledzi status wysłania i doręczenia.
 */
object MessageSender {
    const val ACTION_SENT = "com.blacksingularities.opensms.SMS_SENT"
    const val ACTION_DELIVERED = "com.blacksingularities.opensms.SMS_DELIVERED"

    fun send(
        context: Context,
        recipients: List<String>,
        text: String,
        attachments: List<Attachment> = emptyList(),
        subId: Int = -1,
        subject: String? = null,
    ): SendResult {
        require(recipients.isNotEmpty()) { "Brak odbiorcy" }
        val settings = Settings.get(context)
        var body = text.trim()
        if (settings.signature.isNotBlank() && body.isNotEmpty()) body += "\n" + settings.signature.trim()
        if (settings.stripAccents && attachments.isEmpty()) body = stripAccents(body)
        val sub = if (subId >= 0) subId else settings.defaultSubId.takeIf { it >= 0 } ?: Sims.defaultSmsSubId()

        val segments = if (body.isEmpty()) 0 else SmsMessage.calculateLength(body, false)[0]
        val mms = attachments.isNotEmpty() ||
            !subject.isNullOrBlank() ||
            (recipients.size > 1 && settings.groupAsMms) ||
            (settings.longAsMms && segments > settings.longAsMmsSegments)

        if (mms) {
            val threadId = MmsSender.send(context, recipients, body, attachments, sub, subject)
            return SendResult(threadId, true)
        }
        var threadId = -1L
        for (r in recipients) threadId = sendSms(context, r, body, sub)
        if (recipients.size > 1) threadId = MessageStore(context).threadIdFor(recipients)
        return SendResult(threadId, false)
    }

    /** Wysyła jeden SMS (dzielony na części) i zapisuje go w skrzynce nadawczej. */
    fun sendSms(context: Context, address: String, body: String, subId: Int): Long {
        val settings = Settings.get(context)
        val store = MessageStore(context)
        val threadId = store.threadIdFor(listOf(address))
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.SUBSCRIPTION_ID, subId)
            put(Telephony.Sms.STATUS, if (settings.deliveryReports) Telephony.Sms.STATUS_PENDING else Telephony.Sms.STATUS_NONE)
        }
        val uri = context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values) ?: error("Nie można zapisać wiadomości")
        transmit(context, uri, address, body, subId, settings.deliveryReports)
        return threadId
    }

    /** Ponowna wysyłka nieudanego SMS (ten sam wpis w bazie). */
    fun retrySms(context: Context, uri: Uri, address: String, body: String, subId: Int) {
        MessageStore(context).setSmsType(uri, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
        transmit(context, uri, address, body, subId, Settings.get(context).deliveryReports)
    }

    private fun transmit(context: Context, uri: Uri, address: String, body: String, subId: Int, delivery: Boolean) {
        val manager = Sims.smsManager(context, subId)
        val parts = manager.divideMessage(body)
        val sent = ArrayList<PendingIntent>()
        val delivered = ArrayList<PendingIntent>()
        parts.indices.forEach { i ->
            sent += statusIntent(context, ACTION_SENT, uri, i, parts.size)
            if (delivery) delivered += statusIntent(context, ACTION_DELIVERED, uri, i, parts.size)
        }
        try {
            if (parts.size == 1) {
                manager.sendTextMessage(address, null, body, sent[0], delivered.firstOrNull())
            } else {
                manager.sendMultipartTextMessage(address, null, parts, sent, if (delivery) delivered else null)
            }
        } catch (e: Exception) {
            MessageStore(context).setSmsType(uri, Telephony.Sms.MESSAGE_TYPE_FAILED)
            throw e
        }
    }

    private fun statusIntent(context: Context, action: String, uri: Uri, part: Int, total: Int): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java)
            .setAction(action)
            .setData(uri)
            .putExtra(SmsStatusReceiver.EXTRA_PART, part)
            .putExtra(SmsStatusReceiver.EXTRA_PARTS, total)
        return PendingIntent.getBroadcast(context, part, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    /** „Zażółć” → „Zazolc”: SMS bez polskich znaków mieści 160 zamiast 70 znaków. */
    fun stripAccents(text: String): String {
        val replaced = text.replace('ł', 'l').replace('Ł', 'L')
        return Normalizer.normalize(replaced, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    }
}
