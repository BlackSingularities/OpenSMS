package com.blacksingularities.opensms.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import java.nio.charset.Charset

/**
 * Dostęp do bazy SMS/MMS systemu (Telephony). Jako domyślna aplikacja SMS OpenSMS sama
 * zapisuje wiadomości przychodzące i wysyłane – system robi to tylko za inne aplikacje.
 */
class MessageStore(context: Context) {
    private val app = context.applicationContext
    private val cr = app.contentResolver

    // --- Rozmowy ---

    fun conversations(): List<Conversation> {
        val addresses = canonicalAddresses()
        val unread = unreadCounts()
        val out = ArrayList<Conversation>()
        runCatching {
            cr.query(
                THREADS_URI,
                arrayOf("_id", "date", "message_count", "recipient_ids", "snippet", "snippet_cs", "has_attachment", "error"),
                null,
                null,
                "date DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val count = c.getInt(2)
                    if (count == 0) continue
                    val id = c.getLong(0)
                    val recipients = (c.getString(3) ?: "").split(' ').mapNotNull { it.toLongOrNull()?.let(addresses::get) }
                    val hasAttachment = c.getInt(6) == 1
                    val snippet = decodeMmsText(c.getString(4), c.getInt(5)).ifBlank { if (hasAttachment) "Załącznik MMS" else "" }
                    out += Conversation(
                        threadId = id,
                        addresses = recipients,
                        snippet = snippet,
                        date = c.getLong(1),
                        unread = unread[id] ?: 0,
                        hasAttachment = hasAttachment,
                        messageCount = count,
                        error = c.getInt(7) != 0,
                    )
                }
            }
        }
        return out
    }

    fun threadAddresses(threadId: Long): List<String> {
        val ids = runCatching {
            cr.query(THREADS_URI, arrayOf("_id", "recipient_ids"), "_id = ?", arrayOf(threadId.toString()), null)?.use { c ->
                if (c.moveToFirst()) c.getString(1) else null
            }
        }.getOrNull() ?: return emptyList()
        val map = canonicalAddresses()
        return ids.split(' ').mapNotNull { it.toLongOrNull()?.let(map::get) }
    }

    fun threadIdFor(addresses: Collection<String>): Long =
        Telephony.Threads.getOrCreateThreadId(app, addresses.toSet())

    private fun canonicalAddresses(): Map<Long, String> {
        val map = HashMap<Long, String>()
        runCatching {
            cr.query(Uri.parse("content://mms-sms/canonical-addresses"), arrayOf("_id", "address"), null, null, null)?.use { c ->
                while (c.moveToNext()) map[c.getLong(0)] = c.getString(1) ?: ""
            }
        }
        return map
    }

    private fun unreadCounts(): Map<Long, Int> {
        val map = HashMap<Long, Int>()
        fun count(uri: Uri, selection: String) = runCatching {
            cr.query(uri, arrayOf("thread_id"), selection, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val t = c.getLong(0)
                    map[t] = (map[t] ?: 0) + 1
                }
            }
        }
        count(Telephony.Sms.Inbox.CONTENT_URI, "read = 0")
        count(Telephony.Mms.Inbox.CONTENT_URI, "read = 0")
        return map
    }

    fun unreadTotal(): Int = unreadCounts().values.sum()

    // --- Wiadomości w rozmowie ---

    fun messages(threadId: Long, limit: Int): List<Message> {
        val sms = querySms("thread_id = ?", arrayOf(threadId.toString()), "date DESC LIMIT $limit")
        val mms = queryMms("thread_id = ?", arrayOf(threadId.toString()), "date DESC LIMIT $limit")
        return (sms + mms).sortedBy { it.date }.takeLast(limit)
    }

    fun message(isMms: Boolean, id: Long): Message? =
        if (isMms) queryMms("_id = ?", arrayOf(id.toString()), null).firstOrNull()
        else querySms("_id = ?", arrayOf(id.toString()), null).firstOrNull()

    /** Ostatnie nieprzeczytane wiadomości rozmowy (do powiadomienia). */
    fun unreadIn(threadId: Long, limit: Int = 8): List<Message> {
        val sms = querySms("thread_id = ? AND read = 0 AND type = 1", arrayOf(threadId.toString()), "date DESC LIMIT $limit")
        val mms = queryMms("thread_id = ? AND read = 0 AND msg_box = 1", arrayOf(threadId.toString()), "date DESC LIMIT $limit")
        return (sms + mms).sortedBy { it.date }.takeLast(limit)
    }

    /** Wyszukiwanie w treści SMS i w tekstach MMS. */
    fun search(query: String, limit: Int = 200): List<Message> {
        if (query.isBlank()) return emptyList()
        val like = "%" + query.replace("%", "\\%").replace("_", "\\_") + "%"
        val sms = querySms("body LIKE ? ESCAPE '\\'", arrayOf(like), "date DESC LIMIT $limit")
        val mmsIds = runCatching {
            cr.query(Uri.parse("content://mms/part"), arrayOf("mid"), "ct = 'text/plain' AND text LIKE ? ESCAPE '\\'", arrayOf(like), null)?.use { c ->
                buildList { while (c.moveToNext()) add(c.getLong(0)) }
            }
        }.getOrNull().orEmpty().distinct().take(limit)
        val mms = if (mmsIds.isEmpty()) emptyList() else queryMms("_id IN (${mmsIds.joinToString(",")})", null, null)
        return (sms + mms).sortedByDescending { it.date }.take(limit)
    }

    private fun querySms(selection: String?, args: Array<String>?, order: String?): List<Message> {
        val out = ArrayList<Message>()
        runCatching {
            cr.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf("_id", "thread_id", "address", "body", "date", "date_sent", "type", "read", "status", "sub_id"),
                selection,
                args,
                order,
            )?.use { c ->
                while (c.moveToNext()) {
                    val type = c.getInt(6)
                    val status = c.getInt(8)
                    out += Message(
                        id = c.getLong(0),
                        isMms = false,
                        threadId = c.getLong(1),
                        address = c.getString(2) ?: "",
                        body = c.getString(3) ?: "",
                        date = c.getLong(4),
                        dateSent = c.getLong(5),
                        box = smsBox(type),
                        read = c.getInt(7) == 1,
                        delivery = when {
                            type == Telephony.Sms.MESSAGE_TYPE_INBOX -> Delivery.NONE
                            status == Telephony.Sms.STATUS_COMPLETE -> Delivery.DELIVERED
                            status == Telephony.Sms.STATUS_PENDING -> Delivery.PENDING
                            status >= Telephony.Sms.STATUS_FAILED -> Delivery.FAILED
                            else -> Delivery.NONE
                        },
                        subId = c.getIntOrNull(9) ?: -1,
                    )
                }
            }
        }
        return out
    }

    private fun queryMms(selection: String?, args: Array<String>?, order: String?): List<Message> {
        val failed = Settings.get(app).failedMms
        val out = ArrayList<Message>()
        runCatching {
            cr.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf("_id", "thread_id", "date", "date_sent", "msg_box", "read", "sub", "sub_cs", "m_type", "ct_l", "sub_id", "m_size"),
                selection,
                args,
                order,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val box = c.getInt(4)
                    val type = c.getInt(8)
                    val parts = if (type == M_NOTIFICATION_IND) emptyList() else parts(id)
                    val incoming = box == Telephony.Mms.MESSAGE_BOX_INBOX
                    out += Message(
                        id = id,
                        isMms = true,
                        threadId = c.getLong(1),
                        address = mmsAddress(id, incoming),
                        body = parts.filter { it.isText }.joinToString("\n") { it.text.orEmpty() }.trim(),
                        date = c.getLong(2) * 1000,
                        dateSent = c.getLong(3) * 1000,
                        box = when {
                            id.toString() in failed -> Box.FAILED
                            incoming -> Box.INBOX
                            box == Telephony.Mms.MESSAGE_BOX_SENT -> Box.SENT
                            box == Telephony.Mms.MESSAGE_BOX_DRAFTS -> Box.DRAFT
                            else -> Box.OUTBOX
                        },
                        read = c.getInt(5) == 1,
                        delivery = Delivery.NONE,
                        subId = c.getIntOrNull(10) ?: -1,
                        parts = parts,
                        subject = decodeMmsText(c.getString(6), c.getInt(7)).takeIf { it.isNotBlank() },
                        pendingUrl = if (type == M_NOTIFICATION_IND) c.getString(9) else null,
                        sizeBytes = c.getLong(11),
                    )
                }
            }
        }
        return out
    }

    private fun parts(mmsId: Long): List<MmsPart> {
        val out = ArrayList<MmsPart>()
        runCatching {
            cr.query(Uri.parse("content://mms/$mmsId/part"), arrayOf("_id", "ct", "name", "cl", "text", "chset"), null, null, "seq ASC")?.use { c ->
                while (c.moveToNext()) {
                    val ct = (c.getString(1) ?: "application/octet-stream").lowercase()
                    out += MmsPart(
                        id = c.getLong(0),
                        contentType = ct,
                        name = c.getString(2) ?: c.getString(3),
                        text = if (ct == "text/plain") c.getString(4) else null,
                    )
                }
            }
        }
        return out
    }

    /** Nadawca MMS (typ 137 „from”) albo pierwszy odbiorca (151 „to”). */
    private fun mmsAddress(mmsId: Long, incoming: Boolean): String = runCatching {
        cr.query(Uri.parse("content://mms/$mmsId/addr"), arrayOf("address", "type"), null, null, null)?.use { c ->
            var from: String? = null
            var to: String? = null
            while (c.moveToNext()) {
                val a = c.getString(0) ?: continue
                if (a == "insert-address-token") continue
                when (c.getInt(1)) {
                    ADDR_FROM -> from = a
                    ADDR_TO -> if (to == null) to = a
                }
            }
            if (incoming) from ?: to else to ?: from
        }
    }.getOrNull() ?: ""

    // --- Zmiany ---

    fun markRead(threadId: Long) {
        val v = ContentValues().apply {
            put("read", 1)
            put("seen", 1)
        }
        runCatching { cr.update(Telephony.Sms.CONTENT_URI, v, "thread_id = ? AND (read = 0 OR seen = 0)", arrayOf(threadId.toString())) }
        runCatching { cr.update(Telephony.Mms.CONTENT_URI, v, "thread_id = ? AND (read = 0 OR seen = 0)", arrayOf(threadId.toString())) }
    }

    fun markUnread(threadId: Long) {
        val last = messages(threadId, 1).lastOrNull { it.incoming } ?: return
        runCatching { cr.update(last.uri, ContentValues().apply { put("read", 0) }, null, null) }
    }

    fun markAllRead() {
        val v = ContentValues().apply {
            put("read", 1)
            put("seen", 1)
        }
        runCatching { cr.update(Telephony.Sms.Inbox.CONTENT_URI, v, "read = 0", null) }
        runCatching { cr.update(Telephony.Mms.Inbox.CONTENT_URI, v, "read = 0", null) }
    }

    fun deleteThread(threadId: Long) {
        runCatching { cr.delete(Uri.withAppendedPath(Telephony.Threads.CONTENT_URI, threadId.toString()), null, null) }
    }

    fun deleteMessage(m: Message) {
        runCatching { cr.delete(m.uri, null, null) }
    }

    fun setSmsType(uri: Uri, type: Int) {
        runCatching { cr.update(uri, ContentValues().apply { put(Telephony.Sms.TYPE, type) }, null, null) }
    }

    fun insertIncomingSms(address: String, body: String, dateSent: Long, subId: Int, read: Boolean = false): Uri? {
        val v = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.DATE_SENT, dateSent)
            put(Telephony.Sms.READ, if (read) 1 else 0)
            put(Telephony.Sms.SEEN, if (read) 1 else 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            put(Telephony.Sms.SUBSCRIPTION_ID, subId)
        }
        return runCatching { cr.insert(Telephony.Sms.Inbox.CONTENT_URI, v) }.getOrNull()
    }

    /** Wiadomość z kopii zapasowej – dowolny typ i data. */
    fun insertSmsRaw(address: String, body: String, date: Long, dateSent: Long, type: Int, read: Boolean): Boolean {
        val v = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, date)
            put(Telephony.Sms.DATE_SENT, dateSent)
            put(Telephony.Sms.READ, if (read) 1 else 0)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.TYPE, type)
        }
        return runCatching { cr.insert(Telephony.Sms.CONTENT_URI, v) != null }.getOrDefault(false)
    }

    fun smsExists(address: String, body: String, date: Long): Boolean = runCatching {
        cr.query(Telephony.Sms.CONTENT_URI, arrayOf("_id"), "address = ? AND body = ? AND date = ?", arrayOf(address, body, date.toString()), null)?.use { it.count > 0 }
    }.getOrNull() ?: false

    /** Wszystkie SMS (do kopii zapasowej i statystyk), od najnowszych. */
    fun allSms(): List<Message> = querySms(null, null, "date DESC")

    fun allMmsCount(): Pair<Int, Int> {
        var inbox = 0
        var sent = 0
        runCatching {
            cr.query(Telephony.Mms.CONTENT_URI, arrayOf("msg_box"), null, null, null)?.use { c ->
                while (c.moveToNext()) if (c.getInt(0) == Telephony.Mms.MESSAGE_BOX_INBOX) inbox++ else sent++
            }
        }
        return inbox to sent
    }

    companion object {
        val THREADS_URI: Uri = Uri.parse("content://mms-sms/conversations?simple=true")
        const val ADDR_FROM = 137
        const val ADDR_TO = 151
        const val ADDR_CC = 130
        const val M_SEND_REQ = 128
        const val M_NOTIFICATION_IND = 130
        const val M_RETRIEVE_CONF = 132

        fun smsBox(type: Int): Box = when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> Box.INBOX
            Telephony.Sms.MESSAGE_TYPE_SENT -> Box.SENT
            Telephony.Sms.MESSAGE_TYPE_FAILED -> Box.FAILED
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> Box.DRAFT
            else -> Box.OUTBOX
        }

        /**
         * Teksty MMS (temat, fragment) są w bazie zapisane bajt po bajcie jako ISO-8859-1,
         * a właściwe kodowanie jest w osobnej kolumnie (106 = UTF-8).
         */
        fun decodeMmsText(raw: String?, charset: Int): String {
            if (raw.isNullOrEmpty()) return ""
            if (charset == 0) return raw
            val cs = mibCharset(charset) ?: return raw
            return runCatching { String(raw.toByteArray(Charsets.ISO_8859_1), cs) }.getOrDefault(raw)
        }

        fun encodeMmsText(text: String): String = String(text.toByteArray(Charsets.UTF_8), Charsets.ISO_8859_1)

        fun mibCharset(mib: Int): Charset? = com.blacksingularities.opensms.mms.mibCharset(mib)
    }
}

private fun Cursor.getIntOrNull(i: Int): Int? = if (i < 0 || isNull(i)) null else getInt(i)
