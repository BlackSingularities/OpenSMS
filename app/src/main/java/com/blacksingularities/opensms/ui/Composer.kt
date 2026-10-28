package com.blacksingularities.opensms.ui

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
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShortText
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
        if (vcard != null) attachments = attachments + vcard else vm.toast = "Nie można odczytać kontaktu"
    }

    val sendable = (text.isNotBlank() || attachments.isNotEmpty()) && s.addresses.isNotEmpty() && vm.busy == null
    fun send() {
        if (!sendable) return
        if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        vm.send(s, text, attachments, subId)
    }

    Column(Modifier.fillMaxWidth().background(Palette.Ink).padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)) {
        AnimatedVisibility(visible = attachments.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 48.dp, bottom = 6.dp),
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
                Icon(Icons.Rounded.Add, "Dodaj", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Palette.PanelHigh)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            ) {
                Box {
                    if (text.isEmpty()) {
                        Text(
                            if (attachments.isNotEmpty()) "Dodaj opis…" else if (s.addresses.size > 1 && vm.settings.groupAsMms) "Wiadomość grupowa (MMS)" else "Wiadomość",
                            color = Palette.TextFaint,
                            fontSize = 16.sp,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = TextStyle(color = Color.White, fontSize = (16 * vm.settings.textSize.scale).sp, lineHeight = (21 * vm.settings.textSize.scale).sp),
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
                            if (attachments.isEmpty()) scheduleMenu = true else vm.toast = "Planować można tylko SMS bez załączników"
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (vm.busy != null) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.AutoMirrored.Rounded.Send, "Wyślij (przytrzymaj, aby zaplanować)", tint = if (sendable) Palette.Ink else Palette.TextFaint, modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    if (attachMenu) {
        ActionSheet(title = "Dodaj do wiadomości", onDismiss = { attachMenu = false }) {
            ActionRow(Icons.Rounded.PhotoLibrary, "Zdjęcia i filmy", "Zdjęcia są zmniejszane do limitu MMS (${vm.settings.mmsLimit.label})") {
                attachMenu = false
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
            }
            ActionRow(Icons.Rounded.InsertDriveFile, "Plik", "Nagranie, dokument, dowolny plik") {
                attachMenu = false
                pickFile.launch(arrayOf("audio/*", "video/*", "image/*", "text/*", "*/*"))
            }
            ActionRow(Icons.Rounded.ContactPage, "Wizytówka kontaktu", "Wyślij kontakt jako plik vCard") {
                attachMenu = false
                pickContact.launch(null)
            }
            ActionRow(Icons.Rounded.ShortText, "Szablony", "Gotowe szybkie odpowiedzi") {
                attachMenu = false
                templates = true
            }
            ActionRow(Icons.Rounded.Schedule, "Zaplanuj wysłanie", "Albo przytrzymaj przycisk wysyłania") {
                attachMenu = false
                if (text.isBlank()) vm.toast = "Najpierw wpisz treść wiadomości" else scheduleMenu = true
            }
        }
    }

    if (templates) {
        ActionSheet(title = "Szablony", onDismiss = { templates = false }) {
            if (vm.settings.templates.isEmpty()) {
                Text("Brak szablonów. Dodasz je w Ustawieniach.", color = Palette.TextDim, fontSize = 14.sp, modifier = Modifier.padding(20.dp))
            }
            vm.settings.templates.forEach { t ->
                ActionRow(Icons.Rounded.ShortText, t.text) {
                    templates = false
                    val v = value
                    val newText = v.text.substring(0, v.selection.start) + t.text + v.text.substring(v.selection.end)
                    value = TextFieldValue(newText, TextRange(v.selection.start + t.text.length))
                }
            }
        }
    }

    if (scheduleMenu) {
        ScheduleSheet(onDismiss = { scheduleMenu = false }) { time ->
            scheduleMenu = false
            vm.schedule(s, text, time, subId)
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
        if (asMms) "MMS (długa wiadomość)" else "$left · ${plural(parts, "SMS", "SMS-y", "SMS-ów")}",
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
                        else -> Icons.Rounded.InsertDriveFile
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
            Icon(Icons.Rounded.Close, "Usuń załącznik", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

/** Szybkie terminy wysyłki i własna data z systemowego kalendarza. */
@Composable
fun ScheduleSheet(onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val context = LocalContext.current
    fun at(daysAhead: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysAhead)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val now = System.currentTimeMillis()
    val options = buildList {
        add("Za godzinę" to now + 3_600_000L)
        if (at(0, 20) > now + 600_000) add("Dziś wieczorem, 20:00" to at(0, 20))
        add("Jutro rano, 8:00" to at(1, 8))
        add("Jutro w południe, 12:00" to at(1, 12))
    }
    ActionSheet(title = "Zaplanuj wysłanie", onDismiss = onDismiss) {
        options.forEach { (label, time) ->
            ActionRow(Icons.Rounded.Schedule, label, formatFull(time).substringBeforeLast(':')) { onPick(time) }
        }
        ActionRow(Icons.Rounded.Schedule, "Wybierz datę i godzinę…") {
            val c = Calendar.getInstance()
            DatePickerDialog(context, { _, y, mo, d ->
                TimePickerDialog(context, { _, h, mi ->
                    val t = Calendar.getInstance().apply { set(y, mo, d, h, mi, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    if (t <= System.currentTimeMillis()) onDismiss() else onPick(t)
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.minDate = System.currentTimeMillis() - 1000
            }.show()
        }
    }
}
