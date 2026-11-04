package com.blacksingularities.opensms.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.blacksingularities.opensms.data.BubbleSpacing
import com.blacksingularities.opensms.data.BubbleTint
import com.blacksingularities.opensms.data.BubbleWidth
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.data.TimeMode
import com.blacksingularities.opensms.ui.theme.Palette

const val MIN_ZOOM = 0.6f
const val MAX_ZOOM = 2f

/** Kolory dymka: tło, tekst i (dla stylu „Kontur”) obwódka. */
@Immutable
data class BubbleColors(val background: Color, val content: Color, val outline: Color? = null) {
    /** Ciemny tekst na jasnym tle – linki i wyróżnienia muszą być ciemne. */
    val lightBackground: Boolean get() = content == Palette.Ink
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
) {
    val fontScale: Float get() = zoom * textScale

    fun dp(v: Float): Dp = (v * zoom).dp
    fun sp(v: Float): TextUnit = (v * fontScale).sp

    /** Drobne napisy (godzina, nadawca) rosną wolniej niż treść, żeby nie dominowały. */
    fun smallSp(v: Float): TextUnit = (v * (1f + (fontScale - 1f) * 0.6f)).coerceAtLeast(9f).sp

    fun shape(out: Boolean, groupedWithOlder: Boolean, groupedWithNewer: Boolean): RoundedCornerShape {
        val big = dp(radius)
        // Spłaszczony róg w serii: ułamek promienia, ale zawsze choć trochę zaokrąglony.
        val small = dp((radius * 0.28f).coerceAtLeast(if (radius > 0f) 2f else 0f))
        // Dymek w serii jest spłaszczony od strony sąsiada, samotny zostaje zaokrąglony dookoła.
        return if (out) {
            RoundedCornerShape(topStart = big, topEnd = if (groupedWithOlder) small else big, bottomEnd = if (groupedWithNewer) small else big, bottomStart = big)
        } else {
            RoundedCornerShape(topStart = if (groupedWithOlder) small else big, topEnd = big, bottomEnd = big, bottomStart = if (groupedWithNewer) small else big)
        }
    }

    fun colors(out: Boolean, failed: Boolean = false): BubbleColors = when {
        failed -> BubbleColors(Palette.Record.copy(alpha = 0.18f), Color.White)
        !out -> BubbleColors(Palette.BubbleIn, Color.White)
        tint == BubbleTint.WHITE -> BubbleColors(Palette.BubbleOut, Palette.Ink)
        tint == BubbleTint.SILVER -> BubbleColors(Color(0xFFCFCFCF), Palette.Ink)
        tint == BubbleTint.GRAPHITE -> BubbleColors(Color(0xFF3A3A3A), Color.White)
        else -> BubbleColors(Color.Transparent, Color.White, outline = Color(0x59FFFFFF))
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
        )
    }
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
