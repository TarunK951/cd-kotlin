package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DeliveryBenefit(val date: LocalDate, val scheduled: Boolean, val free: Boolean)

/** Source milkOfferWeek rules: only scheduled dates advance the gift allowance. */
fun deliveryBenefits(
    sub: Subscription,
    orders: List<Order>,
    today: LocalDate,
    count: Int = 14,
): List<DeliveryBenefit> {
    val delivered =
        orders
            .filter {
                it.status == "Delivered" &&
                    it.items.any { line -> line.subscriptionId == sub.id && !line.line.freeGift }
            }
            .map { it.deliveryDate }
            .distinct()
            .size
    var ordinal =
        if (sub.plan.offer == "weekly")
            orders
                .filter {
                    it.status == "Delivered" &&
                        it.items.any { line -> line.subscriptionId == sub.id }
                }
                .map { it.deliveryDate }
                .distinct()
                .size
        else delivered.coerceAtMost(7)
    return (1..count).map { offset ->
        val date = today.plusDays(offset.toLong())
        val scheduled = scheduledOn(sub, date)
        val elapsed = ChronoUnit.DAYS.between(LocalDate.parse(sub.plan.startDate), date)
        val free =
            scheduled &&
                when (sub.plan.offer) {
                    "welcome" -> ordinal in 4..6
                    "weekly" -> ordinal in 7..9
                    "monthly" -> elapsed in 21..27
                    else -> false
                }
        if (scheduled) ordinal++
        DeliveryBenefit(date, scheduled, free)
    }
}

fun customDeliveryDates(start: LocalDate, weekdays: Set<Int>, count: Int = 40): List<String> =
    (0L..799L)
        .asSequence()
        .map { start.plusDays(it) }
        .filter { it.dayOfWeek.value in weekdays }
        .take(count)
        .map { it.toString() }
        .toList()
