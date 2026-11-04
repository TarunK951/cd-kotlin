package com.countryd.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.countryd.app.ui.CheckoutFailureDialog
import com.countryd.app.ui.CountryTheme
import com.countryd.app.ui.FailedPurchaseLine
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckoutFailureTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun orderFailureKeepsCartAndAllowsRetry() {
        var retry = 0
        showFailure(onRetry = { retry++ })
        compose.onNodeWithText("Order failed").assertIsDisplayed()
        compose.onNodeWithText("No charge was made. Your cart is saved.").assertIsDisplayed()
        compose.onNodeWithText("Fresh Paneer").assertIsDisplayed()
        compose.onNodeWithText("Not ordered").assertIsDisplayed()
        compose.onNodeWithText("Back to cart").assertIsDisplayed()
        screenshot("order-failed")
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(1, retry) }
    }

    @Test
    fun subscriptionFailureReturnsToChoice() {
        var back = 0
        showFailure(subscription = true, onBack = { back++ })
        compose.onNodeWithText("Subscription failed").assertIsDisplayed()
        compose.onNodeWithText("No charge was made. You can try again.").assertIsDisplayed()
        compose.onNodeWithText("Not scheduled").assertIsDisplayed()
        screenshot("subscription-failed")
        compose.onNodeWithText("Back to subscription choice").performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test
    fun unknownOutcomeOffersStatusCheckWithoutClaimingNoCharge() {
        var check = 0
        showFailure(pending = true, onRetry = { check++ })
        compose.onNodeWithText("Order status").assertIsDisplayed()
        compose.onNodeWithText("No charge was made", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Try again").assertDoesNotExist()
        compose.onNodeWithText("Check status").performClick()
        compose.runOnIdle { assertEquals(1, check) }
    }

    private fun showFailure(
        subscription: Boolean = false,
        pending: Boolean = false,
        onBack: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            CountryTheme {
                CheckoutFailureDialog(
                    subscription = subscription,
                    pending = pending,
                    busy = false,
                    error = "",
                    date = "2026-11-06",
                    lines = listOf(
                        FailedPurchaseLine(
                            "Fresh Paneer",
                            if (subscription) "Starts 6 Nov 2026 · 200 g" else "6 Nov 2026 · 200 g",
                            imagePath = "assets/reference/paneer.jpg",
                        )
                    ),
                    address = "Whitefield, Bengaluru",
                    onBack = onBack,
                    onRetry = onRetry,
                )
            }
        }
    }

    private fun screenshot(name: String) {
        val output = InstrumentationRegistry.getArguments()
            .getString("additionalTestOutputDir") ?: return
        compose.waitForIdle()
        val image = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try {
            val folder = File(output).apply { mkdirs() }
            File(folder, "$name.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally {
            image.recycle()
        }
    }
}
