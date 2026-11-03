package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.Instant
import java.time.ZoneId

data class RechargeQuote(val amount: Int, val valid: Boolean, val bonus: Int) {
    val credit
        get() = if (valid) amount + bonus else 0
}

fun rechargeQuote(input: String, firstPayment: Boolean): RechargeQuote {
    val amount = input.toIntOrNull() ?: 0
    val valid = Regex("[0-9]+").matches(input) && amount in (if (firstPayment) 500 else 10)..10000
    return RechargeQuote(amount, valid, if (valid && amount >= 500) 50 else 0)
}

data class BundleOffer(val id: String, val productId: String, val required: Int, val free: Int)

val bundleOffers =
    listOf(
        BundleOffer("curd-duo", "p5", 1, 1),
        BundleOffer("milk-week", "p1", 4, 3),
        BundleOffer("weekend-bread", "p13", 2, 1),
    )

fun offerAvailable(
    offer: BundleOffer,
    session: Session,
    now: Long = System.currentTimeMillis(),
): Boolean {
    val day = Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Kolkata")).dayOfWeek.value
    return if (offer.id == "weekend-bread") day in listOf(6, 7) else now < session.offerEndsAt
}

fun offerQuantity(offer: BundleOffer, cart: List<CartLine>, catalogue: List<Product>): Int {
    val p = catalogue.firstOrNull { it.id == offer.productId } ?: return 0
    return cart
        .filter {
            it.productId == p.id &&
                it.mode == "Once" &&
                !it.freeGift &&
                (it.variant == null || it.variant.unit == p.unit && it.variant.price == p.price)
        }
        .sumOf { it.qty }
}

fun freeGiftLines(
    session: Session,
    catalogue: List<Product>,
    now: Long = System.currentTimeMillis(),
): List<OrderLine> =
    bundleOffers
        .filter {
            offerAvailable(it, session, now) &&
                offerQuantity(it, session.cart, catalogue) >= it.required
        }
        .map { OrderLine(CartLine(it.productId, it.free, freeGift = true)) }
