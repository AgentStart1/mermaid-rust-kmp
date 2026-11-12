package com.storyteller_f.project

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class SampleThemeContrastTest {
    @Test
    fun lightThemeTextHasAccessibleContrast() = assertTextContrast(SampleLightColorScheme)

    @Test
    fun darkThemeTextHasAccessibleContrast() = assertTextContrast(SampleDarkColorScheme)

    private fun assertTextContrast(scheme: ColorScheme) {
        val pairs = mapOf(
            "page text" to (scheme.onBackground to scheme.background),
            "input text" to (scheme.onSurface to scheme.surface),
            "input label" to (scheme.onSurfaceVariant to scheme.surface),
            "focused input label" to (scheme.primary to scheme.surface),
            "SVG source text" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "button text" to (scheme.onPrimary to scheme.primary),
            "error text" to (scheme.error to scheme.background),
        )
        pairs.forEach { (name, colors) ->
            val ratio = contrast(colors.first, colors.second)
            assertTrue(ratio >= 4.5f, "$name contrast was $ratio, expected at least 4.5:1")
        }
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val first = foreground.luminance()
        val second = background.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }
}
