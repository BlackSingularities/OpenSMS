package com.blacksingularities.opensms.mms

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 * Minimalny koder i dekoder PDU MMS (OMA MMS Encapsulation 1.2, kodowanie WSP).
 * Obsługuje to, czego potrzebuje aplikacja: m-send-req, m-acknowledge-ind (wysyłanie)
 * oraz m-notification-ind, m-retrieve-conf i m-send-conf (odbiór).
 */
object Pdu {
    // Typy wiadomości (X-Mms-Message-Type).
    const val SEND_REQ = 0x80
    const val SEND_CONF = 0x81
    const val NOTIFICATION_IND = 0x82
    const val NOTIFYRESP_IND = 0x83
    const val RETRIEVE_CONF = 0x84
    const val ACKNOWLEDGE_IND = 0x85
    const val DELIVERY_IND = 0x86

    // Pola nagłówka (wartość z ustawionym najwyższym bitem).
    private const val H_BCC = 0x81
    private const val H_CC = 0x82
    private const val H_CONTENT_LOCATION = 0x83
    private const val H_CONTENT_TYPE = 0x84
    private const val H_DATE = 0x85
    private const val H_DELIVERY_REPORT = 0x86
    private const val H_EXPIRY = 0x88
    private const val H_FROM = 0x89
    private const val H_MESSAGE_CLASS = 0x8A
    private const val H_MESSAGE_ID = 0x8B
    private const val H_MESSAGE_TYPE = 0x8C
    private const val H_MMS_VERSION = 0x8D
    private const val H_MESSAGE_SIZE = 0x8E
    private const val H_PRIORITY = 0x8F
    private const val H_READ_REPORT = 0x90
    private const val H_REPORT_ALLOWED = 0x91
    private const val H_RESPONSE_STATUS = 0x92
    private const val H_STATUS = 0x95
    private const val H_SUBJECT = 0x96
    private const val H_TO = 0x97
    private const val H_TRANSACTION_ID = 0x98

    // Nagłówki części multipart.
    private const val P_CONTENT_LOCATION = 0x8E
    private const val P_CONTENT_ID = 0xC0

    // Parametry typu treści.
    private const val PARAM_CHARSET = 0x81
    private const val PARAM_NAME_OLD = 0x85
    private const val PARAM_FILENAME_OLD = 0x86
    private const val PARAM_TYPE = 0x89
    private const val PARAM_START = 0x8A
    private const val PARAM_NAME = 0x97
    private const val PARAM_FILENAME = 0x98

    private const val UTF8 = 106
    const val MULTIPART_RELATED = "application/vnd.wap.multipart.related"
    const val MULTIPART_MIXED = "application/vnd.wap.multipart.mixed"

    /** Typy treści o stałych numerach (WSP, tabela 40) – tylko te spotykane w MMS. */
    private val WELL_KNOWN = mapOf(
        0x00 to "*/*", 0x01 to "text/*", 0x02 to "text/html", 0x03 to "text/plain",
        0x06 to "text/x-vcalendar", 0x07 to "text/x-vcard", 0x08 to "text/vnd.wap.wml",
        0x1C to "image/*", 0x1D to "image/gif", 0x1E to "image/jpeg", 0x1F to "image/tiff",
        0x20 to "image/png", 0x21 to "image/vnd.wap.wbmp",
        0x22 to "application/vnd.wap.multipart.*", 0x23 to MULTIPART_MIXED,
        0x27 to "application/xml", 0x28 to "text/xml",
        0x33 to MULTIPART_RELATED, 0x3E to "application/vnd.wap.mms-message",
    )

    class Part(
        val contentType: String,
        val data: ByteArray,
        val name: String? = null,
        val contentId: String? = null,
        val contentLocation: String? = null,
        val charset: Int = 0,
    ) {
        val text: String
            get() = String(data, mibCharset(if (charset == 0) UTF8 else charset) ?: Charsets.UTF_8)
    }

    class Parsed(
        val messageType: Int,
        val transactionId: String?,
        val from: String?,
        val to: List<String>,
        val cc: List<String>,
        val subject: String?,
        val date: Long,
        val messageId: String?,
        val contentLocation: String?,
        val messageSize: Long,
        val expiry: Long,
        val responseStatus: Int,
        val contentType: String?,
        val parts: List<Part>,
    )

    // ---------------------------------------------------------------- Kodowanie

    /** Żądanie wysłania (m-send-req). [to] – numery albo adresy e-mail. */
    fun encodeSendReq(
        transactionId: String,
        to: List<String>,
        subject: String?,
        parts: List<Part>,
        deliveryReport: Boolean,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(H_MESSAGE_TYPE); out.write(SEND_REQ)
        out.write(H_TRANSACTION_ID); writeText(out, transactionId)
        out.write(H_MMS_VERSION); out.write(0x92) // 1.2
        out.write(H_DATE); writeLong(out, System.currentTimeMillis() / 1000)
        // From: „wstaw adres” – uzupełnia sieć.
        out.write(H_FROM); out.write(1); out.write(0x81)
        for (a in to) {
            out.write(H_TO); writeEncodedString(out, recipientAddress(a))
        }
        if (!subject.isNullOrBlank()) {
            out.write(H_SUBJECT); writeEncodedString(out, subject)
        }
        out.write(H_MESSAGE_CLASS); out.write(0x80) // personal
        out.write(H_PRIORITY); out.write(0x81) // normal
        out.write(H_DELIVERY_REPORT); out.write(if (deliveryReport) 0x80 else 0x81)
        out.write(H_READ_REPORT); out.write(0x81)
        out.write(H_EXPIRY)
        val exp = ByteArrayOutputStream().apply { write(0x81); writeLong(this, 7L * 24 * 3600) } // czas względny: 7 dni
        writeValueLength(out, exp.size()); exp.writeTo(out)

        // Content-Type: multipart/related; type="application/smil"; start="<smil>"
        out.write(H_CONTENT_TYPE)
        val smil = parts.firstOrNull { it.contentType == "application/smil" }
        val ct = ByteArrayOutputStream()
        ct.write(0xB3)
        if (smil != null) {
            ct.write(PARAM_START); writeText(ct, smil.contentId?.let { "<$it>" } ?: "<smil>")
            ct.write(PARAM_TYPE); writeText(ct, "application/smil")
        }
        writeValueLength(out, ct.size()); ct.writeTo(out)

        writeBody(out, parts)
        return out.toByteArray()
    }

    /** Potwierdzenie odbioru dla centrum MMS (m-acknowledge-ind). */
    fun encodeAcknowledgeInd(transactionId: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(H_MESSAGE_TYPE); out.write(ACKNOWLEDGE_IND)
        out.write(H_TRANSACTION_ID); writeText(out, transactionId)
        out.write(H_MMS_VERSION); out.write(0x92)
        out.write(H_REPORT_ALLOWED); out.write(0x81)
        return out.toByteArray()
    }

    private fun recipientAddress(a: String): String {
        if (a.contains('@')) return a
        val n = a.filter { it.isDigit() || it == '+' }
        return "$n/TYPE=PLMN"
    }

    private fun writeBody(out: ByteArrayOutputStream, parts: List<Part>) {
        writeUintvar(out, parts.size.toLong())
        for (p in parts) {
            val headers = ByteArrayOutputStream()
            // Typ treści części (z parametrami charset i name).
            val ctv = ByteArrayOutputStream()
            val code = WELL_KNOWN.entries.firstOrNull { it.value == p.contentType }?.key
            if (code != null) ctv.write(0x80 or code) else writeText(ctv, p.contentType)
            if (p.contentType.startsWith("text/") || p.charset != 0) {
                ctv.write(PARAM_CHARSET); writeShortOrLong(ctv, if (p.charset != 0) p.charset.toLong() else UTF8.toLong())
            }
            p.name?.let { ctv.write(PARAM_NAME_OLD); writeText(ctv, it) }
            writeValueLength(headers, ctv.size()); ctv.writeTo(headers)
            p.contentLocation?.let { headers.write(P_CONTENT_LOCATION); writeText(headers, it) }
            p.contentId?.let {
                headers.write(P_CONTENT_ID)
                headers.write(0x22) // ciąg w cudzysłowie
                headers.write("<$it>".toByteArray(Charsets.US_ASCII)); headers.write(0)
            }
            writeUintvar(out, headers.size().toLong())
            writeUintvar(out, p.data.size.toLong())
            headers.writeTo(out)
            out.write(p.data)
        }
    }

    private fun writeText(out: ByteArrayOutputStream, s: String) {
        val b = s.toByteArray(Charsets.UTF_8)
        if (b.isNotEmpty() && (b[0].toInt() and 0xFF) >= 0x80) out.write(0x7F) // znacznik cytatu
        out.write(b); out.write(0)
    }

    /** Ciąg z kodowaniem: długość, charset UTF-8, tekst. Czysty ASCII – zwykły tekst. */
    private fun writeEncodedString(out: ByteArrayOutputStream, s: String) {
        if (s.all { it.code in 0x20..0x7E }) {
            writeText(out, s); return
        }
        val b = s.toByteArray(Charsets.UTF_8)
        writeValueLength(out, 1 + b.size + 1)
        out.write(0x80 or UTF8)
        out.write(b); out.write(0)
    }

    private fun writeValueLength(out: ByteArrayOutputStream, len: Int) {
        if (len <= 30) out.write(len) else {
            out.write(31); writeUintvar(out, len.toLong())
        }
    }

    private fun writeUintvar(out: ByteArrayOutputStream, value: Long) {
        var v = value
        val tmp = ArrayList<Int>()
        tmp += (v and 0x7F).toInt()
        v = v ushr 7
        while (v > 0) {
            tmp += ((v and 0x7F) or 0x80).toInt()
            v = v ushr 7
        }
        for (i in tmp.indices.reversed()) out.write(tmp[i])
    }

    private fun writeLong(out: ByteArrayOutputStream, value: Long) {
        val bytes = ArrayList<Int>()
        var v = value
        do {
            bytes += (v and 0xFF).toInt(); v = v ushr 8
        } while (v > 0)
        out.write(bytes.size)
        for (i in bytes.indices.reversed()) out.write(bytes[i])
    }

    private fun writeShortOrLong(out: ByteArrayOutputStream, value: Long) {
        if (value < 0x80) out.write(0x80 or value.toInt()) else writeLong(out, value)
    }

    // ---------------------------------------------------------------- Dekodowanie

    fun parse(data: ByteArray): Parsed {
        val r = Reader(data)
        var type = -1
        var tid: String? = null
        var from: String? = null
        val to = ArrayList<String>()
        val cc = ArrayList<String>()
        var subject: String? = null
        var date = 0L
        var mid: String? = null
        var location: String? = null
        var size = 0L
        var expiry = 0L
        var status = 0
        var contentType: String? = null
        var parts: List<Part> = emptyList()

        while (r.hasMore()) {
            val field = r.u8()
            if (field < 0x80) {
                // Nagłówek tekstowy (rozszerzenie) – nazwa i wartość jako tekst.
                r.pos--
                r.text(); r.text()
                continue
            }
            when (field) {
                H_MESSAGE_TYPE -> type = r.u8()
                H_TRANSACTION_ID -> tid = r.text()
                H_FROM -> {
                    val len = r.valueLength()
                    val end = r.pos + len
                    val token = r.u8()
                    if (token == 0x80) from = cleanAddress(r.encodedString())
                    r.pos = end
                }
                H_TO -> to += cleanAddress(r.encodedString())
                H_CC -> cc += cleanAddress(r.encodedString())
                H_BCC -> r.encodedString()
                H_SUBJECT -> subject = r.encodedString()
                H_DATE -> date = r.longInteger()
                H_MESSAGE_ID -> mid = r.text()
                H_CONTENT_LOCATION -> location = r.text()
                H_MESSAGE_SIZE -> size = r.longInteger()
                H_RESPONSE_STATUS, H_STATUS -> status = r.u8()
                H_EXPIRY -> {
                    val len = r.valueLength()
                    val end = r.pos + len
                    val token = r.u8()
                    val v = r.longInteger()
                    expiry = if (token == 0x81) System.currentTimeMillis() / 1000 + v else v
                    r.pos = end
                }
                H_CONTENT_TYPE -> {
                    contentType = r.contentType().first
                    // Treść zaczyna się zaraz po typie treści – to zawsze ostatni nagłówek.
                    if (contentType.startsWith("application/vnd.wap.multipart")) {
                        parts = r.multipart()
                    } else if (r.hasMore()) {
                        parts = listOf(Part(contentType, r.rest()))
                    }
                    break
                }
                else -> r.skipValue()
            }
        }
        return Parsed(type, tid, from, to, cc, subject, date, mid, location, size, expiry, status, contentType, parts)
    }

    /** „+48600100200/TYPE=PLMN” → „+48600100200”. */
    private fun cleanAddress(a: String): String = a.substringBefore("/TYPE=").trim()

    private class Reader(val b: ByteArray) {
        var pos = 0

        fun hasMore() = pos < b.size
        fun u8(): Int = if (pos < b.size) b[pos++].toInt() and 0xFF else throw IllegalStateException("Koniec danych PDU")

        fun rest(): ByteArray = b.copyOfRange(pos, b.size).also { pos = b.size }

        fun text(): String {
            if (pos < b.size && (b[pos].toInt() and 0xFF) == 0x7F) pos++
            if (pos < b.size && (b[pos].toInt() and 0xFF) == 0x22) pos++
            val start = pos
            while (pos < b.size && b[pos].toInt() != 0) pos++
            val s = String(b, start, pos - start, Charsets.UTF_8)
            if (pos < b.size) pos++
            return s
        }

        fun uintvar(): Long {
            var v = 0L
            while (true) {
                val x = u8()
                v = (v shl 7) or (x and 0x7F).toLong()
                if (x and 0x80 == 0) return v
            }
        }

        fun valueLength(): Int {
            val x = u8()
            return when {
                x < 31 -> x
                x == 31 -> uintvar().toInt()
                else -> throw IllegalStateException("Błędna długość wartości")
            }
        }

        fun longInteger(): Long {
            val x = u8()
            if (x >= 0x80) return (x and 0x7F).toLong()
            var v = 0L
            repeat(x) { v = (v shl 8) or u8().toLong() }
            return v
        }

        fun integer(): Long {
            val x = b[pos].toInt() and 0xFF
            return if (x >= 0x80) { pos++; (x and 0x7F).toLong() } else longInteger()
        }

        /** Ciąg tekstowy z opcjonalnym kodowaniem znaków. */
        fun encodedString(): String {
            val x = b[pos].toInt() and 0xFF
            if (x >= 32) return text()
            val len = valueLength()
            val end = pos + len
            val charset = integer().toInt()
            var start = pos
            if (start < end && (b[start].toInt() and 0xFF) == 0x7F) start++
            var stop = end
            while (stop > start && b[stop - 1].toInt() == 0) stop--
            val cs = mibCharset(charset) ?: Charsets.UTF_8
            pos = end
            return String(b, start, stop - start, cs)
        }

        /** Pomija wartość nieznanego nagłówka według ogólnych reguł WSP. */
        fun skipValue() {
            val x = b[pos].toInt() and 0xFF
            when {
                x < 31 -> { pos++; pos += x }
                x == 31 -> { pos++; val l = uintvar(); pos += l.toInt() }
                x < 128 -> text()
                else -> pos++
            }
        }

        /** Typ treści z parametrami: (typ, parametry). */
        fun contentType(): Pair<String, Map<Int, String>> {
            val params = HashMap<Int, String>()
            val x = b[pos].toInt() and 0xFF
            if (x >= 0x80) {
                pos++
                return (WELL_KNOWN[x and 0x7F] ?: "application/octet-stream") to params
            }
            if (x >= 32) return text().lowercase() to params
            val len = valueLength()
            val end = pos + len
            val y = b[pos].toInt() and 0xFF
            val type = when {
                y >= 0x80 -> { pos++; WELL_KNOWN[y and 0x7F] ?: "application/octet-stream" }
                y >= 32 -> text().lowercase()
                else -> { val code = longInteger().toInt(); WELL_KNOWN[code] ?: "application/octet-stream" }
            }
            while (pos < end) {
                val p = b[pos].toInt() and 0xFF
                if (p < 0x80) {
                    // Parametr tekstowy: nazwa i wartość.
                    val name = text().lowercase()
                    val value = if (pos < end && (b[pos].toInt() and 0xFF) >= 0x80) { integer().toString() } else text()
                    when (name) {
                        "charset" -> params[PARAM_CHARSET] = value
                        "name" -> params[PARAM_NAME] = value
                        "filename" -> params[PARAM_FILENAME] = value
                    }
                    continue
                }
                pos++
                when (p) {
                    PARAM_CHARSET -> {
                        val c = b[pos].toInt() and 0xFF
                        params[PARAM_CHARSET] = if (c in 32..127) {
                            text().let { n -> if (n.equals("utf-8", true)) "106" else n }
                        } else {
                            integer().toString()
                        }
                    }
                    PARAM_TYPE -> {
                        val c = b[pos].toInt() and 0xFF
                        params[PARAM_TYPE] = if (c >= 0x80) { pos++; WELL_KNOWN[c and 0x7F] ?: "" } else text()
                    }
                    PARAM_NAME, PARAM_NAME_OLD -> params[PARAM_NAME] = encodedString()
                    PARAM_FILENAME, PARAM_FILENAME_OLD -> params[PARAM_FILENAME] = encodedString()
                    PARAM_START -> params[PARAM_START] = text()
                    else -> skipValue()
                }
            }
            pos = end
            return type to params
        }

        fun multipart(): List<Part> {
            val count = uintvar().toInt()
            val out = ArrayList<Part>(count)
            repeat(count) {
                val headersLen = uintvar().toInt()
                val dataLen = uintvar().toInt()
                val headersEnd = pos + headersLen
                val (ct, params) = contentType()
                var location: String? = null
                var id: String? = null
                while (pos < headersEnd) {
                    val h = b[pos].toInt() and 0xFF
                    if (h >= 0x80) {
                        pos++
                        when (h) {
                            P_CONTENT_LOCATION -> location = text()
                            P_CONTENT_ID -> id = text().removePrefix("<").removeSuffix(">")
                            else -> skipValue()
                        }
                    } else {
                        val name = text().lowercase()
                        val value = text()
                        when (name) {
                            "content-location" -> location = value
                            "content-id" -> id = value.removePrefix("<").removeSuffix(">")
                        }
                    }
                }
                pos = headersEnd
                val end = (pos + dataLen).coerceAtMost(b.size)
                val data = b.copyOfRange(pos, end)
                pos = end
                out += Part(
                    contentType = ct,
                    data = data,
                    name = params[PARAM_NAME] ?: params[PARAM_FILENAME] ?: location,
                    contentId = id,
                    contentLocation = location,
                    charset = params[PARAM_CHARSET]?.toIntOrNull() ?: 0,
                )
            }
            return out
        }
    }
}

/** Kodowanie znaków z numeru MIBenum (IANA), używanego w MMS. */
fun mibCharset(mib: Int): Charset? = when (mib) {
    106 -> Charsets.UTF_8
    3 -> Charsets.US_ASCII
    4 -> Charsets.ISO_8859_1
    1000, 1013 -> Charsets.UTF_16BE
    1014 -> Charsets.UTF_16LE
    1015 -> Charsets.UTF_16
    5 -> runCatching { Charset.forName("ISO-8859-2") }.getOrNull()
    2250 -> runCatching { Charset.forName("windows-1250") }.getOrNull()
    2252 -> runCatching { Charset.forName("windows-1252") }.getOrNull()
    else -> null
}
