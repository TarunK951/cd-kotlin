package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun CartScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    onPlaced: (String) -> Unit,
) {
    CheckoutScreen(state, vm, go, onPlaced, embedded = true) {
        val s = state.session
        OutlinedButton(
            onClick = { go("offers") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(CountryIcons.Tag, null, Modifier.size(17.dp))
            Text("Offers & coupons", Modifier.weight(1f).padding(start = 10.dp))
            Text("›")
        }
        s.cart.forEach { line ->
            val p = state.products.first { it.id == line.productId }
            CountryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssetImage(productImage(p), p.name, Modifier.size(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Caption(line.variant?.unit ?: p.unit)
                        val plan = line.plan
                        if (plan != null) {
                            Caption("Subscription · ${plan.frequency} · ${plan.startDate}")
                        } else {
                            Caption("One-time order")
                            Text(
                                money(lineTotal(line, p)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Quantity(line.qty, { vm.quantity(line.key, it - line.qty) }, minimum = 0)
                }
                if (line.addons.isNotEmpty()) Caption(line.addons.joinToString { it.name })
                line.plan?.let { plan ->
                    val summary = packSummary(line)
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Mint, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${summary.deliveries} delivery days",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Caption("${line.qty} × ${line.variant?.unit ?: p.unit} / delivery")
                            Caption("${summary.chargedPacks} paid packs · ${plan.slot}")
                            if (summary.freePacks > 0) Caption("${summary.freePacks} free packs")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                money(lineTotal(line, p)),
                                color = Green,
                                fontWeight = FontWeight.Bold,
                            )
                            Caption("Plan subtotal")
                        }
                    }
                }
            }
        }
        freeGiftLines(s, state.products).forEach { gift ->
            val product = state.products.first { it.id == gift.line.productId }
            CountryCard(color = Mint) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AssetImage(productImage(product), product.name, Modifier.size(44.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${product.name} · Free gift")
                        Caption("${gift.line.qty} × ${product.unit}")
                    }
                    Text("FREE", color = Green, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (s.vipActive) {
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFBEDC7), Color(0xFFFFF8E6), Color(0xFFF0D995))
                        ),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(CountryIcons.Crown, null, Modifier.size(24.dp), tint = Color(0xFF805B12))
                Column(Modifier.weight(1f)) {
                    Text("Delight VIP", fontWeight = FontWeight.Bold)
                    Caption("Complimentary pass")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹0", fontWeight = FontWeight.Bold)
                    Caption("NO FEE")
                }
            }
        }
    }
}
