package com.blacksingularities.opensms.ui

import com.blacksingularities.opensms.R
import com.blacksingularities.opensms.tr

import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ShortText
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.ZoomIn
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.blacksingularities.opensms.data.BubbleSpacing
import com.blacksingularities.opensms.data.BubbleTint
import com.blacksingularities.opensms.data.BubbleWidth
import com.blacksingularities.opensms.data.TimeMode
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
import com.blacksingularities.opensms.ui.components.RowIcon
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
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, page: SettingsPage, onRequestPermissions: () -> Unit) {
    val title = when (page) {
        SettingsPage.MAIN -> tr(R.string.ustawienia)
        SettingsPage.BLOCKED -> tr(R.string.zablokowane)
        SettingsPage.BLOCKED_LOG -> tr(R.string.zatrzymane_wiadomosci)
        SettingsPage.TEMPLATES -> tr(R.string.szablony)
        SettingsPage.SCHEDULED -> tr(R.string.zaplanowane)
        SettingsPage.STATS -> tr(R.string.statystyki)
    }
    Column(Modifier.fillMaxSize().background(Palette.Ink).screenPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, tr(R.string.wstecz), flat = true, onClick = { vm.back() })
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(start = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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

private fun swipeShort(a: SwipeAction) = when (a) {
    SwipeAction.NONE -> tr(R.string.brak)
    SwipeAction.ARCHIVE -> tr(R.string.archiw)
    SwipeAction.READ -> tr(R.string.czytane)
    SwipeAction.PIN -> tr(R.string.przypnij)
    SwipeAction.DELETE -> tr(R.string.usun)
}

/** Wygląd dymków w rozmowie – z podglądem, który od razu pokazuje każdą zmianę. */
@Composable
private fun ConversationLookGroup(vm: AppViewModel) {
    val s = vm.settings
    Group(tr(R.string.wyglad_rozmowy)) {
        MessageStylePreview(MessageStyle.from(s), Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(Icons.Rounded.ZoomIn)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(tr(R.string.powiekszenie_dymkow), fontSize = 15.sp, color = Color.White)
                    Text(
                        tr(R.string.s_1_w_rozmowie_zmienisz_je, (s.bubbleZoom * 100).roundToInt()),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Palette.TextDim,
                    )
                }
                if (s.bubbleZoom != 1f) PillButton("100%", primary = false, onClick = { s.bubbleZoom = 1f })
            }
            Slider(
                value = s.bubbleZoom,
                onValueChange = { s.bubbleZoom = (it * 20).roundToInt() / 20f },
                valueRange = MIN_ZOOM..MAX_ZOOM,
                modifier = Modifier.padding(start = 40.dp).height(36.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Palette.PanelHigher),
            )
        }
        SwitchRow(Icons.Rounded.TouchApp, tr(R.string.powiekszanie_dwoma_palcami), tr(R.string.w_rozmowie_i_na_liscie), s.pinchZoom) { s.pinchZoom = it }
        CompactChoice(Icons.Rounded.ColorLens, tr(R.string.kolor_moich_wiadomosci), BubbleTint.entries, s.bubbleTint, { it.label }) { s.bubbleTint = it }
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowIcon(Icons.Rounded.RoundedCorner)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(tr(R.string.zaokraglenie_dymkow), fontSize = 15.sp, color = Color.White)
                    Text(
                        when {
                            s.bubbleRadius <= 0f -> tr(R.string.kanciaste_rogi)
                            s.bubbleRadius >= 28f -> tr(R.string.maksymalnie_okragle_28_dp)
                            else -> "${s.bubbleRadius.roundToInt()} dp"
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Palette.TextDim,
                    )
                }
                if (s.bubbleRadius != 22f) PillButton(tr(R.string.domyslne), primary = false, onClick = { s.bubbleRadius = 22f })
            }
            Slider(
                value = s.bubbleRadius,
                onValueChange = { s.bubbleRadius = it.roundToInt().toFloat() },
                valueRange = 0f..28f,
                steps = 27,
                modifier = Modifier.padding(start = 40.dp).height(36.dp),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Palette.PanelHigher),
            )
        }
        CompactChoice(Icons.Rounded.AspectRatio, tr(R.string.szerokosc_dymkow), BubbleWidth.entries, s.bubbleWidth, { it.label }) { s.bubbleWidth = it }
        CompactChoice(Icons.Rounded.FormatLineSpacing, tr(R.string.odstepy), BubbleSpacing.entries, s.bubbleSpacing, { it.label }) { s.bubbleSpacing = it }
        CompactChoice(Icons.Rounded.AccessTime, tr(R.string.godzina_wiadomosci), TimeMode.entries, s.timeMode, { it.label }) { s.timeMode = it }
        SwitchRow(Icons.Rounded.EmojiEmotions, tr(R.string.duze_emoji), tr(R.string.wiadomosc_z_samych_emoji_bez), s.bigEmoji) { s.bigEmoji = it }
        SwitchRow(Icons.Rounded.Mood, tr(R.string.emotikony_jako_emoji), tr(R.string.s_3_i_podobne_pokazuja_sie), s.emoticons) { s.emoticons = it }
        SwitchRow(Icons.Rounded.CalendarToday, tr(R.string.naglowki_dni), tr(R.string.dzisiaj_wczoraj_i_daty_miedzy), s.dayHeaders) { s.dayHeaders = it }
        SwitchRow(Icons.Rounded.AccountCircle, tr(R.string.awatary_w_grupach), tr(R.string.zdjecie_nadawcy_przy_wiadomosciach_w), s.senderAvatars) { s.senderAvatars = it }
        SwitchRow(Icons.Rounded.AddReaction, tr(R.string.reakcje_jako_emoji), tr(R.string.reakcje_jako_emoji_opis), s.reactions) { s.reactions = it }
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
            vm.toast = r.fold({ tr(R.string.zapisano_kopie_1, plural(it, tr(R.string.wiadomosc_2), tr(R.string.wiadomosci_2), tr(R.string.wiadomosci_2))) }, { it.message ?: tr(R.string.blad_zapisu) })
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            backupProgress = 0f
            val r = withContext(Dispatchers.IO) { runCatching { Backup.import(context, uri) { p -> backupProgress = p } } }
            backupProgress = null
            vm.toast = r.fold({ tr(R.string.przywrocono_1_z_2_pozostale, it.added, it.total) }, { it.message ?: tr(R.string.blad_odczytu) })
            vm.reload()
        }
    }

    UpdateBanner(vm)

    Group(tr(R.string.wyglad)) {
        CompactChoice(Icons.Rounded.FontDownload, tr(R.string.rozmiar_tekstu_wiadomosci), TextSize.entries, s.textSize, { it.label }, short = {
            when (it) {
                TextSize.SMALL -> "S"
                TextSize.NORMAL -> "M"
                TextSize.LARGE -> "L"
                TextSize.HUGE -> "XL"
            }
        }) { s.textSize = it }
        SwitchRow(Icons.Rounded.FormatColorFill, tr(R.string.kolorowe_awatary), tr(R.string.inicjaly_na_kolorowym_tle_zamiast), s.colorAvatars) { s.colorAvatars = it }
        SwitchRow(Icons.Rounded.AccessTime, tr(R.string.czas_wzgledny), tr(R.string.s_5_min_zamiast_godziny_dla), s.relativeTimes) { s.relativeTimes = it }
        SwitchRow(Icons.Rounded.SimCard, tr(R.string.oznaczenie_karty_sim), tr(R.string.przy_wiadomosciach_gdy_w_telefonie), s.showSimBadge) { s.showSimBadge = it }
        SwitchRow(Icons.Rounded.Vibration, tr(R.string.wibracja_przyciskow), tr(R.string.przy_przytrzymaniu_i_przesuwaniu_rozmow), s.haptics) { s.haptics = it }
    }

    ConversationLookGroup(vm)

    Group(tr(R.string.gesty_na_liscie)) {
        CompactChoice(Icons.Rounded.SwipeRight, tr(R.string.przesuniecie_w_prawo), SwipeAction.entries, s.swipeRight, { it.label }, short = { swipeShort(it) }) { s.swipeRight = it }
        CompactChoice(Icons.Rounded.SwipeLeft, tr(R.string.przesuniecie_w_lewo), SwipeAction.entries, s.swipeLeft, { it.label }, short = { swipeShort(it) }) { s.swipeLeft = it }
        InfoRow(Icons.Rounded.TouchApp, tr(R.string.przelaczanie_kart), tr(R.string.przesun_palcem_w_bok_po))
        if (s.listZoom != 1f) ActionRow(Icons.Rounded.ZoomIn, tr(R.string.powiekszenie_listy_1, (s.listZoom * 100).roundToInt()), tr(R.string.dotknij_aby_wrocic_do_100)) { s.listZoom = 1f }
    }

    Group(tr(R.string.wysylanie)) {
        SwitchRow(Icons.Rounded.DoneAll, tr(R.string.raporty_doreczenia), tr(R.string.informacja_dostarczono_pod_wyslanym_sms), s.deliveryReports) { s.deliveryReports = it }
        ActionRow(Icons.Rounded.Draw, tr(R.string.podpis), s.signature.ifBlank { tr(R.string.brak_dotknij_aby_dodac) }) { signatureDialog = true }
        SwitchRow(Icons.AutoMirrored.Rounded.KeyboardReturn, tr(R.string.enter_wysyla), tr(R.string.klawisz_enter_wysyla_zamiast_nowej), s.enterSends) { s.enterSends = it }
        SwitchRow(Icons.Rounded.Spellcheck, tr(R.string.bez_polskich_znakow), tr(R.string.zamienia_aa_ll_sms_miesci), s.stripAccents) { s.stripAccents = it }
        SwitchRow(Icons.Rounded.Groups, tr(R.string.rozmowy_grupowe_jako_mms), tr(R.string.wylaczone_osobny_sms_do_kazdego), s.groupAsMms) { s.groupAsMms = it }
        SwitchRow(Icons.Rounded.Image, tr(R.string.dlugie_wiadomosci_jako_mms), tr(R.string.zamiast_wielu_czesci_sms), s.longAsMms) { s.longAsMms = it }
        if (s.longAsMms) {
            CompactChoice(Icons.AutoMirrored.Rounded.TextSnippet, tr(R.string.prog_zamiany_na_mms), listOf(2, 3, 4, 6, 8), s.longAsMmsSegments, { tr(R.string.powyzej_1, plural(it, tr(R.string.czesc_one), tr(R.string.czesc_few), tr(R.string.czesc_many))) }, short = { it.toString() }) {
                s.longAsMmsSegments = it
            }
        }
        CompactChoice(Icons.Rounded.Image, tr(R.string.limit_rozmiaru_mms), MmsLimit.entries, s.mmsLimit, { it.label }, hint = tr(R.string.zdjecia_sa_zmniejszane)) { s.mmsLimit = it }
        SwitchRow(Icons.Rounded.CloudDownload, tr(R.string.automatyczne_pobieranie_mms), tr(R.string.wylaczone_mms_pobierasz_przyciskiem_w), s.autoDownloadMms) { s.autoDownloadMms = it }
        if (vm.sims.size > 1) {
            val options = listOf(-1) + vm.sims.map { it.subId }
            CompactChoice(Icons.Rounded.SimCard, tr(R.string.domyslna_karta_sim), options, if (s.defaultSubId in options) s.defaultSubId else -1, { id ->
                vm.sims.firstOrNull { it.subId == id }?.let { "SIM ${it.slot + 1} · ${it.name}" } ?: tr(R.string.jak_w_systemie)
            }, short = { id -> vm.sims.firstOrNull { it.subId == id }?.let { "SIM ${it.slot + 1}" } ?: tr(R.string.system) }) { s.defaultSubId = it }
        }
    }

    Group(tr(R.string.powiadomienia)) {
        SwitchRow(Icons.Rounded.Notifications, tr(R.string.powiadomienia), null, s.notifications) { s.notifications = it }
        SwitchRow(Icons.Rounded.Visibility, tr(R.string.tresc_w_powiadomieniu), tr(R.string.wylaczone_tylko_nowa_wiadomosc), s.notificationPreview, enabled = s.notifications) { s.notificationPreview = it }
        SwitchRow(Icons.Rounded.ScreenLockPortrait, tr(R.string.tresc_na_ekranie_blokady), tr(R.string.tresc_na_ekranie_blokady_opis), s.lockScreenContent, enabled = s.notifications && s.notificationPreview) { s.lockScreenContent = it }
        SwitchRow(Icons.Rounded.Key, tr(R.string.kopiowanie_kodow), tr(R.string.przycisk_kopiuj_123456_przy_kodach), s.copyCodeAction) { s.copyCodeAction = it }
        SwitchRow(Icons.Rounded.TaskAlt, tr(R.string.odpowiedz_oznacza_przeczytane), null, s.markReadOnReply) { s.markReadOnReply = it }
        SwitchRow(Icons.Rounded.Warning, tr(R.string.bledy_wysylania), tr(R.string.powiadomienie_gdy_wiadomosc_nie_dojdzie), s.notifyFailures) { s.notifyFailures = it }
        ActionRow(Icons.Rounded.NotificationsActive, tr(R.string.dzwiek_i_wibracje), tr(R.string.ustawienia_kanalu_powiadomien_w_systemie)) {
            runCatching {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(AndroidSettings.EXTRA_CHANNEL_ID, Notifier.CHANNEL_MESSAGES),
                )
            }
        }
    }

    Group(tr(R.string.prywatnosc_i_filtry)) {
        ActionRow(Icons.Rounded.Block, tr(R.string.zablokowane_numery_i_slowa), tr(R.string.s_1_numerow_2_slow, s.blockedNumbers.size, s.blockedKeywords.size)) {
            vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED))
        }
        SwitchRow(Icons.Rounded.Shield, tr(R.string.filtr_spamu), tr(R.string.filtr_spamu_opis), s.spamFilter) { s.spamFilter = it }
        SwitchRow(Icons.Rounded.PersonOff, tr(R.string.blokuj_nieznanych), tr(R.string.wiadomosci_spoza_kontaktow_trafiaja_do), s.blockUnknown) { s.blockUnknown = it }
        SwitchRow(Icons.Rounded.Fingerprint, tr(R.string.blokada_aplikacji), tr(R.string.blokada_aplikacji_opis), s.appLock) { on ->
            val secure = context.getSystemService(android.app.KeyguardManager::class.java)?.isDeviceSecure == true
            if (on && !secure) {
                vm.toast = tr(R.string.ustaw_blokade_ekranu)
            } else {
                s.appLock = on
                (context as? android.app.Activity)?.window?.let { w ->
                    if (on) w.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    else w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        ActionRow(Icons.Rounded.History, tr(R.string.zatrzymane_wiadomosci), plural(s.blockedLog.size, tr(R.string.wiadomosc_2), tr(R.string.wiadomosci_2), tr(R.string.wiadomosci_2))) {
            vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED_LOG))
        }
        SwitchRow(Icons.Rounded.Delete, tr(R.string.pytaj_przed_usunieciem), null, s.confirmDelete) { s.confirmDelete = it }
    }

    Group(tr(R.string.narzedzia)) {
        ActionRow(Icons.AutoMirrored.Rounded.ShortText, tr(R.string.szablony_odpowiedzi), plural(s.templates.size, tr(R.string.szablon), tr(R.string.szablony_few), tr(R.string.szablonow))) {
            vm.push(Screen.SettingsScreen(SettingsPage.TEMPLATES))
        }
        ActionRow(Icons.Rounded.Schedule, tr(R.string.zaplanowane_wiadomosci), plural(s.scheduled.size, tr(R.string.wiadomosc_2), tr(R.string.wiadomosci_2), tr(R.string.wiadomosci_2))) {
            vm.push(Screen.SettingsScreen(SettingsPage.SCHEDULED))
        }
        ActionRow(Icons.Rounded.BarChart, tr(R.string.statystyki), tr(R.string.ile_piszesz_z_kim_i)) {
            vm.loadStats(); vm.push(Screen.SettingsScreen(SettingsPage.STATS))
        }
        ActionRow(Icons.Rounded.Upload, tr(R.string.eksportuj_sms_do_pliku), tr(R.string.kopia_zapasowa_w_formacie_json)) {
            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            exportLauncher.launch("OpenSMS-kopia-$stamp.json")
        }
        ActionRow(Icons.Rounded.Download, tr(R.string.importuj_sms_z_pliku), tr(R.string.pomija_wiadomosci_ktore_juz_sa)) {
            importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
        }
        backupProgress?.let { p ->
            LinearProgressIndicator(progress = { p }, color = Color.White, trackColor = Color.White.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
    }

    Group(tr(R.string.aktualizacje)) {
        SwitchRow(Icons.Rounded.SystemUpdate, tr(R.string.sprawdzaj_przy_starcie), tr(R.string.nowe_wersje_aplikacji), s.autoUpdateCheck) { s.autoUpdateCheck = it }
        val status = when (val u = vm.update) {
            is UpdateState.Checking -> tr(R.string.sprawdzanie)
            is UpdateState.UpToDate -> tr(R.string.masz_najnowsza_wersje)
            is UpdateState.Available -> tr(R.string.dostepna_wersja_1, u.info.version)
            is UpdateState.Downloading -> tr(R.string.pobieranie)
            is UpdateState.Ready, is UpdateState.Installing -> tr(R.string.instalowanie)
            is UpdateState.Failed -> u.message
            UpdateState.Idle -> if (s.lastUpdateCheck > 0) tr(R.string.ostatnio_1, formatFull(s.lastUpdateCheck).substringBeforeLast(':')) else tr(R.string.jeszcze_nie_sprawdzano)
        }
        ActionRow(Icons.Rounded.Restore, tr(R.string.sprawdz_teraz), status) { vm.checkUpdate(silent = false) }
        InfoRow(Icons.Rounded.Code, tr(R.string.wersja), UpdateManager.currentVersion)
    }

    Group("O aplikacji") {
        ActionRow(Icons.Rounded.Person, tr(R.string.pomysl_i_wykonanie), "Adam Rędzikowski · redzikowski.com") {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://redzikowski.com"))) }
        }
        ActionRow(Icons.Rounded.Email, tr(R.string.kontakt), "kontakt@redzikowski.com") {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("mailto:kontakt@redzikowski.com"))) }
        }
        ActionRow(Icons.Rounded.LocalCafe, tr(R.string.postaw_kawe), tr(R.string.wesprzyj_rozwoj_na_buycoffee_to)) {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buycoffee.to/adamredzikowski"))) }
        }
    }

    Group(tr(R.string.system)) {
        ActionRow(Icons.Rounded.Security, tr(R.string.uprawnienia), tr(R.string.kontakty_karty_sim_powiadomienia)) { onRequestPermissions() }
        ActionRow(Icons.Rounded.AdminPanelSettings, tr(R.string.ustawienia_aplikacji_w_systemie)) {
            runCatching {
                context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
            }
        }
        ActionRow(Icons.Rounded.Schedule, tr(R.string.dokladne_alarmy), if (Scheduler.canExact(context)) tr(R.string.wlaczone_zaplanowane_wysylaja_sie_co) else tr(R.string.wylaczone_wysylka_moze_sie_opoznic)) {
            runCatching { context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) }
        }
    }

    Text(
        tr(R.string.opensms_1_otwarte_oprogramowanie_mit, UpdateManager.currentVersion),
        color = Palette.TextFaint,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        textAlign = TextAlign.Center,
    )

    if (signatureDialog) {
        var text by remember { mutableStateOf(s.signature) }
        DarkDialog(
            title = tr(R.string.podpis),
            onDismiss = { signatureDialog = false },
            buttons = {
                PillButton(tr(R.string.anuluj), primary = false, onClick = { signatureDialog = false })
                PillButton(tr(R.string.zapisz), onClick = { s.signature = text.trim(); signatureDialog = false })
            },
        ) {
            Text(tr(R.string.dopisywany_na_koncu_kazdej_wysylanej), color = Palette.TextDim, fontSize = 13.sp)
            DarkField(text, { text = it }, tr(R.string.podpis), singleLine = false, minLines = 2)
        }
    }
}

@Composable
private fun ColumnScope.BlockedPage(vm: AppViewModel) {
    val s = vm.settings
    var number by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }
    Group(tr(R.string.numery)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DarkField(number, { number = it }, tr(R.string.numer_lub_nazwa_nadawcy), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Add, tr(R.string.dodaj), active = number.isNotBlank(), enabled = number.isNotBlank(), onClick = {
                s.blockedNumbers = s.blockedNumbers + number.trim(); number = ""
            })
        }
        if (s.blockedNumbers.isEmpty()) Text(tr(R.string.brak_zablokowanych_numerow), color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        s.blockedNumbers.sorted().forEach { n ->
            ActionRow(Icons.Rounded.Block, formatNumber(n), tr(R.string.dotknij_aby_odblokowac)) { vm.unblock(n); vm.toast = tr(R.string.odblokowano_1, n) }
        }
    }
    Group(tr(R.string.slowa_kluczowe)) {
        Text(
            tr(R.string.wiadomosci_zawierajace_te_slowa_bez),
            color = Palette.TextDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp),
        )
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DarkField(keyword, { keyword = it }, tr(R.string.slowo_lub_fraza), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            RoundIcon(Icons.Rounded.Add, tr(R.string.dodaj), active = keyword.isNotBlank(), enabled = keyword.isNotBlank(), onClick = {
                s.blockedKeywords = s.blockedKeywords + keyword.trim(); keyword = ""
            })
        }
        s.blockedKeywords.sorted().forEach { k ->
            ActionRow(Icons.AutoMirrored.Rounded.TextSnippet, k, tr(R.string.dotknij_aby_usunac)) { s.blockedKeywords = s.blockedKeywords - k }
        }
    }
    ActionRow(Icons.Rounded.History, tr(R.string.zatrzymane_wiadomosci), plural(s.blockedLog.size, tr(R.string.wiadomosc_2), tr(R.string.wiadomosci_2), tr(R.string.wiadomosci_2))) {
        vm.push(Screen.SettingsScreen(SettingsPage.BLOCKED_LOG))
    }
}

@Composable
private fun ColumnScope.BlockedLogPage(vm: AppViewModel) {
    val s = vm.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    SwitchRow(Icons.Rounded.History, tr(R.string.zapisuj_zatrzymane_wiadomosci), tr(R.string.wylaczone_zablokowane_wiadomosci_sa_od), s.keepBlockedLog) { s.keepBlockedLog = it }
    if (s.blockedLog.isEmpty()) {
        EmptyState(Icons.Rounded.Block, tr(R.string.nic_nie_zatrzymano), tr(R.string.tu_pojawia_sie_wiadomosci_od))
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
                PillButton(tr(R.string.przywroc_do_skrzynki), primary = false, icon = Icons.Rounded.Restore, onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { MessageStore(context).insertIncomingSms(e.address, e.body, e.date, -1, read = true) }
                        vm.toast = tr(R.string.przywrocono_do_skrzynki)
                    }
                })
                PillButton(tr(R.string.kopiuj), primary = false, icon = Icons.Rounded.ContentCopy, onClick = { MediaActions.copy(context, e.body); vm.toast = tr(R.string.skopiowano) })
            }
        }
    }
    PillButton(tr(R.string.wyczysc_liste), danger = true, onClick = { confirmClear = true })
    if (confirmClear) {
        ConfirmDialog(tr(R.string.wyczyscic), tr(R.string.lista_zatrzymanych_wiadomosci_zostanie_usunieta), tr(R.string.wyczysc), onDismiss = { confirmClear = false }) { s.clearBlockedLog() }
    }
}

@Composable
private fun ColumnScope.TemplatesPage(vm: AppViewModel) {
    val s = vm.settings
    var editing by remember { mutableStateOf<Template?>(null) }
    Text(tr(R.string.szablony_wstawiasz_w_rozmowie_przyciskiem), color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
    Group(null) {
        if (s.templates.isEmpty()) Text(tr(R.string.brak_szablonow), color = Palette.TextDim, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        s.templates.forEach { t ->
            ActionRow(Icons.AutoMirrored.Rounded.ShortText, t.text, trailing = {
                RoundIcon(Icons.Rounded.Delete, tr(R.string.usun_szablon), size = 36.dp, flat = true, tint = Palette.Record, onClick = { s.saveTemplates(s.templates - t) })
            }) { editing = t }
        }
    }
    PillButton(tr(R.string.nowy_szablon), icon = Icons.Rounded.Add, onClick = { editing = Template(0, "") })
    editing?.let { t ->
        var text by remember(t) { mutableStateOf(t.text) }
        DarkDialog(
            title = if (t.id == 0L) tr(R.string.nowy_szablon) else tr(R.string.edytuj_szablon),
            onDismiss = { editing = null },
            buttons = {
                PillButton(tr(R.string.anuluj), primary = false, onClick = { editing = null })
                PillButton(tr(R.string.zapisz), enabled = text.isNotBlank(), onClick = {
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
            DarkField(text, { text = it }, tr(R.string.tresc), singleLine = false, minLines = 3)
        }
    }
}

@Composable
private fun ColumnScope.ScheduledPage(vm: AppViewModel) {
    val s = vm.settings
    val context = LocalContext.current
    LaunchedEffect(Unit) { s.reloadScheduled() }
    if (!Scheduler.canExact(context)) {
        ActionRow(Icons.Rounded.Warning, tr(R.string.wlacz_dokladne_alarmy), tr(R.string.bez_nich_system_moze_opoznic)) {
            runCatching { context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))) }
        }
    }
    if (s.scheduled.isEmpty()) {
        EmptyState(Icons.Rounded.Schedule, tr(R.string.brak_zaplanowanych), tr(R.string.w_rozmowie_przytrzymaj_przycisk_wysylania))
        return
    }
    s.scheduled.forEach { m ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(formatFull(m.time).substringBeforeLast(':'), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                if (m.confirm) {
                    val waiting = m.time <= System.currentTimeMillis()
                    Text(
                        if (waiting) tr(R.string.czeka_na_decyzje) else tr(R.string.zapyta_przed_wyslaniem),
                        color = if (waiting) Palette.Accent else Palette.TextDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(tr(R.string.do_2) + m.addresses.joinToString(", ") { com.blacksingularities.opensms.data.Contacts.displayName(context, it) }, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Text(m.text, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(tr(R.string.wyslij_teraz), icon = Icons.AutoMirrored.Rounded.Send, onClick = {
                    Scheduler.cancel(context, m.id)
                    vm.send(Screen.Thread(null, m.addresses), m.text, emptyList(), m.subId)
                    vm.toast = tr(R.string.wysylanie_2)
                })
                PillButton(tr(R.string.anuluj), primary = false, icon = Icons.Rounded.Delete, onClick = { Scheduler.cancel(context, m.id); vm.toast = tr(R.string.anulowano) })
                PillButton(tr(R.string.edytuj), primary = false, icon = Icons.Rounded.Edit, onClick = {
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
        StatTile(tr(R.string.odebrane_sms), st.smsIn.toString(), Modifier.weight(1f))
        StatTile(tr(R.string.wyslane_sms), st.smsOut.toString(), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(tr(R.string.odebrane_mms), st.mmsIn.toString(), Modifier.weight(1f))
        StatTile(tr(R.string.wyslane_mms), st.mmsOut.toString(), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(tr(R.string.rozmowy), st.threads.toString(), Modifier.weight(1f))
        StatTile(tr(R.string.srednia_dlugosc), "${st.avgLength} zn.", Modifier.weight(1f))
    }
    StatTile(tr(R.string.najczesciej_piszesz_o), "${st.busiestHour}:00–${st.busiestHour + 1}:00", Modifier.fillMaxWidth())
    if (st.top.isNotEmpty()) {
        Group(tr(R.string.najczestsi_rozmowcy_sms)) {
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
