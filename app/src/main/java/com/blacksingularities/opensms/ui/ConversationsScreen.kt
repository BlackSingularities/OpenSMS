package com.blacksingularities.opensms.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MarkChatRead
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.Message
import com.blacksingularities.opensms.data.SwipeAction
import com.blacksingularities.opensms.ui.components.ActionRow
import com.blacksingularities.opensms.ui.components.ActionSheet
import com.blacksingularities.opensms.ui.components.Avatar
import com.blacksingularities.opensms.ui.components.ConfirmDialog
import com.blacksingularities.opensms.ui.components.EmptyState
import com.blacksingularities.opensms.ui.components.RoundIcon
import com.blacksingularities.opensms.ui.components.TabChip
import com.blacksingularities.opensms.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ConversationsScreen(vm: AppViewModel) {
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Set<Long>?>(null) }
    var confirmBlock by remember { mutableStateOf<Set<Long>?>(null) }
    val listState = rememberLazyListState()
    val visible = vm.visibleConversations()

    Box(Modifier.fillMaxSize().background(Palette.Ink).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
            when {
                vm.selection.isNotEmpty() -> SelectionBar(vm, onDelete = { confirmDelete = vm.selection }, onBlock = { confirmBlock = vm.selection })
                vm.searching -> SearchBar(vm)
                else -> Header(vm, onMenu = { menu = true })
            }
            if (vm.searching) {
                SearchResults(vm)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                ) {
                    item(key = "banner") { UpdateBanner(vm, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
                    item(key = "filters") { Filters(vm) }
                    if (vm.loaded && visible.isEmpty()) {
                        item(key = "empty") {
                            when (vm.filter) {
                                Filter.ALL -> EmptyState(Icons.Rounded.Forum, "Brak rozmów", "Napisz pierwszą wiadomość przyciskiem na dole ekranu.")
                                Filter.UNREAD -> EmptyState(Icons.Rounded.DoneAll, "Wszystko przeczytane", "Nie ma nieprzeczytanych wiadomości.")
                                Filter.PINNED -> EmptyState(Icons.Rounded.PushPin, "Brak przypiętych", "Przytrzymaj rozmowę i wybierz pinezkę, żeby była zawsze na górze.")
                                Filter.ARCHIVED -> EmptyState(Icons.Rounded.Inbox, "Archiwum jest puste", "Zarchiwizowane rozmowy znikają z głównej listy, ale nowe wiadomości dalej przychodzą.")
                            }
                        }
                    }
                    items(visible, key = { it.conv.threadId }) { item ->
                        SwipeRow(vm, item) {
                            ConversationRow(
                                vm = vm,
                                item = item,
                                selected = item.conv.threadId in vm.selection,
                                onClick = {
                                    if (vm.selection.isNotEmpty()) vm.toggleSelect(item.conv.threadId) else vm.openThread(item.conv.threadId)
                                },
                                onLongClick = { vm.toggleSelect(item.conv.threadId) },
                            )
                        }
                    }
                }
            }
        }

        // Nowa wiadomość – biała pigułka jak główny przycisk w OpenPDF.
        AnimatedVisibility(
            visible = vm.selection.isEmpty() && !vm.searching,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
        ) {
            val expanded = listState.firstVisibleItemIndex < 2
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable { vm.push(Screen.Compose()) }
                    .padding(horizontal = if (expanded) 20.dp else 17.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Edit, "Nowa wiadomość", tint = Palette.Ink, modifier = Modifier.size(24.dp))
                AnimatedVisibility(visible = expanded) {
                    Text("Nowa wiadomość", color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }

    if (menu) {
        ActionSheet(title = null, onDismiss = { menu = false }) {
            ActionRow(Icons.Rounded.DoneAll, "Oznacz wszystkie jako przeczytane") { menu = false; vm.markAllRead() }
            ActionRow(Icons.Rounded.Archive, "Archiwum", "Rozmowy ukryte z głównej listy") { menu = false; vm.filter = Filter.ARCHIVED }
            ActionRow(Icons.Rounded.Schedule, "Zaplanowane wiadomości", plural(vm.settings.scheduled.size, "wiadomość", "wiadomości", "wiadomości")) {
                menu = false; vm.push(Screen.SettingsScreen(SettingsPage.SCHEDULED))
            }
            ActionRow(Icons.Rounded.Block, "Zablokowane", "Numery, słowa kluczowe i zatrzymane wiadomości") {
                menu = false; vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED))
            }
            ActionRow(Icons.Rounded.Settings, "Ustawienia") { menu = false; vm.push(Screen.SettingsScreen()) }
        }
    }
    confirmDelete?.let { ids ->
        ConfirmDialog(
            title = if (ids.size == 1) "Usunąć rozmowę?" else "Usunąć rozmowy (${ids.size})?",
            text = "Wszystkie wiadomości z ${if (ids.size == 1) "tej rozmowy" else "tych rozmów"} zostaną trwale usunięte z telefonu.",
            confirm = "Usuń",
            onDismiss = { confirmDelete = null },
            onConfirm = { vm.deleteThreads(ids) },
        )
    }
    confirmBlock?.let { ids ->
        val addresses = vm.conversations.filter { it.conv.threadId in ids }.flatMap { it.conv.addresses }.distinct()
        ConfirmDialog(
            title = "Zablokować?",
            text = "Nowe wiadomości od: ${addresses.joinToString(", ")} nie będą pokazywane. Rozmowy trafią do archiwum. Zablokowane wiadomości znajdziesz w ustawieniach.",
            confirm = "Zablokuj",
            onDismiss = { confirmBlock = null },
            onConfirm = { vm.blockAddresses(addresses, ids) },
        )
    }
}

@Composable
private fun Header(vm: AppViewModel, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (vm.filter == Filter.ARCHIVED) "Archiwum" else "Wiadomości", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            val unread = vm.unreadCount
            Text(
                if (unread > 0) plural(unread, "nieprzeczytana", "nieprzeczytane", "nieprzeczytanych") else "Wszystko przeczytane",
                fontSize = 13.sp,
                color = Palette.TextDim,
            )
        }
        RoundIcon(Icons.Rounded.Search, "Szukaj", onClick = { vm.openSearch() })
        Spacer(Modifier.width(8.dp))
        RoundIcon(Icons.Rounded.MoreVert, "Menu", badge = if (vm.updatePending) "1" else null, onClick = onMenu)
    }
}

@Composable
private fun Filters(vm: AppViewModel) {
    val unreadThreads = vm.conversations.count { it.conv.unread > 0 }
    val pinned = vm.conversations.count { it.pinned }
    val archived = vm.conversations.count { it.archived }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabChip(null, Filter.ALL.label, vm.filter == Filter.ALL) { vm.filter = Filter.ALL }
        TabChip(null, Filter.UNREAD.label, vm.filter == Filter.UNREAD, count = unreadThreads) { vm.filter = Filter.UNREAD }
        if (pinned > 0 || vm.filter == Filter.PINNED) TabChip(Icons.Rounded.PushPin, Filter.PINNED.label, vm.filter == Filter.PINNED) { vm.filter = Filter.PINNED }
        if (archived > 0 || vm.filter == Filter.ARCHIVED) TabChip(Icons.Rounded.Archive, Filter.ARCHIVED.label, vm.filter == Filter.ARCHIVED, count = archived) { vm.filter = Filter.ARCHIVED }
    }
}

@Composable
private fun SelectionBar(vm: AppViewModel, onDelete: () -> Unit, onBlock: () -> Unit) {
    val ids = vm.selection
    val selected = vm.conversations.filter { it.conv.threadId in ids }
    val allArchived = selected.isNotEmpty() && selected.all { it.archived }
    val anyUnread = selected.any { it.conv.unread > 0 }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.Rounded.Close, "Anuluj zaznaczenie", onClick = { vm.selection = emptySet() })
        Text(ids.size.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(start = 14.dp).weight(1f))
        RoundIcon(Icons.Rounded.PushPin, "Przypnij", flat = true, onClick = { vm.pinSelected() })
        RoundIcon(if (anyUnread) Icons.Rounded.MarkChatRead else Icons.Rounded.MarkChatUnread, if (anyUnread) "Przeczytane" else "Nieprzeczytane", flat = true, onClick = { vm.markRead(ids, anyUnread) })
        RoundIcon(if (allArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, if (allArchived) "Przywróć" else "Archiwizuj", flat = true, onClick = { vm.archive(ids, !allArchived) })
        RoundIcon(Icons.Rounded.Block, "Zablokuj", flat = true, onClick = onBlock)
        RoundIcon(Icons.Rounded.Delete, "Usuń", flat = true, tint = Palette.Record, onClick = {
            if (vm.settings.confirmDelete) onDelete() else vm.deleteThreads(ids)
        })
    }
}

@Composable
private fun SearchBar(vm: AppViewModel) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Zamknij wyszukiwanie", onClick = { vm.closeSearch() })
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.weight(1f).clip(RoundedCornerShape(50)).background(Palette.PanelHigh).padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (vm.query.isEmpty()) Text("Szukaj w wiadomościach i kontaktach", color = Palette.TextFaint, fontSize = 15.sp)
            BasicTextField(
                value = vm.query,
                onValueChange = { vm.updateQuery(it) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (vm.query.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Close, "Wyczyść", onClick = { vm.updateQuery("") })
        }
    }
}

@Composable
private fun SearchResults(vm: AppViewModel) {
    val q = vm.query.trim()
    val convMatches = if (q.isEmpty()) emptyList() else vm.conversations.filter { c ->
        c.title.contains(q, ignoreCase = true) || c.conv.addresses.any { it.filter(Char::isDigit).contains(q.filter(Char::isDigit).ifEmpty { "\u0000" }) }
    }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 40.dp)) {
        if (q.length >= 2 && convMatches.isEmpty() && vm.searchResults.isEmpty()) {
            item { EmptyState(Icons.Rounded.SearchOff, "Nic nie znaleziono", "Spróbuj innego słowa albo fragmentu numeru.") }
        }
        if (convMatches.isNotEmpty()) {
            item { SectionLabel("Rozmowy") }
            items(convMatches, key = { "c" + it.conv.threadId }) { item ->
                ConversationRow(vm, item, selected = false, onClick = { vm.openThread(item.conv.threadId) }, onLongClick = {})
            }
        }
        if (vm.searchResults.isNotEmpty()) {
            item { SectionLabel("Wiadomości · ${vm.searchResults.size}") }
            items(vm.searchResults, key = { "m" + it.key }) { m -> SearchHit(vm, m, q) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = Palette.Accent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun SearchHit(vm: AppViewModel, m: Message, q: String) {
    val title = vm.titleFor(m.threadId).ifEmpty { m.address }
    Row(
        Modifier.fillMaxWidth().clickable { vm.openThread(m.threadId, highlight = q) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(title, null, size = 40.dp, colored = vm.settings.colorAvatars)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatListTime(m.date, false), color = Palette.TextFaint, fontSize = 12.sp)
            }
            Text(highlight(snippetAround(m.body, q), q), color = Palette.TextDim, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
        }
    }
}

private fun snippetAround(text: String, q: String): String {
    val i = text.indexOf(q, ignoreCase = true)
    if (i < 40) return text
    return "…" + text.substring(i - 30)
}

fun highlight(text: String, q: String) = buildAnnotatedString {
    if (q.isBlank()) {
        append(text); return@buildAnnotatedString
    }
    var start = 0
    while (true) {
        val i = text.indexOf(q, start, ignoreCase = true)
        if (i < 0) {
            append(text.substring(start)); break
        }
        append(text.substring(start, i))
        withStyle(SpanStyle(color = Palette.Ink, background = Color.White, fontWeight = FontWeight.Bold)) { append(text.substring(i, i + q.length)) }
        start = i + q.length
    }
}

/** Przesunięcie rozmowy w lewo / w prawo wykonuje akcję z ustawień. */
@Composable
private fun SwipeRow(vm: AppViewModel, item: ConversationItem, content: @Composable () -> Unit) {
    val left = vm.settings.swipeLeft
    val right = vm.settings.swipeRight
    if (left == SwipeAction.NONE && right == SwipeAction.NONE) {
        content(); return
    }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    var armed by remember { mutableStateOf(false) }
    val action = when {
        offset.value > 0 -> right
        offset.value < 0 -> left
        else -> SwipeAction.NONE
    }
    Box(Modifier.fillMaxWidth()) {
        if (action != SwipeAction.NONE) {
            Row(
                Modifier
                    .matchParentSize()
                    .background(if (action == SwipeAction.DELETE) Palette.Record else if (armed) Color.White else Palette.PanelHigher)
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (offset.value > 0) Arrangement.Start else Arrangement.End,
            ) {
                val tint = if (armed && action != SwipeAction.DELETE) Palette.Ink else Color.White
                Icon(swipeIcon(action, item), null, tint = tint)
                Spacer(Modifier.width(8.dp))
                Text(swipeLabel(action, item), color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(Palette.Ink)
                .pointerInput(item.conv.threadId, left, right) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val done = abs(offset.value) >= threshold
                            val a = if (offset.value > 0) right else left
                            scope.launch { offset.animateTo(0f) }
                            armed = false
                            if (done) runSwipe(vm, a, item)
                        },
                        onDragCancel = {
                            armed = false
                            scope.launch { offset.animateTo(0f) }
                        },
                    ) { change, dx ->
                        val next = offset.value + dx
                        val allowed = (next > 0 && right != SwipeAction.NONE) || (next < 0 && left != SwipeAction.NONE)
                        if (!allowed && next != 0f) return@detectHorizontalDragGestures
                        change.consume()
                        scope.launch { offset.snapTo(next) }
                        val nowArmed = abs(next) >= threshold
                        if (nowArmed != armed) {
                            armed = nowArmed
                            if (nowArmed && vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
        ) { content() }
    }
}

private fun swipeIcon(a: SwipeAction, item: ConversationItem): ImageVector = when (a) {
    SwipeAction.ARCHIVE -> if (item.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive
    SwipeAction.READ -> if (item.conv.unread > 0) Icons.Rounded.MarkChatRead else Icons.Rounded.MarkChatUnread
    SwipeAction.PIN -> Icons.Rounded.PushPin
    SwipeAction.DELETE -> Icons.Rounded.Delete
    SwipeAction.NONE -> Icons.Rounded.Close
}

private fun swipeLabel(a: SwipeAction, item: ConversationItem): String = when (a) {
    SwipeAction.ARCHIVE -> if (item.archived) "Przywróć" else "Archiwizuj"
    SwipeAction.READ -> if (item.conv.unread > 0) "Przeczytane" else "Nieprzeczytane"
    SwipeAction.PIN -> if (item.pinned) "Odepnij" else "Przypnij"
    SwipeAction.DELETE -> "Usuń"
    SwipeAction.NONE -> ""
}

private fun runSwipe(vm: AppViewModel, a: SwipeAction, item: ConversationItem) {
    val ids = setOf(item.conv.threadId)
    when (a) {
        SwipeAction.ARCHIVE -> vm.archive(ids, !item.archived)
        SwipeAction.READ -> vm.markRead(ids, item.conv.unread > 0)
        SwipeAction.PIN -> {
            vm.selection = ids; vm.pinSelected()
        }
        SwipeAction.DELETE -> vm.deleteThreads(ids)
        SwipeAction.NONE -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationRow(
    vm: AppViewModel,
    item: ConversationItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val c = item.conv
    val unread = c.unread > 0
    val draft = vm.settings.drafts[c.threadId]
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) Palette.PanelHigh else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(item.title, item.photo, size = 52.dp, group = c.addresses.size > 1, colored = vm.settings.colorAvatars, selected = selected)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (unread) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.muted) Icon(Icons.Rounded.NotificationsOff, "Wyciszona", tint = Palette.TextFaint, modifier = Modifier.padding(start = 5.dp).size(14.dp))
                if (item.pinned) Icon(Icons.Rounded.PushPin, "Przypięta", tint = Palette.TextDim, modifier = Modifier.padding(start = 5.dp).size(14.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    formatListTime(c.date, vm.settings.relativeTimes),
                    color = if (unread) Color.White else Palette.TextFaint,
                    fontSize = 12.sp,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val text = buildAnnotatedString {
                    if (!draft.isNullOrBlank()) {
                        withStyle(SpanStyle(color = Palette.Record, fontWeight = FontWeight.Bold)) { append("Szkic: ") }
                        append(draft)
                    } else {
                        append(c.snippet.replace('\n', ' '))
                    }
                }
                if (c.error) Icon(Icons.Rounded.Error, "Błąd wysyłania", tint = Palette.Record, modifier = Modifier.padding(end = 4.dp).size(15.dp))
                Text(
                    text,
                    color = if (unread) Color.White else Palette.TextDim,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (unread) {
                    Text(
                        if (c.unread > 99) "99+" else c.unread.toString(),
                        color = Palette.Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
