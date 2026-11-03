package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.*

data class PackSummary(val deliveries: Int, val freePacks: Int, val chargedPacks: Int)

fun planOccurs(plan: Plan, date: LocalDate): Boolean {
    val start = runCatching { LocalDate.parse(plan.startDate) }.getOrNull() ?: return false
    val elapsed = ChronoUnit.DAYS.between(start, date)
    if (elapsed < 0) return false
    return when (plan.frequency) {
        "Daily" -> true
        "Alternate days" -> elapsed % 2 == 0L
        "Weekly" -> elapsed % 7 == 0L
        else -> date.toString() in plan.dates
    }
}

fun scheduledOn(sub: Subscription, date: LocalDate): Boolean =
    sub.status == "Active" &&
        date.toString() !in sub.skippedDates &&
        !(sub.vacationStart.isNotEmpty() &&
            date.toString() >= sub.vacationStart &&
            date.toString() <= sub.vacationEnd) &&
        planOccurs(sub.plan, date)

fun nextDelivery(sub: Subscription, today: LocalDate = LocalDate.now()): LocalDate? =
    (1L..366L).map(today::plusDays).firstOrNull { scheduledOn(sub, it) }

fun packSummary(line: CartLine): PackSummary {
    if (line.mode != "Subscribe") return PackSummary(1, 0, line.qty)
    val plan = line.plan ?: Plan()
    var deliveries = if (plan.offer == "weekly") 10 else 7
    var free = if (plan.offer in listOf("welcome", "weekly")) 3 else 0
    if (plan.offer == "monthly") {
        val start = LocalDate.parse(plan.startDate)
        deliveries = (0L..27L).count { planOccurs(plan, start.plusDays(it)) }
        free = (21L..27L).count { planOccurs(plan, start.plusDays(it)) }
    }
    return PackSummary(deliveries, free, max(0, deliveries * line.qty - free))
}

fun lineTotal(line: CartLine, product: Product): Double {
    if (line.freeGift) return 0.0
    val packs = packSummary(line)
    return (line.variant?.price ?: product.price) * packs.chargedPacks +
        line.addons.sumOf { it.price } * line.qty * packs.deliveries
}

fun totals(
    items: Double,
    session: Session,
    tip: Double = 0.0,
    usePoints: Boolean = false,
    useWallet: Boolean = false,
): Totals {
    val savings = if (session.vipActive) min(max(items, 0.0), 83.0) else 0.0
    val coupon =
        if (session.coupon == "DELIGHT10" && items >= 200) min(10.0, items - savings) else 0.0
    val delivery = if (items >= 200) 0.0 else 18.0
    val points =
        if (usePoints) max(0, min(session.points, floor(items - savings - coupon).toInt())) else 0
    val total = max(0.0, items - savings - coupon - points + delivery + max(tip, 0.0))
    val wallet = if (useWallet) max(0.0, min(session.wallet, total)) else 0.0
    return Totals(
        items,
        savings,
        coupon,
        delivery,
        points,
        total,
        wallet,
        total - wallet,
        floor(max(0.0, items - savings - coupon - points) / 20).toInt(),
    )
}

fun money(value: Double): String =
    "₹" +
        if (value == floor(value)) value.toLong().toString()
        else "%.2f".format(java.util.Locale.US, value)

fun matchesPreference(p: Product, preference: String): Boolean =
    when (preference) {
        "Vegetarian" -> p.isVeg
        "Plant-based" ->
            p.isVeg && p.category !in listOf("Milk & Dairy", "Farm Eggs") && p.id != "p14"
        else -> true
    }

private val aliases =
    mapOf(
        "fruits" to "fruit",
        "veggies" to "vegetable",
        "vegetables" to "vegetable",
        "veg" to "vegetable",
        "eggs" to "egg",
        "bananas" to "banana",
        "tomatoes" to "tomato",
        "apples" to "apple",
        "yogurt" to "curd",
        "yoghurt" to "curd",
        "dahi" to "curd",
        "palak" to "spinach",
        "carrots" to "carrot",
        "chawal" to "rice",
        "daal" to "dal",
        "dals" to "dal",
        "lentils" to "dal",
        "pulses" to "dal",
        "haldi" to "turmeric",
        "badam" to "almond",
        "almonds" to "almond",
        "atta" to "flour",
        "gajar" to "carrot",
    )

private fun normalize(value: String) =
    value
        .lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .map { aliases[it] ?: it }

fun searchProducts(catalogue: List<Product>, query: String): List<Product> {
    val terms = normalize(query)
    if (terms.isEmpty()) return emptyList()
    return catalogue
        .map { p ->
            val name = normalize(p.name).joinToString(" ")
            val text =
                normalize(
                        "${p.name} ${p.category} ${p.subCategory} ${p.description} ${if (p.highProtein) "high protein" else ""}"
                    )
                    .joinToString(" ")
            Triple(p, terms.all(text::contains), terms.count(name::contains))
        }
        .filter { it.second }
        .sortedByDescending { it.third }
        .map { it.first }
}

data class Nutrients(val protein: Double, val fiber: Double)

private val nutrients =
    mapOf(
        "p1" to Triple(3.2, 0.0, "ml"),
        "p2" to Triple(18.0, 0.0, "g"),
        "p3" to Triple(3.8, 0.0, "ml"),
        "p4" to Triple(3.2, 0.0, "ml"),
        "p5" to Triple(3.5, 0.0, "g"),
        "p7" to Triple(6.0, 0.0, "pcs"),
        "p8" to Triple(6.0, 0.0, "pcs"),
        "p10" to Triple(1.1, 2.6, "g"),
        "p11" to Triple(0.9, 1.2, "g"),
        "p12" to Triple(0.3, 2.4, "g"),
        "p13" to Triple(9.0, 6.0, "g"),
        "p15" to Triple(13.0, 10.0, "g"),
        "p16" to Triple(2.8, 2.6, "g"),
        "p17" to Triple(2.9, 2.2, "g"),
    )

fun packNutrients(id: String, unit: String, qty: Int = 1): Nutrients? {
    val n = nutrients[id] ?: return null
    val match =
        Regex("^(\\d+(?:\\.\\d+)?)\\s*(kg|g|ml|l|pcs?)$", RegexOption.IGNORE_CASE).matchEntire(unit)
            ?: return null
    val measure = match.groupValues[2].lowercase()
    val basis =
        if (measure in listOf("l", "ml")) "ml" else if (measure.startsWith("pc")) "pcs" else "g"
    if (basis != n.third) return null
    val mult =
        match.groupValues[1].toDouble() * (if (measure in listOf("l", "kg")) 1000 else 1) * qty /
            if (basis == "pcs") 1 else 100
    return Nutrients(n.first * mult, n.second * mult)
}
