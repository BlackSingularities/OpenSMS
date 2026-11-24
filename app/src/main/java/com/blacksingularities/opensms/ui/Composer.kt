package com.blacksingularities.opensms.ui

import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.tr

import com.blacksingularities.opensms.ui.components.SwitchRow
import androidx.compose.material.icons.rounded.NotificationsActive
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsMessage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.Attachment
import com.blacksingularities.opensms.mms.MmsSender
import com.blacksingularities.opensms.ui.components.ActionRow
import com.blacksingularities.opensms.ui.components.ActionSheet
import com.blacksingularities.opensms.ui.components.rememberBitmap
import com.blacksingularities.opensms.ui.theme.Palette
import kotlinx.coroutines.delay
import java.util.Calendar

/** Pole pisania: załączniki, szablony, wybór karty SIM, licznik znaków, wysyłka i planowanie. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Composer(vm: AppViewModel, s: Screen.Thread) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var value by remember(s.threadId, s.text, s.attachments) { mutableStateOf(TextFieldValue(s.text, TextRange(s.text.length))) }
    var attachments by remember(s.threadId, s.attachments) { mutableStateOf(s.attachments) }
    var attachMenu by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf(false) }
    var scheduleMenu by remember { mutableStateOf(false) }
    val lastSub = vm.messages.lastOrNull()?.subId ?: -1
    var subId by remember(s.threadId, lastSub) {
        mutableStateOf(if (lastSub >= 0 && vm.sims.any { it.subId == lastSub }) lastSub else vm.settings.defaultSubId)
    }
    val text = value.text

    // Szkic zapisywany na bieżąco i przy wyjściu z rozmowy.
    val currentText by rememberUpdatedState(text)
    LaunchedEffect(text) {
        delay(700)
        vm.saveDraft(s.threadId, text)
    }
    DisposableEffect(s.threadId) { onDispose { vm.saveDraft(s.threadId, currentText) } }

    fun addUris(uris: List<Uri>) {
        val added = uris.mapNotNull { u -> runCatching { MmsSender.describe(context, u) }.getOrNull() }
        attachments = (attachments + added).take(10)
    }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { addUris(it) }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { addUris(it) }
    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val vcard = runCatching {
            context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    Attachment(
                        Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, c.getString(0)),
                        "text/x-vcard",
                        (c.getString(1) ?: "kontakt") + ".vcf",
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
        if (vcard != null) attachments = attachments + vcard else vm.toast = tr(R.string.nie_mozna_odczytac_kontaktu)
    }

    val sendable = (text.isNotBlank() || attachments.isNotEmpty()) && s.addresses.isNotEmpty() && vm.busy == null
    // Pole czyści się od razu po naciśnięciu „Wyślij” – tekst nie zostaje w polu ani w szkicu.
    // Gdy wysyłka się nie uda, treść i załączniki wracają do pola.
    fun clearField() {
        value = TextFieldValue("")
        attachments = emptyList()
        vm.saveDraft(s.threadId, "")
    }
    fun send() {
        if (!sendable) return
        if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val sentText = text
        val sentAttachments = attachments
        clearField()
        vm.send(s, sentText, sentAttachments, subId) {
            if (value.text.isEmpty() && attachments.isEmpty()) {
                value = TextFieldValue(sentText, TextRange(sentText.length))
                attachments = sentAttachments
            }
        }
    }

    Column(Modifier.fillMaxWidth().background(Palette.Ink).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)) {
        AnimatedVisibility(visible = attachments.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 52.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                attachments.forEach { a -> AttachmentChip(a) { attachments = attachments - a } }
            }
        }
        if (vm.sims.size > 1) {
            val sim = vm.sims.firstOrNull { it.subId == subId } ?: vm.sims.first()
            Row(
                Modifier
                    .padding(start = 52.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Palette.PanelHigh)
                    .clickable {
                        val i = vm.sims.indexOf(sim)
                        subId = vm.sims[(i + 1) % vm.sims.size].subId
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.SimCard, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("SIM ${sim.slot + 1} · ${sim.name}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Palette.PanelHigh).clickable { attachMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, tr(R.string.dodaj), tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Palette.PanelHigh)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                // Podpowiedź ma ten sam krój i wysokość linii co wpisywany tekst, więc nic nie skacze przy pierwszej literze.
                val fieldStyle = TextStyle(color = Color.White, fontSize = (16 * vm.settings.textSize.scale).sp, lineHeight = (21 * vm.settings.textSize.scale).sp)
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text(
                            if (attachments.isNotEmpty()) tr(R.string.dodaj_opis) else if (s.addresses.size > 1 && vm.settings.groupAsMms) tr(R.string.wiadomosc_grupowa_mms) else tr(R.string.wiadomosc),
                            style = fieldStyle.copy(color = Palette.TextFaint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = if (vm.settings.enterSends) ImeAction.Send else ImeAction.Default,
                        ),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        singleLine = false,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 22.dp),
                    )
                }
                Counter(vm, text, attachments, s.addresses.size)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (sendable) Color.White else Palette.PanelHigh)
                    .combinedClickable(
                        enabled = sendable,
                        onClick = { send() },
                        onLongClick = {
                            if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (attachments.isEmpty()) scheduleMenu = true else vm.toast = tr(R.string.planowac_mozna_tylko_sms_bez)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (vm.busy != null) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, tr(R.string.wyslij_przytrzymaj_aby_zaplanowac), tint = if (sendable) Palette.Ink else Palette.TextFaint, modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    if (attachMenu) {
        ActionSheet(title = tr(R.string.dodaj_do_wiadomosci), onDismiss = { attachMenu = false }) {
            ActionRow(Icons.Rounded.PhotoLibrary, tr(R.string.zdjecia_i_filmy), tr(R.string.zdjecia_sa_zmniejszane_do_limitu, vm.settings.mmsLimit.label)) {
                attachMenu = false
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
            ActionRow(Icons.AutoMirrored.Rounded.InsertDriveFile, tr(R.string.plik), tr(R.string.nagranie_dokument_dowolny_plik)) {
                attachMenu = false
                pickFile.launch(arrayOf("audio/*", "video/*", "image/*", "text/*", "*/*"))
            }
            ActionRow(Icons.Rounded.ContactPage, tr(R.string.wizytowka_kontaktu), tr(R.string.wyslij_kontakt_jako_plik_vcard)) {
                attachMenu = false
                pickContact.launch(null)
            }
            ActionRow(Icons.AutoMirrored.Rounded.ShortText, tr(R.string.szablony), tr(R.string.gotowe_szybkie_odpowiedzi)) {
                attachMenu = false
                templates = true
            }
            ActionRow(Icons.Rounded.Schedule, tr(R.string.zaplanuj_wyslanie), tr(R.string.albo_przytrzymaj_przycisk_wysylania)) {
                attachMenu = false
                if (text.isBlank()) vm.toast = tr(R.string.najpierw_wpisz_tresc_wiadomosci) else scheduleMenu = true
            }
        }
    }

    if (templates) {
        ActionSheet(title = tr(R.string.szablony), onDismiss = { templates = false }) {
            if (vm.settings.templates.isEmpty()) {
                Text(tr(R.string.brak_szablonow_dodasz_je_w), color = Palette.TextDim, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
            }
            vm.settings.templates.forEach { t ->
                ActionRow(Icons.AutoMirrored.Rounded.ShortText, t.text) {
                    templates = false
                    val v = value
                    val newText = v.text.substring(0, v.selection.start) + t.text + v.text.substring(v.selection.end)
                    value = TextFieldValue(newText, TextRange(v.selection.start + t.text.length))
                }
            }
        }
    }

    if (scheduleMenu) {
        ScheduleSheet(vm, onDismiss = { scheduleMenu = false }) { time, confirm ->
            scheduleMenu = false
            val scheduledText = text
            clearField()
            vm.schedule(s, scheduledText, time, subId, confirm)
        }
    }
}

@Composable
private fun Counter(vm: AppViewModel, text: String, attachments: List<Attachment>, recipients: Int) {
    val mms = attachments.isNotEmpty() || (recipients > 1 && vm.settings.groupAsMms)
    if (mms) {
        Text("MMS", color = Palette.TextFaint, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
        return
    }
    if (text.length < 50) return
    val body = if (vm.settings.stripAccents) com.blacksingularities.opensms.sms.MessageSender.stripAccents(text) else text
    val calc = remember(body) { SmsMessage.calculateLength(body, false) }
    val parts = calc[0]
    val left = calc[2]
    val asMms = vm.settings.longAsMms && parts > vm.settings.longAsMmsSegments
    Text(
        if (asMms) tr(R.string.mms_dluga_wiadomosc) else "$left · ${plural(parts, tr(R.string.sms_one), tr(R.string.sms_few), tr(R.string.sms_many))}",
        color = if (parts > 3 && !asMms) Color(0xFFFFB74D) else Palette.TextFaint,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 3.dp),
    )
}

@Composable
private fun AttachmentChip(a: Attachment, onRemove: () -> Unit) {
    val visual = a.mime.startsWith("image/") || a.mime.startsWith("video/")
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(Palette.PanelHigh)) {
        if (visual) {
            val bmp = rememberBitmap(a.uri, 256, video = a.mime.startsWith("video/"))
            if (bmp != null) Image(bmp.asImageBitmap(), a.name, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp))
        } else {
            Column(Modifier.size(72.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    when {
                        a.mime.startsWith("audio/") -> Icons.Rounded.AudioFile
                        a.mime.contains("vcard") -> Icons.Rounded.ContactPage
                        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
                    },
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Text(a.name, color = Palette.TextDim, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 11.sp)
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f)).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, tr(R.string.usun_zalacznik), tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** Szybkie terminy wysyłki i własna data z systemowego kalendarza. */
@Composable
fun ScheduleSheet(vm: AppViewModel, onDismiss: () -> Unit, onPick: (Long, Boolean) -> Unit) {
    val context = LocalContext.current
    val settings = vm.settings
    fun at(daysAhead: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysAhead)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val now = System.currentTimeMillis()
    val options = buildList {
        add(tr(R.string.za_godzine) to now + 3_600_000L)
        if (at(0, 20) > now + 600_000) add(tr(R.string.dzis_wieczorem_20_00) to at(0, 20))
        add(tr(R.string.jutro_rano_8_00) to at(1, 8))
        add(tr(R.string.jutro_w_poludnie_12_00) to at(1, 12))
    }
    ActionSheet(title = tr(R.string.zaplanuj_wyslanie), onDismiss = onDismiss) {
        // Zapamiętany wybór: wysłać samo albo najpierw zapytać w powiadomieniu.
        SwitchRow(
            Icons.Rounded.NotificationsActive,
            tr(R.string.zapytaj_przed_wyslaniem),
            if (settings.scheduleConfirm) tr(R.string.o_czasie_przyjdzie_powiadomienie_wyslij) else tr(R.string.wiadomosc_wyjdzie_sama_o_wybranej),
            settings.scheduleConfirm,
        ) { settings.scheduleConfirm = it }
        options.forEach { (label, time) ->
            ActionRow(Icons.Rounded.Schedule, label, formatFull(time).substringBeforeLast(':')) { onPick(time, settings.scheduleConfirm) }
        }
        ActionRow(Icons.Rounded.Schedule, tr(R.string.wybierz_date_i_godzine)) {
            val c = Calendar.getInstance()
            DatePickerDialog(context, { _, y, mo, d ->
                TimePickerDialog(context, { _, h, mi ->
                    val t = Calendar.getInstance().apply { set(y, mo, d, h, mi, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    if (t <= System.currentTimeMillis()) onDismiss() else onPick(t, settings.scheduleConfirm)
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }
    }
}
