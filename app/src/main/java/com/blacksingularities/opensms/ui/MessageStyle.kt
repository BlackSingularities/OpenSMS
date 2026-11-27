package com.blacksingularities.opensms.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.BubbleSkin
import com.blacksingularities.opensms.data.BubbleSpacing
import com.blacksingularities.opensms.data.BubbleTint
import com.blacksingularities.opensms.data.BubbleWidth
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.data.TimeMode
import com.blacksingularities.opensms.ui.theme.Palette

const val MIN_ZOOM = 0.6f
const val MAX_ZOOM = 2f

/** Kolory dymka: tło (lub gradient), tekst, obwódka i krój pisma. */
@Immutable
data class BubbleColors(
    val background: Color,
    val content: Color,
    val outline: Color? = null,
    /** Gradient tła; [background] zostaje jako kolor zastępczy (np. dla znaczka kodu). */
    val brush: Brush? = null,
    val outlineWidth: Dp = 1.dp,
    val mono: Boolean = false,
) {
    /**
     * Jasne tło dymka (po nałożeniu na czarne tło rozmowy) – wyróżnienia muszą być ciemne.
     * Liczone z tła, nie z koloru tekstu: szary tekst (np. „Bez dymków”) bywa ciemniejszy
     * niż połowa skali, a mimo to leży na czerni.
     */
    val lightBackground: Boolean get() = background.compositeOver(Palette.Ink).luminance() > 0.5f
}

/** Tekst czytelny na danym tle: biały na ciemnym, prawie czarny na jasnym. */
fun readableOn(bg: Color): Color = if (bg.luminance() > 0.5f) Color(0xFF101010) else Color.White

/** Kolory skina dla własnych i odebranych wiadomości. */
data class SkinPalette(val out: BubbleColors, val inc: BubbleColors)

private fun grad(a: Long, b: Long) = Brush.linearGradient(listOf(Color(a), Color(b)))
private fun mid(a: Long, b: Long) = lerp(Color(a), Color(b), 0.5f)

/** Ustawienia własnego skina (kolory ARGB, [out2] = 0 – bez gradientu). */
@Immutable
data class CustomBubble(val out: Int, val out2: Int, val inc: Int, val outline: Boolean, val tail: Boolean)

fun skinPalette(skin: BubbleSkin, tint: BubbleTint, custom: CustomBubble): SkinPalette = when (skin) {
    BubbleSkin.CLASSIC -> SkinPalette(
        when (tint) {
            BubbleTint.WHITE -> BubbleColors(Palette.BubbleOut, Palette.Ink)
            BubbleTint.SILVER -> BubbleColors(Color(0xFFCFCFCF), Palette.Ink)
            BubbleTint.GRAPHITE -> BubbleColors(Color(0xFF3A3A3A), Color.White)
            BubbleTint.OUTLINE -> BubbleColors(Color.Transparent, Color.White, outline = Color(0x59FFFFFF))
        },
        BubbleColors(Palette.BubbleIn, Color.White),
    )
    BubbleSkin.IOS -> SkinPalette(BubbleColors(Color(0xFF0A84FF), Color.White), BubbleColors(Color(0xFF26252A), Color.White))
    BubbleSkin.IOS_SMS -> SkinPalette(BubbleColors(Color(0xFF30D158), Color.White), BubbleColors(Color(0xFF26252A), Color.White))
    BubbleSkin.MATERIAL -> SkinPalette(BubbleColors(Color(0xFFA8C7FA), Color(0xFF062E6F)), BubbleColors(Color(0xFF2B2930), Color(0xFFE6E0E9)))
    BubbleSkin.OCEAN -> SkinPalette(
        BubbleColors(mid(0xFF0061FF, 0xFF00B4D8), Color.White, brush = grad(0xFF0061FF, 0xFF00B4D8)),
        BubbleColors(Color(0xFF132331), Color(0xFFDDF3FF)),
    )
    BubbleSkin.SUNSET -> SkinPalette(
        BubbleColors(mid(0xFFFF7A3D, 0xFFDD2476), Color.White, brush = grad(0xFFFF7A3D, 0xFFDD2476)),
        BubbleColors(Color(0xFF2A1A20), Color(0xFFFFE4E1)),
    )
    BubbleSkin.AURORA -> SkinPalette(
        BubbleColors(mid(0xFF7F00FF, 0xFF00A6C9), Color.White, brush = grad(0xFF7F00FF, 0xFF00A6C9)),
        BubbleColors(Color(0xFF1C1730), Color(0xFFEDE7FF)),
    )
    BubbleSkin.NEON -> SkinPalette(
        BubbleColors(Color(0xFF1A0420), Color(0xFFFFD1EC), outline = Color(0xFFFF2E97), outlineWidth = 1.5.dp),
        BubbleColors(Color(0xFF021320), Color(0xFFCFFAFF), outline = Color(0xFF00E5FF), outlineWidth = 1.5.dp),
    )
    BubbleSkin.GLASS -> SkinPalette(
        BubbleColors(Color.White.copy(alpha = 0.17f), Color.White, outline = Color.White.copy(alpha = 0.30f)),
        BubbleColors(Color.White.copy(alpha = 0.07f), Color.White, outline = Color.White.copy(alpha = 0.14f)),
    )
    BubbleSkin.PAPER -> SkinPalette(BubbleColors(Color(0xFFF3E9D2), Color(0xFF2B2118)), BubbleColors(Color(0xFF3A332B), Color(0xFFF3E9D2)))
    BubbleSkin.TERMINAL -> SkinPalette(
        BubbleColors(Color(0xFF071A0C), Color(0xFF39FF6A), outline = Color(0xFF1F8F3D), mono = true),
        BubbleColors(Color(0xFF0B0B0B), Color(0xFFB7FFC8), outline = Color(0xFF2A3A2E), mono = true),
    )
    BubbleSkin.COMIC -> SkinPalette(
        BubbleColors(Color(0xFFFFE14D), Color(0xFF111111), outline = Color.White, outlineWidth = 2.dp),
        BubbleColors(Color.White, Color(0xFF111111), outline = Color(0xFFFFE14D), outlineWidth = 2.dp),
    )
    BubbleSkin.MINIMAL -> SkinPalette(BubbleColors(Color.Transparent, Color.White), BubbleColors(Color.Transparent, Color(0xFFB8B8B8)))
    BubbleSkin.CUSTOM -> {
        val o = Color(custom.out)
        val o2 = if (custom.out2 != 0) Color(custom.out2) else null
        val i = Color(custom.inc)
        val outColor = if (o2 != null) lerp(o, o2, 0.5f) else o
        SkinPalette(
            BubbleColors(
                outColor,
                readableOn(outColor),
                outline = if (custom.outline) readableOn(outColor).copy(alpha = 0.45f) else null,
                brush = o2?.let { Brush.linearGradient(listOf(o, it)) },
            ),
            BubbleColors(i, readableOn(i), outline = if (custom.outline) readableOn(i).copy(alpha = 0.3f) else null),
        )
    }
}

/** Kolory ARGB skina do przeniesienia w „Własny”: własne, drugi kolor (0 – brak), odebrane. */
fun skinSeed(skin: BubbleSkin, tint: BubbleTint): Triple<Int, Int, Int> {
    fun Color.argb() = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
    fun solid(c: Color) = if (c.alpha < 0.5f) lerp(Color.Black, c.copy(alpha = 1f), c.alpha.coerceAtLeast(0.15f)) else c
    val p = skinPalette(skin, tint, CustomBubble(0, 0, 0, false, true))
    val (a, b) = when (skin) {
        BubbleSkin.OCEAN -> 0xFF0061FF.toInt() to 0xFF00B4D8.toInt()
        BubbleSkin.SUNSET -> 0xFFFF7A3D.toInt() to 0xFFDD2476.toInt()
        BubbleSkin.AURORA -> 0xFF7F00FF.toInt() to 0xFF00A6C9.toInt()
        else -> solid(p.out.background).argb() to 0
    }
    return Triple(a, b, solid(p.inc.background).argb())
}

/**
 * Kształt dymka: zaokrąglony prostokąt, spłaszczony od strony sąsiadów w serii, opcjonalnie
 * z ogonkiem. Gdy skin ma ogonek, ciało dymka jest odsunięte o [tail] od krawędzi po stronie
 * ogonka w każdym dymku serii, więc krawędzie zostają w jednej linii.
 */
class BubbleShape(
    private val big: Dp,
    private val small: Dp,
    private val out: Boolean,
    private val older: Boolean,
    private val newer: Boolean,
    private val tail: Dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = with(density) { tail.toPx() }
        val w = size.width
        val h = size.height
        val maxR = (minOf(w - t, h) / 2f).coerceAtLeast(0f)
        val b = with(density) { big.toPx() }.coerceAtMost(maxR)
        val sm = with(density) { small.toPx() }.coerceAtMost(maxR)
        val drawTail = t > 0f && !newer
        // Rogi po stronie nadawcy (prawa dla własnych, lewa dla odebranych) spłaszczone w serii.
        val sideTop = if (older) sm else b
        val sideBottom = if (newer || drawTail) sm else b
        val left = if (out) 0f else t
        val right = if (out) w - t else w
        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    left, 0f, right, h,
                    topLeftCornerRadius = CornerRadius(if (out) b else sideTop),
                    topRightCornerRadius = CornerRadius(if (out) sideTop else b),
                    bottomRightCornerRadius = CornerRadius(if (out) sideBottom else b),
                    bottomLeftCornerRadius = CornerRadius(if (out) b else sideBottom),
                ),
            )
        }
        if (!drawTail) return Outline.Generic(body)
        // Ogonek liczony dla prawej strony; dla odebranych odbity w poziomie.
        fun x(v: Float) = if (out) v else w - v
        val edge = w - t
        val up = minOf(h * 0.6f, t * 3.2f)
        val back = minOf(t * 2.2f, (edge - left).coerceAtLeast(0f) * 0.5f)
        val tailPath = Path().apply {
            moveTo(x(edge), h - up)
            cubicTo(x(edge), h - up * 0.35f, x(edge + t * 0.35f), h - t * 0.15f, x(w), h)
            cubicTo(x(edge - t * 0.4f), h + t * 0.05f, x(edge - back * 0.65f), h - t * 0.1f, x(edge - back), h - t * 0.55f)
            lineTo(x(edge - back), h - up)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, body, tailPath))
    }
}

/**
 * Wszystko, co decyduje o wyglądzie wiadomości. Powiększenie gestem ([zoom]) skaluje
 * czcionki, marginesy i rogi razem, więc dymki rosną i maleją płynnie jak całość.
 */
@Immutable
data class MessageStyle(
    val zoom: Float,
    val textScale: Float,
    /** Promień rogów dymka w dp (przed pomnożeniem przez [zoom]). */
    val radius: Float,
    val width: BubbleWidth,
    val tint: BubbleTint,
    val spacing: BubbleSpacing,
    val timeMode: TimeMode,
    val bigEmoji: Boolean,
    val emoticons: Boolean,
    val dayHeaders: Boolean,
    val senderAvatars: Boolean,
    val skin: BubbleSkin = BubbleSkin.CLASSIC,
    val custom: CustomBubble = CustomBubble(0xFF0A84FF.toInt(), 0, 0xFF26252A.toInt(), false, true),
) {
    val fontScale: Float get() = zoom * textScale
    private val palette: SkinPalette = skinPalette(skin, tint, custom)

    /** Czy dymki mają ogonek (i rezerwują na niego miejsce z boku). */
    val hasTail: Boolean get() = if (skin == BubbleSkin.CUSTOM) custom.tail else skin.tail
    val tailWidth: Dp get() = if (hasTail) dp(7f) else 0.dp

    fun dp(v: Float): Dp = (v * zoom).dp
    fun sp(v: Float): TextUnit = (v * fontScale).sp

    /** Drobne napisy (godzina, nadawca) rosną wolniej niż treść, żeby nie dominowały. */
    fun smallSp(v: Float): TextUnit = (v * (1f + (fontScale - 1f) * 0.6f)).coerceAtLeast(9f).sp

    fun shape(out: Boolean, groupedWithOlder: Boolean, groupedWithNewer: Boolean): Shape {
        val big = dp(radius)
        // Spłaszczony róg w serii: ułamek promienia, ale zawsze choć trochę zaokrąglony.
        val small = dp((radius * 0.28f).coerceAtLeast(if (radius > 0f) 2f else 0f))
        return BubbleShape(big, small, out, groupedWithOlder, groupedWithNewer, tailWidth)
    }

    fun colors(out: Boolean, failed: Boolean = false): BubbleColors = when {
        failed -> BubbleColors(Palette.Record.copy(alpha = 0.18f), Color.White)
        out -> palette.out
        else -> palette.inc
    }

    companion object {
        fun from(s: Settings, zoom: Float = s.bubbleZoom) = MessageStyle(
            zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM),
            textScale = s.textSize.scale,
            radius = s.bubbleRadius.coerceIn(0f, 28f),
            width = s.bubbleWidth,
            tint = s.bubbleTint,
            spacing = s.bubbleSpacing,
            timeMode = s.timeMode,
            bigEmoji = s.bigEmoji,
            emoticons = s.emoticons,
            dayHeaders = s.dayHeaders,
            senderAvatars = s.senderAvatars,
            skin = s.bubbleSkin,
            custom = CustomBubble(s.customOut, s.customOut2, s.customIn, s.customOutline, s.customTail),
        )
    }
}

/**
 * Tło dymka według skina: przycięcie do kształtu, kolor lub gradient, obwódka i miejsce
 * na ogonek. [bare] – sam tekst (np. duże emoji) bez tła, ale z tym samym wyrównaniem.
 */
fun Modifier.bubbleSurface(style: MessageStyle, c: BubbleColors, shape: Shape, out: Boolean, bare: Boolean = false): Modifier {
    var m = this.clip(shape)
    if (!bare) {
        m = if (c.brush != null) m.background(c.brush, shape) else m.background(c.background, shape)
        if (c.outline != null) m = m.border(c.outlineWidth, c.outline, shape)
    }
    val t = style.tailWidth
    return if (t > 0.dp) m.padding(start = if (out) 0.dp else t, end = if (out) t else 0.dp) else m
}

/**
 * Powiększanie dwoma palcami. Zdarzenia są czytane przed listą (faza Initial) i zjadane
 * tylko wtedy, gdy na ekranie są co najmniej dwa palce – przewijanie jednym palcem,
 * dotknięcia i przytrzymania dymków działają bez zmian.
 */
@Composable
fun Modifier.pinchToZoom(enabled: Boolean, onZoom: (Float) -> Unit, onStart: () -> Unit, onEnd: () -> Unit): Modifier {
    if (!enabled) return this
    val zoom by rememberUpdatedState(onZoom)
    val start by rememberUpdatedState(onStart)
    val end by rememberUpdatedState(onEnd)
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var zooming = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2) {
                    if (!zooming) {
                        zooming = true
                        start()
                    }
                    val z = event.calculateZoom()
                    if (z != 1f) zoom(z)
                    event.changes.forEach { it.consume() }
                } else if (zooming) {
                    // Po odjęciu jednego palca reszta gestu nie przewija listy.
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (zooming) end()
        }
    }
}
