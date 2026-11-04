package com.countryd.app.api

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class FeedbackConnection(
    val restoring: Boolean = true,
    val busy: Boolean = false,
    val homeRefreshing: Boolean = false,
    val catalogueRefreshing: Boolean = false,
    val issueReceipt: JsonObject? = null,
    val photoProgress: Float? = null,
    val photoFailed: Boolean = false,
    val customer: JsonObject? = null,
    val otp: JsonObject? = null,
    val otpRequestCount: Int = 0,
    val prompt: JsonObject? = null,
    val manualPrompt: JsonObject? = null,
    val orders: List<JsonObject>? = null,
    val order: JsonObject? = null,
    val placedOrders: List<JsonObject> = emptyList(),
    val feedbackCartItemIds: Set<Long> = emptySet(),
    val placedFromFeedback: Boolean = false,
    val checkoutFailure: String? = null,
    val checkoutFailureDate: String? = null,
    val checkoutFailureItems: List<JsonObject> = emptyList(),
    val checkoutFailureAddressId: Long? = null,
    val failedBuyNow: JsonObject? = null,
    val checkoutPending: Boolean = false,
    val subscriptionFailure: String? = null,
    val subscriptionPending: Boolean = false,
    val failedSubscription: JsonObject? = null,
    val subscriptionCreationCount: Int = 0,
    val result: JsonObject? = null,
    val cart: JsonObject? = null,
    val cartSummary: JsonObject? = null,
    val cartRecommendations: List<JsonObject> = emptyList(),
    val buyAgainRecommendations: List<JsonObject> = emptyList(),
    val addresses: JsonObject? = null,
    val selectedAddressId: Long? = null,
    val subscriptions: JsonObject? = null,
    val cartNotice: String? = null,
    val detail: JsonObject? = null,
    val products: List<JsonObject> = emptyList(),
    val product: JsonObject? = null,
    val options: List<JsonObject> = emptyList(),
    val message: String = "",
    val error: String = "",
)

class FeedbackConnectionModel(app: Application) : AndroidViewModel(app) {
    private val api = FeedbackApi(app)
    private val store = TokenStore(app)
    private val commerceJournal = app.getSharedPreferences("connected-commerce", android.content.Context.MODE_PRIVATE)
    private var token: String? = null
    private val mutable = MutableStateFlow(FeedbackConnection())
    val state = mutable.asStateFlow()
    private val queuedReads = mutableSetOf<String>()
    private var reloadCartBeforeAdjustment = false

    init { restore() }

    fun restore() = run {
        change { it.copy(restoring = true) }
        try {
            token = withContext(Dispatchers.IO) { store.read() }
            if (token != null) {
                val customer = call("/api/auth/me", secure = true).jsonObject
                change { it.copy(customer = customer) }
                restoreCommerceAttempts()
            }
        } finally {
            change { it.copy(restoring = false) }
        }
    }

    private fun change(block: (FeedbackConnection) -> FeedbackConnection) = mutable.update(block)

    private fun run(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        change { it.copy(busy = true, error = "") }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is ApiFailure && e.status == 401 && token != null) clearSession()
                change {
                    it.copy(
                        error =
                            if (e is java.io.IOException) "Could not reach the server. Try again."
                            else e.message ?: "Something went wrong. Try again."
                    )
                }
            } finally {
                change { it.copy(busy = false) }
            }
        }
    }

    private fun readWhenIdle(key: String, block: suspend () -> Unit) {
        if (!queuedReads.add(key)) return
        val sessionToken = token
        viewModelScope.launch {
            do {
                state.first { !it.busy }
            } while (state.value.busy)
            if (sessionToken != token) {
                queuedReads.remove(key)
                return@launch
            }
            run {
                try {
                    block()
                } finally {
                    queuedReads.remove(key)
                }
            }
        }
    }

    private suspend fun call(
        path: String,
        method: String = "GET",
        body: JsonObject? = null,
        secure: Boolean = false,
    ) =
        api.request(
            path,
            method,
            if (secure) token ?: throw ApiFailure(401, "UNAUTHORIZED", "Sign in again.") else null,
            body,
        )

    private suspend fun clearSession() {
        token = null
        withContext(Dispatchers.IO) { store.clear() }
        mutable.value = FeedbackConnection(restoring = false, busy = true)
    }

    fun logout() = run { clearSession() }

    fun changePhone() = change { it.copy(otp = null, error = "", message = "") }

    fun requestOtp(phone: String) = run {
        val otp =
            call("/api/auth/otp/request", "POST", buildJsonObject { put("phone", phone) })
                .jsonObject
        change {
            it.copy(
                otp = otp,
                otpRequestCount = it.otpRequestCount + 1,
                message = otp.str("message"),
            )
        }
    }

    fun authenticate(code: String, name: String, area: String, city: String) = run {
        val otp = mutable.value.otp ?: return@run
        val registered = otp.flag("registered")
        val result =
            try {
                call(
                        if (registered) "/api/auth/login" else "/api/auth/register",
                        "POST",
                        buildJsonObject {
                            put("phone", otp.str("phone"))
                            put("otp", code)
                            if (!registered) {
                                put("name", name.trim())
                                put("area", area.trim())
                                put("city", city.trim())
                            }
                        },
                    )
                    .jsonObject
            } catch (e: ApiFailure) {
                if (e.code in setOf("NOT_REGISTERED", "PHONE_ALREADY_REGISTERED"))
                    change {
                        it.copy(
                            otp =
                                JsonObject(
                                    otp +
                                        ("registered" to
                                            JsonPrimitive(e.code == "PHONE_ALREADY_REGISTERED"))
                                )
                        )
                    }
                throw e
            }
        val access =
            result.str("accessToken").also {
                require(it.isNotBlank()) { "The server returned no sign-in token." }
            }
        val customer = requireNotNull(result.obj("customer"))
        withContext(Dispatchers.IO) { store.save(access) }
        token = access
        change { FeedbackConnection(restoring = false, busy = true, customer = customer) }
        restoreCommerceAttempts()
    }

    private fun journalKey(kind: String): String {
        val customer = requireNotNull(mutable.value.customer)
        val id = customer.str("customerId").ifBlank { customer.str("id") }.ifBlank { customer.str("phone") }
        require(id.isNotBlank()) { "Could not identify this account." }
        return "$kind:$id"
    }

    private suspend fun readAttempt(kind: String): CommerceAttempt? {
        val key = journalKey(kind)
        return withContext(Dispatchers.IO) {
            commerceJournal.getString(key, null)?.let(CommerceAttempt::fromJson)
        }
    }

    private suspend fun saveAttempt(kind: String, attempt: CommerceAttempt?) {
        val key = journalKey(kind)
        withContext(Dispatchers.IO) {
            val edit = commerceJournal.edit()
            if (attempt == null) edit.remove(key) else edit.putString(key, attempt.toJson().toString())
            check(edit.commit()) { "Could not save the request on this device." }
        }
    }

    private suspend fun restoreCommerceAttempts() {
        val checkout = readAttempt("checkout")
        val subscription = readAttempt("subscription")
        change {
            it.copy(
                checkoutPending = checkout != null,
                checkoutFailureDate = checkout?.request?.str("deliveryDate")?.ifBlank { null },
                checkoutFailureItems = checkout?.items.orEmpty().ifEmpty {
                    checkout?.buyNow?.let(::listOf).orEmpty()
                },
                checkoutFailureAddressId = checkout?.request?.num("addressId"),
                failedBuyNow = checkout?.buyNow,
                checkoutFailure = checkout?.let { pendingCheckoutMessage },
                subscriptionPending = subscription != null,
                failedSubscription = subscription?.request,
                subscriptionFailure = subscription?.let { pendingSubscriptionMessage },
            )
        }
    }

    private val pendingCheckoutMessage =
        "We couldn’t confirm whether your order was placed. Check its status before trying again."
    private val pendingSubscriptionMessage =
        "We couldn’t confirm whether your subscription started. Check its status before trying again."

    private fun isUnconfirmed(error: Exception) = when (error) {
        is ApiTransportFailure -> error.requestMayHaveArrived
        is ApiFailure -> error.status == 408 || error.status >= 500
        else -> true
    }

    private fun commerceError(error: Exception) =
        if (error is java.io.IOException) "Could not reach the server. Try again."
        else error.message ?: "Something went wrong. Try again."

    fun next() = refreshHome(false)

    fun refreshHome() = refreshHome(true)

    private fun refreshHome(refreshCatalogue: Boolean) = readWhenIdle("home") {
        change { it.copy(homeRefreshing = true) }
        try {
            val prompt = coroutineScope {
                val orders = async { loadOrders() }
                val subscriptions = async { loadSubscriptions(); mutable.value.subscriptions }
                val history = requireNotNull(subscriptions.await())
                val purchases = orders.await().filterNot { it.str("status").equals("CANCELLED", ignoreCase = true) }
                val subscribedProducts = history.list("subscriptions").map { it.str("productId") }.toSet()
                val counts = purchases.flatMap { order -> order.list("items").map { it.str("productId") }.distinct() }
                    .groupingBy { it }.eachCount()
                withContext(Dispatchers.IO) {
                    purchases.filter { it.str("status").equals("DELIVERED", ignoreCase = true) }
                        .sortedByDescending { it.str("deliveryDate") }
                        .firstNotNullOfOrNull { order ->
                            order.list("items").firstNotNullOfOrNull { item ->
                                val product = item.str("productId")
                                val name = item.str("productName")
                                if (product.isBlank() || name.isBlank() || counts[product] != 1 ||
                                    product in subscribedProducts || item.num("orderItemId") <= 0 ||
                                    item.obj("feedback")?.flag("canRateNow") != true ||
                                    commerceJournal.getBoolean(journalKey("feedback-skip:$product"), false)
                                ) null
                                else buildJsonObject {
                                    put("orderItemId", item.num("orderItemId"))
                                    put("productId", product)
                                    put("productName", name)
                                    put("productUnit", item.str("unit"))
                                    put("imageUrl", item.str("imageUrl"))
                                    put("deliveryDate", order.str("deliveryDate"))
                                    put("area", order.str("deliveryAddress"))
                                    put("title", "How was your $name?")
                                    put("ratingMin", 1)
                                    put("ratingMax", 5)
                                }
                            }
                        }
                }
            }
            change { it.copy(prompt = prompt) }
            if (refreshCatalogue) {
                loadCatalogue()
                refreshAfterWrite { loadBuyAgain() }
            }
            loadCart()
            if (mutable.value.addresses == null) refreshAfterWrite { loadAddresses() }
        } finally {
            change { it.copy(homeRefreshing = false) }
        }
    }

    fun answer(rating: Int?, manual: Boolean = false) = run {
        val prompt =
            (if (manual) mutable.value.manualPrompt else mutable.value.prompt) ?: return@run
        if (rating == null && !manual) {
            val key = journalKey("feedback-skip:${prompt.str("productId")}")
            withContext(Dispatchers.IO) {
                check(commerceJournal.edit().putBoolean(key, true).commit()) {
                    "Could not save your choice on this device."
                }
            }
            change { it.copy(prompt = null) }
            next()
            return@run
        }
        val ratingPrompt = if (manual) prompt else requestRatingPrompt(prompt.num("orderItemId"))
        val product = JsonObject(prompt + ratingPrompt)
        val result = when {
            rating == null -> {
                call("/api/feedback-prompts/${product.num("promptId")}/skip", "POST", buildJsonObject {}, true)
                null
            }
            else -> ratingDraft(product, rating)
        }
        change {
            it.copy(
                prompt = it.prompt?.takeIf { current -> manual && current.str("productId") != product.str("productId") },
                manualPrompt = if (manual) null else it.manualPrompt,
                result = result,
                issueReceipt = null,
                photoFailed = false,
            )
        }
    }

    private suspend fun saveRating(product: JsonObject, rating: Int): JsonObject {
        val id = product.num("promptId")
        require(id > 0) { "Feedback is unavailable for this item." }
        val reply = call(
            "/api/feedback-prompts/$id/rating", "POST",
            buildJsonObject { put("rating", rating) }, true,
        ).jsonObject
        return JsonObject(reply + mapOf("rating" to JsonPrimitive(rating), "ratedProduct" to product))
    }

    private suspend fun ratingDraft(product: JsonObject, rating: Int, existingForm: JsonObject? = null): JsonObject {
        val form = existingForm ?: if (rating > 3) null else kotlin.run {
            val productId = Uri.encode(product.str("productId"))
            val options = call("/api/products/$productId/issue-options").jsonArray
            buildJsonObject {
                put("options", options)
                put("multiSelect", true)
                put("noteAllowed", true)
                put("photoAllowed", true)
                put("noteMaxLength", 500)
            }
        }
        return buildJsonObject {
            put("rating", rating)
            put("ratedProduct", product)
            put("ratingDraft", true)
            if (form != null) put("issueForm", form)
        }
    }

    fun editIssueRating(rating: Int) = run {
        val draft = mutable.value.result ?: return@run
        if (!draft.flag("ratingDraft") || mutable.value.issueReceipt != null) return@run
        require(rating in 1..5)
        val updated = ratingDraft(requireNotNull(draft.obj("ratedProduct")), rating, draft.obj("issueForm"))
        change { it.copy(result = updated) }
    }

    fun savePositiveRating(onSaved: (JsonObject) -> Unit) = run {
        val draft = mutable.value.result ?: return@run
        val rating = draft.num("rating").toInt()
        if (!draft.flag("ratingDraft") || rating !in 4..5) return@run
        val saved = saveRating(requireNotNull(draft.obj("ratedProduct")), rating)
        change { it.copy(result = saved) }
        onSaved(saved)
        refreshAfterWrite { loadBuyAgain() }
    }

    fun closeResult() {
        val current = mutable.value
        val completed = current.detail != null || current.issueReceipt != null ||
            current.result?.let { !it.flag("ratingDraft") } == true
        change { it.copy(result = null, detail = null, issueReceipt = null, photoFailed = false, error = "") }
        if (completed) next()
    }

    fun removeIssuePhoto() = change { it.copy(photoFailed = false, error = "") }

    fun issues(codes: List<String>, note: String, photo: Uri?) = run {
        val draft = mutable.value.result ?: return@run
        val receipt = mutable.value.issueReceipt ?: saveRating(
            requireNotNull(draft.obj("ratedProduct")), draft.num("rating").toInt(),
        ).also { saved -> change { it.copy(issueReceipt = saved) } }
        val id = receipt.num("feedbackId")
        require(id > 0) { "The server did not return a feedback reference." }
        val issueForm = receipt.obj("issueForm")
        require(codes.size >= (issueForm?.num("minSelections") ?: 0)) {
            "Choose the requested number of issues before submitting."
        }
        var detail = call(
            "/api/feedback/$id/issues", "PUT",
            buildJsonObject {
                putJsonArray("issueCodes") { codes.forEach { add(it) } }
                if (note.isNotBlank()) put("note", note.trim())
            }, true,
        ).jsonObject
        if (photo != null) {
            change { it.copy(photoProgress = 0f, photoFailed = false) }
            try {
                api.photo(id, token ?: throw ApiFailure(401, "UNAUTHORIZED", "Sign in again."), photo) { progress ->
                    change { it.copy(photoProgress = progress) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                change { it.copy(photoFailed = true) }
                throw error
            } finally {
                change { it.copy(photoProgress = null) }
            }
            refreshAfterWrite { detail = call("/api/feedback/$id", secure = true).jsonObject }
        }
        change { it.copy(result = null, detail = detail, issueReceipt = null, photoFailed = false, message = detail.str("message")) }
    }

    fun feedback(id: Long) = run {
        val reply = call("/api/feedback/$id", secure = true).jsonObject
        change { it.copy(detail = reply) }
    }

    fun add(
        product: String,
        quantity: Int,
        date: String? = null,
        feedbackId: Long? = null,
        onAdded: () -> Unit = {},
    ) = run {
        addCartItem(product, quantity, date, feedbackId)
        onAdded()
        refreshAfterWrite { refreshCartMetadata() }
    }

    private suspend fun addCartItem(
        product: String,
        quantity: Int,
        date: String? = null,
        feedbackId: Long? = null,
    ) {
        val reply =
            call(
                    "/api/cart/items",
                    "POST",
                    buildJsonObject {
                        put("productId", product)
                        put("quantity", quantity)
                        if (date != null) put("deliveryDate", date)
                        if (feedbackId != null) put("feedbackId", feedbackId)
                    },
                    true,
                )
                .jsonObject
        val cart = reply.obj("cart") ?: loadCart()
        val reorderedItem = if (feedbackId == null) null else cart.list("items").firstOrNull {
            it.str("productId") == product && (date == null || it.str("deliveryDate") == date)
        }?.num("cartItemId")?.takeIf { it > 0 }
        change { current ->
            val next = current.withCart(cart)
            next.copy(
                result = null,
                feedbackCartItemIds = reorderedItem?.let { next.feedbackCartItemIds + it }
                    ?: next.feedbackCartItemIds,
                cartNotice = reply.str("message").ifBlank { "Added to cart" },
                message = "",
            )
        }
    }

    private fun FeedbackConnection.withCart(cart: JsonObject) = copy(
        cart = cart,
        cartSummary = cart,
        feedbackCartItemIds = feedbackCartItemIds.intersect(cart.list("items").map { it.num("cartItemId") }.toSet()),
    )

    private suspend fun loadCart(): JsonObject {
        val cart = call("/api/cart", secure = true).jsonObject
        change { it.withCart(cart) }
        reloadCartBeforeAdjustment = false
        return cart
    }

    fun cartQuantities() = readWhenIdle("cart-quantities") { loadCart() }

    fun adjustProductQuantity(productId: String, delta: Int) = run {
        require(delta == -1 || delta == 1)
        val cart = if (reloadCartBeforeAdjustment) loadCart() else mutable.value.cart ?: loadCart()
        val lines = cart.list("items")
            .filter { it.str("productId") == productId && it.num("quantity") > 0 }
            .sortedBy { it.str("deliveryDate") }
        try {
            if (lines.isEmpty()) {
                if (delta > 0) addCartItem(productId, 1)
                return@run
            }
            val item = lines.firstOrNull { delta < 0 || it.num("quantity") < 20 }
            requireNotNull(item) { "You can add up to 20 per delivery." }
            val id = item.num("cartItemId")
            val quantity = item.num("quantity").toInt() + delta
            if (quantity == 0) removeCartItem(id)
            else patchCartItem(id, quantity, date = null)
        } catch (error: Exception) {
            reloadCartBeforeAdjustment = true
            throw error
        }
    }

    fun clearCartNotice() = change { it.copy(cartNotice = null) }

    private suspend fun refreshAfterWrite(refresh: suspend () -> Unit) {
        try {
            refresh()
        } catch (error: ApiFailure) {
            if (error.status == 401) throw error
        } catch (_: java.io.IOException) {
            // The write succeeded. A later screen refresh can reload ancillary data.
        }
    }

    private suspend fun refreshCartMetadata() {
        val summary = call("/api/cart/summary", secure = true).jsonObject
        change { it.copy(cartSummary = summary) }
        val pairs = call("/api/recommendations/for-cart", secure = true).jsonObject
        change { it.copy(cartRecommendations = pairs.list("items")) }
        loadBuyAgain()
    }

    private suspend fun loadBuyAgain() {
        val reply = call("/api/recommendations/buy-again", secure = true).jsonObject
        change { it.copy(buyAgainRecommendations = reply.list("items")) }
    }

    private suspend fun patchCartItem(id: Long, quantity: Int?, date: String?) {
        val reply =
            call(
                    "/api/cart/items/$id",
                    "PATCH",
                    buildJsonObject {
                        if (quantity != null) put("quantity", quantity)
                        if (date != null) put("deliveryDate", date)
                    },
                    true,
                )
                .jsonObject
        val cart = reply.obj("cart") ?: loadCart()
        change { it.withCart(cart) }
    }

    fun updateCartItem(id: Long, quantity: Int? = null, date: String? = null) = run {
        patchCartItem(id, quantity, date)
        refreshAfterWrite { refreshCartMetadata() }
    }

    fun clearCart() = run {
        val cart = call("/api/cart", "DELETE", secure = true).jsonObject
        change { it.withCart(cart) }
        refreshAfterWrite { refreshCartMetadata() }
    }

    private suspend fun loadAddresses() {
        val reply = call("/api/addresses", secure = true).jsonObject
        updateAddresses(reply)
    }

    private fun updateAddresses(reply: JsonObject) {
        change { current ->
            current.copy(
                addresses = reply,
                selectedAddressId =
                    current.selectedAddressId?.takeIf { id ->
                        reply.list("addresses").any { it.num("addressId") == id }
                    },
            )
        }
    }

    fun addresses() = run { loadAddresses() }

    fun selectAddress(id: Long) = change { it.copy(selectedAddressId = id) }

    fun saveAddress(
        id: Long?,
        body: JsonObject,
        selectAfterSave: Boolean = false,
        onSaved: () -> Unit = {},
    ) = run {
        val saved =
            call(
                    if (id == null) "/api/addresses" else "/api/addresses/$id",
                    if (id == null) "POST" else "PUT",
                    body,
                    true,
                )
                .jsonObject
        val current = mutable.value.addresses
        val savedId = saved.num("addressId")
        val defaultId =
            if (saved.flag("isDefault")) savedId else current?.num("defaultAddressId") ?: 0
        val records =
            current?.list("addresses").orEmpty().filterNot { it.num("addressId") == savedId } +
                saved
        updateAddresses(
            buildJsonObject {
                put("defaultAddressId", defaultId)
                put(
                    "addresses",
                    JsonArray(
                        records.map { address ->
                            JsonObject(
                                address +
                                    ("isDefault" to
                                        JsonPrimitive(address.num("addressId") == defaultId))
                            )
                        }
                    ),
                )
            }
        )
        if (selectAfterSave) selectAddress(savedId)
        onSaved()
        refreshAfterWrite { loadAddresses() }
    }

    fun defaultAddress(id: Long) = run {
        updateAddresses(
            call("/api/addresses/$id/default", "POST", buildJsonObject {}, true).jsonObject
        )
    }

    fun deleteAddress(id: Long) = run {
        updateAddresses(call("/api/addresses/$id", "DELETE", secure = true).jsonObject)
    }

    private suspend fun loadSubscriptions() {
        val reply = call("/api/subscriptions", secure = true).jsonObject
        change { it.copy(subscriptions = reply) }
    }

    fun subscriptions() = run {
        loadSubscriptions()
    }

    private suspend fun deliveryAddressId(requested: Long = 0): Long {
        if (mutable.value.addresses == null) loadAddresses()
        val current = mutable.value
        val addressId = requested.takeIf { it > 0 } ?: current.selectedAddressId ?:
            current.addresses?.num("defaultAddressId") ?: 0
        val address = current.addresses?.list("addresses")?.find { it.num("addressId") == addressId }
        require(address != null && address.str("line1").isNotBlank() &&
            address.str("area").isNotBlank() && address.str("city").isNotBlank() &&
            address.str("pincode").matches(Regex("[1-9][0-9]{5}"))) {
            "Choose a complete delivery address."
        }
        return addressId
    }

    fun createSubscription(body: JsonObject) = run { submitSubscription(body) }

    private suspend fun submitSubscription(body: JsonObject) {
        change { it.copy(failedSubscription = body) }
        if (readAttempt("subscription") != null) {
            recoverSubscription()
            return
        }
        var posted = false
        try {
            loadSubscriptions()
            val current = mutable.value.subscriptions?.list("subscriptions").orEmpty()
            require(current.none {
                it.str("productId") == body.str("productId") && it.str("status") in setOf("ACTIVE", "PAUSED")
            }) { "You already have a subscription for this product. Manage it in My subscriptions." }
            val request = JsonObject(body + mapOf(
                "addressId" to JsonPrimitive(deliveryAddressId(body.num("addressId"))),
                "frequency" to JsonPrimitive(body.str("frequency").ifBlank { "DAILY" }),
            ))
            val attempt = CommerceAttempt(request, current.map { it.num("subscriptionId") }.toSet())
            saveAttempt("subscription", attempt)
            posted = true
            val reply = call("/api/subscriptions", "POST", request, true).jsonObject
            val subscription = reply.obj("subscription") ?: reply.takeIf { it.num("subscriptionId") > 0 }
                ?: kotlin.run {
                    loadSubscriptions()
                    mutable.value.subscriptions?.let(attempt::createdSubscription)
                }
            checkNotNull(subscription) { "The server did not return the subscription details." }
            finishSubscription(subscription, reply.str("message").ifBlank { "Subscription created" })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val pending = posted && isUnconfirmed(error)
            if (posted && !pending) saveAttempt("subscription", null)
            val message = if (pending) pendingSubscriptionMessage else commerceError(error)
            change { it.copy(subscriptionFailure = message, subscriptionPending = pending, error = message) }
            if (error is ApiFailure && error.status == 401) throw error
        }
    }

    private suspend fun finishSubscription(plan: JsonObject, message: String) {
        saveAttempt("subscription", null)
        change { current ->
            val subscriptions = current.subscriptions
            current.copy(
                subscriptions = JsonObject(subscriptions.orEmpty() + ("subscriptions" to JsonArray(
                    subscriptions?.list("subscriptions").orEmpty().filterNot { old ->
                        old.num("subscriptionId") == plan.num("subscriptionId")
                    } + plan
                ))),
                cartNotice = message,
                subscriptionCreationCount = current.subscriptionCreationCount + 1,
                subscriptionPending = false,
                subscriptionFailure = null,
                failedSubscription = null,
                buyAgainRecommendations = current.buyAgainRecommendations.filterNot {
                    it.str("productId") == plan.str("productId")
                },
                error = "",
            )
        }
    }

    private suspend fun recoverSubscription() {
        val attempt = readAttempt("subscription") ?: return
        change {
            it.copy(subscriptionPending = true, failedSubscription = attempt.request, subscriptionFailure = pendingSubscriptionMessage, error = pendingSubscriptionMessage)
        }
        loadSubscriptions()
        val matching = mutable.value.subscriptions?.let(attempt::createdSubscription)
        if (matching != null) finishSubscription(matching, "Subscription created")
    }

    fun retrySubscription() = run {
        if (readAttempt("subscription") != null) recoverSubscription()
        else mutable.value.failedSubscription?.let { submitSubscription(it) }
    }

    fun clearSubscriptionFailure() = change { it.copy(subscriptionFailure = null, error = "") }

    fun updateSubscription(id: Long, body: JsonObject) = run {
        call("/api/subscriptions/$id", "PATCH", body, true)
        refreshAfterWrite { loadSubscriptions() }
    }

    fun subscriptionAction(id: Long, action: String) = run {
        if (action == "cancel") call("/api/subscriptions/$id", "DELETE", secure = true)
        else call("/api/subscriptions/$id/$action", "POST", buildJsonObject {}, true)
        refreshAfterWrite {
            loadSubscriptions()
            loadBuyAgain()
        }
    }

    fun cart() = run {
        loadCart()
        loadAddresses()
        refreshCartMetadata()
    }

    private suspend fun removeCartItem(id: Long) {
        val cart =
            try {
                call("/api/cart/items/$id", "DELETE", secure = true).jsonObject
            } catch (error: ApiFailure) {
                if (error.status == 401) throw error
                // The request can succeed even when its response is lost.
                val latest = call("/api/cart", secure = true).jsonObject
                if (latest.list("items").any { it.num("cartItemId") == id }) throw error
                latest
            }
        change { it.withCart(cart).copy(message = "") }
    }

    fun remove(id: Long) = run {
        removeCartItem(id)
        refreshAfterWrite { refreshCartMetadata() }
    }

    fun placeOrder(date: String? = null) = run { submitOrder(date) }

    private suspend fun submitOrder(date: String?) {
        change {
            it.copy(
                checkoutFailureDate = date,
                failedBuyNow = null,
                checkoutFailureItems = it.cart?.list("items").orEmpty().filter { line ->
                    date == null || line.str("deliveryDate") == date
                },
                checkoutFailureAddressId = it.selectedAddressId ?: it.addresses?.num("defaultAddressId"),
            )
        }
        if (readAttempt("checkout") != null) {
            recoverCheckout()
            return
        }
        var posted = false
        try {
            val addressId = deliveryAddressId()
            val (cart, existing) = currentCheckoutData()
            change { it.copy(cart = cart) }
            val items = cart.list("items").filter { date == null || it.str("deliveryDate") == date }
            change { it.copy(checkoutFailureItems = items, checkoutFailureAddressId = addressId) }
            require(items.isNotEmpty()) { "There are no items to order for this delivery." }
            val body = buildJsonObject {
                if (date != null) put("deliveryDate", date)
                put("addressId", addressId)
            }
            val attempt = CommerceAttempt(body, existing.map { it.num("orderId") }.toSet(), items)
            saveAttempt("checkout", attempt)
            posted = true
            postCheckout(attempt)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val pending = posted && isUnconfirmed(error)
            if (posted && !pending) saveAttempt("checkout", null)
            val message = if (pending) pendingCheckoutMessage else commerceError(error)
            change { it.copy(checkoutFailure = message, checkoutPending = pending, error = message) }
            if (error is ApiFailure && error.status == 401) throw error
        }
    }

    fun buyNow(productId: String, quantity: Int, date: String) = run {
        val intent = buildJsonObject {
            put("productId", productId)
            put("quantity", quantity)
            put("deliveryDate", date)
        }
        submitBuyNow(intent)
    }

    private suspend fun submitBuyNow(intent: JsonObject) {
        if (readAttempt("checkout") != null) {
            recoverCheckout()
            return
        }
        val date = intent.str("deliveryDate")
        val productId = intent.str("productId")
        val quantity = intent.num("quantity")
        val product = mutable.value.products.firstOrNull { it.str("productId") == productId }
            ?: mutable.value.product?.takeIf { it.str("productId") == productId }
        val summary = JsonObject(intent + mapOf(
            "productName" to JsonPrimitive(product?.str("name").orEmpty()),
            "unit" to JsonPrimitive(product?.str("unit").orEmpty()),
        ))
        change {
            it.copy(failedBuyNow = intent, checkoutFailureDate = date, checkoutFailureItems = listOf(summary))
        }
        var startingStage = false
        var stagedCart: JsonObject? = null
        try {
            require(productId.isNotBlank() && quantity in 1..20) { "Choose a valid product quantity." }
            require(LocalDate.parse(date) > LocalDate.now()) { "Choose a future delivery date." }
            val addressId = deliveryAddressId()
            val (cart, existing) = currentCheckoutData()
            change { it.withCart(cart).copy(checkoutFailureAddressId = addressId) }
            require(cart.list("items").none { it.str("deliveryDate") == date }) {
                "Your cart already uses that delivery date. Choose another date for Buy now."
            }
            val request = buildJsonObject {
                put("deliveryDate", date)
                put("addressId", addressId)
            }
            val attempt = CommerceAttempt(
                request, existing.map { it.num("orderId") }.toSet(), buyNow = summary,
            )
            saveAttempt("checkout", attempt)
            startingStage = true
            val stageBody = buildJsonObject {
                put("productId", productId)
                put("quantity", quantity)
                put("deliveryDate", date)
            }
            val reply = call("/api/cart/items", "POST", stageBody, true).jsonObject
            val cartAfterStage = reply.obj("cart") ?: call("/api/cart", secure = true).jsonObject
            stagedCart = cartAfterStage
            val staged = requireNotNull(attempt.stagedBuyNowItem(cartAfterStage)) {
                "Could not isolate this item for the chosen delivery date."
            }
            placeStagedBuyNow(attempt, staged, cartAfterStage)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val pending = startingStage && (stagedCart != null || isUnconfirmed(error))
            if (startingStage && !pending) saveAttempt("checkout", null)
            val message = if (pending) pendingCheckoutMessage else commerceError(error)
            change { state ->
                val current = stagedCart?.let { state.withCart(it) } ?: state
                current.copy(checkoutFailure = message, checkoutPending = pending, error = message)
            }
            if (error is ApiFailure && error.status == 401) throw error
        }
    }

    private suspend fun placeStagedBuyNow(attempt: CommerceAttempt, staged: JsonObject, cart: JsonObject) {
        val ordering = attempt.copy(items = listOf(staged))
        saveAttempt("checkout", ordering)
        change { it.copy(checkoutFailureItems = listOf(staged)) }
        try {
            postCheckout(ordering)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val pending = isUnconfirmed(error)
            if (!pending) saveAttempt("checkout", attempt.copy(items = emptyList()))
            val message = if (pending) pendingCheckoutMessage
                else "${commerceError(error)} Your Buy now item is still in the cart for retry."
            change {
                it.withCart(cart).copy(checkoutFailure = message, checkoutPending = pending, error = message)
            }
            if (error is ApiFailure && error.status == 401) throw error
        }
    }

    private suspend fun postCheckout(attempt: CommerceAttempt) {
        val reply = call("/api/orders", "POST", attempt.request, true).jsonObject
        val placed = reply.list("orders")
        check(placed.isNotEmpty() && placed.all { it.num("orderId") > 0 && it.str("deliveryDate").isNotBlank() }) {
            "The server did not return an order confirmation."
        }
        finishCheckout(attempt, placed, reply.str("message"), reply.obj("cart"))
    }

    private suspend fun finishCheckout(
        attempt: CommerceAttempt,
        placed: List<JsonObject>,
        message: String,
        confirmedCart: JsonObject? = null,
    ) {
        saveAttempt("checkout", null)
        change { current ->
            val deliveryDates = placed.map { it.str("deliveryDate") }.toSet()
            val orderedIds = attempt.items.filter { it.str("deliveryDate") in deliveryDates }
                .map { it.num("cartItemId") }.toSet()
            val remaining = current.cart?.list("items").orEmpty().filterNot {
                it.num("cartItemId") in orderedIds
            }
            val cart = confirmedCart ?: current.cart?.let {
                JsonObject(it + mapOf(
                    "items" to JsonArray(remaining),
                    "totalQuantity" to JsonPrimitive(remaining.sumOf { it.num("quantity") }),
                    "totalAmount" to JsonPrimitive(remaining.sumOf { it.str("lineTotal").toDoubleOrNull() ?: 0.0 }),
                ))
            }
            val orders = (placed + current.orders.orEmpty()).distinctBy { it.num("orderId") }
            current.copy(
                cart = cart,
                cartSummary = cart,
                cartRecommendations = emptyList(),
                orders = orders,
                placedOrders = placed,
                placedFromFeedback = orderedIds.any { it in current.feedbackCartItemIds },
                feedbackCartItemIds = current.feedbackCartItemIds - orderedIds,
                checkoutPending = false,
                checkoutFailure = null,
                checkoutFailureDate = null,
                checkoutFailureItems = emptyList(),
                checkoutFailureAddressId = null,
                failedBuyNow = null,
                message = message,
                error = "",
            )
        }
    }

    private suspend fun recoverCheckout(retryReadyBuyNow: Boolean = false) {
        val attempt = readAttempt("checkout") ?: return
        change {
            it.copy(
                checkoutPending = true,
                checkoutFailure = pendingCheckoutMessage,
                checkoutFailureDate = attempt.request.str("deliveryDate").ifBlank { null },
                checkoutFailureItems = attempt.items.ifEmpty { it.checkoutFailureItems },
                checkoutFailureAddressId = attempt.request.num("addressId"),
                failedBuyNow = attempt.buyNow,
            )
        }
        val (cart, orders) = currentCheckoutData()
        change { it.copy(cart = cart, cartSummary = cart) }
        if (attempt.buyNow != null && attempt.items.isEmpty()) {
            val staged = attempt.stagedBuyNowItem(cart) ?: return
            if (retryReadyBuyNow) {
                placeStagedBuyNow(attempt, staged, cart)
            } else {
                change {
                    it.copy(
                        checkoutPending = false,
                        checkoutFailureItems = listOf(staged),
                        checkoutFailure = "The order wasn’t placed. Your Buy now item is ready to retry.",
                        error = "",
                    )
                }
            }
            return
        }
        val placed = attempt.placedOrders(orders, cart)
        if (placed.isNotEmpty()) finishCheckout(attempt, placed, "Order placed successfully", cart)
    }

    fun retryCheckout() = run {
        val current = mutable.value
        val saved = readAttempt("checkout")
        when {
            saved != null -> recoverCheckout(retryReadyBuyNow = !current.checkoutPending)
            current.failedBuyNow != null -> submitBuyNow(current.failedBuyNow)
            else -> submitOrder(current.checkoutFailureDate)
        }
    }

    fun clearCheckoutFailure() = change { it.copy(checkoutFailure = null, error = "") }

    private suspend fun currentCheckoutData() = coroutineScope {
        val cart = async { call("/api/cart", secure = true).jsonObject }
        val orders = async { loadOrders() }
        cart.await() to orders.await()
    }

    fun dismissOrderConfirmation() = change { it.copy(placedOrders = emptyList(), placedFromFeedback = false, message = "") }

    private suspend fun loadOrders(): List<JsonObject> {
        val orders = call("/api/orders", secure = true).jsonArray.map { it.jsonObject }
        change { it.copy(orders = orders) }
        return orders
    }

    fun orders() = run { loadOrders() }

    fun order(id: Long) = run {
        val order = call("/api/orders/$id", secure = true).jsonObject
        change { current ->
            current.copy(order = order, orders = current.orders?.map { if (it.num("orderId") == id) order else it })
        }
    }

    private suspend fun requestRatingPrompt(itemId: Long): JsonObject {
        val reply =
            call(
                    "/api/feedback-prompts/rate-now",
                    "POST",
                    buildJsonObject { put("orderItemId", itemId) },
                    true,
                )
                .jsonObject
        val prompt = reply.obj("prompt")?.takeIf { reply.flag("show") }
        return requireNotNull(prompt) { reply.str("message").ifBlank { "Feedback is unavailable for this item." } }
    }

    fun rateNow(itemId: Long) = run {
        val prompt = requestRatingPrompt(itemId)
        change {
            it.copy(
                manualPrompt = prompt,
                message = "",
            )
        }
    }

    fun dismissManualPrompt() = change { it.copy(manualPrompt = null) }

    private suspend fun loadCatalogue() {
        val products = call("/api/products").jsonArray.map { it.jsonObject }
        change { it.copy(products = products) }
    }

    fun products() = readWhenIdle("products") {
        change { it.copy(catalogueRefreshing = true) }
        try {
            loadCatalogue()
            if (token != null) {
                if (mutable.value.cart == null) loadCart()
                if (mutable.value.buyAgainRecommendations.isEmpty()) loadBuyAgain()
            }
        } finally {
            change { it.copy(catalogueRefreshing = false) }
        }
    }

    fun product(id: String) = run {
        val encoded = Uri.encode(id)
        val product = call("/api/products/$encoded").jsonObject
        change { it.copy(product = product) }
    }

    fun options(id: String) = run {
        val safeId = android.net.Uri.encode(id)
        val options = call("/api/products/$safeId/issue-options").jsonArray.map { it.jsonObject }
        change { it.copy(options = options) }
    }
}
