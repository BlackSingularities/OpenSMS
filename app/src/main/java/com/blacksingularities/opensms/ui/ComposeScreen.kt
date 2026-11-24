package com.blacksingularities.opensms.ui

import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
            vm.toast = tr(R.string.na_ten_adres_nie_da)
            return
        }
        if (recipients.none { normalizeNumber(it) == normalizeNumber(a) }) recipients = recipients + a
        query = ""
    }

    fun proceed() {
        val list = if (recipients.isEmpty() && canUseRaw) listOf(query.trim()) else recipients
        if (list.isEmpty()) {
            vm.toast = tr(R.string.wybierz_odbiorce)
            return
        }
        vm.openAddresses(list, s.text, s.attachments, replaceTop = true)
    }

    Column(Modifier.fillMaxSize().background(Palette.Ink).screenPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, tr(R.string.wstecz), flat = true, onClick = { vm.back() })
            Column(Modifier.weight(1f).padding(start = 6.dp)) {
                Text(tr(R.string.nowa_wiadomosc), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                if (s.attachments.isNotEmpty() || s.text.isNotEmpty()) {
                    Text(
                        listOfNotNull(
                            s.attachments.takeIf { it.isNotEmpty() }?.let { plural(it.size, tr(R.string.zalacznik_2), tr(R.string.zalaczniki), tr(R.string.zalacznikow)) },
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
                    Text(tr(R.string.dalej), color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            Text(tr(R.string.do_odbiorcy), color = Palette.TextDim, fontSize = 15.sp, modifier = Modifier.padding(end = 8.dp))
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
                        Icon(Icons.Rounded.Close, tr(R.string.usun_odbiorce), tint = Palette.Ink, modifier = Modifier.padding(start = 3.dp).size(14.dp))
                    }
                }
                Box(Modifier.width(220.dp).padding(vertical = 6.dp)) {
                    if (query.isEmpty()) Text(if (recipients.isEmpty()) tr(R.string.imie_lub_numer) else tr(R.string.dodaj_kolejna_osobe), color = Palette.TextFaint, fontSize = 15.sp)
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
            RoundIcon(if (dialpad) Icons.Rounded.Keyboard else Icons.Rounded.Dialpad, if (dialpad) tr(R.string.klawiatura) else tr(R.string.klawiatura_numeryczna), size = 38.dp, flat = true, onClick = { dialpad = !dialpad })
        }
        if (recipients.size > 1) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Groups, null, tint = Palette.TextDim, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (vm.settings.groupAsMms) tr(R.string.rozmowa_grupowa_wiadomosc_mms_do, recipients.size) else tr(R.string.osobny_sms_do_kazdej_z, recipients.size),
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
                            Text(tr(R.string.wyslij_do_1, formatNumber(query.trim())), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(tr(R.string.numer_spoza_kontaktow), color = Palette.TextDim, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (q.isEmpty() && recent.isNotEmpty()) {
                item { Label(tr(R.string.ostatnie)) }
                items(recent, key = { "r" + it.conv.threadId }) { c ->
                    PersonRow(c.title, formatNumber(c.conv.addresses[0]), c.photo, vm.settings.colorAvatars, starred = false, chosen = c.conv.addresses[0] in recipients) {
                        add(c.conv.addresses[0])
                    }
                }
            }
            if (filtered.isNotEmpty()) {
                item { Label(if (q.isEmpty()) tr(R.string.kontakty) else tr(R.string.pasujace_kontakty)) }
                items(filtered, key = { "c" + it.id + ":" + it.number }) { c -> ContactRow(vm, c, c.number in recipients) { add(c.number) } }
            } else if (!Contacts.hasPermission(context)) {
                item { EmptyState(Icons.Rounded.PersonSearch, tr(R.string.brak_dostepu_do_kontaktow), tr(R.string.wpisz_numer_albo_zezwol_opensms)) }
            } else if (q.isNotEmpty() && !canUseRaw) {
                item { EmptyState(Icons.Rounded.PersonSearch, tr(R.string.nie_znaleziono), tr(R.string.wpisz_pelny_numer_telefonu_aby)) }
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
                if (starred) Icon(Icons.Rounded.Star, tr(R.string.ulubiony), tint = Palette.TextDim, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            }
            Text(subtitle, color = Palette.TextDim, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** Porównanie bez wielkości liter i polskich znaków: „lukasz” znajdzie „Łukasz”. */
private fun fold(s: String): String =
    Normalizer.normalize(s.lowercase().replace('ł', 'l'), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

