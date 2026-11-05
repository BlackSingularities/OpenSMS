package com.blacksingularities.opensms.notify

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.blacksingularities.opensms.MainActivity
import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.data.Contacts

/**
 * Długotrwałe skróty rozmów. Dzięki nim Android pokazuje powiadomienia w sekcji „Rozmowy”,
 * pozwala ustawić rozmowę jako priorytetową i proponuje ją w arkuszu udostępniania.
 */
object ConversationShortcuts {
    const val SHARE_CATEGORY = "com.blacksingularities.opensms.SHARE_TARGET"
    private const val PREFIX = "thread_"

    fun id(threadId: Long) = PREFIX + threadId

    fun threadOf(shortcutId: String?): Long? =
        shortcutId?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)?.toLongOrNull()

    /** Dodaje albo odświeża skrót rozmowy; zwraca jego identyfikator (null, gdy system odmówił). */
    fun push(context: Context, threadId: Long, addresses: List<String>): String? = runCatching {
        if (addresses.isEmpty()) return null
        val id = id(threadId)
        val title = Contacts.title(context, addresses).ifBlank { addresses.first() }
        val people = addresses.map { a ->
            Person.Builder().setName(Contacts.lookup(context, a)?.name ?: a).setKey(a).build()
        }
        val photo = if (addresses.size == 1) Contacts.lookup(context, addresses[0])?.photo else null
        val icon = photo?.let { loadIcon(context, it) } ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(Notifier.EXTRA_THREAD, threadId)
        val shortcut = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(title)
            .setLongLabel(title)
            .setLongLived(true)
            .setPersons(people.toTypedArray())
            .setIcon(icon)
            .setIntent(intent)
            .setLocusId(LocusIdCompat(id))
            .setCategories(setOf(SHARE_CATEGORY))
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        id
    }.getOrNull()

    fun remove(context: Context, threadIds: Collection<Long>) {
        val ids = threadIds.map { id(it) }
        if (ids.isEmpty()) return
        runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, ids) }
        runCatching { ShortcutManagerCompat.removeLongLivedShortcuts(context, ids) }
    }

    private fun loadIcon(context: Context, uri: Uri): IconCompat? = runCatching {
        val bmp: Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ ->
            d.setTargetSize(128, 128)
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        IconCompat.createWithAdaptiveBitmap(bmp)
    }.getOrNull()
}
