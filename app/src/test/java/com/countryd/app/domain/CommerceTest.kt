package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CommerceTest {
    private val milk =
        Product(
            "p1",
            "Cow Milk",
            "Milk & Dairy",
            unit = "1 L",
            price = 68.0,
            mrp = 75.0,
            description = "Fresh milk",
            highProtein = true,
            variants =
                listOf(
                    Variant("small", "500 ml", "500 ml", 36.0),
                    Variant("large", "1 Litre", "1 L", 68.0),
                ),
        )

    @Test
    fun quickAddUsesDisplayedPack() {
        assertEquals("large", milk.defaultVariant?.id)
    }

    @Test
    fun welcomeOfferHasThreeFreePacksPerPlan() {
        val line = CartLine("p1", 2, "Subscribe", plan = Plan(offer = "welcome"))
        assertEquals(PackSummary(7, 3, 11), packSummary(line))
        assertEquals(748.0, lineTotal(line, milk), 0.001)
    }

    @Test
    fun addOnsRemainChargeableOnFreeDeliveries() {
        val line =
            CartLine(
                "p1",
                2,
                "Subscribe",
                addons = listOf(Addon("pouch", "Pouch", 15.0)),
                plan = Plan(offer = "welcome"),
            )
        assertEquals(958.0, lineTotal(line, milk), 0.001)
    }

    @Test
    fun weeklyOfferCountsTenDeliveries() {
        assertEquals(
            PackSummary(10, 3, 7),
            packSummary(CartLine("p1", mode = "Subscribe", plan = Plan(offer = "weekly"))),
        )
    }

    @Test
    fun monthlyAlternateScheduleChargesOnlyScheduledPacks() {
        val line =
            CartLine(
                "p1",
                2,
                "Subscribe",
                plan =
                    Plan(frequency = "Alternate days", startDate = "2026-11-04", offer = "monthly"),
            )
        assertEquals(PackSummary(14, 3, 25), packSummary(line))
    }

    @Test
    fun weeklyCadenceDoesNotBecomeAlternateDays() {
        val plan = Plan(frequency = "Weekly", startDate = "2026-11-04")
        assertTrue(planOccurs(plan, LocalDate.parse("2026-11-11")))
        assertFalse(planOccurs(plan, LocalDate.parse("2026-11-06")))
    }

    @Test
    fun pausedSkippedAndVacationDaysAreExcluded() {
        val sub =
            Subscription(
                "s",
                "p1",
                1,
                "1 L",
                68.0,
                Plan(startDate = "2026-11-04"),
                skippedDates = setOf("2026-11-05"),
                vacationStart = "2026-11-07",
                vacationEnd = "2026-11-09",
            )
        assertFalse(scheduledOn(sub, LocalDate.parse("2026-11-05")))
        assertFalse(scheduledOn(sub, LocalDate.parse("2026-11-09")))
        assertTrue(scheduledOn(sub, LocalDate.parse("2026-11-10")))
        assertFalse(scheduledOn(sub.copy(status = "Paused"), LocalDate.parse("2026-11-10")))
    }

    @Test
    fun customPlanOnlyDeliversOnSelectedDates() {
        val plan = Plan("Custom", "2026-11-04", listOf("2026-11-04", "2026-11-07"))
        assertTrue(planOccurs(plan, LocalDate.parse("2026-11-07")))
        assertFalse(planOccurs(plan, LocalDate.parse("2026-11-05")))
    }

    @Test
    fun rewardsWalletAndVipCannotMakePayableNegative() {
        val s =
            Session(
                vipExpiresAt = Long.MAX_VALUE,
                wallet = 500.0,
                points = 500,
                coupon = "DELIGHT10",
            )
        val bill = totals(250.0, s, usePoints = true, useWallet = true)
        assertEquals(83.0, bill.savings, 0.001)
        assertEquals(10.0, bill.coupon, 0.001)
        assertEquals(157, bill.pointsUsed)
        assertEquals(0.0, bill.toPay, 0.001)
    }

    @Test
    fun deliveryFeeThresholdIsPreserved() {
        assertEquals(18.0, totals(199.0, Session()).delivery, 0.001)
        assertEquals(0.0, totals(200.0, Session()).delivery, 0.001)
    }

    @Test
    fun walletUseIsCappedAtBillTotal() {
        val bill = totals(250.0, Session(wallet = 500.0), useWallet = true)
        assertEquals(250.0, bill.walletUsed, 0.001)
        assertEquals(0.0, bill.toPay, 0.001)
    }

    @Test
    fun searchSupportsLocalNames() {
        val curd = milk.copy(id = "p5", name = "Fresh Curd", description = "Fresh curd")
        assertEquals(listOf(curd), searchProducts(listOf(milk, curd), "dahi"))
    }

    @Test
    fun plantPreferenceExcludesDairyAndHoney() {
        assertFalse(matchesPreference(milk, "Plant-based"))
        assertTrue(matchesPreference(milk, "Vegetarian"))
        assertFalse(
            matchesPreference(milk.copy(id = "p14", category = "Bakery & Breakfast"), "Plant-based")
        )
    }

    @Test
    fun missingNutritionIsNotZero() {
        assertNull(packNutrients("p24", "1 kg"))
        assertEquals(32.0, packNutrients("p1", "1 L")!!.protein, 0.001)
        assertEquals(36.0, packNutrients("p7", "6 pcs")!!.protein, 0.001)
    }

    @Test
    fun feedbackUsesDeliveredDateAndSuppressesActiveSubscriptions() {
        val paneer = milk.copy(id = "p2", name = "Fresh Paneer")
        val order =
            Order(
                "o1",
                "2026-11-01T00:00:00Z",
                "2026-11-01",
                "Morning",
                "Home",
                listOf(OrderLine(CartLine("p2"))),
                totals(120.0, Session()),
                status = "Delivered",
                deliveredAt = "2026-11-01T06:00:00Z",
            )
        val now = Instant.parse("2026-11-03T06:00:00Z")
        val today = LocalDate.parse("2026-11-03")
        assertEquals(
            1,
            feedbackRequests(Session(orders = listOf(order)), listOf(paneer), today, now).size,
        )
        val sub = Subscription("s", "p2", 1, "200 g", 120.0, Plan())
        assertTrue(
            feedbackRequests(
                    Session(orders = listOf(order), subscriptions = listOf(sub)),
                    listOf(paneer),
                    today,
                    now,
                )
                .isEmpty()
        )
    }

    @Test
    fun feedbackCooldownAppliesAcrossProducts() {
        val session =
            Session(feedbackEvents = listOf(FeedbackEvent("r", "p2", "2026-11-02T00:00:00Z")))
        assertTrue(
            feedbackRequests(
                    session,
                    listOf(milk),
                    LocalDate.parse("2026-11-03"),
                    Instant.parse("2026-11-03T00:00:00Z"),
                )
                .isEmpty()
        )
    }
}
