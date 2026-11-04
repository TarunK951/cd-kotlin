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
fun CheckoutScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    onPlaced: (String) -> Unit,
    embedded: Boolean = false,
    basket: @Composable ColumnScope.() -> Unit = {},
) {
    val s = state.session
    var date by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var deliveryPicker by rememberSaveable { mutableStateOf(false) }
    var preference by rememberSaveable { mutableStateOf("Delivery") }
    val slot = "Morning, 6:00 – 7:00 AM"
    var tip by rememberSaveable { mutableDoubleStateOf(0.0) }
    var points by rememberSaveable { mutableStateOf(false) }
    var wallet by rememberSaveable { mutableStateOf(false) }
    var instructions by rememberSaveable {
        mutableStateOf(listOf("Avoid ringing bell", "Leave at the door"))
    }
    var coupon by rememberSaveable { mutableStateOf(s.coupon) }
    var payment by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val items = s.cart.sumOf { lineTotal(it, state.products.first { p -> p.id == it.productId }) }
    val bill = totals(items, s, tip, points, wallet)
    fun placeOrder() {
        val id =
            vm.checkout(
                date,
                slot,
                tip,
                (if (s.orderBag) instructions + "Add a carry bag" else instructions).toSet(),
                points,
                wallet,
            )
        if (id != null) onPlaced(id) else error = "Check your address, basket and delivery date."
    }
    if (s.cart.isEmpty()) {
        ScreenBody {
            EmptyState("Your basket is empty", "Choose products before checking out.", "Shop now") {
                go("shop")
            }
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            ScreenBody {
                basket()
                if (!embedded)
                    CountryCard {
                        Heading("Delivery address")
                        Text(s.address?.display ?: "Choose a delivery address")
                        TextButton({ go("addresses") }) { Text("Change address ›") }
                    }
                CountryCard {
                    Text(
                        "Delivery preferences",
                        fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    CartPreferenceTabs(preference) {
                        preference = it
                        deliveryPicker = false
                    }
                    if (preference == "Delivery") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (date == LocalDate.now().plusDays(1).toString()) "Tomorrow"
                                    else
                                        LocalDate.parse(date)
                                            .format(
                                                java.time.format.DateTimeFormatter.ofPattern(
                                                    "EEE, d MMM"
                                                )
                                            ),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(slot, fontSize = 10.sp, lineHeight = 14.sp, color = Muted)
                            }
                            TextButton({ deliveryPicker = !deliveryPicker }) {
                                Text("Change", fontSize = 11.sp)
                                Icon(
                                    CountryIcons.Calendar,
                                    null,
                                    Modifier.padding(start = 6.dp).size(18.dp),
                                )
                            }
                        }
                        if (deliveryPicker) DeliveryDatePanel(date, slot) { date = it }
                    }
                    if (preference == "Instructions") {
                        CartInstructionCards(instructions) { label ->
                            instructions =
                                if (label in instructions) instructions - label
                                else instructions + label
                        }
                    }
                    if (preference == "Tip") CartTipOptions(tip) { tip = it }
                }
                if (!embedded)
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
                Text(
                    "Use your balance",
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BalanceCard(
                        "Wallet",
                        money(s.wallet),
                        "wallet",
                        s.wallet > 0,
                        wallet,
                        { wallet = it },
                        Modifier.weight(1f),
                    )
                    BalanceCard(
                        "Coins",
                        s.points.toString(),
                        "coins",
                        s.points > 0,
                        points,
                        { points = it },
                        Modifier.weight(1f),
                    )
                }
                if (wallet && s.wallet < bill.total)
                    CountryCard(color = Mint) {
                        Text("Add ${money(bill.total-s.wallet)} to cover this order")
                        TextButton({ go("wallet") }) { Text("Top up wallet") }
                    }
                CountryCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Nutrition")
                            val known =
                                s.cart.mapNotNull { line ->
                                    state.products
                                        .find { it.id == line.productId }
                                        ?.let { product ->
                                            packNutrients(
                                                product.id,
                                                line.variant?.unit ?: product.unit,
                                                line.qty,
                                            )
                                        }
                                }
                            Caption(
                                if (known.isEmpty()) "Values unavailable"
                                else
                                    "${grams(known.sumOf { it.protein })} protein · ${grams(known.sumOf { it.fiber })} fibre"
                            )
                        }
                        TextButton({ go("nutrition") }) { Text("Details") }
                    }
                }
                Bill(bill, tip, title = "Bill summary") {
                    s.cart.forEach { line ->
                        val product = state.products.first { it.id == line.productId }
                        BillRow(product.name, money(lineTotal(line, product)))
                        Caption("${line.qty} × ${line.variant?.unit ?: product.unit}")
                    }
                    if (s.vipActive) BillRow("Delight VIP pass · Free", "₹0")
                }
                if (embedded) CartExtras(state, vm, go)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
        Surface(color = Canvas) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BillRow("Order total", money(bill.total), bold = true)
                PrimaryButton("Place order", enabled = s.address != null) {
                    if (bill.walletUsed > 0 && bill.toPay == 0.0) placeOrder() else payment = true
                }
            }
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
                    placeOrder()
                }) {
                    Text("Complete demo checkout")
                }
            },
            dismissButton = { TextButton({ payment = false }) { Text("Cancel") } },
        )
}

@Composable
private fun BalanceCard(
    label: String,
    value: String,
    artwork: String,
    enabled: Boolean,
    selected: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = { onChange(!selected) },
        enabled = enabled,
        modifier = modifier,
        color = if (selected) Mint else androidx.compose.ui.graphics.Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selected) Green else Line),
    ) {
        Row(
            Modifier.heightIn(min = 64.dp).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AssetImage("native-art/balance-$artwork.png", null, Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp)
                Text(value, fontSize = 13.sp)
            }
            Checkbox(selected, onCheckedChange = null)
        }
    }
}

@Composable
private fun DeliveryDatePanel(date: String, slot: String, onDate: (String) -> Unit) {
    val tomorrow = LocalDate.now().plusDays(1)
    val dayFormat = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
    val monthFormat = java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.ENGLISH)
    Column(
        Modifier.fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Color(0xFFFBF8F0),
                androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
            .border(1.dp, Line, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Delivery date & slot",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        Caption(
            LocalDate.parse(date)
                .format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "EEE d MMM",
                        java.util.Locale.ENGLISH,
                    )
                )
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tileWidth = ((maxWidth - 18.dp) / 4).coerceIn(50.dp, 64.dp)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (0L..6L).forEach { offset ->
                    val day = tomorrow.plusDays(offset)
                    val selected = day.toString() == date
                    val foreground = if (selected) androidx.compose.ui.graphics.Color.White else Ink
                    Surface(
                        onClick = { onDate(day.toString()) },
                        modifier = Modifier.width(tileWidth),
                        color = if (selected) Green else androidx.compose.ui.graphics.Color.White,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) Green else Line),
                    ) {
                        Column(
                            Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (offset == 0L) "Tomorrow" else day.format(dayFormat).uppercase(),
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                color = foreground,
                            )
                            Text(
                                day.dayOfMonth.toString(),
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                color = foreground,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            )
                            Text(
                                day.format(monthFormat),
                                fontSize = 9.sp,
                                lineHeight = 12.sp,
                                color = foreground,
                            )
                        }
                    }
                }
            }
        }
        Surface(
            color = Mint,
            border = BorderStroke(1.dp, Green),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RadioButton(selected = true, onClick = null)
                Column {
                    Text(
                        slot,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Green,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
