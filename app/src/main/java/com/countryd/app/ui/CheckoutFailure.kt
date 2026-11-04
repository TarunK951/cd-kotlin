package com.countryd.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.countryd.app.api.FeedbackConnection
import com.countryd.app.api.FeedbackConnectionModel
import com.countryd.app.api.list
import com.countryd.app.api.num
import com.countryd.app.api.str
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val FailedRed = Color(0xFFE52525)
private val FailedInk = Color(0xFFAB2D2D)
private val FailedSurface = Color(0xFFFFE5E5)
private val PendingSurface = Color(0xFFFFF0D0)
private val PendingInk = Color(0xFF6B4C13)
private val RetryGreen = Color(0xFF357A4E)

internal data class FailedPurchaseLine(
    val name: String,
    val detail: String,
    val serverItem: JsonObject? = null,
    val imagePath: String? = null,
)

@Composable
internal fun ConnectedCheckoutFailure(state: FeedbackConnection, model: FeedbackConnectionModel) {
    val subscription = state.checkoutFailure == null && state.subscriptionFailure != null
    val error = if (subscription) state.subscriptionFailure else state.checkoutFailure
    if (error == null) return
    val pending = if (subscription) state.subscriptionPending else state.checkoutPending
    val request = state.failedSubscription.takeIf { subscription }
    val addressId =
        if (subscription) request?.num("addressId")?.takeIf { it > 0 }
        else state.checkoutFailureAddressId
    val address =
        state.addresses?.list("addresses")?.firstOrNull { it.num("addressId") == addressId }
            ?: state.activeDeliveryAddress()
    val items =
        state.checkoutFailureItems.ifEmpty {
            state.cart?.list("items").orEmpty().filter {
                state.checkoutFailureDate == null || it.str("deliveryDate") == state.checkoutFailureDate
            }
        }
    val date = if (subscription) request?.str("startDate") else {
        state.checkoutFailureDate ?: items.map { it.str("deliveryDate") }.distinct().singleOrNull()
    }
    val lines = if (subscription && request != null) {
        val productId = request.str("productId")
        val product = state.products.firstOrNull { it.str("productId") == productId }
            ?: state.product?.takeIf { it.str("productId") == productId }
        val name = product?.str("name").orEmpty().ifBlank { productId.ifBlank { "Subscription" } }
        listOf(
            FailedPurchaseLine(
                name,
                listOfNotNull(
                    date?.takeIf(String::isNotBlank)?.let { "Starts ${feedbackDate(it)}" },
                    product?.str("unit")?.takeIf(String::isNotBlank),
                ).joinToString(" · "),
                JsonObject(product.orEmpty() + mapOf(
                    "productId" to JsonPrimitive(productId),
                    "productName" to JsonPrimitive(name),
                )),
            )
        )
    } else items.map { item ->
        FailedPurchaseLine(
            item.str("productName").ifBlank { "Cart item" },
            listOf(
                item.str("deliveryDate").takeIf(String::isNotBlank)?.let(::feedbackDate).orEmpty(),
                item.str("unit"),
            ).filter(String::isNotBlank).joinToString(" · "),
            item,
        )
    }
    CheckoutFailureDialog(
        subscription = subscription,
        pending = pending,
        busy = state.busy,
        error = error,
        date = date,
        lines = lines,
        address = address?.str("fullAddress").orEmpty(),
        onBack = if (subscription) model::clearSubscriptionFailure else model::clearCheckoutFailure,
        onRetry = if (subscription) model::retrySubscription else model::retryCheckout,
    )
}

@Composable
internal fun CheckoutFailureDialog(
    subscription: Boolean,
    pending: Boolean,
    busy: Boolean,
    error: String,
    date: String?,
    lines: List<FailedPurchaseLine>,
    address: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val title = when {
        pending -> if (subscription) "Subscription status" else "Order status"
        subscription -> "Subscription failed"
        else -> "Order failed"
    }
    val heading = when {
        pending -> "We couldn't confirm the ${if (subscription) "subscription" else "order"}"
        subscription -> "We couldn't set up your subscription"
        else -> "We couldn't place your order"
    }
    val message = when {
        pending -> "Check the status before trying to place another ${if (subscription) "subscription" else "order"}."
        subscription -> "Your subscription wasn't created."
        date.isNullOrBlank() -> "Your order wasn't placed."
        else -> "Your order wasn't placed for ${feedbackDate(date)}."
    }
    val notice = when {
        pending -> "We're still checking what happened."
        subscription -> "No charge was made. You can try again."
        else -> "No charge was made. Your cart is saved."
    }
    val chip = when {
        pending -> "Status unconfirmed"
        subscription -> "Not scheduled"
        else -> "Not ordered"
    }
    val accent = if (pending) PendingInk else FailedRed
    val tint = if (pending) PendingSurface else FailedSurface
    val noticeInk = if (pending) PendingInk else FailedInk

    Dialog(
        onDismissRequest = { if (!busy) onBack() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp)) {
                    IconButton(onClick = onBack, enabled = !busy, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(CountryIcons.ArrowLeft, "Back", tint = Ink)
                    }
                    Text(
                        title,
                        Modifier.align(Alignment.Center).padding(horizontal = 52.dp),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = onBack, enabled = !busy, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(CountryIcons.Close, "Close", tint = Muted)
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier.size(20.dp).background(accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (pending) CountryIcons.CircleHelp else CountryIcons.Close,
                                null,
                                Modifier.size(13.dp),
                                tint = Color.White,
                            )
                        }
                        Text(notice, color = noticeInk, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(30.dp))
                    Box(
                        Modifier.size(80.dp).background(tint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(52.dp).background(accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (pending) CountryIcons.CircleHelp else CountryIcons.Close,
                                null,
                                Modifier.size(28.dp),
                                tint = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(heading, color = Ink, fontSize = 25.sp, lineHeight = 31.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = Muted, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                    if (error.isNotBlank()) {
                        Text(
                            error,
                            Modifier.padding(top = 6.dp),
                            color = Muted,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (lines.isNotEmpty() || address.isNotBlank()) {
                        Spacer(Modifier.height(26.dp))
                        CountryCard {
                            lines.take(2).forEach { line ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Canvas),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        when {
                                            line.serverItem != null -> FeedbackProductImage(line.serverItem)
                                            line.imagePath != null -> AssetImage(line.imagePath, line.name, Modifier.fillMaxSize(), ContentScale.Crop)
                                            else -> Icon(CountryIcons.ShoppingBag, null, tint = Muted)
                                        }
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(line.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        if (line.detail.isNotBlank()) Text(line.detail, color = Muted, fontSize = 12.sp)
                                        Row(
                                            Modifier.clip(CircleShape).background(tint).padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Icon(CountryIcons.Calendar, null, Modifier.size(13.dp), tint = noticeInk)
                                            Text(chip, color = noticeInk, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            if (lines.size > 2) Text("+${lines.size - 2} more items", color = Muted, fontSize = 12.sp)
                            if (address.isNotBlank()) Text(address, color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onRetry,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetryGreen),
                    ) {
                        Text(
                            when {
                                busy && pending -> "Checking status…"
                                busy -> "Trying again…"
                                pending -> "Check status"
                                else -> "Try again"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                    ) {
                        Text(if (subscription) "Back to subscription choice" else "Back to cart", color = RetryGreen)
                    }
                }
            }
        }
    }
}
