package com.countryd.app.api

import kotlinx.serialization.json.*

internal data class CommerceAttempt(
    val request: JsonObject,
    val knownIds: Set<Long>,
    val items: List<JsonObject> = emptyList(),
    val buyNow: JsonObject? = null,
) {
    fun toJson() = buildJsonObject {
        put("request", request)
        putJsonArray("knownIds") { knownIds.forEach { add(it) } }
        put("items", JsonArray(items))
        buyNow?.let { put("buyNow", it) }
    }

    fun stagedBuyNowItem(cart: JsonObject): JsonObject? {
        val intent = buyNow ?: return null
        return cart.list("items").filter { it.str("deliveryDate") == request.str("deliveryDate") }
            .singleOrNull()?.takeIf {
                it.num("cartItemId") > 0 && it.str("productId") == intent.str("productId") &&
                    it.num("quantity") == intent.num("quantity")
            }
    }

    fun placedOrders(orders: List<JsonObject>, cart: JsonObject): List<JsonObject> {
        if (
            items.isEmpty() ||
                items.any {
                    it.num("cartItemId") <= 0 ||
                        it.str("productId").isBlank() ||
                        it.num("quantity") <= 0
                }
        )
            return emptyList()
        val cartItemIds = items.map { it.num("cartItemId") }.toSet()
        if (cart.list("items").any { it.num("cartItemId") in cartItemIds }) return emptyList()
        val dates = items.groupBy { it.str("deliveryDate") }
        val found =
            dates.map { (date, ordered) ->
                orders
                    .filter { order ->
                        order.num("orderId") > 0 &&
                            order.num("orderId") !in knownIds &&
                            order.str("deliveryDate") == date &&
                            order.deliveryAddressId() == request.num("addressId") &&
                            itemQuantities(order.list("items")) == itemQuantities(ordered)
                    }
                    .singleOrNull() ?: return emptyList()
            }
        return found
    }

    fun createdSubscription(plans: JsonObject): JsonObject? =
        plans
            .list("subscriptions")
            .filter { plan ->
                plan.num("subscriptionId") > 0 &&
                    plan.num("subscriptionId") !in knownIds &&
                    plan.str("productId") == request.str("productId") &&
                    plan.num("quantity") == request.num("quantity") &&
                    plan.str("startDate").ifBlank { plan.str("nextDeliveryDate") } ==
                        request.str("startDate") &&
                    plan.str("frequency") == request.str("frequency") &&
                    plan.deliveryAddressId() == request.num("addressId")
            }
            .singleOrNull()

    private fun JsonObject.deliveryAddressId() =
        num("addressId").takeIf { it > 0 } ?: num("deliveryAddressId")

    private fun itemQuantities(lines: List<JsonObject>) =
        lines
            .groupingBy { it.str("productId") }
            .fold(0L) { quantity, item -> quantity + item.num("quantity") }

    companion object {
        fun fromJson(value: String): CommerceAttempt? =
            runCatching {
                    val record = Json.parseToJsonElement(value).jsonObject
                    CommerceAttempt(
                        request = requireNotNull(record.obj("request")),
                        knownIds =
                            record["knownIds"]!!.jsonArray.map { it.jsonPrimitive.long }.toSet(),
                        items = record.list("items"),
                buyNow = record.obj("buyNow"),
                    )
                }
                .getOrNull()
    }
}
