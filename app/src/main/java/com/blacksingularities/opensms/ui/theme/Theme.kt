package com.blacksingularities.opensms.ui.theme

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

object Palette {
    // Czarno-biały interfejs jak w OpenPDF: akcent = biel, czerwień dla błędów i usuwania.
    val Accent = Color(0xFFFFFFFF)
    val Record = Color(0xFFFF3B47)
    val Ok = Color(0xFF4CD964)
    val Ink = Color(0xFF000000)
    val Panel = Color(0xFF000000)
    val PanelHigh = Color(0xFF1C1C1C)
    val PanelHigher = Color(0xFF2A2A2A)
    val TextDim = Color(0xB3FFFFFF)
    val TextFaint = Color(0x66FFFFFF)
    val Hairline = Color(0x1FFFFFFF)

    /** Dymki: wychodzące – białe, przychodzące – ciemnoszare. */
    val BubbleOut = Color(0xFFFFFFFF)
    val BubbleIn = Color(0xFF1C1C1C)

    /** Stonowane kolory awatarów (gdy włączone „kolorowe awatary”). */
    val AvatarColors = listOf(
        Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350), Color(0xFFAB47BC),
        Color(0xFFFFA726), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFEC407A),
        Color(0xFF8D6E63), Color(0xFF78909C),
    )
}

private val scheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.Ink,
    secondary = Color(0xFFE0E0E0),
    onSecondary = Palette.Ink,
    background = Palette.Ink,
    onBackground = Color.White,
    surface = Palette.Panel,
    onSurface = Color.White,
    surfaceVariant = Palette.PanelHigh,
    onSurfaceVariant = Palette.TextDim,
    surfaceContainerLow = Palette.Panel,
    surfaceContainer = Palette.PanelHigh,
    surfaceContainerHigh = Palette.PanelHigh,
    surfaceContainerHighest = Palette.PanelHigher,
    outline = Color(0x33FFFFFF),
    error = Palette.Record,
)

@Composable
fun OpenSmsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(handleColor = Color.White, backgroundColor = Color.White.copy(alpha = 0.3f)),
            content = content,
        )
    }
}
