package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class FeedbackPolicy(val productId: String, val days: Long, val issues: List<String>)

val feedbackPolicies =
    listOf(
        FeedbackPolicy(
            "p2",
            1,
            listOf("Hard texture", "Unusual smell", "Near expiry", "Damaged pack", "Other"),
        ),
        FeedbackPolicy(
            "p5",
            1,
            listOf("Too sour", "Watery texture", "Unusual smell", "Near expiry", "Other"),
        ),
        FeedbackPolicy(
            "p13",
            2,
            listOf("Stale or dry", "Mould", "Damaged slices", "Near expiry", "Other"),
        ),
        FeedbackPolicy(
            "p24",
            5,
            listOf("Coarse texture", "Unusual smell", "Insects in pack", "Damaged pack", "Other"),
        ),
        FeedbackPolicy(
            "p6",
            7,
            listOf("Unusual taste", "Unusual smell", "Unexpected texture", "Leaking jar", "Other"),
        ),
    )

fun feedbackRequests(
    session: Session,
    catalogue: List<Product>,
    today: LocalDate = LocalDate.now(),
    now: Instant = Instant.now(),
): List<FeedbackRequest> {
    if (
        session.feedbackEvents.any {
            runCatching { ChronoUnit.HOURS.between(Instant.parse(it.shownAt), now) < 72 }
                .getOrDefault(false)
        }
    )
        return emptyList()
    val suppressed =
        session.feedbackEvents.map { it.productId }.toSet() +
            session.feedback.filter { !it.request.demo }.map { it.request.productId }
    val counts =
        session.orders
            .filter { it.status == "Delivered" }
            .flatMap { o -> o.items.map { it.line.productId }.distinct() }
            .groupingBy { it }
            .eachCount()
    return session.orders
        .filter { it.status == "Delivered" }
        .flatMap { order ->
            order.items.mapIndexedNotNull { index, item ->
                val policy =
                    feedbackPolicies.find { it.productId == item.line.productId }
                        ?: return@mapIndexedNotNull null
                if (item.line.freeGift) return@mapIndexedNotNull null
                if (
                    (item.line.plan != null || item.subscriptionId != null) &&
                        session.subscriptions.none {
                            it.productId == item.line.productId &&
                                it.status == "Past" &&
                                (item.subscriptionId == null || it.id == item.subscriptionId)
                        }
                )
                    return@mapIndexedNotNull null
                if (
                    item.line.productId in suppressed ||
                        session.subscriptions.any {
                            it.productId == item.line.productId && it.status != "Past"
                        }
                )
                    return@mapIndexedNotNull null
                val date =
                    runCatching { LocalDate.parse(order.deliveryDate) }.getOrNull()
                        ?: return@mapIndexedNotNull null
                if (date.plusDays(policy.days) > today) return@mapIndexedNotNull null
                if (
                    order.deliveredAt != null &&
                        runCatching {
                                ChronoUnit.SECONDS.between(Instant.parse(order.deliveredAt), now) <
                                    policy.days * 86400
                            }
                            .getOrDefault(true)
                )
                    return@mapIndexedNotNull null
                val p =
                    catalogue.find { it.id == item.line.productId } ?: return@mapIndexedNotNull null
                FeedbackRequest(
                    "${order.id}:$index:${p.id}",
                    order.id,
                    "${order.id}:$index",
                    p.id,
                    p.name,
                    item.line.variant?.unit ?: p.unit,
                    order.deliveryDate,
                    order.address.split(',').takeLast(2).joinToString(", ").trim(),
                    policy.issues,
                )
            }
        }
        .sortedWith(
            compareBy<FeedbackRequest> { (counts[it.productId] ?: 0) > 1 }
                .thenBy { it.deliveryDate }
                .thenBy { it.id }
        )
        .take(1)
}

fun sampleFeedback(product: Product, today: LocalDate = LocalDate.now()): FeedbackRequest {
    val policy = feedbackPolicies.first { it.productId == product.id }
    return FeedbackRequest(
        "feedback-preview:${product.id}",
        "PREVIEW-${product.id}",
        "PREVIEW-${product.id}:0",
        product.id,
        product.name,
        product.unit,
        today.minusDays(policy.days).toString(),
        "Indiranagar · sample area",
        policy.issues,
        true,
    )
}
