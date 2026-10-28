package com.blacksingularities.opensms.data

import android.net.Uri

/** Rozmowa (wątek) z dostawcy Telephony. */
data class Conversation(
    val threadId: Long,
    val addresses: List<String>,
    val snippet: String,
    val date: Long,
    val unread: Int,
    val hasAttachment: Boolean,
    val messageCount: Int,
    val error: Boolean,
)

enum class Box { INBOX, SENT, OUTBOX, FAILED, DRAFT }

enum class Delivery { NONE, PENDING, DELIVERED, FAILED }

data class MmsPart(
    val id: Long,
    val contentType: String,
    val name: String?,
    val text: String?,
) {
    val uri: Uri get() = Uri.parse("content://mms/part/$id")
    val isImage get() = contentType.startsWith("image/")
    val isVideo get() = contentType.startsWith("video/")
    val isAudio get() = contentType.startsWith("audio/")
    val isText get() = contentType == "text/plain"
    val isSmil get() = contentType == "application/smil"
    val isVcard get() = contentType.contains("vcard", ignoreCase = true)
    val isMedia get() = !isText && !isSmil
}

data class Message(
    val id: Long,
    val isMms: Boolean,
    val threadId: Long,
    /** Nadawca (wiadomości przychodzące) albo odbiorca (wychodzące SMS). */
    val address: String,
    val body: String,
    val date: Long,
    val dateSent: Long,
    val box: Box,
    val read: Boolean,
    val delivery: Delivery,
    val subId: Int,
    val parts: List<MmsPart> = emptyList(),
    val subject: String? = null,
    /** MMS jeszcze nie pobrany: adres treści u operatora. */
    val pendingUrl: String? = null,
    val sizeBytes: Long = 0,
) {
    val key: String get() = (if (isMms) "m" else "s") + id
    val incoming: Boolean get() = box == Box.INBOX
    val uri: Uri get() = Uri.parse(if (isMms) "content://mms/$id" else "content://sms/$id")
    val media: List<MmsPart> get() = parts.filter { it.isMedia }
}

data class Contact(
    val id: Long,
    val name: String,
    val number: String,
    val label: String,
    val photo: Uri?,
    val starred: Boolean,
)

/** Załącznik przygotowany do wysłania. */
data class Attachment(
    val uri: Uri,
    val mime: String,
    val name: String,
)

data class SimCard(val subId: Int, val slot: Int, val name: String, val number: String?, val color: Int)
