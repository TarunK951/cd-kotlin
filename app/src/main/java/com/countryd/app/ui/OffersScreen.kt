package com.countryd.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import kotlin.math.ceil
import kotlinx.coroutines.delay

@Composable
fun OffersScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val session = state.session
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val subtotal =
        session.cart.sumOf { line ->
            state.products.find { it.id == line.productId }?.let { lineTotal(line, it) } ?: 0.0
        }
    val seconds = ceil((session.offerEndsAt - now).coerceAtLeast(0) / 1000.0).toLong()
    ScreenBody {
        Caption("A little closer to your next reward.")
        CountryCard {
            Text("Apply coupon", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Caption(
                "Demo code DELIGHT10 · extra ₹10 off items of ₹200 or more. One coupon per order."
            )
            if (session.coupon.isNotEmpty()) {
                Text(
                    if (subtotal >= 200) "DELIGHT10 applied · ₹10 saved"
                    else "Add ${money(200-subtotal)} in items to use DELIGHT10",
                    color = Green,
                )
                TextButton({
                    vm.update { it.copy(coupon = "") }
                    error = ""
                }) {
                    Text("Remove")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        EntryField(
                            "Coupon code",
                            code,
                            {
                                code = it.uppercase()
                                error = ""
                            },
                        )
                    }
                    OutlinedButton({
                        error =
                            when {
                                code.trim() != "DELIGHT10" ->
                                    "This code is not available. Try DELIGHT10."
                                subtotal < 200 ->
                                    "Add ${money(200-subtotal)} in items to use this coupon."
                                else -> ""
                            }
                        if (error.isEmpty()) vm.update { it.copy(coupon = "DELIGHT10") }
                    }) {
                        Text("Apply")
                    }
                }
            }
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
        CountryCard(color = Mint) {
            Text("Up to ₹83 club savings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Caption(
                if (subtotal >= 200) "Free delivery included"
                else "${money(200-subtotal)} away from free delivery"
            )
        }
        if (seconds > 0)
            Caption("Today's bundle offers end in ${"%02d:%02d".format(seconds/60, seconds%60)}")
        bundleOffers
            .filter { offerAvailable(it, session, now) }
            .forEach { offer ->
                state.products
                    .find { it.id == offer.productId }
                    ?.let { product ->
                        val quantity = offerQuantity(offer, session.cart, state.products)
                        val remaining = (offer.required - quantity).coerceAtLeast(0)
                        CountryCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Buy ${offer.required}, get ${offer.free} free",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Caption("${product.name} · ${product.unit}")
                                }
                                if (remaining == 0) Text("Added", color = Green)
                                else
                                    TextButton({
                                        vm.add(
                                            CartLine(product.id, variant = product.defaultVariant)
                                        )
                                    }) {
                                        Text("Add")
                                    }
                            }
                            LinearProgressIndicator(
                                progress = {
                                    (quantity.toFloat() / offer.required).coerceIn(0f, 1f)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = Green,
                                trackColor = Mint,
                            )
                            Text(
                                "${quantity.coerceAtMost(offer.required)} / ${offer.required} packs · ${if (remaining == 0) "Unlocked" else "${money(remaining*product.price)} left to unlock"}",
                                color = Green,
                            )
                            Caption(
                                if (remaining == 0)
                                    "${offer.free} free ${if (offer.free == 1) "pack" else "packs"} unlocked"
                                else
                                    "Add $remaining more ${if (remaining == 1) "pack" else "packs"} at ${money(product.price)} each to unlock"
                            )
                            TextButton({ go("product/${product.id}") }) { Text("View product ›") }
                        }
                    }
            }
        if (bundleOffers.none { offerAvailable(it, session, now) })
            Caption("Today’s one-hour bundle offers have ended")
        Caption("Demo offers · standard packs · one bundle per order. Gifts apply automatically.")
        PrimaryButton("Explore milk plans") { go("milk-offer") }
    }
}
