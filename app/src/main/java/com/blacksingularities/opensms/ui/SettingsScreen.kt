package com.blacksingularities.opensms.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.SwipeLeft
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.data.Backup
import com.blacksingularities.opensms.data.MessageStore
import com.blacksingularities.opensms.data.MmsLimit
import com.blacksingularities.opensms.data.SwipeAction
import com.blacksingularities.opensms.data.Template
import com.blacksingularities.opensms.data.TextSize
import com.blacksingularities.opensms.data.formatNumber
import com.blacksingularities.opensms.notify.Notifier
import com.blacksingularities.opensms.schedule.Scheduler
import com.blacksingularities.opensms.ui.components.ActionRow
import com.blacksingularities.opensms.ui.components.CompactChoice
import com.blacksingularities.opensms.ui.components.ConfirmDialog
import com.blacksingularities.opensms.ui.components.DarkDialog
import com.blacksingularities.opensms.ui.components.DarkField
import com.blacksingularities.opensms.ui.components.EmptyState
import com.blacksingularities.opensms.ui.components.Group
import com.blacksingularities.opensms.ui.components.InfoRow
import com.blacksingularities.opensms.ui.components.PillButton
import com.blacksingularities.opensms.ui.components.RoundIcon
import com.blacksingularities.opensms.ui.components.SwitchRow
import com.blacksingularities.opensms.ui.components.VSpace
import com.blacksingularities.opensms.ui.theme.Palette
import com.blacksingularities.opensms.update.UpdateManager
import com.blacksingularities.opensms.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, page: SettingsPage, onRequestPermissions: () -> Unit) {
    val title = when (page) {
        SettingsPage.MAIN -> "Ustawienia"
        SettingsPage.BLOCKED -> "Zablokowane"
        SettingsPage.BLOCKED_LOG -> "Zatrzymane wiadomości"
        SettingsPage.TEMPLATES -> "Szablony"
        SettingsPage.SCHEDULED -> "Zaplanowane"
        SettingsPage.STATS -> "Statystyki"
    }
    Column(Modifier.fillMaxSize().background(Palette.Ink).windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Wstecz", flat = true, onClick = { vm.back() })
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(start = 6.dp))
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (page) {
                    SettingsPage.MAIN -> MainSettings(vm, onRequestPermissions)
                    SettingsPage.BLOCKED -> BlockedPage(vm)
                    SettingsPage.BLOCKED_LOG -> BlockedLogPage(vm)
                    SettingsPage.TEMPLATES -> TemplatesPage(vm)
                    SettingsPage.SCHEDULED -> ScheduledPage(vm)
                    SettingsPage.STATS -> StatsPage(vm)
                }
                VSpace(32.dp)
            }
        }
    }
}

@Composable
private fun ColumnScope.MainSettings(vm: AppViewModel, onRequestPermissions: () -> Unit) {
    val s = vm.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signatureDialog by remember { mutableStateOf(false) }
    var backupProgress by remember { mutableStateOf<Float?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupProgress = 0f
            val r = withContext(Dispatchers.IO) { runCatching { Backup.export(context, uri) } }
            backupProgress = null
            vm.toast = r.fold({ "Zapisano kopię: ${plural(it, "wiadomość", "wiadomości", "wiadomości")}" }, { it.message ?: "Błąd zapisu" })
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupProgress = 0f
            val r = withContext(Dispatchers.IO) { runCatching { Backup.import(context, uri) { p -> backupProgress = p } } }
            backupProgress = null
            vm.toast = r.fold({ "Przywrócono ${it.added} z ${it.total} (pozostałe już były w telefonie)" }, { it.message ?: "Błąd odczytu" })
            vm.reload()
        }
    }

    UpdateBanner(vm)

    Group("Wygląd") {
        CompactChoice(Icons.Rounded.FontDownload, "Rozmiar tekstu wiadomości", TextSize.entries, s.textSize, { it.label }, short = {
            when (it) {
                TextSize.SMALL -> "S"
                TextSize.NORMAL -> "M"
                TextSize.LARGE -> "L"
                TextSize.HUGE -> "XL"
            }
        }) { s.textSize = it }
        SwitchRow(Icons.Rounded.FormatColorFill, "Kolorowe awatary", "Inicjały na kolorowym tle zamiast szarego", s.colorAvatars) { s.colorAvatars = it }
        SwitchRow(Icons.Rounded.AccessTime, "Czas względny", "„5 min” zamiast godziny dla najnowszych rozmów", s.relativeTimes) { s.relativeTimes = it }
        SwitchRow(Icons.Rounded.SimCard, "Oznaczenie karty SIM", "Przy wiadomościach, gdy w telefonie są dwie karty", s.showSimBadge) { s.showSimBadge = it }
        SwitchRow(Icons.Rounded.Vibration, "Wibracja przycisków", "Przy przytrzymaniu i przesuwaniu rozmów", s.haptics) { s.haptics = it }
    }

    Group("Gesty na liście") {
        CompactChoice(Icons.Rounded.SwipeRight, "Przesunięcie w prawo", SwipeAction.entries, s.swipeRight, { it.label }) { s.swipeRight = it }
        CompactChoice(Icons.Rounded.SwipeLeft, "Przesunięcie w lewo", SwipeAction.entries, s.swipeLeft, { it.label }) { s.swipeLeft = it }
    }

    Group("Wysyłanie") {
        SwitchRow(Icons.Rounded.DoneAll, "Raporty doręczenia", "Informacja „Dostarczono” pod wysłanym SMS", s.deliveryReports) { s.deliveryReports = it }
        ActionRow(Icons.Rounded.Draw, "Podpis", s.signature.ifBlank { "Brak – dotknij, aby dodać" }) { signatureDialog = true }
        SwitchRow(Icons.Rounded.KeyboardReturn, "Enter wysyła", "Klawisz Enter wysyła zamiast nowej linii", s.enterSends) { s.enterSends = it }
        SwitchRow(Icons.Rounded.Spellcheck, "Bez polskich znaków", "Zamienia ą→a, ł→l… – SMS mieści 160 zamiast 70 znaków", s.stripAccents) { s.stripAccents = it }
        SwitchRow(Icons.Rounded.Groups, "Rozmowy grupowe jako MMS", "Wyłączone: osobny SMS do każdego odbiorcy", s.groupAsMms) { s.groupAsMms = it }
        SwitchRow(Icons.Rounded.Image, "Długie wiadomości jako MMS", "Zamiast wielu części SMS", s.longAsMms) { s.longAsMms = it }
        if (s.longAsMms) {
            CompactChoice(Icons.AutoMirrored.Rounded.TextSnippet, "Próg zamiany na MMS", listOf(2, 3, 4, 6, 8), s.longAsMmsSegments, { "powyżej ${plural(it, "części", "części", "części")}" }, short = { it.toString() }) {
                s.longAsMmsSegments = it
            }
        }
        CompactChoice(Icons.Rounded.Image, "Limit rozmiaru MMS", MmsLimit.entries, s.mmsLimit, { it.label }, hint = "zdjęcia są zmniejszane") { s.mmsLimit = it }
        SwitchRow(Icons.Rounded.CloudDownload, "Automatyczne pobieranie MMS", "Wyłączone: MMS pobierasz przyciskiem w rozmowie", s.autoDownloadMms) { s.autoDownloadMms = it }
        if (vm.sims.size > 1) {
            val options = listOf(-1) + vm.sims.map { it.subId }
            CompactChoice(Icons.Rounded.SimCard, "Domyślna karta SIM", options, if (s.defaultSubId in options) s.defaultSubId else -1, { id ->
                vm.sims.firstOrNull { it.subId == id }?.let { "SIM ${it.slot + 1} · ${it.name}" } ?: "Jak w systemie"
            }, short = { id -> vm.sims.firstOrNull { it.subId == id }?.let { "SIM ${it.slot + 1}" } ?: "System" }) { s.defaultSubId = it }
        }
    }

    Group("Powiadomienia") {
        SwitchRow(Icons.Rounded.Notifications, "Powiadomienia", null, s.notifications) { s.notifications = it }
        SwitchRow(Icons.Rounded.Visibility, "Treść w powiadomieniu", "Wyłączone: tylko „Nowa wiadomość”", s.notificationPreview, enabled = s.notifications) { s.notificationPreview = it }
        SwitchRow(Icons.Rounded.Key, "Kopiowanie kodów", "Przycisk „Kopiuj 123456” przy kodach SMS", s.copyCodeAction) { s.copyCodeAction = it }
        SwitchRow(Icons.Rounded.TaskAlt, "Odpowiedź oznacza przeczytane", null, s.markReadOnReply) { s.markReadOnReply = it }
        SwitchRow(Icons.Rounded.Warning, "Błędy wysyłania", "Powiadomienie, gdy wiadomość nie dojdzie", s.notifyFailures) { s.notifyFailures = it }
        ActionRow(Icons.Rounded.NotificationsActive, "Dźwięk i wibracje", "Ustawienia kanału powiadomień w systemie") {
            runCatching {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(AndroidSettings.EXTRA_CHANNEL_ID, Notifier.CHANNEL_MESSAGES),
                )
            }
        }
    }

    Group("Prywatność i filtry") {
        ActionRow(Icons.Rounded.Block, "Zablokowane numery i słowa", "${s.blockedNumbers.size} numerów · ${s.blockedKeywords.size} słów") {
            vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED))
        }
        SwitchRow(Icons.Rounded.PersonOff, "Blokuj nieznanych", "Wiadomości spoza kontaktów trafiają do zatrzymanych", s.blockUnknown) { s.blockUnknown = it }
        ActionRow(Icons.Rounded.History, "Zatrzymane wiadomości", plural(s.blockedLog.size, "wiadomość", "wiadomości", "wiadomości")) {
            vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED_LOG))
        }
        SwitchRow(Icons.Rounded.Delete, "Pytaj przed usunięciem", null, s.confirmDelete) { s.confirmDelete = it }
    }

    Group("Narzędzia") {
        ActionRow(Icons.AutoMirrored.Rounded.ShortText, "Szablony odpowiedzi", plural(s.templates.size, "szablon", "szablony", "szablonów")) {
            vm.push(Screen.SettingsScreen(SettingsPage.TEMPLATES))
        }
        ActionRow(Icons.Rounded.Schedule, "Zaplanowane wiadomości", plural(s.scheduled.size, "wiadomość", "wiadomości", "wiadomości")) {
            vm.push(Screen.SettingsScreen(SettingsPage.SCHEDULED))
        }
        ActionRow(Icons.Rounded.BarChart, "Statystyki", "Ile piszesz, z kim i kiedy") {
            vm.loadStats(); vm.push(Screen.SettingsScreen(SettingsPage.STATS))
        }
        ActionRow(Icons.Rounded.Upload, "Eksportuj SMS do pliku", "Kopia zapasowa w formacie JSON") {
            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            exportLauncher.launch("OpenSMS-kopia-$stamp.json")
        }
        ActionRow(Icons.Rounded.Download, "Importuj SMS z pliku", "Pomija wiadomości, które już są w telefonie") {
            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
        }
        backupProgress?.let { p ->
            LinearProgressIndicator(progress = { p }, color = Color.White, trackColor = Color.White.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
    }

    Group("Aktualizacje") {
        SwitchRow(Icons.Rounded.SystemUpdate, "Sprawdzaj przy starcie", "Nowe wersje z GitHub Releases", s.autoUpdateCheck) { s.autoUpdateCheck = it }
        val status = when (val u = vm.update) {
            is UpdateState.Checking -> "Sprawdzanie…"
            is UpdateState.UpToDate -> "Masz najnowszą wersję"
            is UpdateState.Available -> "Dostępna wersja ${u.info.version}"
            is UpdateState.Downloading -> "Pobieranie…"
            is UpdateState.Ready, is UpdateState.Installing -> "Instalowanie…"
            is UpdateState.Failed -> u.message
            UpdateState.Idle -> if (s.lastUpdateCheck > 0) "Ostatnio: ${formatFull(s.lastUpdateCheck).substringBeforeLast(':')}" else "Jeszcze nie sprawdzano"
        }
        ActionRow(Icons.Rounded.Restore, "Sprawdź teraz", status) { vm.checkUpdate(silent = false) }
        InfoRow(Icons.Rounded.Code, "Wersja", UpdateManager.currentVersion)
        ActionRow(Icons.Rounded.Code, "Kod źródłowy i wydania", UpdateManager.REPO_PAGE.removePrefix("https://")) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.RELEASES_PAGE))) }
        }
    }

    Group("System") {
        ActionRow(Icons.Rounded.Security, "Uprawnienia", "Kontakty, karty SIM, powiadomienia") { onRequestPermissions() }
        ActionRow(Icons.Rounded.AdminPanelSettings, "Ustawienia aplikacji w systemie") {
            runCatching {
                context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
            }
        }
        ActionRow(Icons.Rounded.Schedule, "Dokładne alarmy", if (Scheduler.canExact(context)) "Włączone – zaplanowane wysyłają się co do minuty" else "Wyłączone – wysyłka może się opóźnić") {
            runCatching { context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) }
        }
    }

    Text(
        "OpenSMS ${UpdateManager.currentVersion} · otwarte oprogramowanie (MIT)\nWiadomości zostają w telefonie – aplikacja łączy się z internetem tylko po aktualizacje.",
        color = Palette.TextFaint,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
    )

    if (signatureDialog) {
        var text by remember { mutableStateOf(s.signature) }
        DarkDialog(
            title = "Podpis",
            onDismiss = { signatureDialog = false },
            buttons = {
                PillButton("Anuluj", primary = false, onClick = { signatureDialog = false })
                PillButton("Zapisz", onClick = { s.signature = text.trim(); signatureDialog = false })
            },
        ) {
            Text("Dopisywany na końcu każdej wysyłanej wiadomości.", color = Palette.TextDim, fontSize = 13.sp)
            DarkField(text, { text = it }, "Podpis", singleLine = false, minLines = 2)
        }
    }
}

@Composable
private fun ColumnScope.BlockedPage(vm: AppViewModel) {
    val s = vm.settings
    var number by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    Group("Numery") {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DarkField(number, { number = it }, "Numer lub nazwa nadawcy", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Add, "Dodaj", active = number.isNotBlank(), enabled = number.isNotBlank(), onClick = {
                s.blockedNumbers = s.blockedNumbers + number.trim(); number = ""
            })
        }
        if (s.blockedNumbers.isEmpty()) Text("Brak zablokowanych numerów.", color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        s.blockedNumbers.sorted().forEach { n ->
            ActionRow(Icons.Rounded.Block, formatNumber(n), "Dotknij, aby odblokować") { vm.unblock(n); vm.toast = "Odblokowano $n" }
        }
    }
    Group("Słowa kluczowe") {
        Text(
            "Wiadomości zawierające te słowa (bez względu na wielkość liter) są zatrzymywane.",
            color = Palette.TextDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp),
        )
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DarkField(keyword, { keyword = it }, "Słowo lub fraza", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Add, "Dodaj", active = keyword.isNotBlank(), enabled = keyword.isNotBlank(), onClick = {
                s.blockedKeywords = s.blockedKeywords + keyword.trim(); keyword = ""
            })
        }
        s.blockedKeywords.sorted().forEach { k ->
            ActionRow(Icons.AutoMirrored.Rounded.TextSnippet, k, "Dotknij, aby usunąć") { s.blockedKeywords = s.blockedKeywords - k }
        }
    }
    ActionRow(Icons.Rounded.History, "Zatrzymane wiadomości", plural(s.blockedLog.size, "wiadomość", "wiadomości", "wiadomości")) {
        vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED_LOG))
    }
}

@Composable
private fun ColumnScope.BlockedLogPage(vm: AppViewModel) {
    val s = vm.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    SwitchRow(Icons.Rounded.History, "Zapisuj zatrzymane wiadomości", "Wyłączone: zablokowane wiadomości są od razu odrzucane", s.keepBlockedLog) { s.keepBlockedLog = it }
    if (s.blockedLog.isEmpty()) {
        EmptyState(Icons.Rounded.Block, "Nic nie zatrzymano", "Tu pojawią się wiadomości od zablokowanych nadawców i ze słowami kluczowymi.")
        return
    }
    s.blockedLog.forEach { e ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatNumber(e.address), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(formatListTime(e.date, false), color = Palette.TextFaint, fontSize = 12.sp)
            }
            Text(e.reason, color = Palette.Record, fontSize = 11.sp)
            Text(e.body, color = Palette.TextDim, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 6, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Przywróć do skrzynki", primary = false, icon = Icons.Rounded.Restore, onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { MessageStore(context).insertIncomingSms(e.address, e.body, e.date, -1, read = true) }
                        vm.toast = "Przywrócono do skrzynki"
                    }
                })
                PillButton("Kopiuj", primary = false, icon = Icons.Rounded.ContentCopy, onClick = { MediaActions.copy(context, e.body); vm.toast = "Skopiowano" })
            }
        }
    }
    PillButton("Wyczyść listę", danger = true, onClick = { confirmClear = true })
    if (confirmClear) {
        ConfirmDialog("Wyczyścić?", "Lista zatrzymanych wiadomości zostanie usunięta.", "Wyczyść", onDismiss = { confirmClear = false }) { s.clearBlockedLog() }
    }
}

@Composable
private fun ColumnScope.TemplatesPage(vm: AppViewModel) {
    val s = vm.settings
    var editing by remember { mutableStateOf<Template?>(null) }
    Text("Szablony wstawiasz w rozmowie przyciskiem „+” → Szablony.", color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
    Group(null) {
        if (s.templates.isEmpty()) Text("Brak szablonów.", color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        s.templates.forEach { t ->
            ActionRow(Icons.AutoMirrored.Rounded.ShortText, t.text, trailing = {
                RoundIcon(Icons.Rounded.Delete, "Usuń szablon", size = 36.dp, flat = true, tint = Palette.Record, onClick = { s.saveTemplates(s.templates - t) })
            }) { editing = t }
        }
    }
    PillButton("Nowy szablon", icon = Icons.Rounded.Add, onClick = { editing = Template(0, "") })
    editing?.let { t ->
        var text by remember(t) { mutableStateOf(t.text) }
        DarkDialog(
            title = if (t.id == 0L) "Nowy szablon" else "Edytuj szablon",
            onDismiss = { editing = null },
            buttons = {
                PillButton("Anuluj", primary = false, onClick = { editing = null })
                PillButton("Zapisz", enabled = text.isNotBlank(), onClick = {
                    val list = if (t.id == 0L) {
                        s.templates + Template(System.currentTimeMillis(), text.trim())
                    } else {
                        s.templates.map { if (it.id == t.id) it.copy(text = text.trim()) else it }
                    }
                    s.saveTemplates(list)
                    editing = null
                })
            },
        ) {
            DarkField(text, { text = it }, "Treść", singleLine = false, minLines = 3)
        }
    }
}

@Composable
private fun ColumnScope.ScheduledPage(vm: AppViewModel) {
    val s = vm.settings
    val context = LocalContext.current
    LaunchedEffect(Unit) { s.reloadScheduled() }
    if (!Scheduler.canExact(context)) {
        ActionRow(Icons.Rounded.Warning, "Włącz dokładne alarmy", "Bez nich system może opóźnić wysyłkę o kilka minut") {
            runCatching { context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) }
        }
    }
    if (s.scheduled.isEmpty()) {
        EmptyState(Icons.Rounded.Schedule, "Brak zaplanowanych", "W rozmowie przytrzymaj przycisk wysyłania, aby wybrać datę i godzinę.")
        return
    }
    s.scheduled.forEach { m ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(formatFull(m.time).substringBeforeLast(':'), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            }
            Text("Do: " + m.addresses.joinToString(", ") { com.blacksingularities.opensms.data.Contacts.displayName(context, it) }, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Text(m.text, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Wyślij teraz", icon = Icons.AutoMirrored.Rounded.Send, onClick = {
                    Scheduler.cancel(context, m.id)
                    vm.send(Screen.Thread(null, m.addresses), m.text, emptyList(), m.subId)
                    vm.toast = "Wysyłanie…"
                })
                PillButton("Anuluj", primary = false, icon = Icons.Rounded.Delete, onClick = { Scheduler.cancel(context, m.id); vm.toast = "Anulowano" })
                PillButton("Edytuj", primary = false, icon = Icons.Rounded.Edit, onClick = {
                    Scheduler.cancel(context, m.id)
                    vm.openAddresses(m.addresses, m.text)
                })
            }
        }
    }
}

@Composable
private fun ColumnScope.StatsPage(vm: AppViewModel) {
    val st = vm.stats
    if (st == null) {
        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = Color.White)
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Odebrane SMS", st.smsIn.toString(), Modifier.weight(1f))
        StatTile("Wysłane SMS", st.smsOut.toString(), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Odebrane MMS", st.mmsIn.toString(), Modifier.weight(1f))
        StatTile("Wysłane MMS", st.mmsOut.toString(), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Rozmowy", st.threads.toString(), Modifier.weight(1f))
        StatTile("Średnia długość", "${st.avgLength} zn.", Modifier.weight(1f))
    }
    StatTile("Najczęściej piszesz o", "${st.busiestHour}:00–${st.busiestHour + 1}:00", Modifier.fillMaxWidth())
    if (st.top.isNotEmpty()) {
        Group("Najczęstsi rozmówcy (SMS)") {
            val max = st.top.maxOf { it.second }.coerceAtLeast(1)
            st.top.forEach { (name, n) ->
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row {
                        Text(name, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(n.toString(), color = Palette.TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
                        Box(Modifier.fillMaxWidth(n.toFloat() / max).height(6.dp).clip(RoundedCornerShape(50)).background(Color.White))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Palette.PanelHigh).padding(16.dp)) {
        Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(label, color = Palette.TextDim, fontSize = 12.sp)
    }
}
