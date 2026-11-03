package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class OffersTest {
    private val curd =
        Product(
            "p5",
            "Fresh Curd",
            "Milk & Dairy",
            unit = "400 g",
            price = 60.0,
            mrp = 70.0,
            description = "Curd",
        )

    @Test
    fun firstRechargeRequiresAtLeastFiveHundred() {
        assertFalse(rechargeQuote("499", true).valid)
        assertEquals(550, rechargeQuote("500", true).credit)
    }

    @Test
    fun subsequentRechargeMustBeWholeRupeesWithinLimits() {
        listOf("9", "10001", "10.5", "abc", "-10").forEach {
            assertFalse(it, rechargeQuote(it, false).valid)
        }
        assertEquals(10, rechargeQuote("10", false).credit)
        assertEquals(10050, rechargeQuote("10000", false).credit)
    }

    @Test
    fun bundleDoesNotMultiplyFreeGiftForExtraPacks() {
        val session = Session(offerEndsAt = Long.MAX_VALUE, cart = listOf(CartLine("p5", 5)))
        assertEquals(1, freeGiftLines(session, listOf(curd)).single().line.qty)
    }

    @Test
    fun subscriptionAndDifferentPackDoNotUnlockBundle() {
        val session =
            Session(
                offerEndsAt = Long.MAX_VALUE,
                cart =
                    listOf(
                        CartLine("p5", mode = "Subscribe"),
                        CartLine("p5", variant = Variant("small", "Small", "200 g", 35.0)),
                    ),
            )
        assertEquals(0, offerQuantity(bundleOffers.first(), session.cart, listOf(curd)))
    }

    @Test
    fun expiredOffersDoNotGiveGifts() {
        val session = Session(offerEndsAt = 0, cart = listOf(CartLine("p5")))
        assertTrue(
            freeGiftLines(
                    session,
                    listOf(curd),
                    Instant.parse("2026-11-03T10:00:00Z").toEpochMilli(),
                )
                .isEmpty()
        )
    }

    @Test
    fun breadWeekendUsesIndiaTime() {
        val fridayLateUtc = Instant.parse("2026-11-06T20:00:00Z").toEpochMilli()
        assertTrue(offerAvailable(bundleOffers.last(), Session(), fridayLateUtc))
        assertFalse(
            offerAvailable(
                bundleOffers.last(),
                Session(),
                Instant.parse("2026-11-08T20:00:00Z").toEpochMilli(),
            )
        )
    }

    @Test
    fun giftsNeverAddToBill() {
        assertEquals(0.0, lineTotal(CartLine("p5", freeGift = true), curd), 0.0)
    }
}
