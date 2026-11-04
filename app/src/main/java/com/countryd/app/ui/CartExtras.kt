package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.countryd.app.*

@Composable
fun CartExtras(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val session = state.session
    val eligible =
        state.products.filter { product -> session.cart.none { it.productId == product.id } }
    val subscribed =
        session.subscriptions.filter { it.status != "Past" }.map { it.productId }.toSet()
    val repeat =
        eligible.filter { product ->
            product.id !in subscribed &&
                session.orders.any { order ->
                    order.status == "Delivered" &&
                        order.items.any {
                            it.line.productId == product.id &&
                                it.subscriptionId == null &&
                                it.line.mode == "Once" &&
                                !it.line.freeGift
                        } &&
                        session.feedback.any {
                            !it.request.demo &&
                                it.request.orderId == order.id &&
                                it.request.productId == product.id &&
                                it.rating >= 4
                        }
                }
        }
    var selected by rememberSaveable { mutableStateOf("Bestsellers") }
    val active = if (selected == "Buy again" && repeat.isEmpty()) "Bestsellers" else selected
    Heading("Offers & extras")
    CountryCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🛍️", fontSize = 26.sp)
            Column(Modifier.weight(1f)) {
                Text("Add a carry bag")
                Text("Free", color = Green, fontSize = 12.sp)
            }
            Switch(
                session.orderBag,
                { checked -> vm.update { it.copy(orderBag = checked) } },
                Modifier.semantics { contentDescription = "Add a free carry bag" },
            )
        }
    }
    CountryCard {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                    Triple("Bestsellers", "bestsellers", Color(0xFFFFF1D6)),
                    Triple("Buy again", "buy-again", Color(0xFFEDF2FF)),
                    Triple("Price drops", "price-drops", Color(0xFFFFF0E2)),
                )
                .filter { it.first != "Buy again" || repeat.isNotEmpty() }
                .forEach { (label, image, background) ->
                    Column(
                        Modifier.weight(1f)
                            .heightIn(min = 76.dp)
                            .background(
                                if (active == label) Green else background,
                                RoundedCornerShape(14.dp),
                            )
                            .selectable(active == label, role = Role.Tab) { selected = label }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AssetImage("native-art/$image.png", null, Modifier.size(42.dp))
                        Text(
                            label,
                            Modifier.padding(top = 4.dp),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = if (active == label) Color.White else Ink,
                        )
                    }
                }
        }
        val picks =
            when (active) {
                "Buy again" -> repeat
                "Price drops" ->
                    eligible.sortedByDescending { (it.mrp - it.price) / it.mrp.coerceAtLeast(1.0) }
                else -> eligible
            }
        if (picks.isEmpty()) Caption("Products from your orders will appear here.")
        else ProductShelf(picks.take(6), session, vm, go)
    }
}
