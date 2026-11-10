package com.blacksingularities.opensms.notify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.drawable.IconCompat
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.Settings

/**
 * Awatary do powiadomień i skrótów rozmów – te same co w aplikacji: zdjęcie kontaktu albo
 * inicjały na kolorowym (lub szarym) tle, a dla grup dwie połówki z awatarami uczestników.
 * Dzięki temu w powiadomieniu widać nadawcę, a nie ikonę aplikacji.
 */
object NotificationAvatars {
    /** Rozmiar bitmapy; ikona adaptacyjna pokazuje środkowe 2/3, więc i tak wychodzi ostro. */
    private const val SIZE = 288

    /** Część bitmapy widoczna po nałożeniu maski ikony adaptacyjnej (72 z 108 dp). */
    private const val SAFE = 72f / 108f

    private val COLORS = intArrayOf(
        0xFF5C6BC0.toInt(), 0xFF26A69A.toInt(), 0xFFEF5350.toInt(), 0xFFAB47BC.toInt(),
        0xFFFFA726.toInt(), 0xFF42A5F5.toInt(), 0xFF66BB6A.toInt(), 0xFFEC407A.toInt(),
        0xFF8D6E63.toInt(), 0xFF78909C.toInt(),
    )
    private const val GREY = 0xFF2A2A2A.toInt()

    /** Kto jest na awatarze: nazwa (do inicjałów i koloru) i ewentualne zdjęcie. */
    data class Face(val name: String, val photo: Uri?)

    fun face(context: Context, address: String): Face {
        val info = Contacts.lookup(context, address)
        return Face(info?.name ?: Contacts.displayName(context, address), info?.photo)
    }

    /** Ikona adaptacyjna jednej osoby – do [androidx.core.app.Person] i skrótu rozmowy. */
    fun icon(context: Context, face: Face): IconCompat =
        IconCompat.createWithAdaptiveBitmap(draw(context, listOf(face), adaptive = true))

    /** Ikona rozmowy: jedna osoba albo dwie połówki dla grupy. */
    fun conversationIcon(context: Context, addresses: List<String>): IconCompat =
        IconCompat.createWithAdaptiveBitmap(draw(context, addresses.take(2).map { face(context, it) }, adaptive = true))

    /** Okrągła bitmapa na duży obrazek powiadomienia (starsze układy i powiadomienia o błędach). */
    fun largeIcon(context: Context, addresses: List<String>): Bitmap? = runCatching {
        if (addresses.isEmpty()) return null
        draw(context, addresses.take(2).map { face(context, it) }, adaptive = false)
    }.getOrNull()

    private fun draw(context: Context, faces: List<Face>, adaptive: Boolean): Bitmap {
        val colored = Settings.get(context).colorAvatars
        val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        // Obszar faktycznie widoczny: w ikonie adaptacyjnej środek, w zwykłej całość przycięta do koła.
        val visible = if (adaptive) {
            val inset = SIZE * (1 - SAFE) / 2
            RectF(inset, inset, SIZE - inset, SIZE - inset)
        } else {
            RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat())
        }
        if (!adaptive) {
            // Wszystko, co narysujemy niżej, ląduje w kole.
            val path = android.graphics.Path().apply { addOval(visible, android.graphics.Path.Direction.CW) }
            canvas.clipPath(path)
        }
        val list = faces.ifEmpty { listOf(Face("#", null)) }
        if (list.size == 1) {
            drawFace(context, canvas, list[0], full(visible, adaptive), visible, colored)
        } else {
            // Grupa: lewa i prawa połówka, rozdzielone cienką czarną szczeliną.
            val mid = visible.centerX()
            val gap = visible.width() * 0.02f
            val left = RectF(if (adaptive) 0f else visible.left, 0f, mid - gap, SIZE.toFloat())
            val right = RectF(mid + gap, 0f, if (adaptive) SIZE.toFloat() else visible.right, SIZE.toFloat())
            canvas.drawColor(0xFF000000.toInt())
            drawFace(context, canvas, list[0], left, RectF(visible.left, visible.top, mid - gap, visible.bottom), colored)
            drawFace(context, canvas, list[1], right, RectF(mid + gap, visible.top, visible.right, visible.bottom), colored)
        }
        return bmp
    }

    /** W ikonie adaptacyjnej tło sięga krawędzi, żeby maska nigdy nie odsłoniła pustych rogów. */
    private fun full(visible: RectF, adaptive: Boolean) =
        if (adaptive) RectF(0f, 0f, SIZE.toFloat(), SIZE.toFloat()) else visible

    /**
     * Rysuje jedną osobę: [area] wypełnia tłem, a zdjęcie lub inicjały układa tak,
     * żeby mieściły się w widocznej części [visible].
     */
    private fun drawFace(context: Context, canvas: Canvas, face: Face, area: RectF, visible: RectF, colored: Boolean) {
        val photo = face.photo?.let { loadPhoto(context, it) }
        val bg = if (colored) COLORS[(face.name.hashCode() and 0x7fffffff) % COLORS.size] else GREY
        canvas.save()
        canvas.clipRect(area)
        canvas.drawColor(bg)
        if (photo != null) {
            // Zdjęcie przycięte „na wypełnienie” widocznej części (bez dodatkowego powiększenia przez maskę);
            // poza nią krawędzie zdjęcia się przeciągają, więc nigdzie nie zostaje goły kolor.
            val target = RectF(area.left, area.top, area.right, area.bottom)
            val scale = maxOf(visible.width() / photo.width, visible.height() / photo.height)
            val m = Matrix().apply {
                setScale(scale, scale)
                postTranslate(
                    visible.centerX() - photo.width * scale / 2,
                    visible.centerY() - photo.height * scale / 2,
                )
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                shader = BitmapShader(photo, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(m) }
            }
            canvas.drawRect(target, paint)
        } else {
            // W połówce grupy mieści się jedna litera; w pełnym awatarze – jak w aplikacji – dwie.
            val half = visible.width() < visible.height() * 0.8f
            val text = initials(face.name).let { if (half) it.take(1) else it }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                textSize = visible.height() * if (half) 0.34f else 0.36f
            }
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            canvas.drawText(text, visible.centerX(), visible.centerY() - bounds.exactCenterY(), paint)
        }
        canvas.restore()
    }

    private fun loadPhoto(context: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val k = SIZE.toFloat() / minOf(w, h).coerceAtLeast(1)
            if (k < 1f) d.setTargetSize((w * k).toInt().coerceAtLeast(1), (h * k).toInt().coerceAtLeast(1))
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()

    /** Te same zasady co w awatarze w aplikacji. */
    private fun initials(name: String): String {
        val words = name.trim().split(' ', '-').filter { it.isNotEmpty() && it[0].isLetter() }
        return when {
            words.isEmpty() -> if (name.any { it.isDigit() }) "#" else name.take(1).uppercase().ifEmpty { "#" }
            words.size == 1 -> words[0].take(1).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }
}
