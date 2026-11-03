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
fun NutritionScreen(state: CountryState, go: (String) -> Unit = {}) {
    val session = state.session
    val known =
        session.cart.mapNotNull { line ->
            val product = state.products.find { it.id == line.productId }
            product?.let { packNutrients(it.id, line.variant?.unit ?: it.unit, line.qty) }
        }
    ScreenBody {
        Heading("Your nutrition, in detail")
        CountryCard {
            Heading("Basket nutrition")
            BillRow(
                "Protein",
                if (known.isEmpty()) "Unavailable" else grams(known.sumOf { it.protein }),
            )
            BillRow(
                "Fiber",
                if (known.isEmpty()) "Unavailable" else grams(known.sumOf { it.fiber }),
            )
            Caption(
                "Estimated values · ${known.size}/${session.cart.size} entries covered · whole packs, not intake."
            )
        }
        Heading("What each basket item contributes")
        session.cart.forEach { line ->
            val product = state.products.first { it.id == line.productId }
            val unit = line.variant?.unit ?: product.unit
            val nutrition = packNutrients(product.id, unit, line.qty)
            CountryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AssetImage(productImage(product), product.name, Modifier.size(52.dp))
                    Column {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Caption(
                            "${line.qty} × $unit · ${if(line.mode=="Subscribe")"Subscription" else "One-time"}"
                        )
                        Caption(
                            if (nutrition == null) "Nutrition unavailable for this pack"
                            else
                                "${grams(nutrition.protein)} protein · ${grams(nutrition.fiber)} fiber"
                        )
                    }
                }
            }
        }
        ScheduledNutrition(state, go)
        Heading("Your subscriptions, ingredient by ingredient")
        session.subscriptions.forEach { sub ->
            val product = state.products.first { it.id == sub.productId }
            val week = weeklyNutrition(listOf(sub))
            CountryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AssetImage(productImage(product), product.name, Modifier.size(52.dp))
                    Column {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Caption("${sub.qty} × ${sub.unit} · ${sub.plan.frequency} · ${sub.status}")
                        Caption(
                            "Next 7 days: ${grams(week.protein)} protein · ${grams(week.fiber)} fiber"
                        )
                        Caption(
                            "${week.packs} deliveries · ${nextDelivery(sub)?.let{"Next $it"} ?: "No upcoming delivery"}"
                        )
                    }
                }
            }
        }
        Caption(
            "Illustrative whole-pack values. Extras and unknown packs excluded. Delivery schedules and consumed food are different; these estimates do not measure what you ate."
        )
    }
}
