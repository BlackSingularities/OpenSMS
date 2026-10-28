package com.blacksingularities.opensms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.Contact
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.canReplyTo
import com.blacksingularities.opensms.data.formatNumber
import com.blacksingularities.opensms.data.normalizeNumber
import com.blacksingularities.opensms.ui.components.Avatar
import com.blacksingularities.opensms.ui.components.EmptyState
import com.blacksingularities.opensms.ui.components.RoundIcon
import com.blacksingularities.opensms.ui.theme.Palette
import java.text.Normalizer

/** Nowa wiadomość: wybór odbiorców (kontakty, numer, kilka osób – rozmowa grupowa). */
@Composable
fun ComposeScreen(vm: AppViewModel, s: Screen.Compose) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var recipients by remember { mutableStateOf<List<String>>(emptyList()) }
    var dialpad by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(dialpad) { runCatching { focus.requestFocus() } }

    val q = fold(query.trim())
    val digits = query.filter { it.isDigit() || it == '+' }
    val filtered = remember(vm.contacts, q) {
        if (q.isEmpty()) {
            vm.contacts
        } else {
            vm.contacts.filter { c ->
                fold(c.name).contains(q) || (digits.length >= 2 && normalizeNumber(c.number).contains(digits.removePrefix("+")))
            }
        }
    }
    // Nadawcy alfanumeryczni (np. „Google”) nie przyjmują wiadomości – nie ma sensu ich proponować.
    val recent = remember(vm.conversations) {
        vm.conversations.filter { it.conv.addresses.size == 1 && canReplyTo(it.conv.addresses[0]) }.take(8)
    }
    val canUseRaw = canReplyTo(query.trim()) && (digits.length >= 3 || query.contains('@'))

    fun add(address: String) {
        val a = address.trim()
        if (a.isEmpty()) return
        if (!canReplyTo(a)) {
            vm.toast = "Na ten adres nie da się wysłać wiadomości"
            return
        }
        if (recipients.none { normalizeNumber(it) == normalizeNumber(a) }) recipients = recipients + a
        query = ""
    }

    fun proceed() {
        val list = if (recipients.isEmpty() && canUseRaw) listOf(query.trim()) else recipients
        if (list.isEmpty()) {
            vm.toast = "Wybierz odbiorcę"
            return
        }
        vm.openAddresses(list, s.text, s.attachments, replaceTop = true)
    }

    Column(Modifier.fillMaxSize().background(Palette.Ink).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", flat = true, onClick = { vm.back() })
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text("Nowa wiadomość", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (s.attachments.isNotEmpty() || s.text.isNotEmpty()) {
                    Text(
                        listOfNotNull(
                            s.attachments.takeIf { it.isNotEmpty() }?.let { plural(it.size, "załącznik", "załączniki", "załączników") },
                            s.text.takeIf { it.isNotEmpty() }?.let { "„" + it.take(30) + (if (it.length > 30) "…”" else "”") },
                        ).joinToString(" · "),
                        color = Palette.TextDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (recipients.isNotEmpty() || canUseRaw) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { proceed() }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Dalej", color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Palette.Ink, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Pole „Do:” z wybranymi odbiorcami
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Palette.PanelHigh)
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Do:", color = Palette.TextDim, fontSize = 15.sp, modifier = Modifier.padding(end = 8.dp))
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                recipients.forEach { r ->
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color.White).clickable { recipients = recipients - r }.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(Contacts.displayName(context, r), color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Icon(Icons.Rounded.Close, "Usuń odbiorcę", tint = Palette.Ink, modifier = Modifier.padding(start = 3.dp).size(14.dp))
                    }
                }
                Box(Modifier.width(220.dp).padding(vertical = 6.dp)) {
                    if (query.isEmpty()) Text(if (recipients.isEmpty()) "Imię lub numer" else "Dodaj kolejną osobę", color = Palette.TextFaint, fontSize = 15.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { v ->
                            if (v.endsWith(",") || v.endsWith(";")) add(v.dropLast(1)) else query = v
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = if (dialpad) KeyboardType.Phone else KeyboardType.Text, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            when {
                                canUseRaw -> add(query)
                                filtered.size == 1 -> add(filtered[0].number)
                                recipients.isNotEmpty() -> proceed()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
            RoundIcon(if (dialpad) Icons.Rounded.Keyboard else Icons.Rounded.Dialpad, if (dialpad) "Klawiatura" else "Klawiatura numeryczna", size = 38.dp, flat = true, onClick = { dialpad = !dialpad })
        }
        if (recipients.size > 1) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Groups, null, tint = Palette.TextDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (vm.settings.groupAsMms) "Rozmowa grupowa – wiadomość MMS do ${recipients.size} osób" else "Osobny SMS do każdej z ${recipients.size} osób",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                )
            }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (canUseRaw) {
                item {
                    Row(
                        Modifier.fillMaxWidth().clickable { add(query) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Dialpad, null, tint = Palette.Ink)
                        }
                        Column(Modifier.padding(start = 14.dp)) {
                            Text("Wyślij do ${formatNumber(query.trim())}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("Numer spoza kontaktów", color = Palette.TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (q.isEmpty() && recent.isNotEmpty()) {
                item { Label("Ostatnie") }
                items(recent, key = { "r" + it.conv.threadId }) { c ->
                    PersonRow(c.title, formatNumber(c.conv.addresses[0]), c.photo, vm.settings.colorAvatars, starred = false, chosen = c.conv.addresses[0] in recipients) {
                        add(c.conv.addresses[0])
                    }
                }
            }
            if (filtered.isNotEmpty()) {
                item { Label(if (q.isEmpty()) "Kontakty" else "Pasujące kontakty") }
                items(filtered, key = { "c" + it.id + ":" + it.number }) { c -> ContactRow(vm, c, c.number in recipients) { add(c.number) } }
            } else if (!Contacts.hasPermission(context)) {
                item { EmptyState(Icons.Rounded.PersonSearch, "Brak dostępu do kontaktów", "Wpisz numer albo zezwól OpenSMS na odczyt kontaktów w ustawieniach.") }
            } else if (q.isNotEmpty() && !canUseRaw) {
                item { EmptyState(Icons.Rounded.PersonSearch, "Nie znaleziono", "Wpisz pełny numer telefonu, aby napisać do osoby spoza kontaktów.") }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        color = Palette.Accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ContactRow(vm: AppViewModel, c: Contact, chosen: Boolean, onClick: () -> Unit) {
    PersonRow(c.name, "${c.label} · ${formatNumber(c.number)}", c.photo, vm.settings.colorAvatars, c.starred, chosen, onClick)
}

@Composable
private fun PersonRow(
    name: String,
    subtitle: String,
    photo: android.net.Uri?,
    colored: Boolean,
    starred: Boolean,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, photo, size = 44.dp, colored = colored, selected = chosen)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (starred) Icon(Icons.Rounded.Star, "Ulubiony", tint = Palette.TextDim, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            }
            Text(subtitle, color = Palette.TextDim, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** Porównanie bez wielkości liter i polskich znaków: „lukasz” znajdzie „Łukasz”. */
private fun fold(s: String): String =
    Normalizer.normalize(s.lowercase().replace('ł', 'l'), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

