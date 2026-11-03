package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun CartScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val s = state.session
    ScreenBody {
        if (s.cart.isEmpty())
            EmptyState("Your basket is empty", "Add fresh essentials to get started.", "Shop now") {
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
                    if (line.addons.isNotEmpty()) Caption(line.addons.joinToString { it.name })
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
                                it.copy(cart = it.cart.filterNot { old -> old.key == line.key })
                            }
                        }) {
                            Text("Remove")
                        }
                        Spacer(Modifier.weight(1f))
                        Quantity(line.qty, { vm.quantity(line.key, it - line.qty) }, minimum = 0)
                    }
                }
            }
            TextButton({ go("offers") }) { Text("View offers & free gifts ›") }
            CountryCard(color = Mint) {
                Caption("Fresh picks to complete your basket")
                ProductShelf(
                    state.products.filter { p -> s.cart.none { it.productId == p.id } }.take(4),
                    s,
                    vm,
                    go,
                )
            }
            val items =
                s.cart.sumOf { lineTotal(it, state.products.first { p -> p.id == it.productId }) }
            val bill = totals(items, s)
            Bill(bill, 0.0)
            PrimaryButton("Continue to checkout · ${money(bill.total)}") { go("checkout") }
        }
    }
}
