package com.countryd.app.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommerceAttemptTest {
    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject

    @Test
    fun buyNowRequiresOneExactItemOnItsDateWithoutTouchingOtherDates() {
        val request = json("""{"deliveryDate":"2026-11-05","addressId":14}""")
        val intent = json("""{"productId":"p2","quantity":2}""")
        val attempt = CommerceAttempt(request, emptySet(), buyNow = intent)
        val cart = json(
            """{"items":[
                {"cartItemId":21,"deliveryDate":"2026-11-05","productId":"p2","quantity":2},
                {"cartItemId":22,"deliveryDate":"2026-11-06","productId":"p5","quantity":1}
            ]}"""
        )
        assertEquals(attempt, CommerceAttempt.fromJson(attempt.toJson().toString()))
        assertEquals(21L, attempt.stagedBuyNowItem(cart)?.num("cartItemId"))
        val conflict = json(
            """{"items":[
                {"cartItemId":21,"deliveryDate":"2026-11-05","productId":"p2","quantity":2},
                {"cartItemId":22,"deliveryDate":"2026-11-05","productId":"p5","quantity":1}
            ]}"""
        )
        assertNull(attempt.stagedBuyNowItem(conflict))
    }

    @Test
    fun checkoutRecoversOnlyNewOrdersForAllAttemptedDatesAndConsumedCartLines() {
        val request = json("""{"addressId":14}""")
        val cart =
            json(
                """{"items":[
                {"cartItemId":21,"deliveryDate":"2026-11-05","productId":"p2","quantity":1},
                {"cartItemId":22,"deliveryDate":"2026-11-06","productId":"p5","quantity":2}
            ]}"""
            )
        val attempt = CommerceAttempt(request, setOf(99), cart.list("items"))
        val first =
            json(
                """{"orderId":101,"deliveryDate":"2026-11-05","addressId":14,
                "items":[{"productId":"p2","quantity":1}]}"""
            )
        val second =
            json(
                """{"orderId":102,"deliveryDate":"2026-11-06","deliveryAddressId":14,
                "items":[{"productId":"p5","quantity":2}]}"""
            )
        val existing =
            json(
                """{"orderId":99,"deliveryDate":"2026-11-05","addressId":14,
                "items":[{"productId":"p2","quantity":1}]}"""
            )
        val wrongAddress =
            json(
                """{"orderId":103,"deliveryDate":"2026-11-05","addressId":15,
                "items":[{"productId":"p2","quantity":1}]}"""
            )
        val emptyCart = json("""{"items":[]}""")
        assertEquals(attempt, CommerceAttempt.fromJson(attempt.toJson().toString()))
        assertEquals(
            listOf(first, second),
            attempt.placedOrders(listOf(existing, wrongAddress, first, second), emptyCart),
        )
        assertTrue(
            attempt.placedOrders(listOf(existing, wrongAddress, second), emptyCart).isEmpty()
        )
        assertTrue(attempt.placedOrders(listOf(first, second), cart).isEmpty())
    }

    @Test
    fun subscriptionRequiresNewIdAndMatchingTerms() {
        val request =
            json(
                """{"productId":"p2","addressId":14,"quantity":2,"startDate":"2026-11-05","frequency":"DAILY"}"""
            )
        val attempt = CommerceAttempt(request, setOf(99))
        assertNull(
            attempt.createdSubscription(
                json(
                    """{"subscriptions":[{"subscriptionId":99,"productId":"p2","addressId":14,
                "quantity":2,"startDate":"2026-11-05","frequency":"DAILY"}]}"""
                )
            )
        )
        assertNull(
            attempt.createdSubscription(
                json(
                    """{"subscriptions":[{"subscriptionId":101,"productId":"p2","addressId":14,
                "quantity":2,"startDate":"2026-11-05","frequency":"ALTERNATE_DAYS"}]}"""
                )
            )
        )
        val matching =
            json(
                """{"subscriptions":[{"subscriptionId":101,"productId":"p2","addressId":14,
                "quantity":2,"startDate":"2026-11-05","frequency":"DAILY"}]}"""
            )
        assertEquals(101L, attempt.createdSubscription(matching)?.num("subscriptionId"))
    }
}
