package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate
import java.time.YearMonth

data class NutritionDay(
    val date: String,
    val protein: Double,
    val fiber: Double,
    val known: Int,
    val packs: Int,
)

data class NutritionReport(val days: List<NutritionDay>) {
    val protein
        get() = days.sumOf { it.protein }

    val fiber
        get() = days.sumOf { it.fiber }

    val known
        get() = days.sumOf { it.known }

    val packs
        get() = days.sumOf { it.packs }
}

fun monthlyNutrition(
    session: Session,
    catalogue: List<Product>,
    month: YearMonth,
    today: LocalDate,
    delivered: Boolean,
): NutritionReport {
    return NutritionReport(
        (1..month.lengthOfMonth()).map { number ->
            val date = month.atDay(number)
            val dated = session.orders.filter { it.deliveryDate == date.toString() }
            val orders =
                dated.filter {
                    if (delivered) it.status == "Delivered"
                    else it.status !in listOf("Delivered", "Cancelled")
                }
            val packs =
                orders
                    .flatMap { it.items }
                    .mapNotNull { item ->
                        catalogue
                            .find { it.id == item.line.productId }
                            ?.let { p ->
                                Triple(p.id, item.line.variant?.unit ?: p.unit, item.line.qty)
                            }
                    }
                    .toMutableList()
            if (!delivered && date > today)
                session.subscriptions
                    .filter { sub ->
                        scheduledOn(sub, date) &&
                            dated.none { order ->
                                order.items.any { it.line.productId == sub.productId }
                            }
                    }
                    .forEach { sub -> packs.add(Triple(sub.productId, sub.unit, sub.qty)) }
            val known = packs.mapNotNull { (id, unit, qty) -> packNutrients(id, unit, qty) }
            NutritionDay(
                date.toString(),
                known.sumOf { it.protein },
                known.sumOf { it.fiber },
                known.size,
                packs.size,
            )
        }
    )
}

fun weeklyNutrition(
    subscriptions: List<Subscription>,
    today: LocalDate = LocalDate.now(),
): NutritionReport =
    NutritionReport(
        (0L..6L).map { offset ->
            val date = today.plusDays(offset)
            val scheduled = subscriptions.filter { scheduledOn(it, date) }
            val known = scheduled.mapNotNull { packNutrients(it.productId, it.unit, it.qty) }
            NutritionDay(
                date.toString(),
                known.sumOf { it.protein },
                known.sumOf { it.fiber },
                known.size,
                scheduled.size,
            )
        }
    )
