package com.blacksingularities.opensms.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.opensms.ui.components.RoundIcon
import com.blacksingularities.opensms.ui.components.rememberBitmap
import com.blacksingularities.opensms.ui.theme.Palette

/** Pełny ekran zdjęcia (powiększanie palcami, dwuklik) albo podgląd filmu z odtwarzaniem w zewnętrznej aplikacji. */
@Composable
fun MediaViewer(vm: AppViewModel, s: Screen.Media) {
    val context = LocalContext.current
    val video = s.mime.startsWith("video/")
    val bmp = rememberBitmap(s.uri, 2400, video = video)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var chrome by remember { mutableStateOf(true) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset = if (scale == 1f) Offset.Zero else offset + pan * scale
    }

    fun openExternal() {
        runCatching { MediaActions.open(context, s.uri, s.mime, s.name) }.onFailure { vm.toast = it.message }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (bmp == null) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            Image(
                bmp.asImageBitmap(),
                s.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (video) openExternal() else chrome = !chrome },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f; offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            },
                        )
                    }
                    .transformable(transform)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            if (video) {
                Box(Modifier.align(Alignment.Center).size(76.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, "Odtwórz", tint = Color.White, modifier = Modifier.size(46.dp))
                }
            }
        }
        if (chrome) {
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIcon(Icons.Rounded.Close, "Zamknij", onClick = { vm.back() })
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(s.name ?: if (video) "Film" else "Zdjęcie", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(s.mime, color = Palette.TextDim, fontSize = 11.sp)
                }
                RoundIcon(Icons.Rounded.Share, "Udostępnij", onClick = {
                    runCatching { MediaActions.share(context, s.uri, s.mime, s.name) }.onFailure { vm.toast = it.message }
                })
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Rounded.Save, "Zapisz", onClick = {
                    runCatching { MediaActions.save(context, s.uri, s.mime, s.name) }
                        .onSuccess { vm.toast = "Zapisano w $it" }
                        .onFailure { vm.toast = it.message ?: "Nie udało się zapisać" }
                })
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Rounded.OpenInNew, "Otwórz w innej aplikacji", onClick = { openExternal() })
            }
        }
    }
}
