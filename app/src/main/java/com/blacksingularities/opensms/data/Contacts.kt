package com.blacksingularities.opensms.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import java.util.concurrent.ConcurrentHashMap

data class ContactInfo(val name: String, val photo: Uri?, val contactId: Long, val lookupKey: String?)

/** Nazwy i zdjęcia kontaktów z pamięcią podręczną (lista rozmów pyta o nie bardzo często). */
object Contacts {
    private val cache = ConcurrentHashMap<String, Any>()
    private val NONE = Any()

    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun clearCache() = cache.clear()

    fun lookup(context: Context, address: String): ContactInfo? {
        if (address.isBlank() || !hasPermission(context)) return null
        val cached = cache[address]
        if (cached != null) return cached as? ContactInfo
        val info = runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.LOOKUP_KEY,
                ),
                null,
                null,
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    ContactInfo(
                        name = c.getString(0) ?: address,
                        photo = c.getString(1)?.let(Uri::parse),
                        contactId = c.getLong(2),
                        lookupKey = c.getString(3),
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
        cache[address] = info ?: NONE
        return info
    }

    fun displayName(context: Context, address: String): String =
        lookup(context, address)?.name ?: formatNumber(address)

    fun title(context: Context, addresses: List<String>): String = when {
        addresses.isEmpty() -> "Brak odbiorcy"
        addresses.size == 1 -> displayName(context, addresses[0])
        else -> addresses.joinToString(", ") { displayName(context, it).substringBefore(' ') }
    }

    fun isKnown(context: Context, address: String) = lookup(context, address) != null

    /** Wszystkie numery telefonów z kontaktów – do wyboru odbiorcy. */
    fun all(context: Context): List<Contact> {
        if (!hasPermission(context)) return emptyList()
        val out = ArrayList<Contact>()
        val seen = HashSet<String>()
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                    ContactsContract.CommonDataKinds.Phone.STARRED,
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val number = c.getString(2) ?: continue
                    val id = c.getLong(0)
                    if (!seen.add("$id:${normalizeNumber(number)}")) continue
                    val label = phoneLabel(c.getInt(3), c.getString(4))
                    out += Contact(id, c.getString(1) ?: number, number, label, c.getString(5)?.let(Uri::parse), c.getInt(6) == 1)
                }
            }
        }
        return out
    }

    fun contactUri(info: ContactInfo): Uri =
        if (info.lookupKey != null) {
            ContactsContract.Contacts.getLookupUri(info.contactId, info.lookupKey)
        } else {
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, info.contactId)
        }

    private fun phoneLabel(type: Int, custom: String?): String = when (type) {
        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Komórka"
        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Dom"
        ContactsContract.CommonDataKinds.Phone.TYPE_WORK, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Praca"
        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Główny"
        ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Inny"
        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> custom ?: "Inny"
        else -> "Telefon"
    }
}

/** Numer w czytelnej postaci (np. +48 600 100 200), adresy e-mail i nazwy nadawców bez zmian. */
fun formatNumber(address: String): String {
    if (address.any { it.isLetter() }) return address
    return runCatching { PhoneNumberUtils.formatNumber(address, "PL") }.getOrNull() ?: address
}
