package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    immediate: Boolean = false,
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
            .filter { if (immediate) it.status != "Cancelled" else it.status == "Delivered" }
            .flatMap { o -> o.items.map { it.line.productId }.distinct() }
            .groupingBy { it }
            .eachCount()
    return session.orders
        .filter { it.status == "Delivered" }
        .flatMap { order ->
            orderFeedbackRequests(order, session, catalogue).filter { request ->
                request.productId !in suppressed &&
                    if (immediate) {
                        counts[request.productId] == 1 &&
                            session.subscriptions.none { it.productId == request.productId }
                    } else {
                        isDue(request, order.deliveredAt, today, now)
                    }
            }
        }
        .sortedWith(
            compareBy<FeedbackRequest> { (counts[it.productId] ?: 0) > 1 }
                .thenBy { it.deliveryDate }
                .thenBy { it.id }
        )
        .take(1)
}

private fun isDue(
    request: FeedbackRequest,
    deliveredAt: String?,
    today: LocalDate,
    now: Instant,
): Boolean {
    val policy = feedbackPolicies.find { it.productId == request.productId } ?: return false
    val date = runCatching { LocalDate.parse(request.deliveryDate) }.getOrNull() ?: return false
    if (date.plusDays(policy.days) > today) return false
    return deliveredAt == null ||
        runCatching {
                ChronoUnit.SECONDS.between(Instant.parse(deliveredAt), now) >= policy.days * 86400
            }
            .getOrDefault(false)
}

fun orderFeedbackRequests(
    order: Order,
    session: Session,
    catalogue: List<Product>,
): List<FeedbackRequest> {
    if (order.status != "Delivered") return emptyList()
    val deliveredDate =
        runCatching { LocalDate.parse(order.deliveryDate) }.getOrNull()
            ?: order.deliveredAt?.let {
                runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    .getOrNull()
            }
            ?: return emptyList()
    return order.items.mapIndexedNotNull { index, item ->
        val line = item.line
        if (
            line.freeGift ||
                session.subscriptions.any { it.productId == line.productId && it.status != "Past" }
        ) {
            return@mapIndexedNotNull null
        }
        if (
            (line.plan != null || item.subscriptionId != null) &&
                session.subscriptions.none {
                    it.productId == line.productId &&
                        it.status == "Past" &&
                        (item.subscriptionId == null || it.id == item.subscriptionId)
                }
        ) {
            return@mapIndexedNotNull null
        }
        val product = catalogue.find { it.id == line.productId } ?: return@mapIndexedNotNull null
        val policy = feedbackPolicies.find { it.productId == product.id }
        FeedbackRequest(
            "${order.id}:$index:${product.id}",
            order.id,
            "${order.id}:$index",
            product.id,
            product.name,
            line.variant?.unit ?: product.unit,
            deliveredDate.toString(),
            order.address.split(',').takeLast(2).joinToString(", ").trim(),
            policy?.issues ?: listOf("Freshness", "Taste or smell", "Packaging", "Other"),
        )
    }
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
