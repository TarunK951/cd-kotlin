package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

fun morningProducts(catalogue: List<Product>, goal: Goal): List<Product> {
    val ids =
        when (goal.focus) {
            "Milk essentials" -> listOf("p1", "p5")
            "More protein" -> listOf("p2", "p7", "p15", "p17")
            "More plants" -> listOf("p15", "p10", "p16", "p17")
            else -> listOf("p15", "p1", "p10")
        }
    return ids.mapNotNull { id -> catalogue.find { it.id == id } }
        .filter { matchesPreference(it, goal.preference) }
}

private val pairings =
    mapOf(
        "p1" to listOf("p15", "p10"),
        "p2" to listOf("p17", "p11", "p5"),
        "p3" to listOf("p15", "p10", "p13"),
        "p4" to listOf("p15", "p10", "p14"),
        "p5" to listOf("p10", "p14", "p15"),
        "p6" to listOf("p13", "p7", "p2"),
        "p7" to listOf("p13", "p11"),
        "p8" to listOf("p13", "p11", "p6"),
        "p9" to listOf("p10", "p12", "p15"),
        "p10" to listOf("p15", "p1", "p13"),
        "p11" to listOf("p2", "p13"),
        "p12" to listOf("p15", "p5", "p14"),
        "p13" to listOf("p14", "p10", "p7"),
        "p14" to listOf("p13", "p15", "p10"),
        "p15" to listOf("p1", "p10", "p14"),
        "p16" to listOf("p2", "p11"),
        "p17" to listOf("p2", "p11"),
    )

fun complementaryProducts(
    cart: List<CartLine>,
    catalogue: List<Product>,
    preference: String,
): List<Product> {
    val ids = cart.map { it.productId }.toSet()
    return cart
        .flatMap { pairings[it.productId].orEmpty() }
        .distinct()
        .filterNot { it in ids }
        .mapNotNull { id -> catalogue.find { it.id == id } }
        .filter { matchesPreference(it, preference) }
        .take(3)
}

data class AssistantReply(
    val text: String,
    val products: List<Product> = emptyList(),
    val detail: String? = null,
    val action: String? = null,
    val orderId: String = "",
    val openGoal: Boolean = false,
    val quantity: Int? = null,
)

fun assistantReply(query: String, session: Session, catalogue: List<Product>): AssistantReply {
    val lower = query.trim().lowercase()
    fun contains(pattern: String) = Regex(pattern).containsMatchIn(lower)
    val preference = session.goal.preference
    val known =
        session.cart.mapNotNull { line ->
            catalogue
                .find { it.id == line.productId }
                ?.let { p -> packNutrients(p.id, line.variant?.unit ?: p.unit, line.qty) }
        }
    fun summary(nutrient: String? = null): String {
        if (session.cart.isEmpty()) return "Your basket is empty."
        if (known.isEmpty()) return "Nutrition estimates are unavailable for these packs."
        val protein = known.sumOf { it.protein }.roundToInt()
        val fiber = known.sumOf { it.fiber }.roundToInt()
        val amounts =
            when (nutrient) {
                "protein" -> "${protein}g protein"
                "fiber" -> "${fiber}g fibre"
                else -> "${protein}g protein and ${fiber}g fibre"
            }
        return "About $amounts in whole packs.${if(known.size<session.cart.size)" Some packs have no data." else ""} Estimated food totals, not intake."
    }
    val fallback = {
        complementaryProducts(session.cart, catalogue, preference).ifEmpty {
            morningProducts(catalogue, session.goal).filter { p ->
                session.cart.none { it.productId == p.id }
            }
        }
    }
    val reply =
        when {
            contains("review(?!.*week)|what.*(?:getting|cart|basket)|show.*basket") -> {
                val subtotal =
                    session.cart.sumOf { line ->
                        lineTotal(line, catalogue.first { it.id == line.productId })
                    }
                val packs = session.cart.sumOf { it.qty }
                AssistantReply(
                    if (session.cart.isEmpty())
                        "Your basket is empty. Tell me what you’d like for breakfast or your week."
                    else
                        "Items total ${money(subtotal)} · $packs pack${if(packs==1)"" else "s"}. ${summary()}",
                    detail = "basket",
                )
            }
            contains("quality|freshness|seal|storage") ->
                AssistantReply(
                    "Check the seal, use-by date and storage instructions. Something damaged? Keep the pack and use order help. Live batch reports aren’t available in this demo.",
                    action = "quality",
                )
            contains("month") -> {
                val today = LocalDate.now()
                val delivered =
                    monthlyNutrition(session, catalogue, YearMonth.from(today), today, true)
                val upcoming =
                    monthlyNutrition(session, catalogue, YearMonth.from(today), today, false)
                AssistantReply(
                    if (delivered.known == 0)
                        "No delivered nutrition estimates this month. Open the calendar to review your deliveries."
                    else
                        "This month: about ${delivered.protein.roundToInt()}g protein and ${delivered.fiber.roundToInt()}g fibre in delivered packs. ${if(upcoming.known>0)"Upcoming: ~${upcoming.protein.roundToInt()}g protein. " else ""}Food estimates, not intake. Compare months in the calendar.",
                    action = "month",
                )
            }
            contains("my morning|my routine|personalised basket") ->
                AssistantReply(
                    "Ideas for ${session.goal.household} people · ${preference.lowercase()}. Adjust packs below to suit your household.",
                    morningProducts(catalogue, session.goal),
                )
            contains(
                "issue|missing|late|damaged|refund|help|delivery|order status|human|agent|support|complaint|charged"
            ) -> {
                val order =
                    session.orders.find { lower.contains(it.id.lowercase()) }
                        ?: session.orders.maxByOrNull { it.createdAt }
                AssistantReply(
                    if (order == null)
                        "No orders saved yet. Tap Customer care; your message is ready there."
                    else
                        "Order ${order.id} · ${order.status} · ${money(order.totals.total)}. Use Customer care to continue with this order and your message. Live tracking and refunds need connected support.",
                    action = "support",
                    orderId = order?.id.orEmpty(),
                )
            }
            contains("fiber|fibre") && !contains("week|month") ->
                AssistantReply(
                    "${summary("fiber")} Try these fibre-containing ingredients.",
                    catalogue
                        .filter {
                            matchesPreference(it, preference) &&
                                (packNutrients(it.id, it.unit)?.fiber ?: 0.0) > 0
                        }
                        .sortedByDescending { packNutrients(it.id, it.unit)?.fiber ?: 0.0 }
                        .take(4),
                )
            contains("add more|suggest more|more items") ->
                AssistantReply(
                    if (fallback().isEmpty()) "Try a product name to find more ingredients."
                    else "A few ideas for your basket. Choose packs below.",
                    fallback(),
                )
            contains("protein") && !contains("week") ->
                if (!session.goal.enabled)
                    AssistantReply(
                        "Set an optional protein goal to compare your basket and find ingredients.",
                        openGoal = true,
                    )
                else
                    AssistantReply(
                        "${summary("protein")} Your goal: ${session.goal.protein}g/day. Packs may last several days.",
                        catalogue
                            .filter {
                                matchesPreference(it, preference) &&
                                    (packNutrients(it.id, it.unit)?.protein ?: 0.0) > 5
                            }
                            .sortedByDescending { packNutrients(it.id, it.unit)?.protein ?: 0.0 }
                            .take(3),
                        detail = "basket",
                    )
            contains("diet|personal|preference|target|goal|setup") ->
                AssistantReply(
                    "Choose your food preference and optional protein goal. Change them anytime.",
                    openGoal = true,
                )
            contains("week|nutrient|nutrition|covered") -> {
                val week = weeklyNutrition(session.subscriptions)
                AssistantReply(
                    if (week.packs == 0)
                        "No deliveries scheduled for the next 7 days. Your weekly view is below."
                    else if (week.known == 0)
                        "Nutrition estimates are unavailable for your scheduled packs. Your delivery view is below."
                    else
                        "Next 7 days: about ${week.protein.roundToInt()}g protein and ${week.fiber.roundToInt()}g fibre in scheduled packs. Pauses and skips included; food totals, not intake.",
                    detail = "week",
                )
            }
            contains("basket|combination|together|what.*have") ->
                AssistantReply(
                    if (fallback().isEmpty()) "Try a product name to find more ingredients."
                    else "A few ideas for your basket. Add only what you need.",
                    fallback(),
                )
            contains("vegetable|paneer|dinner|plant") ->
                AssistantReply(
                    if (preference == "Plant-based")
                        "Sauté spinach, broccoli and tomatoes for a simple vegetable side. Oats can add variety to your week."
                    else
                        "Try paneer with spinach and tomatoes, plus broccoli on the side. Choose ingredients below.",
                    catalogue.filter { it.id in listOf("p2", "p11", "p16", "p17") },
                )
            contains("breakfast|oat|bread|toast") ->
                AssistantReply(
                    if (preference == "Plant-based")
                        "Try banana oats or banana toast. These picks match your plant-based preference."
                    else
                        "Try banana oats with milk, or banana and honey toast. Pick ingredients below.",
                    catalogue.filter {
                        it.id in
                            if (contains("bread|toast")) listOf("p13", "p10", "p14", "p7")
                            else listOf("p15", "p1", "p10")
                    },
                )
            else -> {
                val stop =
                    setOf(
                        "order",
                        "please",
                        "some",
                        "add",
                        "want",
                        "buy",
                        "the",
                        "for",
                        "with",
                        "pack",
                        "packs",
                        "show",
                        "find",
                        "looking",
                        "need",
                        "can",
                        "you",
                        "get",
                        "have",
                        "any",
                        "products",
                        "product",
                    )
                val terms =
                    lower
                        .split(Regex("\\W+"))
                        .filter { it.length > 2 && it !in stop }
                        .joinToString(" ")
                val products =
                    if (terms.isBlank()) emptyList() else searchProducts(catalogue, terms)
                if (products.isNotEmpty())
                    AssistantReply(
                        "Found these products. Choose packs or open details below.",
                        products,
                    )
                else if (contains("jam|pickle|sauce|peanut"))
                    AssistantReply(
                        "That isn’t in the catalogue yet. Try honey with toast and banana instead.",
                        catalogue.filter { it.id in listOf("p13", "p14", "p10") },
                    )
                else
                    AssistantReply(
                        "Try a product name, “breakfast ideas”, “review basket” or “delivery help”."
                    )
            }
        }
    val quantity =
        Regex(
                "\\b(\\d{1,2})\\s*(?:packs?\\b|(?=milk|eggs?|paneer|spinach|broccoli|oats|bread|bananas?|tomatoes|curd|rice|carrots?|dal|oil|almonds?|turmeric))"
            )
            .find(lower)
            ?.groupValues
            ?.get(1)
            ?.toInt()
            ?.coerceIn(1, 20)
    return reply.copy(
        products = reply.products.filter { matchesPreference(it, preference) },
        quantity = quantity,
    )
}
