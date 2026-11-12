package com.storyteller_f.project

import android.app.UiModeManager
import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.WindowCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Uses Android configuration delivery, rather than passing a theme Boolean to App. */
@SdkSuppress(minSdkVersion = 29)
class ThemeConfigurationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private lateinit var modeManager: UiModeManager
    private lateinit var owner: MainActivity
    private var originalMode = UiModeManager.MODE_NIGHT_AUTO
    private var ownsMode = false
    private val composeNightMode = AtomicInteger(Configuration.UI_MODE_NIGHT_UNDEFINED)
    private var lightBackground = Color.Unspecified

    @Before
    fun prepareLightHost() {
        owner = compose.activity
        modeManager = owner.getSystemService(UiModeManager::class.java)
        originalMode = modeManager.nightMode
        assumeTrue("Use a dedicated device in auto/light/dark mode", originalMode in listOf(
            UiModeManager.MODE_NIGHT_AUTO, UiModeManager.MODE_NIGHT_NO, UiModeManager.MODE_NIGHT_YES,
        ))
        val info = owner.packageManager.getActivityInfo(ComponentName(owner, MainActivity::class.java), 0)
        assertTrue("MainActivity must handle uiMode", info.configChanges and ActivityInfo.CONFIG_UI_MODE != 0)
        ownsMode = true
        setSystemMode(UiModeManager.MODE_NIGHT_NO)
        compose.waitForIdle()
    }

    @After
    fun restoreSystemMode() {
        if (ownsMode) setSystemMode(originalMode)
    }

    private fun setSystemMode(mode: Int) {
        val option = when (mode) {
            UiModeManager.MODE_NIGHT_AUTO -> "auto"
            UiModeManager.MODE_NIGHT_NO -> "no"
            UiModeManager.MODE_NIGHT_YES -> "yes"
            else -> error("Unsupported test mode: $mode")
        }
        val result = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("cmd uimode night $option")
        ParcelFileDescriptor.AutoCloseInputStream(result).use { it.readBytes() }
    }

    private fun installFixture(renderer: (String) -> String = { "<svg>fixture</svg>" }) {
        compose.activityRule.scenario.onActivity { activity ->
            assertSame(owner, activity)
            activity.setContent {
                val uiMode = LocalConfiguration.current.uiMode
                SideEffect { composeNightMode.set(uiMode and Configuration.UI_MODE_NIGHT_MASK) }
                App(renderDiagram = renderer)
            }
        }
        compose.waitUntil(10_000) { composeNightMode.get() == Configuration.UI_MODE_NIGHT_NO }
        lightBackground = backgroundPixel()
    }

    private fun changeNightMode(dark: Boolean) {
        setSystemMode(if (dark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO)
        val expected = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        compose.waitUntil(10_000) { composeNightMode.get() == expected }
        compose.waitForIdle()
        compose.activityRule.scenario.onActivity { activity ->
            assertSame("uiMode must retain the Activity and its composition", owner, activity)
            assertEquals(expected, activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            assertEquals(!dark, WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
        }
        // LocalConfiguration reaching Compose is insufficient unless App's default theme also updates.
        val background = backgroundPixel()
        if (dark) {
            assertTrue(background.luminance() < lightBackground.luminance())
        } else {
            assertEquals(lightBackground, background)
        }
    }

    private fun backgroundPixel(): Color = compose.onRoot().captureToImage().toPixelMap()[0, 0]

    @Test
    fun uiModeChangesKeepEditedInput() {
        installFixture()
        compose.onNode(hasSetTextAction()).performTextReplacement("flowchart LR; Edited-->Kept")
        for (dark in listOf(true, false)) {
            changeNightMode(dark)
            compose.onNode(hasSetTextAction()).assertTextContains("flowchart LR; Edited-->Kept")
        }
    }

    @Test
    fun uiModeChangesKeepSuccessfulSvg() {
        val renders = AtomicInteger()
        val svg = "<svg>unchanged source</svg>"
        installFixture { renders.incrementAndGet(); svg }
        compose.onNodeWithText("Render").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(svg)).fetchSemanticsNodes().size == 1 }
        for (dark in listOf(true, false)) {
            changeNightMode(dark)
            compose.onNodeWithText(svg).assertIsDisplayed()
            assertEquals("Theme changes must not restart rendering", 1, renders.get())
        }
    }

    @Test
    fun uiModeChangesKeepRenderError() {
        installFixture { throw IllegalArgumentException("fixture failure") }
        compose.onNodeWithText("Render").performClick()
        val error = "Error: fixture failure"
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(error)).fetchSemanticsNodes().size == 1 }
        for (dark in listOf(true, false)) {
            changeNightMode(dark)
            compose.onNodeWithText(error).assertIsDisplayed()
        }
    }

    @Test
    fun uiModeChangesKeepInFlightRequestAndPublishItsResult() {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val renders = AtomicInteger()
        val svg = "<svg>completed after configuration change</svg>"
        installFixture {
            renders.incrementAndGet()
            started.countDown()
            check(release.await(30, TimeUnit.SECONDS)) { "Test did not release renderer" }
            svg
        }
        try {
            compose.onNodeWithText("Render").performClick()
            assertTrue("Renderer must start before switching", started.await(10, TimeUnit.SECONDS))
            for (dark in listOf(true, false)) changeNightMode(dark)
            assertEquals(1, renders.get())
        } finally {
            release.countDown()
        }
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(svg)).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText(svg).assertIsDisplayed()
        assertEquals(1, renders.get())
    }
}
