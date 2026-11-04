package com.countryd.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CountryState(
    val loading: Boolean = true,
    val products: List<Product> = emptyList(),
    val session: Session = Session(),
    val error: String? = null,
    val feedbackPrompt: FeedbackRequest? = null,
    val sampleRequests: List<FeedbackRequest> = emptyList(),
    val reorderDate: String? = null,
    val feedbackReorderOrderId: String? = null,
)

class CountryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CountryRepository(application)
    private val mutable = MutableStateFlow(CountryState())
    val state = mutable.asStateFlow()
    // Each snapshot is complete; keep only the latest pending write.
    private val writes = Channel<Session>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            try {
                mutable.value =
                    CountryState(
                        false,
                        repository.catalogue(),
                        repository.session.first(),
                        sampleRequests = repository.feedbackSamples(),
                    )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update {
                    it.copy(
                        loading = false,
                        error = "Unable to restore the app. ${e.localizedMessage}",
                    )
                }
            }
        }
        viewModelScope.launch {
            for (snapshot in writes) try {
                repository.save(snapshot)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                mutable.update {
                    it.copy(
                        error = "Changes could not be saved. Please retry before closing the app."
                    )
                }
            }
        }
    }

    fun update(transform: (Session) -> Session) {
        val next = transform(mutable.value.session)
        mutable.update { it.copy(session = next) }
        writes.trySend(next)
    }

    fun clearError() {
        mutable.update { it.copy(error = null) }
    }

    fun retrySave() {
        writes.trySend(state.value.session)
        clearError()
    }

    fun add(line: CartLine) = update { s ->
        val existing = s.cart.find { it.key == line.key }
        s.copy(
            cart =
                if (existing == null) s.cart + line
                else
                    s.cart.map {
                        if (it.key == line.key) it.copy(qty = (it.qty + line.qty).coerceAtMost(20))
                        else it
                    }
        )
    }

    fun stageFeedbackReorder(line: CartLine, date: String) {
        add(line)
        mutable.update { it.copy(reorderDate = date) }
    }

    fun quantity(key: String, delta: Int) = update { s ->
        s.copy(
            cart =
                s.cart
                    .map {
                        if (it.key == key) it.copy(qty = (it.qty + delta).coerceAtMost(20)) else it
                    }
                    .filter { it.qty > 0 }
        )
    }

    fun favorite(id: String) = update {
        it.copy(wishlist = if (id in it.wishlist) it.wishlist - id else it.wishlist + id)
    }

    fun saveAddress(address: Address) = update {
        it.copy(
            addresses = it.addresses.filterNot { a -> a.id == address.id } + address,
            addressId = address.id,
            name = address.name,
        )
    }

    fun search(query: String) {
        if (query.trim().length >= 2)
            update {
                it.copy(
                    recentSearches =
                        (listOf(query.trim()) +
                                it.recentSearches.filterNot { old ->
                                    old.equals(query.trim(), true)
                                })
                            .take(6)
                )
            }
    }

    fun topUp(
        amount: Double,
        method: String,
        transactionId: String = UUID.randomUUID().toString(),
    ) {
        if (state.value.session.transactions.any { it.id == transactionId }) return
        val firstPayment =
            state.value.session.transactions.none { it.method.startsWith("Demo top-up") }
        val quote =
            rechargeQuote(if (amount % 1.0 == 0.0) amount.toInt().toString() else "", firstPayment)
        if (!quote.valid) return
        update {
            it.copy(
                wallet = it.wallet + quote.credit,
                transactions =
                    listOf(
                        WalletTransaction(
                            transactionId,
                            amount,
                            "Demo top-up · $method",
                            Instant.now().toString(),
                            quote.bonus.toDouble(),
                        )
                    ) + it.transactions,
            )
        }
    }

    fun checkout(
        date: String,
        slot: String,
        tip: Double,
        instructions: Set<String>,
        usePoints: Boolean,
        useWallet: Boolean,
    ): String? {
        val s = state.value.session
        val products = state.value.products
        if (
            s.cart.isEmpty() ||
                s.address == null ||
                runCatching { LocalDate.parse(date) > LocalDate.now() }.getOrDefault(false).not()
        )
            return null
        val items = s.cart.sumOf { lineTotal(it, products.first { p -> p.id == it.productId }) }
        val bill = totals(items, s, tip, usePoints, useWallet)
        val id = "CD-${System.currentTimeMillis()}"
        val now = Instant.now().toString()
        var subscriptions = s.subscriptions
        val lines =
            s.cart.mapIndexed { index, line ->
                if (line.mode != "Subscribe") return@mapIndexed OrderLine(line)
                val product = products.first { it.id == line.productId }
                val plan = line.plan ?: Plan(startDate = date, slot = slot)
                val existing =
                    subscriptions.find {
                        it.productId == line.productId &&
                            it.unit == (line.variant?.unit ?: product.unit) &&
                            it.plan == plan &&
                            it.status != "Past"
                    }
                val sub =
                    Subscription(
                        existing?.id ?: "sub-$id-$index",
                        line.productId,
                        line.qty,
                        line.variant?.unit ?: product.unit,
                        line.variant?.price ?: product.price,
                        plan,
                    )
                subscriptions =
                    subscriptions
                        .map {
                            if (it.id == sub.id) sub
                            else if (it.productId == line.productId && it.status != "Past")
                                it.copy(status = "Past")
                            else it
                        }
                        .let { if (existing == null) it + sub else it }
                OrderLine(line, sub.id)
            }
        val order =
            Order(
                id,
                now,
                date,
                slot,
                s.address!!.display,
                lines + freeGiftLines(s, products),
                bill,
                tip,
                instructions,
            )
        update {
            it.copy(
                orders = listOf(order) + it.orders,
                subscriptions = subscriptions,
                cart = emptyList(),
                wallet = it.wallet - bill.walletUsed,
                points = it.points - bill.pointsUsed + bill.pointsEarned,
                pointsEarned = it.pointsEarned + bill.pointsEarned,
                pointsRedeemed = it.pointsRedeemed + bill.pointsUsed,
                vipSaved = it.vipSaved + bill.savings,
                transactions =
                    if (bill.walletUsed > 0)
                        listOf(WalletTransaction("pay-$id", -bill.walletUsed, "Order $id", now)) +
                            it.transactions
                    else it.transactions,
            )
        }
        mutable.update {
            if (it.reorderDate == null) it
            else it.copy(reorderDate = null, feedbackReorderOrderId = id)
        }
        return id
    }

    fun cancelOrder(id: String) = update { s ->
        val order = s.orders.find { it.id == id } ?: return@update s
        if (order.status in listOf("Cancelled", "Out for Delivery", "Delivered")) return@update s
        val walletRefund = order.totals.walletUsed
        s.copy(
            orders = s.orders.map { if (it.id == id) it.copy(status = "Cancelled") else it },
            wallet = s.wallet + walletRefund,
            points =
                (s.points - order.totals.pointsEarned + order.totals.pointsUsed).coerceAtLeast(0),
            pointsEarned = (s.pointsEarned - order.totals.pointsEarned).coerceAtLeast(0),
            pointsRedeemed = (s.pointsRedeemed - order.totals.pointsUsed).coerceAtLeast(0),
            transactions =
                if (walletRefund > 0)
                    listOf(
                        WalletTransaction(
                            "refund-$id",
                            walletRefund,
                            "Order cancelled · $id",
                            Instant.now().toString(),
                        )
                    ) + s.transactions
                else s.transactions,
        )
    }

    fun repeat(order: Order) {
        order.items
            .filterNot { it.line.freeGift }
            .forEach { item -> add(item.line.copy(mode = "Once", plan = null)) }
    }

    fun editSubscription(sub: Subscription) = update {
        it.copy(subscriptions = it.subscriptions.map { old -> if (old.id == sub.id) sub else old })
    }

    fun saveFeedback(response: FeedbackResponse) = update { s ->
        val id = "feedback:${response.request.id}"
        val revision = (s.feedbackOutbox.find { it.record.id == id }?.record?.revision ?: 0) + 1
        val record =
            AdminFeedbackRecord(
                id = id,
                revision = revision,
                product =
                    FeedbackProduct(
                        response.request.productId,
                        response.request.productName,
                        response.request.unit,
                    ),
                order =
                    FeedbackOrder(
                        response.request.orderId,
                        response.request.orderLineId,
                        response.request.deliveryDate,
                    ),
                area = response.request.area,
                rating = response.rating,
                issues = response.issues,
                note = response.note,
                photo =
                    response.photo?.let {
                        FeedbackPhotoMetadata(it.fileName, it.mimeType, it.fileSize)
                    },
                detailsComplete = response.detailsComplete,
                updatedAt = response.savedAt,
            )
        s.copy(
            feedback = s.feedback.filterNot { it.request.id == response.request.id } + response,
            feedbackOutbox = s.feedbackOutbox.filterNot { it.record.id == id } + OutboxItem(record),
        )
    }

    fun markFeedbackShown(request: FeedbackRequest) {
        mutable.update { it.copy(feedbackPrompt = request) }
        if (!request.demo && state.value.session.feedbackEvents.none { it.requestId == request.id })
            update {
                it.copy(
                    feedbackEvents =
                        it.feedbackEvents +
                            FeedbackEvent(request.id, request.productId, Instant.now().toString())
                )
            }
    }

    fun clearFeedbackPrompt() {
        mutable.update { it.copy(feedbackPrompt = null) }
    }

    fun dismissFeedback(request: FeedbackRequest) {
        update { s ->
            s.copy(
                feedbackEvents =
                    if (request.demo) s.feedbackEvents
                    else if (s.feedbackEvents.any { it.requestId == request.id })
                        s.feedbackEvents.map {
                            if (it.requestId == request.id) it.copy(skipped = true) else it
                        }
                    else
                        s.feedbackEvents +
                            FeedbackEvent(
                                request.id,
                                request.productId,
                                Instant.now().toString(),
                                true,
                            ),
                sampleFeedbackDismissed = if (request.demo) true else s.sampleFeedbackDismissed,
            )
        }
        clearFeedbackPrompt()
    }

    fun reset() {
        val deadline = state.value.session.offerEndsAt
        clearFeedbackPrompt()
        update { Session(offerEndsAt = deadline).withStarterCoins() }
    }
}
