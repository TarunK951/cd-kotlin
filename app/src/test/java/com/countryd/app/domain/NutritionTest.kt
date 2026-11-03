package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class NutritionTest {
    private val milk =
        Product(
            "p1",
            "Cow Milk",
            "Milk & Dairy",
            unit = "1 L",
            price = 68.0,
            mrp = 75.0,
            description = "Milk",
        )

    @Test
    fun monthlyReportDoesNotCountSubscriptionAgainWhenOrderExists() {
        val day = "2026-11-04"
        val sub = Subscription("s", "p1", 1, "1 L", 68.0, Plan("Custom", day, listOf(day)))
        val order =
            Order(
                "o",
                "2026-11-03T00:00:00Z",
                day,
                "Morning",
                "Home",
                listOf(OrderLine(CartLine("p1"))),
                totals(68.0, Session()),
            )
        val report =
            monthlyNutrition(
                Session(orders = listOf(order), subscriptions = listOf(sub)),
                listOf(milk),
                YearMonth.of(2026, 11),
                LocalDate.parse("2026-11-03"),
                false,
            )
        assertEquals(1, report.packs)
        assertEquals(32.0, report.protein, .001)
    }

    @Test
    fun deliveredAndUpcomingReportsStaySeparate() {
        val order =
            Order(
                "o",
                "2026-11-01T00:00:00Z",
                "2026-11-01",
                "Morning",
                "Home",
                listOf(OrderLine(CartLine("p1"))),
                totals(68.0, Session()),
                status = "Delivered",
            )
        val session = Session(orders = listOf(order))
        assertEquals(
            1,
            monthlyNutrition(
                    session,
                    listOf(milk),
                    YearMonth.of(2026, 11),
                    LocalDate.parse("2026-11-03"),
                    true,
                )
                .packs,
        )
        assertEquals(
            0,
            monthlyNutrition(
                    session,
                    listOf(milk),
                    YearMonth.of(2026, 11),
                    LocalDate.parse("2026-11-03"),
                    false,
                )
                .packs,
        )
    }

    @Test
    fun calendarReportIncludesLeapDay() {
        assertEquals(
            29,
            monthlyNutrition(
                    Session(),
                    listOf(milk),
                    YearMonth.of(2028, 2),
                    LocalDate.of(2028, 2, 1),
                    false,
                )
                .days
                .size,
        )
    }
}
