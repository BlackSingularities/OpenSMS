package com.blacksingularities.opensms.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Zapis, udostępnianie i otwieranie załączników MMS (kopiowanych do pamięci podręcznej). */
object MediaActions {
    private fun extension(mime: String, name: String?): String {
        name?.substringAfterLast('.', "")?.takeIf { it.length in 2..4 }?.let { return it }
        return when {
            mime == "image/jpeg" -> "jpg"
            mime == "image/png" -> "png"
            mime == "image/gif" -> "gif"
            mime == "image/webp" -> "webp"
            mime.startsWith("video/") -> "mp4"
            mime == "audio/amr" -> "amr"
            mime.startsWith("audio/") -> "m4a"
            mime.contains("vcard") -> "vcf"
            else -> "bin"
        }
    }

    private fun fileName(mime: String, name: String?): String {
        val base = name?.substringBeforeLast('.')?.replace(Regex("[^A-Za-z0-9._-]"), "_")?.takeIf { it.isNotBlank() }
            ?: ("OpenSMS_" + System.currentTimeMillis())
        return base + "." + extension(mime, name)
    }

    /** Kopia w katalogu podręcznym udostępniana innym aplikacjom przez FileProvider. */
    fun shareableUri(context: Context, uri: Uri, mime: String, name: String?): Uri {
        if (uri.scheme == "file") return uri
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fileName(mime, name))
        context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
            ?: error("Nie można odczytać załącznika")
        return FileProvider.getUriForFile(context, context.packageName + ".files", file)
    }

    fun open(context: Context, uri: Uri, mime: String, name: String?) {
        val shared = shareableUri(context, uri, mime, name)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(shared, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            error("Brak aplikacji do otwarcia pliku")
        }
    }

    fun share(context: Context, uri: Uri, mime: String, name: String?) {
        val shared = shareableUri(context, uri, mime, name)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, shared)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Udostępnij").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(intent, "Udostępnij").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Zapis do galerii (zdjęcia, filmy) albo do Pobranych (pozostałe pliki). */
    fun save(context: Context, uri: Uri, mime: String, name: String?): String {
        val cr = context.contentResolver
        val (collection, dir) = when {
            mime.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_PICTURES
            mime.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MOVIES
            mime.startsWith("audio/") -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_MUSIC
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_DOWNLOADS
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName(mime, name))
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/OpenSMS")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = cr.insert(collection, values) ?: error("Nie można zapisać pliku")
        try {
            cr.openInputStream(uri)?.use { input -> cr.openOutputStream(target)?.use { input.copyTo(it) } }
            cr.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            cr.delete(target, null, null)
            throw e
        }
        return "$dir/OpenSMS"
    }

    fun copy(context: Context, text: String, label: String = "Wiadomość") {
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
