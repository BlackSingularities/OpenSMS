package com.blacksingularities.opensms.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.data.BubbleSkin
import com.blacksingularities.opensms.data.Settings
import com.blacksingularities.opensms.tr
import com.blacksingularities.opensms.ui.components.ActionRow
import com.blacksingularities.opensms.ui.components.RowIcon
import com.blacksingularities.opensms.ui.components.SwitchRow
import com.blacksingularities.opensms.ui.theme.Palette

/** Przewijany pasek gotowych skinów; każda karta pokazuje miniaturę rozmowy w danym stylu. */
@Composable
fun BubbleSkinPicker(s: Settings) {
    val base = MessageStyle.from(s, zoom = 0.62f)
    val scroll = rememberScrollState()
    // Wybrany skin widoczny po wejściu w ustawienia, także gdy jest daleko na liście.
    LaunchedEffect(Unit) {
        val i = s.bubbleSkin.ordinal
        if (i > 2) scroll.scrollTo((i - 1) * 300)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(Icons.Rounded.ChatBubble)
            Text(tr(R.string.styl_dymkow), fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll).padding(start = 14.dp, end = 14.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BubbleSkin.entries.forEach { skin ->
                val sel = skin == s.bubbleSkin
                val style = base.copy(skin = skin, radius = if (skin == s.bubbleSkin) s.bubbleRadius else skin.radius)
                Column(
                    Modifier
                        .width(104.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                        .border(if (sel) 2.dp else 1.dp, if (sel) Color.White else Palette.Hairline, RoundedCornerShape(16.dp))
                        .clickable { selectSkin(s, skin) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(10.dp)).background(Palette.Ink).padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                    ) {
                        MiniBubble(style, tr(R.string.ola), out = false)
                        MiniBubble(style, "OK 👍", out = true)
                    }
                    Text(
                        skin.label,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) Color.White else Palette.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBubble(style: MessageStyle, text: String, out: Boolean) {
    val c = style.colors(out)
    Box(Modifier.fillMaxWidth(), contentAlignment = if (out) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            text,
            color = c.content,
            fontSize = 10.sp,
            maxLines = 1,
            fontFamily = if (c.mono) FontFamily.Monospace else null,
            modifier = Modifier
                .bubbleSurface(style, c, style.shape(out, false, false), out)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun selectSkin(s: Settings, skin: BubbleSkin) {
    if (skin == s.bubbleSkin) return
    s.bubbleSkin = skin
    s.bubbleRadius = skin.radius
}

/** Ręczna edycja własnego skina: kolory, gradient, ogonek i obwódka. */
@Composable
fun CustomBubbleEditor(s: Settings) {
    if (s.bubbleSkin != BubbleSkin.CUSTOM) {
        ActionRow(Icons.Rounded.Edit, tr(R.string.edytuj_recznie), tr(R.string.edytuj_recznie_opis)) {
            val (o, o2, i) = skinSeed(s.bubbleSkin, s.bubbleTint)
            s.customOut = o
            s.customOut2 = o2
            s.customIn = i
            s.customTail = s.bubbleSkin.tail
            s.customOutline = s.bubbleSkin == BubbleSkin.NEON || s.bubbleSkin == BubbleSkin.GLASS || s.bubbleSkin == BubbleSkin.COMIC
            s.bubbleSkin = BubbleSkin.CUSTOM
        }
        return
    }
    ColorRow(Icons.Rounded.ChatBubble, tr(R.string.kolor_wyslanych), s.customOut, allowNone = false) { s.customOut = it }
    ColorRow(Icons.Rounded.Gradient, tr(R.string.kolor_gradientu), s.customOut2, allowNone = true) { s.customOut2 = it }
    ColorRow(Icons.Rounded.ChatBubbleOutline, tr(R.string.kolor_odebranych), s.customIn, allowNone = false) { s.customIn = it }
    SwitchRow(Icons.Rounded.Tune, tr(R.string.ogonek), tr(R.string.ogonek_opis), s.customTail) { s.customTail = it }
    SwitchRow(Icons.Rounded.CropSquare, tr(R.string.obwodka), tr(R.string.obwodka_opis), s.customOutline) { s.customOutline = it }
}

private val SWATCHES = listOf(
    0xFF0A84FF, 0xFF30D158, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF375F, 0xFFFF453A, 0xFFFF9F0A, 0xFFFFD60A,
    0xFF64D2FF, 0xFF00C7BE, 0xFFA8C7FA, 0xFFF3E9D2, 0xFFFFFFFF, 0xFFCFCFCF, 0xFF8E8E93, 0xFF3A3A3C,
    0xFF26252A, 0xFF1C1C1E, 0xFF0B0B0B, 0xFF5C3A21, 0xFF173B2A, 0xFF1A2A4A, 0xFF3A1730, 0xFF7F00FF,
).map { it.toInt() }

/** Kolor z gotowej palety albo dokładnie: odcień, nasycenie, jasność. */
@Composable
private fun ColorRow(icon: ImageVector, title: String, value: Int, allowNone: Boolean, onChange: (Int) -> Unit) {
    var precise by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { precise = !precise },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(icon)
            Text(title, fontSize = 15.sp, color = Color.White, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Box(
                Modifier.size(26.dp).clip(CircleShape)
                    .background(if (value == 0) Color.Transparent else Color(value))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (value == 0) Icon(Icons.Rounded.Block, null, tint = Palette.TextDim, modifier = Modifier.size(16.dp))
            }
            Icon(
                Icons.Rounded.ExpandMore,
                null,
                tint = Palette.TextDim,
                modifier = Modifier.padding(start = 6.dp).size(20.dp).rotate(if (precise) 180f else 0f),
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 44.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (allowNone) Swatch(null, value == 0) { onChange(0) }
            SWATCHES.forEach { c -> Swatch(c, c == value) { onChange(c) } }
        }
        AnimatedVisibility(precise && value != 0) {
            HsvSliders(value, onChange)
        }
    }
}

@Composable
private fun Swatch(color: Int?, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color?.let { Color(it) } ?: Palette.PanelHigher),
        contentAlignment = Alignment.Center,
    ) {
        when {
            color == null -> Icon(Icons.Rounded.Block, tr(R.string.bez_gradientu), tint = Palette.TextDim, modifier = Modifier.size(16.dp))
            selected -> Icon(Icons.Rounded.Check, null, tint = readableOn(Color(color)), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HsvSliders(value: Int, onChange: (Int) -> Unit) {
    val hsv = remember(value) { FloatArray(3).also { android.graphics.Color.colorToHSV(value, it) } }
    fun set(i: Int, v: Float) {
        val n = hsv.copyOf().also { it[i] = v }
        onChange(android.graphics.Color.HSVToColor(n))
    }
    val hues = remember { Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }) }
    Column(Modifier.padding(start = 44.dp, top = 8.dp)) {
        HsvSlider(tr(R.string.odcien), hsv[0], 0f..360f, hues) { set(0, it) }
        HsvSlider(
            tr(R.string.nasycenie), hsv[1], 0f..1f,
            Brush.horizontalGradient(listOf(Color.hsv(hsv[0], 0f, hsv[2].coerceAtLeast(0.3f)), Color.hsv(hsv[0], 1f, hsv[2].coerceAtLeast(0.3f)))),
        ) { set(1, it) }
        HsvSlider(
            tr(R.string.jasnosc), hsv[2], 0f..1f,
            Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hsv[0], hsv[1], 1f))),
        ) { set(2, it) }
    }
}

@Composable
private fun HsvSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, track: Brush, onChange: (Float) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = Palette.TextDim)
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(8.dp).clip(RoundedCornerShape(50)).background(track))
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                modifier = Modifier.height(34.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}
