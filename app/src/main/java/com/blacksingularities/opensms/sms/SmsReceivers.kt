package com.blacksingularities.opensms.sms

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import com.blacksingularities.opensms.data.BlockedLogEntry
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Zakres dla krótkich zadań odbiorników (zapis do bazy, powiadomienie). */
internal val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Uruchamia [block] w tle, utrzymując odbiornik przy życiu do końca pracy. */
internal fun BroadcastReceiver.async(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Filtr: zwraca powód blokady albo null. */
internal fun blockReason(context: Context, address: String, body: String): String? {
    val s = Settings.get(context)
    if (s.isBlockedNumber(address)) return "Zablokowany numer"
    s.blockedKeywordIn(body)?.let { return "Słowo kluczowe: $it" }
    if (s.blockUnknown && !Contacts.isKnown(context, address)) return "Nieznany nadawca"
    val systemBlocked = runCatching {
        android.provider.BlockedNumberContract.isBlocked(context, address)
    }.getOrDefault(false)
    if (systemBlocked) return "Zablokowany w systemie"
    return null
}

/** Odbiór SMS – tylko domyślna aplikacja SMS dostaje SMS_DELIVER i sama zapisuje wiadomość. */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull()?.filterNotNull().orEmpty()
        if (parts.isEmpty()) return
        val subId = intent.getIntExtra("android.telephony.extra.SUBSCRIPTION_INDEX", intent.getIntExtra("subscription", -1))
        async {
            // Jedno doręczenie może zawierać części wiadomości od różnych nadawców – grupujemy.
            parts.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "" }.forEach { (address, list) ->
                val body = list.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
                val sent = list.first().timestampMillis
                val reason = blockReason(context, address, body)
                if (reason != null) {
                    Settings.get(context).addBlockedLog(BlockedLogEntry(address, body, System.currentTimeMillis(), reason))
                    return@forEach
                }
                // Wiadomość klasy 0 („flash”) – zapisujemy jak zwykłą, ale powiadomienie też się pojawi.
                val uri = MessageStore(context).insertIncomingSms(address, body, sent, subId) ?: return@forEach
                val threadId = runCatching {
                    context.contentResolver.query(uri, arrayOf("thread_id"), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
                }.getOrNull() ?: return@forEach
                Notifier.showIncoming(context, threadId)
            }
        }
    }
}

/** Wynik wysłania i raport doręczenia dla każdej części SMS. */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uri = intent.data ?: return
        val part = intent.getIntExtra(EXTRA_PART, 0)
        val total = intent.getIntExtra(EXTRA_PARTS, 1)
        val code = resultCode
        async {
            val store = MessageStore(context)
            when (intent.action) {
                MessageSender.ACTION_SENT -> {
                    if (code == Activity.RESULT_OK) {
                        if (part == total - 1 && currentType(context, uri) != Telephony.Sms.MESSAGE_TYPE_FAILED) {
                            store.setSmsType(uri, Telephony.Sms.MESSAGE_TYPE_SENT)
                        }
                    } else {
                        store.setSmsType(uri, Telephony.Sms.MESSAGE_TYPE_FAILED)
                        if (part == 0 || total == 1) Notifier.showFailed(context, uri, errorText(code))
                    }
                }
                MessageSender.ACTION_DELIVERED -> {
                    val pdu = intent.getByteArrayExtra("pdu") ?: return@async
                    val format = intent.getStringExtra("format")
                    val status = runCatching { SmsMessage.createFromPdu(pdu, format).status }.getOrDefault(0)
                    val value = when {
                        status == Telephony.Sms.STATUS_NONE -> return@async
                        status >= Telephony.Sms.STATUS_FAILED -> Telephony.Sms.STATUS_FAILED
                        status >= Telephony.Sms.STATUS_PENDING -> Telephony.Sms.STATUS_PENDING
                        else -> Telephony.Sms.STATUS_COMPLETE
                    }
                    // Wieloczęściowa: doręczona, gdy dotrze ostatnia część.
                    if (value == Telephony.Sms.STATUS_COMPLETE && part != total - 1) return@async
                    runCatching { context.contentResolver.update(uri, ContentValues().apply { put(Telephony.Sms.STATUS, value) }, null, null) }
                }
            }
        }
    }

    private fun currentType(context: Context, uri: android.net.Uri): Int? = runCatching {
        context.contentResolver.query(uri, arrayOf(Telephony.Sms.TYPE), null, null, null)?.use { c -> if (c.moveToFirst()) c.getInt(0) else null }
    }.getOrNull()

    companion object {
        const val EXTRA_PART = "part"
        const val EXTRA_PARTS = "parts"

        fun errorText(code: Int): String = when (code) {
            SmsManager.RESULT_ERROR_NO_SERVICE -> "Brak zasięgu"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "Tryb samolotowy lub wyłączone radio"
            SmsManager.RESULT_ERROR_NULL_PDU -> "Błąd wiadomości"
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Przekroczono limit wysyłania"
            SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED, SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> "Numer skrócony zablokowany"
            SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "Numer spoza listy wybierania ustalonego"
            else -> "Błąd wysyłania (kod $code)"
        }
    }
}

/** Szybka odpowiedź „wyślij SMS” z ekranu połączenia przychodzącego. */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val recipients = intent?.data?.schemeSpecificPart?.substringBefore('?')?.split(',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        if (!text.isNullOrBlank() && recipients.isNotEmpty()) {
            receiverScope.launch {
                runCatching { MessageSender.send(applicationContext, recipients, text) }
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }
}
