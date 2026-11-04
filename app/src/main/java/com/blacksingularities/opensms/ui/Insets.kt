package com.blacksingularities.opensms.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Odstępy od krawędzi ekranu dla całego widoku: po bokach i na dole pełne („bezpieczne”)
 * marginesy, a od góry dokładnie wysokość paska stanu – bez dodatku z wycięcia na kamerę,
 * dzięki czemu treść zaczyna się od razu pod paskiem, bez pustego pasa.
 */
@Composable
fun Modifier.screenPadding(): Modifier = this.windowInsetsPadding(
    WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        .add(WindowInsets.statusBars.only(WindowInsetsSides.Top)),
)
