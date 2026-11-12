package com.storyteller_f.project

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SampleThemeDesktopTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun themeChangesUpdatePageForegroundAndPaletteInBothDirections() {
        val dark = mutableStateOf(false)
        var foreground = Color.Unspecified
        var background = Color.Unspecified
        compose.setContent {
            SampleTheme(dark.value) {
                val currentForeground = LocalContentColor.current
                val currentBackground = MaterialTheme.colorScheme.background
                SideEffect {
                    foreground = currentForeground
                    background = currentBackground
                }
                Text("Theme probe")
            }
        }
        for (isDark in listOf(false, true, false)) {
            compose.runOnIdle { dark.value = isDark }
            compose.runOnIdle {
                val expected = if (isDark) SampleDarkColorScheme else SampleLightColorScheme
                assertEquals(expected.onBackground, foreground)
                assertEquals(expected.background, background)
            }
            compose.onNodeWithText("Theme probe").assertIsDisplayed()
        }
    }

    @Test
    fun changingThemeKeepsEditorContents() {
        val dark = mutableStateOf(false)
        compose.setContent { App(darkTheme = dark.value) }
        compose.onNode(hasSetTextAction()).performTextReplacement("flowchart LR; A-->B")
        for (isDark in listOf(true, false)) {
            compose.runOnIdle { dark.value = isDark }
            compose.onNode(hasSetTextAction()).assertTextContains("flowchart LR; A-->B")
            compose.onNodeWithText("Mermaid Diagram Renderer").assertIsDisplayed()
        }
    }

    @Test
    fun renderedSvgSourceSurvivesThemeChanges() {
        val dark = mutableStateOf(false)
        compose.setContent {
            App(darkTheme = dark.value, renderDiagram = { "<svg>Theme-independent fixture</svg>" })
        }
        compose.onNodeWithText("Render").performClick()
        val svg = hasText("<svg", substring = true)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(svg).fetchSemanticsNodes().isNotEmpty()
        }
        val original = compose.onNode(svg).fetchSemanticsNode().config[SemanticsProperties.Text]
        for (isDark in listOf(true, false)) {
            compose.runOnIdle { dark.value = isDark }
            compose.onNode(svg).assertIsDisplayed()
            assertEquals(original, compose.onNode(svg).fetchSemanticsNode().config[SemanticsProperties.Text])
        }
    }

    @Test
    fun renderErrorRemainsVisibleAcrossThemeChanges() {
        val dark = mutableStateOf(false)
        compose.setContent {
            App(darkTheme = dark.value, renderDiagram = { throw IllegalArgumentException("Invalid diagram") })
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("not a supported diagram")
        compose.onNodeWithText("Render").performClick()
        val error = hasText("Error: Invalid diagram")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(error).fetchSemanticsNodes().isNotEmpty()
        }
        for (isDark in listOf(true, false)) {
            compose.runOnIdle { dark.value = isDark }
            compose.onNode(error).assertIsDisplayed()
        }
    }

    @Test
    fun reopeningAppUsesCurrentTheme() {
        val dark = mutableStateOf(false)
        val entry = mutableStateOf(0)
        compose.setContent {
            key(entry.value) {
                App(darkTheme = dark.value)
            }
        }
        for (isDark in listOf(true, false)) {
            compose.runOnIdle {
                dark.value = isDark
                entry.value++
            }
            compose.onNodeWithText("Mermaid Diagram Renderer").assertIsDisplayed()
            assertEquals(
                if (isDark) SampleDarkColorScheme.background else SampleLightColorScheme.background,
                compose.onRoot().captureToImage().toPixelMap()[0, 0],
            )
        }
    }

    @Test
    fun firstDarkEntryPaintsDarkPageBackground() {
        compose.setContent { App(darkTheme = true) }
        compose.onNodeWithText("Mermaid Diagram Renderer").assertIsDisplayed()
        assertEquals(
            SampleDarkColorScheme.background,
            compose.onRoot().captureToImage().toPixelMap()[0, 0],
        )
    }
}
