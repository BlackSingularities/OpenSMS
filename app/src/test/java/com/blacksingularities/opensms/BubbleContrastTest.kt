package com.blacksingularities.opensms

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.blacksingularities.opensms.data.BubbleSkin
import com.blacksingularities.opensms.data.BubbleTint
import com.blacksingularities.opensms.ui.BubbleColors
import com.blacksingularities.opensms.ui.CustomBubble
import com.blacksingularities.opensms.ui.skinPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tekst i linki w dymku muszą być czytelne w każdym skinie. Linki, wyróżnienia i znaczek kodu
 * dobierają kolor wg [BubbleColors.lightBackground], więc ta flaga musi zgadzać się z tym,
 * czy tekst jest ciemniejszy od faktycznego tła (dymek na czarnym tle rozmowy).
 */
class BubbleContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    private fun check(name: String, c: BubbleColors) {
        val bg = c.background.compositeOver(Color.Black)
        // Próg łapie tekst niewidoczny (czerń na czerni ≈ 1:1); zielony „iOS SMS” z bielą ma celowo ok. 2:1.
        val ratio = contrast(c.content, bg)
        assertTrue("$name: tekst prawie niewidoczny (kontrast $ratio)", ratio >= 1.8f)
        assertEquals("$name: lightBackground nie pasuje do tła", c.content.luminance() < bg.luminance(), c.lightBackground)
    }

    @Test
    fun `kazdy skin ma czytelny tekst i zgodna flage tla`() {
        val custom = listOf(
            CustomBubble(0xFF0A84FF.toInt(), 0, 0xFF26252A.toInt(), false, true),
            CustomBubble(0xFFFFFFFF.toInt(), 0xFFFFE14D.toInt(), 0xFFB8B8B8.toInt(), true, false),
            CustomBubble(0xFF808080.toInt(), 0, 0xFF767676.toInt(), false, true),
        )
        for (skin in BubbleSkin.entries) for (tint in BubbleTint.entries) for (cb in custom) {
            val p = skinPalette(skin, tint, cb)
            check("$skin/$tint out", p.out)
            check("$skin/$tint inc", p.inc)
        }
    }

    @Test
    fun `szary tekst na przezroczystym dymku to ciemne tlo`() {
        // „Bez dymków”: #B8B8B8 ma jasność < 0,5, ale leży na czerni – linki nie mogą być czarne.
        assertEquals(false, BubbleColors(Color.Transparent, Color(0xFFB8B8B8)).lightBackground)
    }
}
