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
        compose.waitForIdle()
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
            val prefix =
                InstrumentationRegistry.getArguments().getString("screenshotPrefix").orEmpty()
            File(directory, "$prefix$name.png").outputStream().use {
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
        compose.onNodeWithText("Sign in").assertDoesNotExist()
        screenshot("login-mobile")
        tap("Continue")
        screenshot("login-otp")
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
        tap("Custom", true)
        compose.onNodeWithText("Custom days").assertExists()
        screenshot("milk-plan")
        compose
            .onNode(hasText("Add plan", substring = true) and hasClickAction())
            .performScrollTo()
            .performClick()
        compose
            .onNode(hasText("Continue to checkout", substring = true) and hasClickAction())
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
    fun offerCardsUnlockGiftAndRejectInvalidCoupon() {
        completeOnboarding()
        tap("Home")
        compose.onNodeWithContentDescription("Quick view offers").performClick()
        compose.onNodeWithText("Your offers").assertExists()
        compose.onNodeWithContentDescription("Coupon code").performTextInput("WRONG")
        compose.onNodeWithContentDescription("Coupon code").performImeAction()
        activity.onActivity {
            it.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.window.decorView.windowToken, 0)
        }
        tap("Apply", true)
        compose.onNodeWithText("This code is not available. Try DELIGHT10.").assertExists()
        compose
            .onAllNodes(hasText("Add") and hasClickAction())
            .onFirst()
            .performScrollTo()
            .performClick()
        compose.onAllNodesWithText("1 free pack unlocked").onFirst().assertExists()
        screenshot("offer-cards")
        compose.onNodeWithContentDescription("Close Your offers").performClick()
        compose.onNodeWithContentDescription("View cart").performClick()
        compose.onNodeWithText("Fresh Curd · Free gift").performScrollTo().assertExists()
        screenshot("basket-gift")
        compose
            .onNodeWithContentDescription("Add a free carry bag")
            .performScrollTo()
            .performClick()
            .assertIsOn()
        tap("Price drops", true)
        tap("Buy again", true)
        screenshot("cart-extras")
        tap("Continue to checkout", substring = true)
        tap("Instructions", true)
        compose.onNodeWithText("Delivery instructions").assertExists()
        tap("Tip", true)
        compose.onNodeWithText("Thank your delivery partner").assertExists()
        tap("Delivery", true)
        compose.onNodeWithText("Morning slot").assertExists()
        screenshot("checkout-preferences")
    }

    @Test
    fun featuredMilkCardOpensSubscriptionProductDetails() {
        completeOnboarding()
        tap("Home")
        compose
            .onAllNodes(hasScrollToIndexAction())
            .onFirst()
            .performScrollToNode(hasText("COW MILK", ignoreCase = false))
        screenshot("featured-milk-cards")
        compose.onNodeWithText("COW MILK", ignoreCase = false).performClick()
        compose.onNodeWithText("Set up my plan").assertExists()
    }

    @Test
    fun mainScreenAndAccountSheetInventory() {
        completeOnboarding()
        tap("Home")
        screenshot("inventory-home")
        tap("Protein")
        screenshot("inventory-protein")
        tap("Subs")
        screenshot("inventory-subs-empty")
        tap("More")
        screenshot("inventory-account")
        tap("Delivery calendar", true)
        screenshot("inventory-calendar")
        tap("More")
        tap("Wishlist", true)
        screenshot("inventory-wishlist")
        compose.onNodeWithContentDescription("Close Your wishlist").performClick()
        tap("Delivery address", true)
        screenshot("inventory-addresses")
        compose.onNodeWithContentDescription("Close Your addresses").performClick()
        tap("Delight VIP pass", true)
        screenshot("inventory-vip")
        compose.onNodeWithContentDescription("Close Your VIP pass").performClick()
        tap("Food preferences & protein goal", true)
        screenshot("inventory-routine")
    }

    @Test
    fun supportSamplesFollowupAndResolutionPersist() {
        completeOnboarding()
        tap("More")
        tap("Help & support", true)
        compose.onNodeWithText("Choose an order").assertExists()
        screenshot("support-help")
        tap("My requests", substring = true)
        tap("View request", true)
        screenshot("support-request")
        compose
            .onNode(hasSetTextAction() and hasContentDescription("Add a follow-up"))
            .performScrollTo()
            .performTextInput("Please check delivery time.")
        activity.onActivity {
            it.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.window.decorView.windowToken, 0)
        }
        tap("Save follow-up", true)
        tap("Resolved", true)
        tap("Resolved")
        compose.onAllNodesWithText("Missing delivery").onFirst().assertExists()
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val saved = runBlocking {
            withTimeout(10000) {
                CountryRepository(context).session.first { session ->
                    session.requests.any {
                        it.topic == "Missing delivery" && it.status == "Resolved"
                    }
                }
            }
        }
        Assert.assertTrue(
            saved.requests
                .first { it.topic == "Missing delivery" }
                .messages
                .any { it.text == "Please check delivery time." }
        )
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
        compose.waitForIdle()
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
