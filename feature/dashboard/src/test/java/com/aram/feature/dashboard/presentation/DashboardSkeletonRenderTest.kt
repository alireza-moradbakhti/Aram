package com.aram.feature.dashboard.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.aram.core.designsystem.AramTheme
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w390dp-h1000dp-xhdpi")
class DashboardSkeletonRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `loading renders the skeleton, not the content`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DashboardHost(darkTheme = true, isLoading = true)
        }
        composeRule.mainClock.advanceTimeBy(650)

        composeRule.onNodeWithTag(SKELETON_TAG).assertExists()
        composeRule.onNodeWithText("Safe to spend").assertDoesNotExist()

        capture("dashboard-skeleton-dark.png")
    }

    @Test
    fun `loading skeleton adapts to the light theme`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            DashboardHost(darkTheme = false, isLoading = true)
        }
        composeRule.mainClock.advanceTimeBy(650)

        composeRule.onNodeWithTag(SKELETON_TAG).assertExists()

        capture("dashboard-skeleton-light.png")
    }

    @Test
    fun `loaded state swaps the skeleton for the dashboard content`() {
        composeRule.setContent {
            DashboardHost(darkTheme = true, isLoading = false)
        }

        composeRule.onNodeWithTag(SKELETON_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("Safe to spend").assertIsDisplayed()

        capture("dashboard-content-dark.png")
    }

    private fun capture(name: String) {
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("../../docs/specs/AR-046-dashboard-loading-skeleton/screenshots")
        if (!dir.isDirectory) dir.mkdirs()
        val file = File(dir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue("Screenshot $file was not written", file.length() > 0L)
    }

    @androidx.compose.runtime.Composable
    private fun DashboardHost(darkTheme: Boolean, isLoading: Boolean) {
        AramTheme(darkTheme = darkTheme) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                DashboardScreen(displayName = "Aram", isLoading = isLoading)
            }
        }
    }
}
