package com.blacksingularities.opensms.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blacksingularities.opensms.ui.theme.Palette

/** Okrągły przycisk z ikoną (jak w OpenPDF): ciemne koło, aktywny – białe z czarną ikoną. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoundIcon(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    badge: String? = null,
    size: Dp = 44.dp,
    flat: Boolean = false,
    tint: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(
        when {
            active -> Color.White
            flat -> Color.Transparent
            else -> Palette.PanelHigh
        },
        tween(160),
        label = "roundBg",
    )
    Box(modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onLongClick = onLongClick?.let { f -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); f() } },
                    onClick = onClick,
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = when {
                    !enabled -> Palette.TextFaint
                    active -> Palette.Ink
                    tint != null -> tint
                    else -> Color.White
                },
                modifier = Modifier.size(size * 0.5f),
            )
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-2).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Record)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Group(
    title: String?,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Column {
        if (title != null) Row(
            Modifier
                .fillMaxWidth()
                .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(start = 4.dp, end = 4.dp, bottom = 3.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                color = Palette.Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f),
            )
            if (collapsible) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    if (expanded) "Zwiń sekcję $title" else "Rozwiń sekcję $title",
                    tint = Palette.Accent,
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 0f else -90f),
                )
            }
        }
        AnimatedVisibility(visible = !collapsible || expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(vertical = 2.dp),
                content = content,
            )
        }
    }
}

@Composable
fun RowIcon(icon: ImageVector, tint: Color = Palette.Accent) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!value) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 14.sp, color = if (enabled) Color.White else Palette.TextFaint)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.scale(0.82f),
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Accent, checkedThumbColor = Palette.Ink),
        )
    }
}

/** Wybór w jednej linii z przełącznikiem segmentowym po prawej. */
@Composable
fun <T> CompactChoice(
    rowIcon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    short: (T) -> String = label,
    hint: String? = null,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(rowIcon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    hint?.let { "${label(selected)} · $it" } ?: label(selected),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Palette.TextDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            Modifier
                .padding(start = 44.dp, top = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(2.dp),
        ) {
            options.forEach { o ->
                val sel = o == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) Color.White else Color.Transparent)
                        .clickable { onSelect(o) }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        short(o),
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) Palette.Ink else Palette.TextDim,
                    )
                }
            }
        }
    }
}

/** Wiersz-akcja: ikona, tytuł, opis; dotknięcie wykonuje akcję. */
@Composable
fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, if (danger) Palette.Record else Palette.Accent)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                fontSize = 14.sp,
                color = when {
                    !enabled -> Palette.TextFaint
                    danger -> Palette.Record
                    else -> Color.White
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 11.sp, color = Palette.TextDim)
            Text(value.ifBlank { "—" }, fontSize = 14.sp, color = Color.White)
        }
    }
}

/** Przycisk-pigułka: biały (główny) albo półprzezroczysty. */
@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    danger: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val bg = when {
        !enabled -> Color.White.copy(alpha = 0.08f)
        danger -> Palette.Record
        primary -> Palette.Accent
        else -> Color.White.copy(alpha = 0.1f)
    }
    val fg = when {
        !enabled -> Palette.TextFaint
        danger -> Color.White
        primary -> Palette.Ink
        else -> Color.White
    }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Ciemne okno dialogowe w stylu aplikacji. */
@Composable
fun DarkDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.PanelHigh)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                buttons()
            }
        }
    }
}

/** Potwierdzenie z jednym przyciskiem akcji. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    danger: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DarkDialog(
        title = title,
        onDismiss = onDismiss,
        buttons = {
            PillButton("Anuluj", primary = false, onClick = onDismiss)
            PillButton(confirm, danger = danger, onClick = { onDismiss(); onConfirm() })
        },
    ) {
        Text(text, color = Palette.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** Arkusz z akcjami (lista wierszy) – jak menu kontekstowe w OpenPDF. */
@Composable
fun ActionSheet(title: String?, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Palette.PanelHigh)
                .padding(vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (title != null) {
                Text(
                    title,
                    fontSize = 13.sp,
                    color = Palette.TextDim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            content()
        }
    }
}

@Composable
fun DarkField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    number: Boolean = false,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Phone else KeyboardType.Text),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
            focusedLabelColor = Color.White,
            unfocusedLabelColor = Palette.TextDim,
            cursorColor = Color.White,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
        ),
    )
}

/** Zakładka-chip (filtry listy rozmów); licznik to mała plakietka w prawym górnym rogu, nie zmienia rozmiaru chipa. */
@Composable
fun TabChip(icon: ImageVector?, label: String, selected: Boolean, modifier: Modifier = Modifier, count: Int = 0, onClick: () -> Unit) {
    Box(modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) Color.White else Palette.PanelHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, label, tint = if (selected) Palette.Ink else Color.White, modifier = Modifier.size(16.dp))
                if (selected) Spacer(Modifier.width(5.dp))
            }
            // Nieaktywne zakładki z ikoną pokazują tylko ikonę; nazwa tylko przy otwartej.
            if (icon == null || selected) Text(label, color = if (selected) Palette.Ink else Color.White, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
        if (count > 0) {
            Text(
                if (count > 99) "99+" else count.toString(),
                color = Color.White,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Palette.Record)
                    .padding(horizontal = 3.dp, vertical = 1.dp),
            )
        }
    }
}

/** Awatar: zdjęcie kontaktu, inicjały albo ikona grupy. */
@Composable
fun Avatar(
    name: String,
    photo: Uri?,
    size: Dp = 48.dp,
    group: Boolean = false,
    colored: Boolean = true,
    selected: Boolean = false,
) {
    val bitmap: Bitmap? = photo?.let { rememberBitmap(it, 256) }
    val color = if (colored) Palette.AvatarColors[(name.hashCode() and 0x7fffffff) % Palette.AvatarColors.size] else Palette.PanelHigher
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (selected) Color.White else color),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(Icons.Rounded.Check, null, tint = Palette.Ink, modifier = Modifier.size(size * 0.5f))
            bitmap != null -> Image(bitmap.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
            group -> Icon(Icons.Rounded.Groups, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
            else -> {
                val initials = initials(name)
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
            }
        }
    }
}

private fun initials(name: String): String {
    val words = name.trim().split(' ', '-').filter { it.isNotEmpty() && it[0].isLetter() }
    return when {
        words.isEmpty() -> if (name.any { it.isDigit() }) "#" else name.take(1).uppercase()
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/** Pusty stan listy: ikona i dwa zdania. */
@Composable
fun EmptyState(icon: ImageVector, title: String, text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Palette.PanelHigh), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(text, color = Palette.TextDim, fontSize = 13.sp, lineHeight = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
