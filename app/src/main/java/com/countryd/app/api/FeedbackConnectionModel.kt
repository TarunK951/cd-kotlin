package com.countryd.app.api

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

data class FeedbackConnection(
    val restoring: Boolean = true,
    val busy: Boolean = false,
    val customer: JsonObject? = null,
    val otp: JsonObject? = null,
    val otpRequestCount: Int = 0,
    val prompt: JsonObject? = null,
    val manualPrompt: JsonObject? = null,
    val orders: List<JsonObject>? = null,
    val order: JsonObject? = null,
    val placedOrders: List<JsonObject> = emptyList(),
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
    private var token: String? = null
    private val mutable = MutableStateFlow(FeedbackConnection())
    val state = mutable.asStateFlow()

    init {
        restore()
    }

    fun restore() = run {
        try {
            token = withContext(Dispatchers.IO) { store.read() }
            if (token != null) {
                val customer = call("/api/auth/me", secure = true).jsonObject
                change { it.copy(customer = customer) }
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
    }

    fun next() = run {
        change { it.copy(prompt = null) }
        val reply = call("/api/feedback-prompts/next", secure = true).jsonObject
        change { it.copy(prompt = reply.obj("prompt").takeIf { _ -> reply.flag("show") }) }
        val summary = call("/api/cart/summary", secure = true).jsonObject
        change { it.copy(cartSummary = summary) }
    }

    fun answer(rating: Int?, manual: Boolean = false) = run {
        val prompt =
            (if (manual) mutable.value.manualPrompt else mutable.value.prompt) ?: return@run
        val id = prompt.num("promptId")
        val reply =
            try {
                call(
                        "/api/feedback-prompts/$id/${if (rating == null) "skip" else "rating"}",
                        "POST",
                        buildJsonObject { if (rating != null) put("rating", rating) },
                        true,
                    )
                    .jsonObject
            } catch (e: ApiFailure) {
                if (e.status in setOf(404, 409))
                    change { if (manual) it.copy(manualPrompt = null) else it.copy(prompt = null) }
                throw e
            }
        change {
            it.copy(
                prompt = if (manual) it.prompt else null,
                manualPrompt = if (manual) null else it.manualPrompt,
                result =
                    reply
                        .takeIf { r ->
                            r.str("nextStep") in setOf("BUY_AGAIN", "ISSUE_DETAILS", "THANK_YOU")
                        }
                        ?.let { JsonObject(it + ("ratedProduct" to prompt)) },
                message = reply.str("message"),
            )
        }
    }

    fun closeResult() = change { it.copy(result = null, detail = null, error = "") }

    fun issues(id: Long, codes: List<String>, note: String, photo: Uri?) = run {
        if (photo != null) {
            api.photo(id, token ?: throw ApiFailure(401, "UNAUTHORIZED", "Sign in again."), photo)
        }
        val reply =
            call(
                    "/api/feedback/$id/issues",
                    "PUT",
                    buildJsonObject {
                        putJsonArray("issueCodes") { codes.forEach { add(it) } }
                        if (note.isNotBlank()) put("note", note.trim())
                    },
                    true,
                )
                .jsonObject
        change { it.copy(result = null, detail = reply, message = reply.str("message")) }
    }

    fun feedback(id: Long) = run {
        val reply = call("/api/feedback/$id", secure = true).jsonObject
        change { it.copy(detail = reply) }
    }

    fun add(product: String, quantity: Int, date: String? = null, feedbackId: Long? = null) = run {
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
        change {
            it.copy(
                result = null,
                cart = reply.obj("cart"),
                cartNotice = reply.str("message").ifBlank { "Added to cart" },
                message = "",
            )
        }
        refreshAfterWrite { refreshCartMetadata() }
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

    fun updateCartItem(id: Long, quantity: Int? = null, date: String? = null) = run {
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
        change { it.copy(cart = reply.obj("cart")) }
        refreshAfterWrite { refreshCartMetadata() }
    }

    fun clearCart() = run {
        val cart = call("/api/cart", "DELETE", secure = true).jsonObject
        change { it.copy(cart = cart) }
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

    fun saveAddress(id: Long?, body: JsonObject) = run {
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
        loadAddresses()
    }

    fun createSubscription(body: JsonObject) = run {
        val reply = call("/api/subscriptions", "POST", body, true).jsonObject
        change { it.copy(cartNotice = reply.str("message").ifBlank { "Subscription created" }) }
        refreshAfterWrite {
            loadSubscriptions()
            loadBuyAgain()
        }
    }

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
        val cart = call("/api/cart", secure = true).jsonObject
        change { it.copy(cart = cart) }
        loadAddresses()
        refreshCartMetadata()
    }

    fun remove(id: Long) = run {
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
        change { it.copy(cart = cart, message = "") }
        refreshAfterWrite { refreshCartMetadata() }
    }

    fun placeOrder(date: String? = null) = run {
        val reply =
            call(
                    "/api/orders",
                    "POST",
                    buildJsonObject {
                        if (date != null) put("deliveryDate", date)
                        mutable.value.selectedAddressId?.let { put("addressId", it) }
                    },
                    true,
                )
                .jsonObject
        val placed = reply.list("orders")
        change { current ->
            val remaining =
                current.cart?.list("items").orEmpty().filterNot { line ->
                    placed.any { it.str("deliveryDate") == line.str("deliveryDate") }
                }
            current.copy(
                cart =
                    current.cart?.let { cart ->
                        JsonObject(
                            cart +
                                mapOf(
                                    "items" to JsonArray(remaining),
                                    "totalQuantity" to
                                        JsonPrimitive(remaining.sumOf { it.num("quantity") }),
                                    "totalAmount" to
                                        JsonPrimitive(
                                            remaining.sumOf {
                                                it.str("lineTotal").toDoubleOrNull() ?: 0.0
                                            }
                                        ),
                                )
                        )
                    },
                placedOrders = placed,
                message = reply.str("message"),
            )
        }
        refreshAfterWrite { refreshCartMetadata() }
    }

    fun dismissOrderConfirmation() = change { it.copy(placedOrders = emptyList(), message = "") }

    fun orders() = run {
        val orders = call("/api/orders", secure = true).jsonArray.map { it.jsonObject }
        change { it.copy(orders = orders) }
    }

    fun order(id: Long) = run {
        val order = call("/api/orders/$id", secure = true).jsonObject
        change { it.copy(order = order) }
    }

    fun rateNow(itemId: Long) = run {
        val reply =
            call(
                    "/api/feedback-prompts/rate-now",
                    "POST",
                    buildJsonObject { put("orderItemId", itemId) },
                    true,
                )
                .jsonObject
        change {
            it.copy(
                manualPrompt = reply.obj("prompt").takeIf { _ -> reply.flag("show") },
                message = "",
            )
        }
    }

    fun dismissManualPrompt() = change { it.copy(manualPrompt = null) }

    fun products() = run {
        val products = call("/api/products").jsonArray.map { it.jsonObject }
        change { it.copy(products = products, options = emptyList()) }
        if (token != null) loadBuyAgain()
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
