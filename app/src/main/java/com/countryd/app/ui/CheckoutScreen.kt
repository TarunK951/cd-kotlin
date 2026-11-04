package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.LocalDate

@Composable
fun CheckoutScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val s = state.session
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var preference by rememberSaveable { mutableStateOf("Delivery") }
    var slot by rememberSaveable { mutableStateOf("Morning, 6:00 – 7:00 AM") }
    var tip by rememberSaveable { mutableDoubleStateOf(0.0) }
    var points by rememberSaveable { mutableStateOf(false) }
    var wallet by rememberSaveable { mutableStateOf(false) }
    var instructions by remember { mutableStateOf(setOf<String>()) }
    var coupon by rememberSaveable { mutableStateOf(s.coupon) }
    var payment by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var complete by remember { mutableStateOf<String?>(null) }
    val items = s.cart.sumOf { lineTotal(it, state.products.first { p -> p.id == it.productId }) }
    val bill = totals(items, s, tip, points, wallet)
    if (complete != null) {
        OrderConfirmation({ go("home") }, { go("order/$complete") })
        return
    }
    if (s.cart.isEmpty()) {
        ScreenBody {
            EmptyState("Your basket is empty", "Choose products before checking out.", "Shop now") {
                go("shop")
            }
        }
        return
    }
    ScreenBody {
        CountryCard {
            Heading("Delivery address")
            Text(s.address?.display ?: "Choose a delivery address")
            TextButton({ go("addresses") }) { Text("Change address ›") }
        }
        CountryCard {
            Heading("Delivery preferences")
            Row(Modifier.fillMaxWidth()) {
                listOf(
                        "Delivery" to CountryIcons.Calendar,
                        "Tip" to CountryIcons.Heart,
                        "Instructions" to CountryIcons.FileText,
                    )
                    .forEach { (label, icon) ->
                        FilterChip(
                            selected = preference == label,
                            onClick = { preference = label },
                            modifier = Modifier.weight(1f),
                            label = { Text(label, fontSize = 11.sp) },
                            leadingIcon = { Icon(icon, null, Modifier.size(14.dp)) },
                        )
                    }
            }
            if (preference == "Delivery") {
                Heading("Delivery date")
                DateOptions(date) { date = it }
                Heading("Morning slot")
                Options(listOf("Morning, 6:00 – 7:00 AM", "Morning, 7:00 – 8:00 AM"), slot) {
                    slot = it
                }
            }
            if (preference == "Instructions") {
                Heading("Delivery instructions")
                listOf(
                        "No bell, please",
                        "Leave at the door",
                        "Leave with security",
                        "Hand it to me",
                    )
                    .forEach { label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                label in instructions,
                                {
                                    instructions =
                                        if (it) instructions + label else instructions - label
                                },
                            )
                            Icon(CountryIcons.FileText, null, Modifier.size(16.dp), tint = Green)
                            Text(label, Modifier.padding(start = 8.dp))
                        }
                    }
            }
            if (preference == "Tip") {
                Heading("Thank your delivery partner")
                Options(
                    listOf("No tip", "₹10", "₹20", "₹30", "₹50"),
                    if (tip == 0.0) "No tip" else "₹${tip.toInt()}",
                ) {
                    tip = it.removePrefix("₹").toDoubleOrNull() ?: 0.0
                }
            }
        }
        CountryCard {
            Heading("Apply coupon")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    coupon,
                    { coupon = it.uppercase() },
                    modifier = Modifier.weight(1f),
                    label = { Text("Coupon code") },
                    singleLine = true,
                )
                TextButton({ vm.update { it.copy(coupon = coupon) } }) { Text("Apply") }
            }
            if (s.coupon.isNotEmpty())
                Caption(
                    if (s.coupon == "DELIGHT10" && items >= 200) "DELIGHT10 applied"
                    else
                        "DELIGHT10 requires a ₹200 basket. Other codes are unavailable in this preview."
                )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(points, { points = it })
            Text("Use ${s.points} reward points")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(wallet, { wallet = it })
            Text("Use wallet · ${money(s.wallet)}")
        }
        if (wallet && s.wallet < bill.total)
            CountryCard(color = Mint) {
                Text("Add ${money(bill.total-s.wallet)} to cover this order")
                TextButton({ go("wallet") }) { Text("Top up wallet") }
            }
        Bill(bill, tip)
        Caption("Demo checkout. No money will be charged and no real delivery will be placed.")
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Place demo order · ${money(bill.toPay)}", enabled = s.address != null) {
            payment = true
        }
    }
    if (payment)
        AlertDialog(
            onDismissRequest = { payment = false },
            title = { Text("Demo checkout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(money(bill.toPay), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "This is a preview. No money will be charged and no real delivery will be placed."
                    )
                }
            },
            confirmButton = {
                TextButton({
                    payment = false
                    val id =
                        vm.checkout(
                            date,
                            slot,
                            tip,
                            if (s.orderBag) instructions + "Add a carry bag" else instructions,
                            points,
                            wallet,
                        )
                    if (id != null) complete = id
                    else error = "Check your address, basket and delivery date."
                }) {
                    Text("Complete demo checkout")
                }
            },
            dismissButton = { TextButton({ payment = false }) { Text("Cancel") } },
        )
}
