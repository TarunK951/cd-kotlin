package com.countryd.app.data

import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Variant(val id: String, val name: String, val unit: String, val price: Double)

@Serializable data class Addon(val id: String, val name: String, val price: Double)

@Serializable
data class Product(
    val id: String,
    val name: String,
    val category: String,
    val subCategory: String = "",
    val unit: String,
    val price: Double,
    val mrp: Double,
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val tag: String = "",
    val description: String,
    val frequency: List<String> = emptyList(),
    val isVeg: Boolean = true,
    val highProtein: Boolean = false,
    val customizable: Boolean = false,
    val variants: List<Variant> = emptyList(),
    val addons: List<Addon> = emptyList(),
) {
    val defaultVariant
        get() =
            variants.firstOrNull { it.unit == unit && it.price == price }
                ?: variants.firstOrNull { it.price == price }
                ?: variants.firstOrNull()
}

@Serializable
data class Plan(
    val frequency: String = "Daily",
    val startDate: String = LocalDate.now().plusDays(1).toString(),
    val dates: List<String> = emptyList(),
    val slot: String = "Morning, 6:00 – 7:00 AM",
    val offer: String = "none",
)

@Serializable
data class CartLine(
    val productId: String,
    val qty: Int = 1,
    val mode: String = "Once",
    val variant: Variant? = null,
    val addons: List<Addon> = emptyList(),
    val plan: Plan? = null,
    val freeGift: Boolean = false,
) {
    val key
        get() = listOf(productId, mode, variant?.id, addons.map { it.id }.sorted(), plan).toString()
}

@Serializable
data class Subscription(
    val id: String,
    val productId: String,
    val qty: Int,
    val unit: String,
    val price: Double,
    val plan: Plan,
    val status: String = "Active",
    val skippedDates: Set<String> = emptySet(),
    val vacationStart: String = "",
    val vacationEnd: String = "",
)

@Serializable data class OrderLine(val line: CartLine, val subscriptionId: String? = null)

@Serializable
data class Order(
    val id: String,
    val createdAt: String,
    val deliveryDate: String,
    val slot: String,
    val address: String,
    val items: List<OrderLine>,
    val totals: Totals,
    val tip: Double = 0.0,
    val instructions: Set<String> = emptySet(),
    val status: String = "Order Placed",
    val rating: Int = 0,
    val deliveredAt: String? = null,
)

@Serializable
data class Totals(
    val items: Double,
    val savings: Double,
    val coupon: Double,
    val delivery: Double,
    val pointsUsed: Int,
    val total: Double,
    val walletUsed: Double,
    val toPay: Double,
    val pointsEarned: Int,
)

@Serializable
data class Address(
    val id: String,
    val label: String,
    val name: String,
    val house: String,
    val locality: String,
    val pincode: String,
) {
    val display
        get() = listOf(house, locality, pincode).filter(String::isNotBlank).joinToString(", ")
}

@Serializable
data class WalletTransaction(
    val id: String,
    val amount: Double,
    val method: String,
    val date: String,
    val bonus: Double = 0.0,
)

@Serializable
data class AutoPay(
    val enabled: Boolean = false,
    val amount: Double = 500.0,
    val threshold: Double = 100.0,
    val method: String = "",
)

@Serializable
data class Goal(
    val protein: Int = 50,
    val preference: String = "Everything",
    val focus: String = "Balanced meals",
    val enabled: Boolean = false,
    val household: Int = 2,
    val configured: Boolean = false,
)

@Serializable
data class SupportMessage(val text: String, val at: String, val fromCustomer: Boolean = true)

@Serializable
data class SupportRequest(
    val id: String,
    val topic: String,
    val issue: String,
    val orderId: String = "",
    val language: String = "English",
    val status: String = "Open",
    val messages: List<SupportMessage> = emptyList(),
)

@Serializable
data class FeedbackRequest(
    val id: String,
    val orderId: String,
    val orderLineId: String,
    val productId: String,
    val productName: String,
    val unit: String,
    val deliveryDate: String,
    val area: String,
    val issues: List<String>,
    val demo: Boolean = false,
    val canReorder: Boolean = true,
    val label: String = "",
)

@Serializable data class FeedbackSamplesFile(val requests: List<FeedbackRequest>)

@Serializable
data class FeedbackEvent(
    val requestId: String,
    val productId: String,
    val shownAt: String,
    val skipped: Boolean = false,
)

@Serializable
data class FeedbackPhoto(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long? = null,
)

@Serializable
data class FeedbackResponse(
    val request: FeedbackRequest,
    val rating: Int,
    val issues: List<String>,
    val note: String,
    val savedAt: String,
    val detailsComplete: Boolean,
    val photo: FeedbackPhoto? = null,
)

@Serializable data class FeedbackProduct(val id: String, val name: String, val unit: String)

@Serializable
data class FeedbackOrder(val id: String, val lineId: String, val deliveryDate: String)

@Serializable
data class AdminFeedbackRecord(
    val schemaVersion: Int = 1,
    val id: String,
    val revision: Int,
    val product: FeedbackProduct,
    val order: FeedbackOrder,
    val area: String,
    val rating: Int,
    val issues: List<String>,
    val note: String,
    val photo: FeedbackPhotoMetadata? = null,
    val detailsComplete: Boolean,
    val updatedAt: String,
)

@Serializable
data class OutboxItem(val record: AdminFeedbackRecord, val status: String = "pending")

@Serializable
data class FeedbackPhotoMetadata(
    val fileName: String,
    val mimeType: String,
    val fileSize: Long? = null,
)

@Serializable
data class Session(
    val offerEndsAt: Long = System.currentTimeMillis() + 3600000L,
    val loggedIn: Boolean = false,
    val phone: String = "",
    val name: String = "",
    val addresses: List<Address> = emptyList(),
    val addressId: String = "",
    val vipExpiresAt: Long = 0,
    val vipSaved: Double = 0.0,
    val cart: List<CartLine> = emptyList(),
    val orderBag: Boolean = false,
    val wishlist: Set<String> = emptySet(),
    val orders: List<Order> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val wallet: Double = 0.0,
    val transactions: List<WalletTransaction> = emptyList(),
    val autoPay: AutoPay = AutoPay(),
    val points: Int = 0,
    val pointsEarned: Int = 0,
    val pointsRedeemed: Int = 0,
    val coupon: String = "",
    val goal: Goal = Goal(),
    val recentSearches: List<String> = emptyList(),
    val requests: List<SupportRequest> = emptyList(),
    val supportDraft: String = "",
    val supportLanguage: String = "English",
    val feedback: List<FeedbackResponse> = emptyList(),
    val feedbackEvents: List<FeedbackEvent> = emptyList(),
    val feedbackOutbox: List<OutboxItem> = emptyList(),
    val sampleFeedbackDismissed: Boolean = false,
    val deliveryAlerts: Boolean = true,
    val offerAlerts: Boolean = false,
    val nextDeliveryExpanded: Boolean = true,
    val assistantMessages: List<SupportMessage> = emptyList(),
) {
    val address
        get() = addresses.find { it.id == addressId } ?: addresses.firstOrNull()

    val vipActive
        get() = vipExpiresAt > System.currentTimeMillis()
}
