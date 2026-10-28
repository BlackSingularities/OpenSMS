package com.blacksingularities.opensms.mms

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.blacksingularities.opensms.data.MessageStore

/** Zapis MMS w bazie systemu: wiersz wiadomości, części i adresy. */
object MmsPersister {
    fun insert(
        context: Context,
        box: Int,
        threadId: Long,
        from: String?,
        to: List<String>,
        cc: List<String>,
        subject: String?,
        date: Long,
        messageId: String?,
        transactionId: String?,
        messageType: Int,
        parts: List<Pdu.Part>,
        read: Boolean,
        subId: Int,
        contentLocation: String? = null,
        size: Long = 0,
        expiry: Long = 0,
    ): Uri? {
        val cr = context.contentResolver
        val v = ContentValues().apply {
            put("thread_id", threadId)
            put("date", date)
            put("date_sent", date)
            put("msg_box", box)
            put("read", if (read) 1 else 0)
            put("seen", if (read) 1 else 0)
            put("m_type", messageType)
            put("v", 18)
            put("ct_t", Pdu.MULTIPART_RELATED)
            put("m_cls", "personal")
            put("pri", 129)
            put("rr", 129)
            put("d_rpt", 129)
            put("sub_id", subId)
            if (!subject.isNullOrBlank()) {
                put("sub", MessageStore.encodeMmsText(subject))
                put("sub_cs", 106)
            }
            messageId?.let { put("m_id", it) }
            transactionId?.let { put("tr_id", it) }
            contentLocation?.let { put("ct_l", it) }
            if (size > 0) put("m_size", size) else put("m_size", parts.sumOf { it.data.size }.toLong())
            if (expiry > 0) put("exp", expiry)
            // Pole „text_only” pomaga innym aplikacjom pokazać fragment bez czytania części.
            put("text_only", if (parts.all { it.contentType == "text/plain" || it.contentType == "application/smil" }) 1 else 0)
        }
        val uri = cr.insert(Telephony.Mms.CONTENT_URI, v) ?: return null
        val id = uri.lastPathSegment ?: return null

        parts.forEachIndexed { i, p ->
            val pv = ContentValues().apply {
                put("mid", id)
                put("seq", if (p.contentType == "application/smil") -1 else i)
                put("ct", p.contentType)
                p.name?.let { put("name", it) }
                p.contentId?.let { put("cid", "<$it>") }
                p.contentLocation?.let { put("cl", it) }
                if (p.contentType == "text/plain") {
                    put("chset", 106)
                    put("text", p.text)
                } else if (p.contentType == "application/smil") {
                    put("text", String(p.data, Charsets.UTF_8))
                }
            }
            val partUri = cr.insert(Uri.parse("content://mms/$id/part"), pv) ?: return@forEachIndexed
            if (p.contentType != "text/plain" && p.contentType != "application/smil") {
                runCatching { cr.openOutputStream(partUri)?.use { it.write(p.data) } }
            }
        }

        fun addr(address: String, type: Int) {
            val av = ContentValues().apply {
                put("msg_id", id)
                put("address", address)
                put("type", type)
                put("charset", 106)
            }
            runCatching { cr.insert(Uri.parse("content://mms/$id/addr"), av) }
        }
        addr(from ?: "insert-address-token", MessageStore.ADDR_FROM)
        to.forEach { addr(it, MessageStore.ADDR_TO) }
        cc.forEach { addr(it, MessageStore.ADDR_CC) }
        return uri
    }

    /** Części zapisanego MMS (do ponownej wysyłki i przekazania). */
    fun readParts(context: Context, mmsId: Long): List<Pdu.Part> {
        val cr = context.contentResolver
        val out = ArrayList<Pdu.Part>()
        cr.query(Uri.parse("content://mms/$mmsId/part"), arrayOf("_id", "ct", "name", "cid", "cl", "text"), null, null, "seq ASC")?.use { c ->
            while (c.moveToNext()) {
                val ct = c.getString(1) ?: continue
                val data = when (ct) {
                    "text/plain", "application/smil" -> (c.getString(5) ?: "").toByteArray(Charsets.UTF_8)
                    else -> runCatching { cr.openInputStream(Uri.parse("content://mms/part/${c.getLong(0)}"))?.use { it.readBytes() } }.getOrNull() ?: continue
                }
                out += Pdu.Part(
                    contentType = ct,
                    data = data,
                    name = c.getString(2),
                    contentId = c.getString(3)?.removePrefix("<")?.removeSuffix(">"),
                    contentLocation = c.getString(4),
                    charset = if (ct == "text/plain") 106 else 0,
                )
            }
        }
        // SMIL na początku – jak w oryginalnej wiadomości.
        return out.sortedBy { if (it.contentType == "application/smil") 0 else 1 }
    }
}
