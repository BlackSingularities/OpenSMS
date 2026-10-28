package com.blacksingularities.opensms.mms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Telephony
import androidx.core.content.FileProvider
import com.blacksingularities.opensms.data.Attachment
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.data.Sims
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Wysyłanie MMS: zdjęcia są zmniejszane do limitu operatora, wiadomość trafia do skrzynki
 * nadawczej, a PDU m-send-req przekazujemy systemowi (SmsManager.sendMultimediaMessage).
 */
object MmsSender {
    const val ACTION_MMS_SENT = "com.blacksingularities.opensms.MMS_SENT"
    const val EXTRA_FILE = "file"

    fun send(
        context: Context,
        recipients: List<String>,
        text: String,
        attachments: List<Attachment>,
        subId: Int,
        subject: String?,
    ): Long {
        val settings = Settings.get(context)
        val limit = settings.mmsLimit.bytes
        val parts = buildParts(context, text, attachments, limit)
        val store = MessageStore(context)
        val threadId = store.threadIdFor(recipients)
        val transactionId = "T" + java.lang.Long.toHexString(System.currentTimeMillis()) + (1000..9999).random()
        val mmsUri = MmsPersister.insert(
            context = context,
            box = Telephony.Mms.MESSAGE_BOX_OUTBOX,
            threadId = threadId,
            from = null,
            to = recipients,
            cc = emptyList(),
            subject = subject,
            date = System.currentTimeMillis() / 1000,
            messageId = null,
            transactionId = transactionId,
            messageType = MessageStore.M_SEND_REQ,
            parts = parts,
            read = true,
            subId = subId,
        ) ?: error("Nie można zapisać MMS")
        dispatch(context, mmsUri, transactionId, recipients, subject, parts, subId)
        return threadId
    }

    /** Ponowna wysyłka MMS zapisanego w bazie. */
    fun retry(context: Context, mmsId: Long) {
        val s = Settings.get(context)
        s.failedMms = s.failedMms - mmsId.toString()
        val uri = Uri.parse("content://mms/$mmsId")
        val cr = context.contentResolver
        var subject: String? = null
        var subId = -1
        cr.query(uri, arrayOf("sub", "sub_cs", "sub_id"), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                subject = MessageStore.decodeMmsText(c.getString(0), c.getInt(1)).takeIf { it.isNotBlank() }
                subId = c.getInt(2)
            }
        }
        val to = ArrayList<String>()
        cr.query(Uri.parse("content://mms/$mmsId/addr"), arrayOf("address", "type"), null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getInt(1) == MessageStore.ADDR_TO) c.getString(0)?.let(to::add)
        }
        val parts = MmsPersister.readParts(context, mmsId)
        cr.update(uri, ContentValues().apply { put("msg_box", Telephony.Mms.MESSAGE_BOX_OUTBOX) }, null, null)
        val tid = "T" + java.lang.Long.toHexString(System.currentTimeMillis())
        dispatch(context, uri, tid, to, subject, parts, subId)
    }

    private fun dispatch(
        context: Context,
        mmsUri: Uri,
        transactionId: String,
        to: List<String>,
        subject: String?,
        parts: List<Pdu.Part>,
        subId: Int,
    ) {
        val pdu = Pdu.encodeSendReq(transactionId, to, subject, parts, Settings.get(context).deliveryReports)
        val dir = File(context.cacheDir, "mms").apply { mkdirs() }
        val file = File(dir, "send-${mmsUri.lastPathSegment}-${System.currentTimeMillis()}.pdu")
        file.writeBytes(pdu)
        val contentUri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val intent = Intent(context, MmsStatusReceiver::class.java)
            .setAction(ACTION_MMS_SENT)
            .setData(mmsUri)
            .putExtra(EXTRA_FILE, file.absolutePath)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        try {
            Sims.smsManager(context, subId).sendMultimediaMessage(context, contentUri, null, null, pending)
        } catch (e: Exception) {
            markFailed(context, mmsUri)
            file.delete()
            throw e
        }
    }

    fun markFailed(context: Context, mmsUri: Uri) {
        val id = mmsUri.lastPathSegment ?: return
        val s = Settings.get(context)
        s.failedMms = s.failedMms + id
        // Dotknięcie bazy odświeża listę (obserwatorzy treści).
        runCatching { context.contentResolver.update(mmsUri, ContentValues().apply { put("msg_box", Telephony.Mms.MESSAGE_BOX_OUTBOX) }, null, null) }
    }

    /** Części MMS: SMIL, tekst i załączniki (zdjęcia zmniejszone do limitu). */
    fun buildParts(context: Context, text: String, attachments: List<Attachment>, limit: Int): List<Pdu.Part> {
        val media = ArrayList<Pdu.Part>()
        val textBytes = text.toByteArray(Charsets.UTF_8)
        // Zapas na nagłówki PDU i SMIL.
        var budget = limit - textBytes.size - 4096
        val images = attachments.count { it.mime.startsWith("image/") && it.mime != "image/gif" }
        attachments.forEachIndexed { i, a ->
            val isImage = a.mime.startsWith("image/") && a.mime != "image/gif"
            val raw: ByteArray
            val mime: String
            var name = sanitizeName(a.name, i)
            if (isImage) {
                val share = (budget / images.coerceAtLeast(1)).coerceAtLeast(40 * 1024)
                raw = compressImage(context, a.uri, share)
                mime = "image/jpeg"
                name = name.substringBeforeLast('.') + ".jpg"
            } else {
                raw = context.contentResolver.openInputStream(a.uri)?.use { it.readBytes() } ?: error("Nie można odczytać załącznika")
                mime = a.mime
                if (raw.size > budget + 4096) error("Załącznik „${a.name}” jest za duży na MMS (${raw.size / 1024} KB)")
            }
            budget -= raw.size
            media += Pdu.Part(mime, raw, name = name, contentId = name, contentLocation = name)
        }
        val out = ArrayList<Pdu.Part>()
        val textPart = if (text.isNotEmpty()) Pdu.Part("text/plain", textBytes, name = "text_0.txt", contentId = "text_0.txt", contentLocation = "text_0.txt", charset = 106) else null
        out += Pdu.Part("application/smil", smil(media, textPart).toByteArray(Charsets.UTF_8), name = "smil.xml", contentId = "smil", contentLocation = "smil.xml")
        out += media
        if (textPart != null) out += textPart
        return out
    }

    private fun sanitizeName(name: String, i: Int): String {
        val clean = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40)
        return clean.ifBlank { "part_$i" }
    }

    /** Prezentacja SMIL: każdy załącznik na osobnym slajdzie, tekst na ostatnim. */
    private fun smil(media: List<Pdu.Part>, text: Pdu.Part?): String {
        val sb = StringBuilder()
        sb.append("<smil><head><layout><root-layout width=\"100%\" height=\"100%\"/>")
        sb.append("<region id=\"Image\" left=\"0\" top=\"0\" width=\"100%\" height=\"80%\" fit=\"meet\"/>")
        sb.append("<region id=\"Text\" left=\"0\" top=\"80%\" width=\"100%\" height=\"20%\" fit=\"scroll\"/>")
        sb.append("</layout></head><body>")
        media.forEachIndexed { i, p ->
            sb.append("<par dur=\"5000ms\">")
            val tag = when {
                p.contentType.startsWith("image/") -> "img"
                p.contentType.startsWith("video/") -> "video"
                p.contentType.startsWith("audio/") -> "audio"
                else -> "ref"
            }
            val region = if (tag == "img" || tag == "video") " region=\"Image\"" else ""
            sb.append("<$tag src=\"${p.contentLocation}\"$region/>")
            if (i == media.lastIndex && text != null) sb.append("<text src=\"${text.contentLocation}\" region=\"Text\"/>")
            sb.append("</par>")
        }
        if (media.isEmpty() && text != null) sb.append("<par dur=\"5000ms\"><text src=\"${text.contentLocation}\" region=\"Text\"/></par>")
        sb.append("</body></smil>")
        return sb.toString()
    }

    /** JPEG mieszczący się w [maxBytes]: najpierw jakość, potem rozdzielczość. */
    fun compressImage(context: Context, uri: Uri, maxBytes: Int): ByteArray {
        var maxSide = 1600
        while (true) {
            val bitmap = decode(context, uri, maxSide)
            for (q in intArrayOf(88, 80, 70, 60, 50)) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, q, out)
                if (out.size() <= maxBytes || (maxSide <= 320 && q == 50)) {
                    bitmap.recycle()
                    return out.toByteArray()
                }
            }
            bitmap.recycle()
            maxSide = (maxSide * 0.75).toInt()
        }
    }

    private fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = maxSide.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    }

    /** Nazwa i typ pliku wybranego przez użytkownika. */
    fun describe(context: Context, uri: Uri): Attachment {
        val cr = context.contentResolver
        var name = uri.lastPathSegment ?: "plik"
        runCatching {
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0)?.let { name = it }
            }
        }
        val mime = cr.getType(uri) ?: guessMime(name)
        return Attachment(uri, mime, name)
    }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "3gp" -> "video/3gpp"
        "mp3" -> "audio/mpeg"
        "m4a", "aac" -> "audio/mp4"
        "amr" -> "audio/amr"
        "ogg" -> "audio/ogg"
        "vcf" -> "text/x-vcard"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}
