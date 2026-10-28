package com.blacksingularities.opensms.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Patterns
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shortcut
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.Box as MsgBox
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.Delivery
import com.blacksingularities.opensms.data.Message
import com.blacksingularities.opensms.data.MmsPart
import com.blacksingularities.opensms.data.formatNumber
import com.blacksingularities.opensms.mms.MmsDownloader
import com.blacksingularities.opensms.ui.components.ActionRow
import com.blacksingularities.opensms.ui.components.ActionSheet
import com.blacksingularities.opensms.ui.components.Avatar
import com.blacksingularities.opensms.ui.components.ConfirmDialog
import com.blacksingularities.opensms.ui.components.DarkDialog
import com.blacksingularities.opensms.ui.components.EmptyState
import com.blacksingularities.opensms.ui.components.InfoRow
import com.blacksingularities.opensms.ui.components.PillButton
import com.blacksingularities.opensms.ui.components.RoundIcon
import com.blacksingularities.opensms.ui.components.rememberBitmap
import com.blacksingularities.opensms.ui.theme.Palette
import com.blacksingularities.opensms.util.OneTimeCode

@Composable
fun ThreadScreen(vm: AppViewModel, s: Screen.Thread) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Message?>(null) }
    var info by remember { mutableStateOf<Message?>(null) }
    var confirmDeleteThread by remember { mutableStateOf(false) }
    var confirmDeleteMsg by remember { mutableStateOf<Message?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }

    val title = remember(s.addresses, vm.conversations) { Contacts.title(context, s.addresses) }
    val single = s.addresses.size == 1
    val contact = remember(s.addresses) { if (single) Contacts.lookup(context, s.addresses[0]) else null }
    val threadId = s.threadId
    val item = vm.conversations.firstOrNull { it.conv.threadId == threadId }
    val messages = if (threadId != null && vm.openThreadId == threadId) vm.messages else emptyList()
    val reversed = remember(messages) { messages.asReversed() }
    val listState = rememberLazyListState()

    // Nowa wiadomość na dole – przewiń, jeśli użytkownik jest przy końcu rozmowy.
    LaunchedEffect(messages.lastOrNull()?.key) {
        if (listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }
    // Przejście z wyszukiwania: przewiń do pierwszej pasującej wiadomości.
    LaunchedEffect(s.highlight, messages.isNotEmpty()) {
        val q = s.highlight ?: return@LaunchedEffect
        val i = reversed.indexOfFirst { it.body.contains(q, ignoreCase = true) }
        if (i >= 0) listState.scrollToItem(i)
    }

    Column(Modifier.fillMaxSize().background(Palette.Ink).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        // Pasek rozmowy
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", flat = true, onClick = { vm.back() })
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable { details = true }.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(title, contact?.photo, size = 40.dp, group = !single, colored = vm.settings.colorAvatars)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            !single -> plural(s.addresses.size, "osoba", "osoby", "osób")
                            contact != null -> formatNumber(s.addresses[0])
                            else -> "Nie ma w kontaktach"
                        },
                        color = Palette.TextDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
            if (single && s.addresses[0].any { it.isDigit() } && s.addresses[0].none { it.isLetter() }) {
                RoundIcon(Icons.Rounded.Call, "Zadzwoń", flat = true, onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + s.addresses[0]))) }
                })
            }
            if (threadId != null) RoundIcon(Icons.Rounded.MoreVert, "Menu rozmowy", flat = true, onClick = { menu = true })
        }

        // Wiadomości
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (threadId == null || (messages.isEmpty() && vm.openThreadId == threadId && item == null)) {
                EmptyState(
                    Icons.Rounded.Person,
                    "Nowa rozmowa",
                    "Napisz pierwszą wiadomość do: ${s.addresses.joinToString(", ") { formatNumber(it) }}",
                    Modifier.align(Alignment.Center),
                )
            }
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                val lastOutgoing = messages.lastOrNull { !it.incoming }?.key
                itemsIndexed(reversed, key = { _, m -> m.key }) { i, m ->
                    val older = reversed.getOrNull(i + 1)
                    val newer = reversed.getOrNull(i - 1)
                    Column(Modifier.fillMaxWidth()) {
                        if (older == null || !isSameDay(older.date, m.date)) DayHeader(m.date)
                        val showSender = !single && m.incoming && (older == null || older.address != m.address || !older.incoming)
                        MessageBubble(
                            vm = vm,
                            m = m,
                            highlight = s.highlight,
                            showSender = showSender,
                            groupedWithNewer = newer != null && newer.incoming == m.incoming && newer.address == m.address && newer.date - m.date < 120_000 && isSameDay(newer.date, m.date),
                            showStatus = m.key == lastOutgoing || m.box == MsgBox.FAILED,
                            onLongClick = { selected = m },
                        )
                    }
                }
                if (vm.canLoadMore && reversed.isNotEmpty()) {
                    item(key = "more") {
                        Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                            PillButton("Wczytaj starsze", primary = false, onClick = { vm.loadMore() })
                        }
                    }
                }
            }
        }

        Composer(vm, s)
    }

    if (menu && threadId != null) {
        val pinned = vm.settings.isPinned(threadId)
        val archived = vm.settings.isArchived(threadId)
        val muted = vm.settings.isMuted(threadId)
        ActionSheet(title = title, onDismiss = { menu = false }) {
            if (single) {
                if (contact != null) {
                    ActionRow(Icons.Rounded.ContactPage, "Pokaż kontakt") {
                        menu = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Contacts.contactUri(contact))) }
                    }
                } else {
                    ActionRow(Icons.Rounded.PersonAdd, "Dodaj do kontaktów") {
                        menu = false
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_INSERT_OR_EDIT).setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                                    .putExtra(ContactsContract.Intents.Insert.PHONE, s.addresses[0]),
                            )
                        }
                    }
                }
            }
            ActionRow(Icons.Rounded.Info, "Szczegóły rozmowy") { menu = false; details = true }
            ActionRow(Icons.Rounded.PushPin, if (pinned) "Odepnij" else "Przypnij") {
                menu = false; vm.selection = setOf(threadId); vm.pinSelected()
            }
            ActionRow(if (muted) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff, if (muted) "Włącz powiadomienia" else "Wycisz powiadomienia") {
                menu = false; vm.toggleMute(threadId)
            }
            ActionRow(if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, if (archived) "Przywróć z archiwum" else "Archiwizuj") {
                menu = false; vm.archive(setOf(threadId), !archived); if (!archived) vm.back()
            }
            ActionRow(Icons.Rounded.Block, "Zablokuj", danger = true) { menu = false; confirmBlock = true }
            ActionRow(Icons.Rounded.Delete, "Usuń rozmowę", danger = true) { menu = false; confirmDeleteThread = true }
        }
    }

    if (details) {
        DarkDialog(title = "Szczegóły rozmowy", onDismiss = { details = false }, buttons = { PillButton("Zamknij", onClick = { details = false }) }) {
            s.addresses.forEach { a ->
                val c = Contacts.lookup(context, a)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Avatar(c?.name ?: a, c?.photo, size = 40.dp, colored = vm.settings.colorAvatars)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(c?.name ?: formatNumber(a), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatNumber(a), color = Palette.TextDim, fontSize = 12.sp)
                    }
                    RoundIcon(Icons.Rounded.ContentCopy, "Kopiuj numer", size = 36.dp, flat = true, onClick = { MediaActions.copy(context, a, "Numer"); vm.toast = "Skopiowano numer" })
                }
            }
            if (item != null) {
                Text(
                    "${plural(item.conv.messageCount, "wiadomość", "wiadomości", "wiadomości")} · ostatnia ${formatFull(item.conv.date)}",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                )
            }
        }
    }

    selected?.let { m ->
        MessageActions(vm, m, onDismiss = { selected = null }, onInfo = { info = m }, onDelete = {
            if (vm.settings.confirmDelete) confirmDeleteMsg = m else vm.deleteMessages(listOf(m))
        })
    }
    info?.let { m -> MessageInfo(m) { info = null } }
    confirmDeleteMsg?.let { m ->
        ConfirmDialog("Usunąć wiadomość?", "Wiadomość zostanie trwale usunięta z telefonu.", "Usuń", onDismiss = { confirmDeleteMsg = null }) { vm.deleteMessages(listOf(m)) }
    }
    if (confirmDeleteThread && threadId != null) {
        ConfirmDialog("Usunąć rozmowę?", "Wszystkie wiadomości z tej rozmowy zostaną trwale usunięte.", "Usuń", onDismiss = { confirmDeleteThread = false }) {
            vm.deleteThreads(setOf(threadId))
        }
    }
    if (confirmBlock) {
        ConfirmDialog(
            "Zablokować ${title}?",
            "Nowe wiadomości od tego nadawcy nie będą pokazywane. Rozmowa trafi do archiwum.",
            "Zablokuj",
            onDismiss = { confirmBlock = false },
        ) {
            vm.blockAddresses(s.addresses, setOfNotNull(threadId))
            vm.back()
        }
    }
}

@Composable
private fun DayHeader(t: Long) {
    Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            formatDayHeader(t),
            color = Palette.TextDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Palette.PanelHigh).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    vm: AppViewModel,
    m: Message,
    highlight: String?,
    showSender: Boolean,
    groupedWithNewer: Boolean,
    showStatus: Boolean,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val out = !m.incoming
    val scale = vm.settings.textSize.scale
    val bg = if (out) Palette.BubbleOut else Palette.BubbleIn
    val fg = if (out) Palette.Ink else Color.White
    val failed = m.box == MsgBox.FAILED
    val big = 20.dp
    val small = 6.dp
    val shape = if (out) {
        RoundedCornerShape(big, big, if (groupedWithNewer) small else big, small)
    } else {
        RoundedCornerShape(big, big, small, if (groupedWithNewer) small else big)
    }
    val onlyEmoji = m.body.isNotEmpty() && m.media.isEmpty() && m.body.length <= 8 && m.body.none { it.isLetterOrDigit() } && m.body.isNotBlank()

    Column(
        Modifier.fillMaxWidth().padding(top = if (groupedWithNewer) 2.dp else 6.dp),
        horizontalAlignment = if (out) Alignment.End else Alignment.Start,
    ) {
        if (showSender) {
            Text(Contacts.displayName(context, m.address), color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, bottom = 2.dp))
        }
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (onlyEmoji) Color.Transparent else if (failed) Palette.Record.copy(alpha = 0.18f) else bg)
                .combinedClickable(
                    onClick = {
                        when {
                            failed -> vm.retry(m)
                            else -> expanded = !expanded
                        }
                    },
                    onLongClick = {
                        if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
        ) {
            if (m.pendingUrl != null) {
                PendingMms(vm, m)
            }
            m.media.forEach { part -> MediaPart(vm, m, part) }
            if (m.subject != null) {
                Text(m.subject, color = if (failed) Color.White else fg, fontWeight = FontWeight.Bold, fontSize = (15 * scale).sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 9.dp))
            }
            if (m.body.isNotEmpty()) {
                Text(
                    linkify(m.body, highlight, out && !failed),
                    color = if (failed) Color.White else fg,
                    fontSize = if (onlyEmoji) (40 * scale).sp else (16 * scale).sp,
                    lineHeight = if (onlyEmoji) (46 * scale).sp else (21 * scale).sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
        // Kod jednorazowy: przycisk kopiowania pod wiadomością.
        if (m.incoming) {
            val code = remember(m.body) { OneTimeCode.find(m.body) }
            if (code != null) {
                Row(
                    Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Palette.PanelHigh)
                        .clickable { MediaActions.copy(context, code, "Kod"); vm.toast = "Skopiowano kod $code" }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Key, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kopiuj kod $code", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (expanded || showStatus || !groupedWithNewer) {
            StatusLine(vm, m, expanded || showStatus)
        }
    }
}

@Composable
private fun StatusLine(vm: AppViewModel, m: Message, full: Boolean) {
    val sim = if (vm.settings.showSimBadge && vm.sims.size > 1) vm.sims.firstOrNull { it.subId == m.subId }?.let { "SIM ${it.slot + 1}" } else null
    val (text, color) = when {
        m.incoming -> formatTime(m.date) to Palette.TextFaint
        m.box == MsgBox.FAILED -> "Nie wysłano · dotknij, aby ponowić" to Palette.Record
        m.box == MsgBox.OUTBOX && vm.isFailedOrStuck(m) -> "Wysyłanie trwa długo · przytrzymaj, aby ponowić" to Palette.Record
        m.box == MsgBox.OUTBOX -> "Wysyłanie…" to Palette.TextFaint
        !full -> formatTime(m.date) to Palette.TextFaint
        m.delivery == Delivery.DELIVERED -> "Dostarczono · ${formatTime(m.date)}" to Palette.TextDim
        m.delivery == Delivery.FAILED -> "Nie doręczono · ${formatTime(m.date)}" to Palette.Record
        m.delivery == Delivery.PENDING -> "Wysłano · ${formatTime(m.date)}" to Palette.TextFaint
        else -> "Wysłano · ${formatTime(m.date)}" to Palette.TextFaint
    }
    Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (m.box == MsgBox.FAILED) Icon(Icons.Rounded.Error, null, tint = Palette.Record, modifier = Modifier.size(13.dp).padding(end = 2.dp))
        Text(listOfNotNull(text, sim, if (m.isMms && full) "MMS" else null).joinToString(" · "), color = color, fontSize = 11.sp)
    }
}

@Composable
private fun PendingMms(vm: AppViewModel, m: Message) {
    val downloading = m.id in MmsDownloader.downloading.value
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            if (downloading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Rounded.Download, null, tint = Color.White)
            }
        }
        Column(Modifier.padding(horizontal = 12.dp)) {
            Text("Wiadomość MMS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                if (downloading) "Pobieranie…" else listOfNotNull(m.sizeBytes.takeIf { it > 0 }?.let(::formatSize), "dotknij, aby pobrać").joinToString(" · "),
                color = Palette.TextDim,
                fontSize = 12.sp,
            )
        }
        if (!downloading) PillButton("Pobierz", onClick = { vm.downloadMms(m) })
    }
}

@Composable
private fun MediaPart(vm: AppViewModel, m: Message, part: MmsPart) {
    val context = LocalContext.current
    when {
        part.isImage || part.isVideo -> {
            val bmp = rememberBitmap(part.uri, 900, video = part.isVideo)
            Box(
                Modifier
                    .padding(3.dp)
                    .widthIn(min = 120.dp, max = 294.dp)
                    .heightIn(min = 80.dp, max = 360.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Palette.PanelHigher)
                    .clickable { vm.push(Screen.Media(part.uri, part.contentType, part.name)) },
                contentAlignment = Alignment.Center,
            ) {
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), part.name, contentScale = ContentScale.Fit, modifier = Modifier.widthIn(max = 294.dp))
                } else {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                if (part.isVideo) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, "Odtwórz", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        else -> {
            val out = !m.incoming
            Row(
                Modifier
                    .padding(6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (out) Color.Black.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.07f))
                    .clickable {
                        runCatching { MediaActions.open(context, part.uri, part.contentType, part.name) }
                            .onFailure { vm.toast = it.message }
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when {
                        part.isAudio -> Icons.Rounded.AudioFile
                        part.isVcard -> Icons.Rounded.ContactPage
                        else -> Icons.Rounded.InsertDriveFile
                    },
                    null,
                    tint = if (out) Palette.Ink else Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Column(Modifier.padding(start = 10.dp).widthIn(max = 200.dp)) {
                    Text(
                        when {
                            part.isAudio -> "Nagranie"
                            part.isVcard -> "Wizytówka"
                            else -> "Plik"
                        },
                        color = if (out) Palette.Ink else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(part.name ?: part.contentType, color = if (out) Palette.Ink.copy(alpha = 0.6f) else Palette.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Linki, adresy e-mail i numery telefonów jako klikalne odnośniki; wyróżnienie szukanej frazy. */
private fun linkify(text: String, highlight: String?, dark: Boolean): AnnotatedString {
    data class Span(val start: Int, val end: Int, val url: String)
    val spans = ArrayList<Span>()
    fun collect(regex: java.util.regex.Pattern, toUrl: (String) -> String) {
        val m = regex.matcher(text)
        while (m.find()) {
            if (spans.none { m.start() < it.end && m.end() > it.start }) spans += Span(m.start(), m.end(), toUrl(m.group()))
        }
    }
    collect(Patterns.EMAIL_ADDRESS) { "mailto:$it" }
    collect(Patterns.WEB_URL) { if (it.startsWith("http", ignoreCase = true)) it else "https://$it" }
    collect(Patterns.PHONE) { "tel:" + it.filter { c -> c.isDigit() || c == '+' } }
    spans.removeAll { it.url.startsWith("tel:") && it.url.length < 11 }
    spans.sortBy { it.start }
    val linkColor = if (dark) Palette.Ink else Color.White
    val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
        var pos = 0
        for (sp in spans) {
            append(text.substring(pos, sp.start))
            withLink(LinkAnnotation.Url(sp.url, styles)) { append(text.substring(sp.start, sp.end)) }
            pos = sp.end
        }
        append(text.substring(pos))
        if (!highlight.isNullOrBlank()) {
            var i = text.indexOf(highlight, 0, ignoreCase = true)
            while (i >= 0) {
                addStyle(SpanStyle(background = if (dark) Color(0x33000000) else Color.White.copy(alpha = 0.35f), fontWeight = FontWeight.Bold), i, i + highlight.length)
                i = text.indexOf(highlight, i + highlight.length, ignoreCase = true)
            }
        }
    }
}

@Composable
private fun MessageActions(vm: AppViewModel, m: Message, onDismiss: () -> Unit, onInfo: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val code = remember(m.body) { OneTimeCode.find(m.body) }
    ActionSheet(title = m.body.take(120).ifEmpty { if (m.media.isNotEmpty()) "Załącznik MMS" else null }, onDismiss = onDismiss) {
        if (m.body.isNotEmpty()) {
            ActionRow(Icons.Rounded.ContentCopy, "Kopiuj tekst") { onDismiss(); MediaActions.copy(context, m.body); vm.toast = "Skopiowano" }
        }
        if (code != null) {
            ActionRow(Icons.Rounded.Key, "Kopiuj kod $code") { onDismiss(); MediaActions.copy(context, code, "Kod"); vm.toast = "Skopiowano kod" }
        }
        if (m.box == MsgBox.FAILED || vm.isFailedOrStuck(m)) {
            ActionRow(Icons.Rounded.Refresh, "Wyślij ponownie") { onDismiss(); vm.retry(m) }
        }
        ActionRow(Icons.Rounded.Shortcut, "Przekaż dalej") {
            onDismiss()
            val attachments = m.media.map { com.blacksingularities.opensms.data.Attachment(it.uri, it.contentType, it.name ?: "zalacznik") }
            vm.push(Screen.Compose(text = m.body, attachments = attachments))
        }
        if (m.body.isNotEmpty()) {
            ActionRow(Icons.Rounded.Share, "Udostępnij") { onDismiss(); MediaActions.shareText(context, m.body) }
        }
        m.media.forEach { p ->
            ActionRow(Icons.Rounded.Save, "Zapisz " + (if (p.isImage) "zdjęcie" else if (p.isVideo) "film" else "plik"), p.name) {
                onDismiss()
                runCatching { MediaActions.save(context, p.uri, p.contentType, p.name) }
                    .onSuccess { vm.toast = "Zapisano w $it" }
                    .onFailure { vm.toast = it.message ?: "Nie udało się zapisać" }
            }
        }
        ActionRow(Icons.Rounded.Info, "Szczegóły") { onDismiss(); onInfo() }
        ActionRow(Icons.Rounded.Delete, "Usuń", danger = true) { onDismiss(); onDelete() }
    }
}

@Composable
private fun MessageInfo(m: Message, onDismiss: () -> Unit) {
    val context = LocalContext.current
    DarkDialog(title = "Szczegóły wiadomości", onDismiss = onDismiss, buttons = { PillButton("Zamknij", onClick = onDismiss) }) {
        InfoRow(Icons.Rounded.Info, "Rodzaj", if (m.isMms) "MMS" else "SMS")
        InfoRow(
            Icons.Rounded.Person,
            if (m.incoming) "Od" else "Do",
            Contacts.displayName(context, m.address) + if (Contacts.isKnown(context, m.address)) " (${formatNumber(m.address)})" else "",
        )
        if (m.incoming && m.dateSent > 0) InfoRow(Icons.Rounded.Info, "Wysłano", formatFull(m.dateSent))
        InfoRow(Icons.Rounded.Info, if (m.incoming) "Odebrano" else "Utworzono", formatFull(m.date))
        InfoRow(
            Icons.Rounded.Info,
            "Stan",
            when (m.box) {
                MsgBox.INBOX -> if (m.read) "Przeczytana" else "Nieprzeczytana"
                MsgBox.SENT -> when (m.delivery) {
                    Delivery.DELIVERED -> "Dostarczona"
                    Delivery.PENDING -> "Wysłana, czeka na raport doręczenia"
                    Delivery.FAILED -> "Nie doręczona"
                    Delivery.NONE -> "Wysłana"
                }
                MsgBox.OUTBOX -> "W trakcie wysyłania"
                MsgBox.FAILED -> "Błąd wysyłania"
                MsgBox.DRAFT -> "Szkic"
            },
        )
        if (!m.isMms && m.body.isNotEmpty()) {
            val calc = android.telephony.SmsMessage.calculateLength(m.body, false)
            InfoRow(Icons.Rounded.Info, "Długość", "${m.body.length} znaków · ${plural(calc[0], "część", "części", "części")} SMS")
        }
        if (m.isMms && m.sizeBytes > 0) InfoRow(Icons.Rounded.Info, "Rozmiar", formatSize(m.sizeBytes))
        if (m.media.isNotEmpty()) InfoRow(Icons.Rounded.InsertDriveFile, "Załączniki", m.media.joinToString(", ") { it.name ?: it.contentType })
    }
}
