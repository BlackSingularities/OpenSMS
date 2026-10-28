package com.blacksingularities.opensms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.ui.components.PillButton
import com.blacksingularities.opensms.ui.theme.Palette
import kotlinx.coroutines.delay

@Composable
fun AppRoot(
    vm: AppViewModel,
    onRequestDefault: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        if (!vm.isDefault) {
            Onboarding(onRequestDefault)
        } else {
            BackHandler(enabled = vm.stack.size > 1 || vm.selection.isNotEmpty() || vm.searching || vm.filter != Filter.ALL) { vm.back() }
            AnimatedContent(
                targetState = vm.screen,
                contentKey = { s ->
                    when (s) {
                        is Screen.Home -> "home"
                        is Screen.Thread -> "thread"
                        is Screen.Compose -> "compose"
                        is Screen.SettingsScreen -> "settings-${s.page}"
                        is Screen.Media -> "media"
                    }
                },
                transitionSpec = {
                    val forward = vm.stack.size > 1 && targetState != Screen.Home
                    if (forward) {
                        (slideInHorizontally { it / 5 } + fadeIn()) togetherWith (fadeOut())
                    } else {
                        fadeIn() togetherWith (slideOutHorizontally { it / 5 } + fadeOut())
                    }
                },
                label = "screen",
            ) { s ->
                when (s) {
                    is Screen.Home -> ConversationsScreen(vm)
                    is Screen.Thread -> ThreadScreen(vm, s)
                    is Screen.Compose -> ComposeScreen(vm, s)
                    is Screen.SettingsScreen -> SettingsScreen(vm, s.page, onRequestPermissions)
                    is Screen.Media -> MediaViewer(vm, s)
                }
            }
        }
        Toast(vm)
    }
}

/** Krótki komunikat na dole ekranu (biała pigułka). */
@Composable
private fun Toast(vm: AppViewModel) {
    val message = vm.toast
    LaunchedEffect(message) {
        if (message != null) {
            delay(2600)
            vm.toast = null
        }
    }
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = message != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            Text(
                message ?: "",
                color = Palette.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 480.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
            )
        }
    }
}

/** Ekran powitalny: OpenSMS musi być domyślną aplikacją SMS, żeby odbierać i wysyłać wiadomości. */
@Composable
private fun Onboarding(onRequestDefault: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Sms, null, tint = Palette.Ink, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("OpenSMS", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Wiadomości SMS i MMS – szybko, prywatnie, bez reklam", fontSize = 14.sp, color = Palette.TextDim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Feature(Icons.Rounded.Image, "SMS i MMS", "Zdjęcia, filmy, nagrania, wizytówki i rozmowy grupowe")
            Feature(Icons.Rounded.NotificationsActive, "Mądre powiadomienia", "Odpowiedź z powiadomienia i kopiowanie kodów jednym dotknięciem")
            Feature(Icons.Rounded.Schedule, "Zaplanowane i szablony", "Wyślij później, szybkie odpowiedzi, podpis")
            Feature(Icons.Rounded.Block, "Filtr spamu", "Blokowanie numerów, słów kluczowych i nieznanych nadawców")
            Feature(Icons.Rounded.Contacts, "Dual SIM i kopia zapasowa", "Wybór karty przy wysyłce, eksport i import wiadomości")
            Feature(Icons.Rounded.SystemUpdate, "Automatyczne aktualizacje", "Nowe wersje prosto z GitHuba")
        }
        Spacer(Modifier.height(28.dp))
        PillButton("Ustaw jako domyślną aplikację SMS", modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(), onClick = onRequestDefault)
        Spacer(Modifier.height(10.dp))
        Text(
            "Android pozwala odbierać i wysyłać SMS tylko domyślnej aplikacji. Wiadomości zostają w telefonie – OpenSMS nie wysyła ich nigdzie.",
            fontSize = 12.sp,
            color = Palette.TextFaint,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.widthIn(max = 420.dp),
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Palette.PanelHigh).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text, color = Palette.TextDim, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
