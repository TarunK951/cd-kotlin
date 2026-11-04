package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun OrdersScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    var cancel by remember { mutableStateOf<String?>(null) }
    ScreenBody {
        Text("My deliveries", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        DeliveryTabs("Orders") { if (it == "My subscriptions") go("subs") }
        Options(listOf("All", "In Progress", "Delivered"), filter) { filter = it }
        val orders =
            state.session.orders.filter {
                when (filter) {
                    "In Progress" -> it.status !in listOf("Delivered", "Cancelled")
                    "Delivered",
                    "Cancelled" -> it.status == filter
                    else -> true
                }
            }
        if (
            orders.isEmpty() &&
                state.session.orders.isEmpty() &&
                state.session.subscriptions.isEmpty() &&
                filter != "In Progress"
        ) {
            Caption("5 sample deliveries")
            FeedbackSampleCards(state, vm, go)
        } else if (orders.isEmpty())
            EmptyState(
                "No orders yet",
                "Your placed orders and receipts will appear here.",
                "Shop fresh essentials",
            ) {
                go("shop")
            }
        orders.forEach { order ->
            CountryCard {
                BillRow(order.id, order.status, true)
                Caption("${order.deliveryDate} · ${order.slot}")
                order.items.take(2).forEach { item ->
                    val p = state.products.first { it.id == item.line.productId }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AssetImage(productImage(p), p.name, Modifier.size(48.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 14.sp)
                            Caption("${item.line.qty} × ${item.line.variant?.unit ?: p.unit}")
                        }
                    }
                }
                BillRow("Total", money(order.totals.total))
                PrimaryButton("View order") { go("order/${order.id}") }
                Row {
                    TextButton({
                        vm.repeat(order)
                        go("cart")
                    }) {
                        Text("Repeat order")
                    }
                    if (order.status in listOf("Order Placed", "Quality Tested"))
                        TextButton({ cancel = order.id }) { Text("Cancel order") }
                }
            }
        }
        if (BuildConfig.DEBUG) {
            TextButton({ go("feedback-samples") }) { Text("Try sample delivered orders ›") }
        }
    }
    if (cancel != null)
        ConfirmCancel({ cancel = null }) {
            vm.cancelOrder(cancel!!)
            cancel = null
        }
}

@Composable
private fun ConfirmCancel(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel this order?") },
        text = {
            Text(
                "Wallet funds and used points will be returned once. Recurring subscriptions are managed separately."
            )
        },
        confirmButton = { TextButton(onConfirm) { Text("Cancel order") } },
        dismissButton = { TextButton(onDismiss) { Text("Keep order") } },
    )
}

@Composable
fun OrderScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit, id: String) {
    val order = state.session.orders.find { it.id == id }
    var cancel by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            ScreenBody {
                if (order == null) {
                    EmptyState(
                        "This order is not on this device",
                        "Open your saved orders to choose another order.",
                        "View orders",
                    ) {
                        go("orders")
                    }
                    return@ScreenBody
                }
                CountryCard(color = Mint) {
                    Caption("ORDER → DELIVERY → HELP")
                    Heading(
                        if (order.status == "Cancelled") "Order cancelled"
                        else "Everything for this order"
                    )
                    Caption("Your items, purchased plan and receipt stay together.")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Icon(CountryIcons.Calendar, null, Modifier.size(16.dp), tint = Green)
                        Text(
                            "${order.deliveryDate} · ${order.slot}",
                            fontSize = 12.sp,
                            color = Green,
                        )
                    }
                }
                if (order.status != "Cancelled") DeliveryProgressCard(order)
                CountryCard {
                    Heading("Manage this order")
                    PrimaryButton("Get help with this order") { go("support?order=${order.id}") }
                    if (order.items.any { it.line.plan != null })
                        TextButton({ go("subs") }) { Text("Manage my subscriptions") }
                    if (order.status in listOf("Order Placed", "Quality Tested"))
                        TextButton({ cancel = true }) { Text("Cancel order") }
                }
                Heading("What you ordered")
                order.items.forEach { item ->
                    val p = state.products.first { it.id == item.line.productId }
                    CountryCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(productImage(p), p.name, Modifier.size(60.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold)
                                Caption("${item.line.qty} × ${item.line.variant?.unit ?: p.unit}")
                            }
                            Text(money(lineTotal(item.line, p)))
                        }
                        item.line.plan?.let {
                            Caption("${it.frequency} from ${it.startDate} · ${it.slot}")
                        }
                    }
                }
                Bill(order.totals, order.tip)
                if (order.instructions.isNotEmpty())
                    CountryCard {
                        Heading("Delivery instructions")
                        Text(order.instructions.joinToString(" · "))
                    }
                PrimaryButton("Buy again") {
                    vm.repeat(order)
                    go("cart")
                }
                if (order.status == "Delivered")
                    CountryCard {
                        Heading("Rate your delivery")
                        RatingStars(order.rating) { rating ->
                            vm.update {
                                it.copy(
                                    orders =
                                        it.orders.map { old ->
                                            if (old.id == id) old.copy(rating = rating) else old
                                        }
                                )
                            }
                        }
                    }
            }
        }
        if (order != null) {
            Surface(color = Canvas) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    PrimaryButton("Continue shopping", arrow = true) { go("home") }
                }
            }
        }
    }
    if (cancel && order != null)
        ConfirmCancel({ cancel = false }) {
            vm.cancelOrder(id)
            cancel = false
        }
}
