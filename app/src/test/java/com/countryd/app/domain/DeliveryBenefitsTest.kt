package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DeliveryBenefitsTest {
    private val today = LocalDate.of(2026, 11, 4)

    private fun sub(offer: String, frequency: String = "Daily") =
        Subscription(
            "s1",
            "p1",
            1,
            "1 L",
            68.0,
            Plan(frequency, today.plusDays(1).toString(), offer = offer),
        )

    @Test
    fun welcomeGiftsAdvanceOnScheduledDatesOnly() {
        val days = deliveryBenefits(sub("welcome", "Alternate days"), emptyList(), today)
        assertEquals(listOf(13, 15, 17), days.filter { it.free }.map { it.date.dayOfMonth })
        assertFalse(days[1].scheduled)
    }

    @Test
    fun weeklyGiftsFollowSevenPaidDeliveries() {
        assertEquals(
            listOf(12, 13, 14),
            deliveryBenefits(sub("weekly"), emptyList(), today)
                .filter { it.free }
                .map { it.date.dayOfMonth },
        )
    }

    @Test
    fun monthlyGiftWeekUsesCalendarDays() {
        val s = sub("monthly").copy(plan = Plan(startDate = "2026-10-14", offer = "monthly"))
        assertEquals(6, deliveryBenefits(s, emptyList(), today).count { it.free })
    }

    @Test
    fun pausedPlansHaveNoGiftDates() {
        assertTrue(
            deliveryBenefits(sub("welcome").copy(status = "Paused"), emptyList(), today).none {
                it.scheduled || it.free
            }
        )
    }

    @Test
    fun escalatedRequestsStayInOpenAndSamplesHaveStableIds() {
        assertEquals(
            "Open",
            requestStatus(sampleSupportTickets().first().copy(status = "Escalated")),
        )
        assertEquals(
            listOf("SR-1001", "SR-1002", "SR-1003"),
            sampleSupportTickets().map { supportReference(it.id) },
        )
    }

    @Test
    fun customMilkDaysGenerateFortyFutureDeliveries() {
        val dates = customDeliveryDates(today, setOf(1, 3, 5))
        assertEquals(40, dates.size)
        assertEquals("2026-11-04", dates.first())
        assertTrue(dates.all { LocalDate.parse(it).dayOfWeek.value in setOf(1, 3, 5) })
        assertTrue(customDeliveryDates(today, emptySet()).isEmpty())
    }
}
