package com.countryd.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject

@Composable
internal fun ConnectedOrders(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    orderId: Long? = null,
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
        val orders =
            if (orderId == null) state.orders
            else state.order?.takeIf { it.num("orderId") == orderId }?.let(::listOf)
        if (orders?.isEmpty() == true) Text("No orders yet.")
        orders.orEmpty().forEach { order ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Order #${order.num("orderId")}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${order.str("deliveryDate")} · ${order.str("status").lowercase().replaceFirstChar { it.uppercase() }}"
                    )
                    order.list("items").forEach { item ->
                        ConnectedOrderItem(item, state.busy, model)
                    }
                    Text("Order total ₹${order.str("totalAmount")}")
                    if (orderId == null) {
                        TextButton(onClick = { openOrder(order.num("orderId")) }) {
                            Text("View order")
                        }
                    }
                }
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.message.isNotBlank() && state.result == null && state.detail == null)
            Text(state.message)
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = ::refresh, enabled = !state.busy) { Text("Refresh") }
    }
}

@Composable
private fun ConnectedOrderItem(item: JsonObject, busy: Boolean, model: FeedbackConnectionModel) {
    HorizontalDivider()
    Text(item.str("productName"), style = MaterialTheme.typography.titleSmall)
    Text("${item.num("quantity")} × ${item.str("unit")} · ₹${item.str("lineTotal")}")
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
    open: (Long?) -> Unit,
) {
    val orders = state.placedOrders
    if (orders.isEmpty()) return
    AlertDialog(
        onDismissRequest = model::dismissOrderConfirmation,
        title = { Text(if (orders.size == 1) "Order placed" else "Orders placed") },
        text = { Text(state.message) },
        confirmButton = {
            TextButton(
                onClick = {
                    model.dismissOrderConfirmation()
                    open(orders.singleOrNull()?.num("orderId"))
                }
            ) {
                Text(if (orders.size == 1) "View order" else "View orders")
            }
        },
        dismissButton = { TextButton(onClick = model::dismissOrderConfirmation) { Text("Done") } },
    )
}
