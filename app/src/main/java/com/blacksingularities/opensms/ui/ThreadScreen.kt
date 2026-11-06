package com.blacksingularities.opensms.ui

import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.tr

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
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
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.Box as MsgBox
import com.blacksingularities.opensms.data.Contacts
import com.blacksingularities.opensms.data.canReplyTo
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
import com.blacksingularities.opensms.data.TimeMode
import com.blacksingularities.opensms.util.Emoticons
import com.blacksingularities.opensms.util.OneTimeCode
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.blacksingularities.opensms.util.Reactions
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
    var picked by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDeleteMany by remember { mutableStateOf<List<Message>>(emptyList()) }
    val selecting = picked.isNotEmpty()

    val title = remember(s.addresses, vm.conversations) { Contacts.title(context, s.addresses) }
    val single = s.addresses.size == 1
    val contact = remember(s.addresses) { if (single) Contacts.lookup(context, s.addresses[0]) else null }
    val threadId = s.threadId
    val item = vm.conversations.firstOrNull { it.conv.threadId == threadId }
    val messages = if (threadId != null && vm.openThreadId == threadId) vm.messages else emptyList()
    // Reakcje SMS („Reacted 👍 to …”) przypinamy do wiadomości, których dotyczą, zamiast pokazywać jako dymki.
    val showReactions = vm.settings.reactions
    val reactions = remember(messages, showReactions) {
        if (showReactions) {
            Reactions.attach(messages, { it.body }, { it.key }, { !it.incoming }, { it.media.isNotEmpty() || it.pendingUrl != null })
        } else {
            Reactions.Attached(emptyMap(), emptySet())
        }
    }
    val reversed = remember(messages, reactions) { messages.asReversed().filter { it.key !in reactions.hidden } }
    // Zaznaczone wiadomości mogły zniknąć (usunięcie, nowa strona) – trzymamy tylko istniejące.
    LaunchedEffect(messages) { picked = picked.filter { k -> messages.any { it.key == k } }.toSet() }
    BackHandler(enabled = selecting) { picked = emptySet() }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    // Powiększenie dymków: na żywo w trakcie gestu, zapisywane po podniesieniu palców.
    var liveZoom by remember { mutableFloatStateOf(vm.settings.bubbleZoom) }
    var zooming by remember { mutableStateOf(false) }
    var showZoom by remember { mutableStateOf(false) }
    LaunchedEffect(vm.settings.bubbleZoom) { if (!zooming) liveZoom = vm.settings.bubbleZoom }
    LaunchedEffect(zooming, liveZoom) {
        if (zooming) {
            showZoom = true
        } else {
            delay(900)
            showZoom = false
        }
    }
    val style = MessageStyle.from(vm.settings, liveZoom)

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

    Column(Modifier.fillMaxSize().background(Palette.Ink).screenPadding().imePadding()) {
        // Pasek zaznaczenia: kopiowanie, przekazanie i usunięcie kilku wiadomości naraz.
        if (selecting) {
            val chosen = messages.filter { it.key in picked }
            val joined = chosen.filter { it.body.isNotBlank() }.joinToString("\n\n") { it.body }
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.Rounded.Close, tr(R.string.anuluj_zaznaczenie), flat = true, onClick = { picked = emptySet() })
                Text(
                    tr(R.string.zaznaczono_1, picked.size),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                if (joined.isNotEmpty()) {
                    RoundIcon(Icons.Rounded.ContentCopy, tr(R.string.kopiuj), flat = true, onClick = {
                        MediaActions.copy(context, joined)
                        vm.toast = tr(R.string.skopiowano)
                        picked = emptySet()
                    })
                }
                RoundIcon(Icons.AutoMirrored.Rounded.Shortcut, tr(R.string.przekaz_dalej), flat = true, onClick = {
                    val attachments = chosen.flatMap { m -> m.media.map { com.blacksingularities.opensms.data.Attachment(it.uri, it.contentType, it.name ?: "attachment") } }.take(10)
                    picked = emptySet()
                    vm.push(Screen.Compose(text = joined, attachments = attachments))
                })
                RoundIcon(Icons.Rounded.Delete, tr(R.string.usun), flat = true, onClick = {
                    if (vm.settings.confirmDelete) confirmDeleteMany = chosen else vm.deleteMessages(chosen)
                    picked = emptySet()
                })
            }
        } else
        // Pasek rozmowy
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, tr(R.string.wstecz), flat = true, onClick = { vm.back() })
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(50)).clickable { details = true }.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(title, contact?.photo, size = 40.dp, group = !single, colored = vm.settings.colorAvatars)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            !single -> plural(s.addresses.size, tr(R.string.osoba), tr(R.string.osoby), tr(R.string.osob))
                            contact != null -> formatNumber(s.addresses[0])
                            else -> tr(R.string.nie_ma_w_kontaktach)
                        },
                        color = Palette.TextDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
            }
            if (single && s.addresses[0].any { it.isDigit() } && s.addresses[0].none { it.isLetter() }) {
                RoundIcon(Icons.Rounded.Call, tr(R.string.zadzwon), flat = true, onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + s.addresses[0]))) }
                })
            }
            if (threadId != null) RoundIcon(Icons.Rounded.MoreVert, tr(R.string.menu_rozmowy), flat = true, onClick = { menu = true })
        }

        // Wiadomości
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pinchToZoom(
                    enabled = vm.settings.pinchZoom,
                    onStart = { zooming = true },
                    onZoom = { z ->
                        val before = liveZoom
                        val next = (liveZoom * z).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        // Delikatne „kliknięcie” przy przejściu przez 100% – łatwo wrócić do zwykłego rozmiaru.
                        if ((before - 1f) * (next - 1f) < 0f && vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        liveZoom = next
                    },
                    onEnd = {
                        zooming = false
                        if (kotlin.math.abs(liveZoom - 1f) < 0.04f) liveZoom = 1f
                        vm.settings.bubbleZoom = liveZoom
                    },
                ),
        ) {
            val horizontalPadding = style.dp(10f).coerceIn(6.dp, 16.dp)
            val bubbleMax = ((maxWidth - horizontalPadding * 2) * style.width.fraction).coerceAtLeast(160.dp)
            if (threadId == null || (messages.isEmpty() && vm.openThreadId == threadId && item == null)) {
                EmptyState(
                    Icons.Rounded.Person,
                    tr(R.string.nowa_rozmowa),
                    tr(R.string.napisz_pierwsza_do_1, s.addresses.joinToString(", ") { formatNumber(it) }),
                    Modifier.align(Alignment.Center),
                )
            }
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 10.dp),
            ) {
                val lastOutgoing = messages.lastOrNull { !it.incoming }?.key
                itemsIndexed(reversed, key = { _, m -> m.key }) { i, m ->
                    val older = reversed.getOrNull(i + 1)
                    val newer = reversed.getOrNull(i - 1)
                    Column(Modifier.fillMaxWidth()) {
                        if (style.dayHeaders && (older == null || !isSameDay(older.date, m.date))) DayHeader(m.date, style)
                        val showSender = !single && m.incoming && (older == null || older.address != m.address || !older.incoming)
                        MessageBubble(
                            vm = vm,
                            m = m,
                            style = style,
                            maxWidth = bubbleMax,
                            showAvatar = !single && style.senderAvatars,
                            highlight = s.highlight,
                            showSender = showSender,
                            groupedWithOlder = groupedWith(m, older),
                            groupedWithNewer = groupedWith(m, newer),
                            showStatus = m.key == lastOutgoing || m.box == MsgBox.FAILED,
                            reactions = reactions.byTarget[m.key].orEmpty(),
                            selecting = selecting,
                            isPicked = m.key in picked,
                            onToggle = { picked = if (m.key in picked) picked - m.key else picked + m.key },
                            onLongClick = { if (selecting) picked = picked + m.key else selected = m },
                        )
                    }
                }
                if (vm.canLoadMore && reversed.isNotEmpty()) {
                    item(key = "more") {
                        Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                            PillButton(tr(R.string.wczytaj_starsze), primary = false, onClick = { vm.loadMore() })
                        }
                    }
                }
            }
            ZoomIndicator(
                visible = showZoom,
                zoom = liveZoom,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                onReset = {
                    liveZoom = 1f
                    vm.settings.bubbleZoom = 1f
                },
            )
        }

        if (s.addresses.isNotEmpty() && s.addresses.none { canReplyTo(it) }) NoReplyNote(s.addresses) else Composer(vm, s)
    }

    if (menu && threadId != null) {
        val pinned = vm.settings.isPinned(threadId)
        val archived = vm.settings.isArchived(threadId)
        val muted = vm.settings.isMuted(threadId)
        ActionSheet(title = title, onDismiss = { menu = false }) {
            if (single) {
                if (contact != null) {
                    ActionRow(Icons.Rounded.ContactPage, tr(R.string.pokaz_kontakt)) {
                        menu = false
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Contacts.contactUri(contact))) }
                    }
                } else {
                    ActionRow(Icons.Rounded.PersonAdd, tr(R.string.dodaj_do_kontaktow)) {
                        menu = false
                        // Nie każda aplikacja kontaktów obsługuje „dodaj lub edytuj” (np. OpenDial przyjmuje
                        // tylko „dodaj”), więc próbujemy kolejno, a gdy nic nie odpowie – mówimy o tym.
                        val phone = s.addresses[0]
                        val tries = listOf(
                            Intent(Intent.ACTION_INSERT_OR_EDIT).setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE),
                            Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI),
                        )
                        val opened = tries.any { i ->
                            runCatching {
                                context.startActivity(i.putExtra(ContactsContract.Intents.Insert.PHONE, phone))
                            }.isSuccess
                        }
                        if (!opened) vm.toast = tr(R.string.brak_aplikacji_kontaktow)
                    }
                }
            }
            ActionRow(Icons.Rounded.Info, tr(R.string.szczegoly_rozmowy)) { menu = false; details = true }
            ActionRow(Icons.Rounded.PushPin, if (pinned) tr(R.string.odepnij) else tr(R.string.przypnij)) {
                menu = false; vm.selection = setOf(threadId); vm.pinSelected()
            }
            ActionRow(if (muted) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff, if (muted) tr(R.string.wlacz_powiadomienia) else tr(R.string.wycisz_powiadomienia)) {
                menu = false; vm.toggleMute(threadId)
            }
            ActionRow(if (archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, if (archived) tr(R.string.przywroc_z_archiwum) else tr(R.string.archiwizuj)) {
                menu = false; vm.archive(setOf(threadId), !archived); if (!archived) vm.back()
            }
            ActionRow(Icons.Rounded.Block, tr(R.string.zablokuj), danger = true) { menu = false; confirmBlock = true }
            ActionRow(Icons.Rounded.Delete, tr(R.string.usun_rozmowe), danger = true) { menu = false; confirmDeleteThread = true }
        }
    }

    if (details) {
        DarkDialog(title = tr(R.string.szczegoly_rozmowy), onDismiss = { details = false }, buttons = { PillButton(tr(R.string.zamknij), onClick = { details = false }) }) {
            s.addresses.forEach { a ->
                val c = Contacts.lookup(context, a)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Avatar(c?.name ?: a, c?.photo, size = 40.dp, colored = vm.settings.colorAvatars)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(c?.name ?: formatNumber(a), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatNumber(a), color = Palette.TextDim, fontSize = 12.sp)
                    }
                    RoundIcon(Icons.Rounded.ContentCopy, tr(R.string.kopiuj_numer), size = 36.dp, flat = true, onClick = { MediaActions.copy(context, a, tr(R.string.numer)); vm.toast = tr(R.string.skopiowano_numer) })
                }
            }
            if (item != null) {
                Text(
                    tr(R.string.ostatnia_1, plural(item.conv.messageCount, tr(R.string.wiadomosc_2), tr(R.string.wiadomosci_2), tr(R.string.wiadomosci_2)), formatFull(item.conv.date)),
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                )
            }
        }
    }

    selected?.let { m ->
        MessageActions(
            vm,
            m,
            canReact = threadId != null && s.addresses.any { canReplyTo(it) },
            onReact = { emoji -> vm.sendQuick(s.addresses, reactionText(emoji, m.body), m.subId) },
            onSelect = { picked = setOf(m.key) },
            onDismiss = { selected = null },
            onInfo = { info = m },
            onDelete = { if (vm.settings.confirmDelete) confirmDeleteMsg = m else vm.deleteMessages(listOf(m)) },
        )
    }
    info?.let { m -> MessageInfo(m) { info = null } }
    if (confirmDeleteMany.isNotEmpty()) {
        val list = confirmDeleteMany
        ConfirmDialog(
            tr(R.string.usunac_wiadomosci_1, list.size),
            tr(R.string.wiadomosci_zostana_usuniete),
            tr(R.string.usun),
            onDismiss = { confirmDeleteMany = emptyList() },
        ) { vm.deleteMessages(list) }
    }
    confirmDeleteMsg?.let { m ->
        ConfirmDialog(tr(R.string.usunac_wiadomosc), tr(R.string.wiadomosc_zostanie_usunieta_z_telefonu), tr(R.string.usun), onDismiss = { confirmDeleteMsg = null }) { vm.deleteMessages(listOf(m)) }
    }
    if (confirmDeleteThread && threadId != null) {
        ConfirmDialog(tr(R.string.usunac_rozmowe), tr(R.string.rozmowa_trafi_do_kosza_na, AppViewModel.TRASH_DAYS), tr(R.string.usun), onDismiss = { confirmDeleteThread = false }) {
            vm.trashThreads(setOf(threadId))
        }
    }
    if (confirmBlock) {
        ConfirmDialog(
            tr(R.string.zablokowac_1, title),
            tr(R.string.nowe_wiadomosci_od_tego_nadawcy),
            tr(R.string.zablokuj),
            onDismiss = { confirmBlock = false },
        ) {
            vm.blockAddresses(s.addresses, setOfNotNull(threadId))
            vm.back()
        }
    }
}

/** Nadawca alfanumeryczny – odpowiedź nie ma dokąd trafić, więc zamiast pola tekstowego jest wyjaśnienie. */
@Composable
private fun NoReplyNote(addresses: List<String>) {
    BottomNotice(
        Icons.Rounded.Block,
        tr(R.string.nie_mozna_odpowiedziec),
        tr(R.string.nadawca_z_nazwy_1, addresses.joinToString(", ")),
    )
}

/**
 * Komunikat w miejscu pola wpisywania: na całą szerokość, bez osobnej karty –
 * tło wyrasta z przezroczystości u góry, nagłówek i opis wyśrodkowane.
 */
@Composable
internal fun BottomNotice(icon: ImageVector, title: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to Palette.PanelHigh.copy(alpha = 0.7f), 1f to Palette.PanelHigh))
            .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text,
            color = Palette.TextDim,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Plakietka z bieżącym powiększeniem – widoczna w trakcie gestu; dotknięcie wraca do 100%. */
@Composable
internal fun ZoomIndicator(visible: Boolean, zoom: Float, modifier: Modifier, onReset: () -> Unit) {
    AnimatedVisibility(visible, modifier = modifier, enter = fadeIn() + scaleIn(initialScale = 0.85f), exit = fadeOut() + scaleOut(targetScale = 0.9f)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Palette.PanelHigher)
                .clickable(onClick = onReset)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ZoomIn, null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("${(zoom * 100).roundToInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (zoom != 1f) {
                Text("  ·  " + tr(R.string.dotknij_100), color = Palette.TextDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DayHeader(t: Long, style: MessageStyle) {
    Box(Modifier.fillMaxWidth().padding(top = style.dp(14f), bottom = style.dp(8f)), contentAlignment = Alignment.Center) {
        Text(
            formatDayHeader(t),
            color = Palette.TextDim,
            fontSize = style.smallSp(12f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        )
    }
}

/** Wiadomości tego samego nadawcy, wysłane w odstępie do dwóch minut tego samego dnia, tworzą serię. */
private fun groupedWith(m: Message, other: Message?): Boolean =
    other != null &&
        other.incoming == m.incoming &&
        other.address == m.address &&
        kotlin.math.abs(other.date - m.date) < 120_000 &&
        isSameDay(other.date, m.date)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    vm: AppViewModel,
    m: Message,
    style: MessageStyle,
    maxWidth: Dp,
    showAvatar: Boolean,
    highlight: String?,
    showSender: Boolean,
    groupedWithOlder: Boolean,
    groupedWithNewer: Boolean,
    showStatus: Boolean,
    reactions: List<Reactions.Pick> = emptyList(),
    selecting: Boolean = false,
    isPicked: Boolean = false,
    onToggle: () -> Unit = {},
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    val out = !m.incoming
    val failed = m.box == MsgBox.FAILED
    val colors = style.colors(out, failed)
    val shape = style.shape(out, groupedWithOlder, groupedWithNewer)
    // Emotikony tekstowe pokazujemy jako emoji; w bazie zostaje oryginalna treść wiadomości.
    val body = remember(m.body, style.emoticons) { if (style.emoticons) Emoticons.apply(m.body) else m.body }
    val onlyEmoji = style.bigEmoji && body.isNotBlank() && m.media.isEmpty() && body.length <= 12 && body.none { it.isLetterOrDigit() }
    val code = remember(m.body, m.incoming) { if (m.incoming) OneTimeCode.find(m.body) else null }
    var copied by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }
    val copyCode: () -> Unit = {
        if (code != null) {
            MediaActions.copy(context, code, tr(R.string.kod))
            if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            copied = true
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = if (groupedWithNewer) style.dp(style.spacing.joined) else style.dp(style.spacing.gap))
            // Tło bez przycinania – znaczek kopiowania kodu może wystawać poza dymek.
            .background(if (isPicked) Color.White.copy(alpha = 0.10f) else Color.Transparent, RoundedCornerShape(14.dp))
            .then(
                if (selecting) {
                    Modifier
                        .clickable(onClick = onToggle)
                        .semantics { selected = isPicked }
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start,
    ) {
        if (selecting) {
            Icon(
                if (isPicked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null,
                tint = if (isPicked) Color.White else Palette.TextFaint,
                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp, end = 8.dp).size(22.dp),
            )
            if (out) Spacer(Modifier.weight(1f))
        }
        // Awatar nadawcy w rozmowach grupowych: przy ostatnim dymku serii, wcześniej puste miejsce.
        if (showAvatar && m.incoming) {
            val avatarSize = style.dp(28f).coerceIn(20.dp, 44.dp)
            Box(Modifier.padding(end = 6.dp, bottom = style.dp(20f).coerceAtMost(28.dp)).size(avatarSize)) {
                if (!groupedWithNewer) {
                    val c = remember(m.address) { Contacts.lookup(context, m.address) }
                    Avatar(c?.name ?: m.address, c?.photo, size = avatarSize, colored = vm.settings.colorAvatars)
                }
            }
        }
        Column(horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
            if (showSender) {
                Text(
                    Contacts.displayName(context, m.address),
                    color = Palette.TextDim,
                    fontSize = style.smallSp(12f),
                    modifier = Modifier.padding(start = style.dp(12f), bottom = 2.dp),
                )
            }
            // Dymek jest przycinany do kształtu, więc znaczek kopiowania kodu rysujemy w nakładce
            // nad nim — dzięki temu może wystawać poza krawędź dymka.
            var bubbleOrigin by remember { mutableStateOf(Offset.Zero) }
            var codeAnchor by remember(code) { mutableStateOf<Offset?>(null) }
            Box(Modifier.onGloballyPositioned { bubbleOrigin = it.positionInRoot() }) {
            Column(
                Modifier
                    .widthIn(max = maxWidth)
                    .clip(shape)
                    .background(if (onlyEmoji) Color.Transparent else colors.background)
                    .then(if (colors.outline != null && !onlyEmoji) Modifier.border(1.dp, colors.outline, shape) else Modifier)
                    .combinedClickable(
                        onClickLabel = if (failed) tr(R.string.wyslij_ponownie) else tr(R.string.pokaz_szczegoly),
                        onLongClickLabel = tr(R.string.opcje_wiadomosci),
                        onClick = {
                            when {
                                selecting -> onToggle()
                                failed -> vm.retry(m)
                                else -> expanded = !expanded
                            }
                        },
                        onLongClick = {
                            if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        },
                    )
                    // Czytnik ekranu: kto i kiedy, zanim przeczyta treść dymka.
                    .semantics {
                        contentDescription = (if (out) tr(R.string.ty) else Contacts.displayName(context, m.address)) + ", " + formatTime(m.date)
                    },
            ) {
                if (m.pendingUrl != null) {
                    PendingMms(vm, m)
                }
                m.media.forEach { part -> MediaPart(vm, m, part, style, maxWidth) }
                if (m.subject != null) {
                    Text(
                        m.subject,
                        color = colors.content,
                        fontWeight = FontWeight.Bold,
                        fontSize = style.sp(15f),
                        modifier = Modifier.padding(start = style.dp(14f), end = style.dp(14f), top = style.dp(9f)),
                    )
                }
                if (body.isNotEmpty()) {
                    CodeAwareText(
                        text = body,
                        annotated = linkify(body, highlight, colors.lightBackground, code, copied, copyCode),
                        code = code,
                        style = style,
                        color = colors.content,
                        fontSize = if (onlyEmoji) style.sp(40f) else style.sp(16f),
                        lineHeight = if (onlyEmoji) style.sp(46f) else style.sp(21f),
                        onAnchor = { codeAnchor = it },
                    )
                }
            }
            val anchor = codeAnchor
            if (code != null && anchor != null) {
                CopyCodeBadge(
                    center = anchor - bubbleOrigin,
                    copied = copied,
                    style = style,
                    dark = colors.lightBackground,
                    bubble = colors.background,
                    onCopy = copyCode,
                )
            }
            }
            if (reactions.isNotEmpty()) ReactionChips(reactions, style)
            val showTime = when (style.timeMode) {
                TimeMode.EVERY -> true
                TimeMode.GROUP -> !groupedWithNewer
                TimeMode.TAP -> false
            }
            if (expanded || showStatus || showTime || m.box == MsgBox.OUTBOX) {
                StatusLine(vm, m, expanded || showStatus, style)
            }
        }
    }
}

/** Reakcje pod dymkiem: emoji z licznikiem; własne reakcje są obwiedzione. */
@Composable
private fun ReactionChips(reactions: List<Reactions.Pick>, style: MessageStyle) {
    val groups = reactions.groupBy { it.emoji }
    val description = groups.entries.joinToString(", ") { (e, l) -> if (l.size > 1) "$e ${l.size}" else e }
    Row(
        Modifier
            .padding(horizontal = style.dp(6f).coerceAtMost(10.dp), vertical = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = tr(R.string.reakcje_1, description) },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        groups.forEach { (emoji, list) ->
            val mine = list.any { it.mine }
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Palette.PanelHigher)
                    .then(if (mine) Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(50)) else Modifier)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, fontSize = style.smallSp(13f))
                if (list.size > 1) Text(" ${list.size}", color = Palette.TextDim, fontSize = style.smallSp(11f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Treść dymka; przy kodzie jednorazowym zgłasza (w układzie okna) punkt prawego górnego rogu kodu,
 * w którym nakładka dymka rysuje znaczek kopiowania.
 */
@Composable
private fun CodeAwareText(
    text: String,
    annotated: AnnotatedString,
    code: String?,
    style: MessageStyle,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    onAnchor: (Offset?) -> Unit,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val range = remember(text, code) { code?.let { findCodeRange(text, it) } }
    Box(Modifier.padding(horizontal = style.dp(14f), vertical = style.dp(9f))) {
        Text(
            annotated,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            onTextLayout = { layout = it },
            modifier = Modifier.onGloballyPositioned { coords ->
                val l = layout
                if (range == null || l == null || range.last >= l.layoutInput.text.length) {
                    onAnchor(null)
                } else {
                    val box = l.getBoundingBox(range.last)
                    val top = l.getLineTop(l.getLineForOffset(range.last))
                    onAnchor(coords.localToRoot(Offset(box.right, top)))
                }
            },
        )
    }
}

/**
 * Znaczek kopiowania kodu: większy cel dotyku (niewidoczna otoczka), delikatny cień
 * i obwódka; po skopiowaniu zmienia się w zielony ✓. Środek leży w [center] (względem dymka),
 * więc znaczek może wystawać poza krawędź dymka.
 */
@Composable
private fun CopyCodeBadge(
    center: Offset,
    copied: Boolean,
    style: MessageStyle,
    dark: Boolean,
    bubble: Color,
    onCopy: () -> Unit,
) {
    val d = style.dp(24f).coerceIn(22.dp, 32.dp)
    val touch = d + 14.dp
    val half = with(LocalDensity.current) { (touch / 2).roundToPx() }
    val ring by animateColorAsState(
        if (copied) Palette.Ok.copy(alpha = 0.7f) else if (dark) Color(0x2A000000) else Color.White.copy(alpha = 0.22f),
        label = "codeRing",
    )
    val fill by animateColorAsState(if (copied) Palette.Ok.copy(alpha = 0.18f).compositeOver(bubble) else bubble, label = "codeFill")
    val ink = if (copied) Palette.Ok else if (dark) Palette.Ink.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
    val press by animateFloatAsState(if (copied) 1.08f else 1f, label = "codeScale")
    Box(
        Modifier
            .offset { IntOffset(center.x.roundToInt() - half + half / 5, center.y.roundToInt() - half) }
            .size(touch)
            .clip(CircleShape)
            .clickable(onClickLabel = tr(R.string.kopiuj_kod), onClick = onCopy),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .graphicsLayer { scaleX = press; scaleY = press }
                .size(d)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(copied, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() }, label = "codeIcon") { done ->
                Icon(
                    if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    if (done) tr(R.string.skopiowano) else tr(R.string.kopiuj_kod),
                    tint = ink,
                    modifier = Modifier.size(d * 0.55f),
                )
            }
        }
    }
}

/**
 * Próbka rozmowy do podglądu w ustawieniach – te same kształty, kolory i skala co w rozmowie.
 * Pokazuje każde ustawienie wyglądu: szerokość (długa wiadomość), odstępy, godzinę, nagłówek dnia,
 * awatary w grupie, duże emoji, emotikony i znaczek kopiowania kodu.
 */
@Composable
fun MessageStylePreview(style: MessageStyle, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.Ink).padding(10.dp)) {
        val max = maxWidth * style.width.fraction
        val now = remember { System.currentTimeMillis() }
        Column(Modifier.fillMaxWidth()) {
            if (style.dayHeaders) DayHeader(now, style)
            PreviewBubble(style, max, tr(R.string.ola), tr(R.string.czesc_wpadniesz_dzis_wieczorem_robimy), out = false, older = false, newer = true, time = "18:02")
            PreviewBubble(style, max, tr(R.string.ola), tr(R.string.bedzie_tez_kuba), out = false, older = true, newer = false, time = "18:02")
            PreviewBubble(style, max, null, tr(R.string.jasne_bede_kolo_19_wezme), out = true, older = false, newer = true, time = "18:05")
            PreviewBubble(style, max, null, "👍🎉", out = true, older = true, newer = false, time = "18:05")
            PreviewCodeBubble(style, max)
        }
    }
}

/** Dymek z kodem w podglądzie – ten sam wygląd co w rozmowie, łącznie z wystającym znaczkiem kopiowania. */
@Composable
private fun PreviewCodeBubble(style: MessageStyle, max: Dp) {
    val c = style.colors(false)
    val sh = style.shape(false, false, false)
    val text = tr(R.string.twoj_kod_logowania_482913)
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1800); copied = false } }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var anchor by remember { mutableStateOf<Offset?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = style.dp(style.spacing.gap))) {
        Box(Modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
            Box(
                Modifier
                    .widthIn(max = max)
                    .clip(sh)
                    .background(c.background)
                    .then(if (c.outline != null) Modifier.border(1.dp, c.outline, sh) else Modifier),
            ) {
                CodeAwareText(
                    text = text,
                    annotated = linkify(text, null, c.lightBackground, "482913", copied) { copied = true },
                    code = "482913",
                    style = style,
                    color = c.content,
                    fontSize = style.sp(16f),
                    lineHeight = style.sp(21f),
                    onAnchor = { anchor = it },
                )
            }
            anchor?.let { CopyCodeBadge(it - origin, copied, style, c.lightBackground, c.background) { copied = true } }
        }
        if (style.timeMode != TimeMode.TAP) PreviewTime(style, "18:07", out = false)
    }
}

@Composable
private fun PreviewBubble(style: MessageStyle, max: Dp, sender: String?, raw: String, out: Boolean, older: Boolean, newer: Boolean, time: String) {
    val c = style.colors(out)
    val sh = style.shape(out, older, newer)
    val text = remember(raw, style.emoticons) { if (style.emoticons) Emoticons.apply(raw) else raw }
    val onlyEmoji = style.bigEmoji && text.length <= 12 && text.none { it.isLetterOrDigit() }
    val avatar = style.senderAvatars && sender != null && !out
    Row(
        Modifier.fillMaxWidth().padding(top = if (older) style.dp(style.spacing.joined) else style.dp(style.spacing.gap)),
        horizontalArrangement = if (out) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        val showTime = when (style.timeMode) {
            TimeMode.EVERY -> true
            TimeMode.GROUP -> !newer
            TimeMode.TAP -> false
        }
        if (avatar) {
            val size = style.dp(28f).coerceIn(20.dp, 44.dp)
            Box(Modifier.padding(end = 6.dp, bottom = if (showTime) style.dp(20f).coerceAtMost(28.dp) else 0.dp).size(size)) {
                if (!newer) Avatar(sender!!, null, size = size, colored = true)
            }
        }
        Column(horizontalAlignment = if (out) Alignment.End else Alignment.Start) {
            if (avatar && !older) {
                Text(sender!!, color = Palette.TextDim, fontSize = style.smallSp(12f), modifier = Modifier.padding(start = style.dp(12f), bottom = 2.dp))
            }
            Text(
                text,
                color = c.content,
                fontSize = if (onlyEmoji) style.sp(40f) else style.sp(16f),
                lineHeight = if (onlyEmoji) style.sp(46f) else style.sp(21f),
                modifier = Modifier
                    .widthIn(max = max)
                    .clip(sh)
                    .background(if (onlyEmoji) Color.Transparent else c.background)
                    .then(if (c.outline != null && !onlyEmoji) Modifier.border(1.dp, c.outline, sh) else Modifier)
                    .padding(horizontal = style.dp(14f), vertical = style.dp(9f)),
            )
            if (showTime) PreviewTime(style, time, out)
        }
    }
}

@Composable
private fun PreviewTime(style: MessageStyle, time: String, out: Boolean) {
    Text(
        if (out) tr(R.string.dostarczono_1, time) else time,
        color = Palette.TextFaint,
        fontSize = style.smallSp(11f),
        modifier = Modifier.padding(horizontal = style.dp(8f).coerceAtMost(12.dp), vertical = 3.dp),
    )
}

@Composable
private fun StatusLine(vm: AppViewModel, m: Message, full: Boolean, style: MessageStyle) {
    val sim = if (vm.settings.showSimBadge && vm.sims.size > 1) vm.sims.firstOrNull { it.subId == m.subId }?.let { "SIM ${it.slot + 1}" } else null
    val (text, color) = when {
        m.incoming -> formatTime(m.date) to Palette.TextFaint
        m.box == MsgBox.FAILED -> tr(R.string.nie_wyslano_dotknij_aby_ponowic) to Palette.Record
        m.box == MsgBox.OUTBOX && vm.isFailedOrStuck(m) -> tr(R.string.wysylanie_trwa_dlugo_przytrzymaj_aby) to Palette.Record
        m.box == MsgBox.OUTBOX -> tr(R.string.wysylanie_2) to Palette.TextFaint
        !full -> formatTime(m.date) to Palette.TextFaint
        m.delivery == Delivery.DELIVERED -> tr(R.string.dostarczono_1, formatTime(m.date)) to Palette.TextDim
        m.delivery == Delivery.FAILED -> tr(R.string.nie_doreczono_1, formatTime(m.date)) to Palette.Record
        m.delivery == Delivery.PENDING -> tr(R.string.wyslano_1, formatTime(m.date)) to Palette.TextFaint
        else -> tr(R.string.wyslano_1, formatTime(m.date)) to Palette.TextFaint
    }
    Row(Modifier.padding(horizontal = style.dp(8f).coerceAtMost(12.dp), vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        if (m.box == MsgBox.FAILED) Icon(Icons.Rounded.Error, null, tint = Palette.Record, modifier = Modifier.size(13.dp).padding(end = 2.dp))
        Text(listOfNotNull(text, sim, if (m.isMms && full) "MMS" else null).joinToString(" · "), color = color, fontSize = style.smallSp(11f))
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
            Text(tr(R.string.wiadomosc_mms), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                if (downloading) tr(R.string.pobieranie) else listOfNotNull(m.sizeBytes.takeIf { it > 0 }?.let(::formatSize), tr(R.string.dotknij_aby_pobrac)).joinToString(" · "),
                color = Palette.TextDim,
                fontSize = 12.sp,
            )
        }
        if (!downloading) PillButton(tr(R.string.pobierz), onClick = { vm.downloadMms(m) })
    }
}

@Composable
private fun MediaPart(vm: AppViewModel, m: Message, part: MmsPart, style: MessageStyle, maxWidth: Dp) {
    val context = LocalContext.current
    when {
        part.isImage || part.isVideo -> {
            val bmp = rememberBitmap(part.uri, 900, video = part.isVideo)
            Box(
                Modifier
                    .padding(3.dp)
                    .widthIn(min = 120.dp, max = maxWidth - 6.dp)
                    .heightIn(min = 80.dp, max = style.dp(360f).coerceIn(200.dp, 640.dp))
                    .clip(RoundedCornerShape((style.dp(style.radius) - 3.dp).coerceAtLeast(3.dp)))
                    .background(Palette.PanelHigher)
                    .clickable { vm.push(Screen.Media(part.uri, part.contentType, part.name)) },
                contentAlignment = Alignment.Center,
            ) {
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), part.name, contentScale = ContentScale.Fit, modifier = Modifier.widthIn(max = maxWidth - 6.dp))
                } else {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                if (part.isVideo) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, tr(R.string.odtworz), tint = Color.White, modifier = Modifier.size(32.dp))
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
                        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
                    },
                    null,
                    tint = if (out) Palette.Ink else Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Column(Modifier.padding(start = 10.dp).widthIn(max = (maxWidth - 70.dp).coerceAtLeast(100.dp))) {
                    Text(
                        when {
                            part.isAudio -> tr(R.string.nagranie)
                            part.isVcard -> tr(R.string.wizytowka)
                            else -> tr(R.string.plik)
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
private fun linkify(
    text: String,
    highlight: String?,
    dark: Boolean,
    code: String? = null,
    copied: Boolean = false,
    onCode: () -> Unit = {},
): AnnotatedString {
    data class Span(val start: Int, val end: Int, val url: String)
    val spans = ArrayList<Span>()
    // Kod w treści: monospace na delikatnym tle, dotknięcie kopiuje. Linki nie mogą na niego nachodzić.
    val codeRange = code?.let { findCodeRange(text, it) }
    if (codeRange != null) spans += Span(codeRange.first, codeRange.last + 1, "")
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
    spans.removeAll { it.url.isEmpty() }
    spans.sortBy { it.start }
    val linkColor = if (dark) Palette.Ink else Color.White
    val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        background = when {
            copied -> Palette.Ok.copy(alpha = 0.28f)
            dark -> Color(0x14000000)
            else -> Color.White.copy(alpha = 0.10f)
        },
    )
    return buildAnnotatedString {
        var pos = 0
        for (sp in spans) {
            append(text.substring(pos, sp.start))
            withLink(LinkAnnotation.Url(sp.url, styles)) { append(text.substring(sp.start, sp.end)) }
            pos = sp.end
        }
        append(text.substring(pos))
        if (codeRange != null) {
            addStyle(codeStyle, codeRange.first, codeRange.last + 1)
            addLink(LinkAnnotation.Clickable("code", linkInteractionListener = { onCode() }), codeRange.first, codeRange.last + 1)
        }
        if (!highlight.isNullOrBlank()) {
            var i = text.indexOf(highlight, 0, ignoreCase = true)
            while (i >= 0) {
                addStyle(SpanStyle(background = if (dark) Color(0x33000000) else Color.White.copy(alpha = 0.35f), fontWeight = FontWeight.Bold), i, i + highlight.length)
                i = text.indexOf(highlight, i + highlight.length, ignoreCase = true)
            }
        }
    }
}

/** Miejsce kodu w treści – także zapisanego ze spacją lub myślnikiem („482 913”, „482-913”). */
private fun findCodeRange(text: String, code: String): IntRange? {
    val i = text.indexOf(code)
    if (i >= 0) return i until i + code.length
    if (code.length == 6 && code.all { it.isDigit() }) {
        val r = Regex(Regex.escape(code.substring(0, 3)) + "[- ]" + Regex.escape(code.substring(3))).find(text)
        if (r != null) return r.range
    }
    return null
}

@Composable
private fun MessageActions(
    vm: AppViewModel,
    m: Message,
    canReact: Boolean,
    onReact: (String) -> Unit,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
    onInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val code = remember(m.body) { OneTimeCode.find(m.body) }
    ActionSheet(title = m.body.take(120).ifEmpty { if (m.media.isNotEmpty()) tr(R.string.zalacznik_mms) else null }, onDismiss = onDismiss) {
        if (canReact && m.body.isNotBlank() && m.box != MsgBox.FAILED) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                REACTION_EMOJI.forEach { emoji ->
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Palette.PanelHigher)
                            .clickable(onClickLabel = tr(R.string.zareaguj_1, emoji)) { onDismiss(); onReact(emoji) }
                            .semantics { contentDescription = tr(R.string.zareaguj_1, emoji) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }
            }
        }
        ActionRow(Icons.Rounded.CheckCircle, tr(R.string.zaznacz)) { onDismiss(); onSelect() }
        if (m.body.isNotEmpty()) {
            ActionRow(Icons.Rounded.ContentCopy, tr(R.string.kopiuj_tekst)) { onDismiss(); MediaActions.copy(context, m.body); vm.toast = tr(R.string.skopiowano) }
        }
        if (code != null) {
            ActionRow(Icons.Rounded.Key, tr(R.string.kopiuj_kod_1, code)) { onDismiss(); MediaActions.copy(context, code, tr(R.string.kod)); vm.toast = tr(R.string.skopiowano_kod) }
        }
        if (m.box == MsgBox.FAILED || vm.isFailedOrStuck(m)) {
            ActionRow(Icons.Rounded.Refresh, tr(R.string.wyslij_ponownie)) { onDismiss(); vm.retry(m) }
        }
        ActionRow(Icons.AutoMirrored.Rounded.Shortcut, tr(R.string.przekaz_dalej)) {
            onDismiss()
            val attachments = m.media.map { com.blacksingularities.opensms.data.Attachment(it.uri, it.contentType, it.name ?: "zalacznik") }
            vm.push(Screen.Compose(text = m.body, attachments = attachments))
        }
        if (m.body.isNotEmpty()) {
            ActionRow(Icons.Rounded.Share, tr(R.string.udostepnij)) { onDismiss(); MediaActions.shareText(context, m.body) }
        }
        m.media.forEach { p ->
            ActionRow(Icons.Rounded.Save, tr(R.string.zapisz_2) + (if (p.isImage) tr(R.string.zdjecie_3) else if (p.isVideo) tr(R.string.film_lc) else tr(R.string.plik_lc)), p.name) {
                onDismiss()
                runCatching { MediaActions.save(context, p.uri, p.contentType, p.name) }
                    .onSuccess { vm.toast = tr(R.string.zapisano_w_1, it) }
                    .onFailure { vm.toast = it.message ?: tr(R.string.nie_udalo_sie_zapisac) }
            }
        }
        ActionRow(Icons.Rounded.Info, tr(R.string.szczegoly)) { onDismiss(); onInfo() }
        ActionRow(Icons.Rounded.Delete, tr(R.string.usun), danger = true) { onDismiss(); onDelete() }
    }
}

private val REACTION_EMOJI = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

/** Treść reakcji w formacie rozpoznawanym przez inne aplikacje (cytat przycięty jak w Google Messages). */
private fun reactionText(emoji: String, body: String): String {
    val oneLine = body.replace('\n', ' ').trim()
    val quote = if (oneLine.length > 60) oneLine.take(60).trimEnd() + "…" else oneLine
    return tr(R.string.reakcja_tekst, emoji, quote)
}

@Composable
private fun MessageInfo(m: Message, onDismiss: () -> Unit) {
    val context = LocalContext.current
    DarkDialog(title = tr(R.string.szczegoly_wiadomosci), onDismiss = onDismiss, buttons = { PillButton(tr(R.string.zamknij), onClick = onDismiss) }) {
        InfoRow(Icons.Rounded.Info, tr(R.string.rodzaj), if (m.isMms) "MMS" else "SMS")
        InfoRow(
            Icons.Rounded.Person,
            if (m.incoming) tr(R.string.od) else tr(R.string.do_3),
            Contacts.displayName(context, m.address) + if (Contacts.isKnown(context, m.address)) " (${formatNumber(m.address)})" else "",
        )
        if (m.incoming && m.dateSent > 0) InfoRow(Icons.Rounded.Info, tr(R.string.wyslano), formatFull(m.dateSent))
        InfoRow(Icons.Rounded.Info, if (m.incoming) tr(R.string.odebrano) else tr(R.string.utworzono), formatFull(m.date))
        InfoRow(
            Icons.Rounded.Info,
            tr(R.string.stan),
            when (m.box) {
                MsgBox.INBOX -> if (m.read) tr(R.string.przeczytana) else tr(R.string.nieprzeczytana)
                MsgBox.SENT -> when (m.delivery) {
                    Delivery.DELIVERED -> tr(R.string.dostarczona)
                    Delivery.PENDING -> tr(R.string.wyslana_czeka_na_raport_doreczenia)
                    Delivery.FAILED -> tr(R.string.nie_doreczona)
                    Delivery.NONE -> tr(R.string.wyslana)
                }
                MsgBox.OUTBOX -> tr(R.string.w_trakcie_wysylania)
                MsgBox.FAILED -> tr(R.string.blad_wysylania)
                MsgBox.DRAFT -> tr(R.string.szkic_2)
            },
        )
        if (!m.isMms && m.body.isNotEmpty()) {
            val calc = android.telephony.SmsMessage.calculateLength(m.body, false)
            InfoRow(Icons.Rounded.Info, tr(R.string.dlugosc), tr(R.string.znakow_czesci_1, m.body.length, plural(calc[0], tr(R.string.czesc_one), tr(R.string.czesc_few), tr(R.string.czesc_many))))
        }
        if (m.isMms && m.sizeBytes > 0) InfoRow(Icons.Rounded.Info, tr(R.string.rozmiar), formatSize(m.sizeBytes))
        if (m.media.isNotEmpty()) InfoRow(Icons.AutoMirrored.Rounded.InsertDriveFile, tr(R.string.zalaczniki_2), m.media.joinToString(", ") { it.name ?: it.contentType })
    }
}
