package com.countryd.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.countryd.app.data.CountryRepository
import com.countryd.app.data.Session
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var activity: ActivityScenario<MainActivity>

    @Before
    fun startFreshSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking { CountryRepository(context).save(Session()) }
        activity = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun closeActivity() {
        activity.close()
    }

    private fun tap(text: String, scroll: Boolean = false, substring: Boolean = false) {
        val node =
            compose.onAllNodes(hasText(text, substring = substring) and hasClickAction()).onLast()
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory =
            File(
                    InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                        ?: File(instrumentation.targetContext.getExternalFilesDir(null), "qa")
                            .absolutePath
                )
                .apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(directory, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    private fun completeOnboarding() {
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("welcome")
        tap("Get started")
        tap("Continue")
        tap("Verify & continue")
        tap("Continue")
        tap("Unlock my free VIP")
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Explore milk products").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Explore milk products")
    }

    @Test
    fun walletCheckoutCancellationAndPersistence() {
        completeOnboarding()
        tap("More")
        tap("Wallet & loyalty", true)
        screenshot("wallet")
        tap("Recharge wallet", true)
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Payment done").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Done")
        compose.onNodeWithText("₹550").assertExists()
        tap("Shop")
        screenshot("shop")
        tap("Milk")
        compose.onNodeWithText("Cow Milk").performClick()
        screenshot("product")
        tap("Add")
        compose.onAllNodesWithText("Cow Milk").onFirst().assertExists()
        compose
            .onNode(hasText("Continue to checkout", substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        compose.onAllNodes(isToggleable()).onLast().performScrollTo().performClick()
        compose
            .onNode(hasText("Place demo order", substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        tap("Complete demo checkout")
        compose.onNodeWithText("Order placed").assertExists()
        tap("View order")
        screenshot("order")
        tap("Cancel order", true)
        tap("Cancel order")
        compose.onNodeWithText("Order cancelled").assertExists()
        tap("Home")
        screenshot("home")
        activity.recreate()
        compose.waitUntil(15000) {
            compose
                .onAllNodesWithContentDescription("Open Delight Wallet")
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                compose.onAllNodesWithText("₹550").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("₹550").assertExists()
        tap("More")
        tap("Orders", true)
        compose.onAllNodesWithText("Cancelled").onLast().assertExists()
    }

    @Test
    fun subscriptionPlanCreatesEditableSchedule() {
        completeOnboarding()
        tap("Shop")
        tap("Milk")
        compose.onNodeWithText("Cow Milk").performClick()
        compose.onNodeWithText("Subscribe").performClick()
        tap("Set up my plan", true)
        screenshot("milk-plan")
        compose
            .onNode(hasText("Add plan", substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        compose
            .onNode(hasText("Continue to checkout", substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        compose
            .onNode(hasText("Place demo order", substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        tap("Complete demo checkout")
        tap("View order")
        tap("Subs")
        screenshot("subscriptions")
        tap("Pause", true)
        tap("Paused", substring = true)
        compose.onAllNodesWithText("Cow Milk").onFirst().assertExists()
        tap("Resume", true)
        tap("Active", substring = true)
        tap("Edit", true)
        compose.onNodeWithText("Packs per delivery").assertExists()
        tap("Save changes", true)
        compose.onAllNodesWithText("Cow Milk").onFirst().assertExists()
    }

    @Test
    fun productFeedbackKeepsItsRequestAndRevisionInTheOutbox() {
        completeOnboarding()
        tap("More")
        tap("Try product feedback", true)
        compose.onNodeWithContentDescription("3 stars").performScrollTo().performClick()
        compose.onNode(isToggleable() and hasText("Hard texture")).performScrollTo().performClick()
        compose
            .onNode(hasSetTextAction() and hasText("Tell us more (optional)"))
            .performScrollTo()
            .performTextInput("Pack was firm.")
        activity.onActivity {
            it.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.window.decorView.windowToken, 0)
        }
        tap("Save feedback", true)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val saved = runBlocking {
            withTimeout(10000) {
                CountryRepository(context).session.first {
                    it.feedback.any { response -> response.note == "Pack was firm." }
                }
            }
        }
        val pending = saved.feedbackOutbox.single()
        Assert.assertEquals("pending", pending.status)
        Assert.assertEquals("p2", pending.record.product.id)
        Assert.assertEquals("SAMPLE-201", pending.record.order.id)
        Assert.assertEquals(3, pending.record.rating)
        Assert.assertTrue(pending.record.revision >= 2)
        Assert.assertEquals(listOf("Hard texture"), pending.record.issues)
        screenshot("feedback")
        activity.recreate()
        compose.waitUntil(15000) {
            compose.onAllNodesWithText("Pack was firm.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Pack was firm.").assertExists()
    }
}
