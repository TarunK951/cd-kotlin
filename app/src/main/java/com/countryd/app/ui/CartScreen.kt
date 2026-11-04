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
fun CartScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val s = state.session
    val items =
        s.cart.sumOf { line -> lineTotal(line, state.products.first { it.id == line.productId }) }
    val bill = totals(items, s)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            ScreenBody {
                if (s.cart.isEmpty())
                    EmptyState(
                        "Your basket is empty",
                        "Add fresh essentials to get started.",
                        "Shop now",
                    ) {
                        go("shop")
                    }
                else {
                    s.cart.forEach { line ->
                        val p = state.products.first { it.id == line.productId }
                        CountryCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AssetImage(productImage(p), p.name, Modifier.size(64.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.SemiBold)
                                    Caption(line.variant?.unit ?: p.unit)
                                    Caption(line.mode)
                                }
                                Text(money(lineTotal(line, p)), fontWeight = FontWeight.Bold)
                            }
                            if (line.addons.isNotEmpty())
                                Caption(line.addons.joinToString { it.name })
                            line.plan?.let {
                                Caption("${it.frequency} · ${it.startDate} · ${it.slot}")
                                val summary = packSummary(line)
                                Caption(
                                    "${summary.deliveries} deliveries · ${summary.freePacks} free packs"
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton({
                                    vm.update {
                                        it.copy(
                                            cart = it.cart.filterNot { old -> old.key == line.key }
                                        )
                                    }
                                }) {
                                    Text("Remove")
                                }
                                Spacer(Modifier.weight(1f))
                                Quantity(
                                    line.qty,
                                    { vm.quantity(line.key, it - line.qty) },
                                    minimum = 0,
                                )
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
                                AssetImage(
                                    productImage(product),
                                    product.name,
                                    Modifier.size(44.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("${product.name} · Free gift")
                                    Caption("${gift.line.qty} × ${product.unit}")
                                }
                                Text("FREE", color = Green, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    TextButton({ go("offers") }) { Text("View offers & free gifts ›") }
                    if (s.vipActive) {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFBEDC7),
                                            Color(0xFFFFF8E6),
                                            Color(0xFFF0D995),
                                        )
                                    ),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                CountryIcons.Crown,
                                null,
                                Modifier.size(24.dp),
                                tint = Color(0xFF805B12),
                            )
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
                    CartExtras(state, vm, go)
                    Bill(bill, 0.0)
                }
            }
        }
        if (s.cart.isNotEmpty()) {
            Surface(color = Canvas) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PrimaryButton("Continue to checkout · ${money(bill.total)}") { go("checkout") }
                }
            }
        }
    }
}
