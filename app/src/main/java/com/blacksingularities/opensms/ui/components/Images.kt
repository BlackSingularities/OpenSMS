package com.blacksingularities.opensms.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pamięć podręczna miniatur (zdjęcia kontaktów, obrazy MMS) – ok. 1/8 pamięci aplikacji. */
private val bitmapCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
    override fun sizeOf(key: String, value: Bitmap) = value.byteCount
}

fun cachedBitmap(uri: Uri, maxPx: Int): Bitmap? = bitmapCache.get("$uri@$maxPx")

/** Dekoduje obraz (z uwzględnieniem obrotu EXIF) w rozmiarze najwyżej [maxPx] na dłuższym boku. */
fun loadBitmap(context: Context, uri: Uri, maxPx: Int, video: Boolean = false): Bitmap? {
    val key = "$uri@$maxPx"
    bitmapCache.get(key)?.let { return it }
    val bmp = runCatching {
        if (video) {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(context, uri)
                r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxPx, maxPx)
            }
        } else {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = maxPx.toFloat() / maxOf(w, h)
                if (scale < 1f) d.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
    }.getOrNull() ?: return null
    bitmapCache.put(key, bmp)
    return bmp
}

@Composable
fun rememberBitmap(uri: Uri, maxPx: Int, video: Boolean = false): Bitmap? {
    val context = LocalContext.current
    val state by produceState(cachedBitmap(uri, maxPx), uri, maxPx) {
        if (value == null) value = withContext(Dispatchers.IO) { loadBitmap(context, uri, maxPx, video) }
    }
    return state
}
