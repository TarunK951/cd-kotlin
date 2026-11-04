package com.countryd.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject

@Composable
internal fun ConnectedOrders(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    orderId: Long? = null,
    openSubscriptions: () -> Unit = {},
    openOrder: (Long) -> Unit = {},
) {
    fun refresh() {
        if (orderId == null) model.orders() else model.order(orderId)
    }
    LaunchedEffect(orderId, state.manualPrompt?.num("promptId")) {
        if (state.manualPrompt == null) {
            model.state.first { !it.busy }
            refresh()
        }
    }
    ScreenBody {
        if (orderId == null)
            DeliveryTabs("Orders") { if (it == "My subscriptions") openSubscriptions() }
        val orders =
            if (orderId == null) state.orders
            else state.order?.takeIf { it.num("orderId") == orderId }?.let(::listOf)
        if (orders?.isEmpty() == true) Text("No orders yet.")
        if (orderId == null) {
            val entries = orders.orEmpty()
            val scheduled =
                entries.filter { it.str("status").equals("SCHEDULED", ignoreCase = true) }
            val delivered =
                entries.filter { it.str("status").equals("DELIVERED", ignoreCase = true) }
            val cancelled =
                entries.filter { it.str("status").equals("CANCELLED", ignoreCase = true) }
            val other = entries - scheduled.toSet() - delivered.toSet() - cancelled.toSet()
            OrderSection("Scheduled", scheduled, state.busy, model, openOrder)
            if (delivered.isNotEmpty() || cancelled.isNotEmpty()) {
                Text("Past orders", style = MaterialTheme.typography.titleLarge)
                OrderSection("Delivered", delivered, state.busy, model, openOrder)
                OrderSection("Cancelled", cancelled, state.busy, model, openOrder)
            }
            OrderSection("Other orders", other, state.busy, model, openOrder)
        } else {
            orders?.singleOrNull()?.let { order -> ConnectedOrderCard(order, state.busy, model) }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.message.isNotBlank() && state.result == null && state.detail == null)
            Text(state.message)
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = ::refresh, enabled = !state.busy) { Text("Refresh") }
    }
}

@Composable
private fun OrderSection(
    title: String,
    orders: List<JsonObject>,
    busy: Boolean,
    model: FeedbackConnectionModel,
    openOrder: (Long) -> Unit,
) {
    if (orders.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    orders.forEach { order ->
        ConnectedOrderCard(order, busy, model) { openOrder(order.num("orderId")) }
    }
}

@Composable
private fun ConnectedOrderCard(
    order: JsonObject,
    busy: Boolean,
    model: FeedbackConnectionModel,
    viewOrder: (() -> Unit)? = null,
) {
    val status = order.str("status")
    val statusLabel =
        status.lowercase(Locale.ROOT).replace('_', ' ').replaceFirstChar {
            it.titlecase(Locale.ROOT)
        }
    val date = order.str("deliveryDate")
    val formattedDate =
        runCatching {
                LocalDate.parse(date)
                    .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH))
            }
            .getOrDefault(date)
    CountryCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Order #${order.num("orderId")}", style = MaterialTheme.typography.titleMedium)
                Text(formattedDate, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (statusLabel.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (status == "CANCELLED") Line else Mint,
                ) {
                    Text(
                        statusLabel,
                        Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = if (status == "CANCELLED") Muted else Green,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        order.list("items").forEach { item ->
            HorizontalDivider(color = Line)
            ConnectedOrderItem(item, busy, model)
        }
        HorizontalDivider(color = Line)
        BillRow("Order total", "₹${order.str("totalAmount")}")
        if (viewOrder != null) {
            OutlinedButton(onClick = viewOrder, modifier = Modifier.fillMaxWidth()) {
                Text("View order")
            }
        }
    }
}

@Composable
private fun ConnectedOrderItem(item: JsonObject, busy: Boolean, model: FeedbackConnectionModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.str("productName"), style = MaterialTheme.typography.titleSmall)
            Text(
                "${item.num("quantity")} × ${item.str("unit")}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text("₹${item.str("lineTotal")}", style = MaterialTheme.typography.titleSmall)
    }
    val feedback = item.obj("feedback") ?: return
    when {
        feedback.flag("canRateNow") ->
            TextButton(onClick = { model.rateNow(item.num("orderItemId")) }, enabled = !busy) {
                Text("Rate now")
            }
        feedback.str("status") == "RATED" ->
            TextButton(onClick = { model.feedback(feedback.num("feedbackId")) }, enabled = !busy) {
                Text("Rated ${feedback.num("rating")}★")
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManualRatingSheet(state: FeedbackConnection, model: FeedbackConnectionModel) {
    val prompt = state.manualPrompt ?: return
    ModalBottomSheet(onDismissRequest = { if (!state.busy) model.dismissManualPrompt() }) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            FeedbackRatingCard(prompt, state.busy) { model.answer(it, manual = true) }
            if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun ConnectedOrderConfirmation(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    continueShopping: () -> Unit,
    openOrders: () -> Unit,
) {
    val orders = state.placedOrders
    if (orders.isEmpty()) return
    FeedbackConfirmation(
        title = "Order success",
        close = {
            model.dismissOrderConfirmation()
            continueShopping()
        },
        successMessage =
            if (orders.size == 1) "Order placed successfully" else "Orders placed successfully",
        viewOrders = {
            model.dismissOrderConfirmation()
            openOrders()
        },
    ) {
        val items = orders.flatMap { it.list("items") }
        val single = items.singleOrNull()
        Text(
            if (single != null) "${single.str("productName")} has been ordered."
            else if (orders.size == 1) "Your order has been placed."
            else "Your orders have been placed.",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(state.message, color = Muted)
        orders.forEach { order ->
            CountryCard {
                Text("Order #${order.num("orderId")}", style = MaterialTheme.typography.titleSmall)
                order.list("items").forEach { item ->
                    FeedbackProductSummary(item)
                    Text(
                        "Quantity ${item.num("quantity")} · ₹${item.str("lineTotal")}",
                        color = Muted,
                    )
                }
                Text(
                    "Delivery · ${order.str("deliveryDate")}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Text(
            "Find your order under Subs → Orders.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
