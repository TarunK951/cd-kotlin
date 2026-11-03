package com.countryd.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.CountryState
import com.countryd.app.domain.*
import java.time.LocalDate
import java.util.Locale

fun grams(value: Double): String = String.format(Locale.US, "%.1f", value).removeSuffix(".0") + "g"

@Composable
fun ScheduledNutrition(state: CountryState, go: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf("Weekly") }
    var day by rememberSaveable { mutableIntStateOf(1) }
    val week = weeklyNutrition(state.session.subscriptions)
    val daily = week.days[day]
    val protein = if (period == "Weekly") week.protein else daily.protein
    val fiber = if (period == "Weekly") week.fiber else daily.fiber
    val count = if (period == "Weekly") week.packs else daily.packs
    val known = if (period == "Weekly") week.known else daily.known
    CountryCard {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Nutrition", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Caption(
                    "${if(known>0)"${grams(protein)} protein · ${grams(fiber)} fibre" else "Nutrition estimates"} · $count deliveries"
                )
            }
            Icon(
                if (expanded) CountryIcons.ChevronUp else CountryIcons.ChevronDown,
                "Expand nutrition information",
                tint = Green,
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nutrition", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    TextButton({ go("information") }) { Text("About", fontSize = 11.sp) }
                    OptionsInline(period) { period = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Protein" to protein, "Fiber" to fiber, "Deliveries" to count.toDouble())
                        .forEach { (label, amount) ->
                            Column(
                                Modifier.weight(1f)
                                    .heightIn(min = 68.dp)
                                    .background(Color(0xFFF6F7F4), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    if (label == "Deliveries") count.toString()
                                    else if (known > 0) grams(amount)
                                    else if (count > 0) "—" else "0g",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Caption(label)
                            }
                        }
                }
                if (period == "Daily")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        week.days.forEachIndexed { index, item ->
                            val date = LocalDate.parse(item.date)
                            FilterChip(
                                day == index,
                                { day = index },
                                label = {
                                    Text(
                                        "${date.dayOfWeek.name.take(3)} ${date.dayOfMonth}",
                                        fontSize = 10.sp,
                                    )
                                },
                            )
                        }
                    }
                Caption(
                    "Approximate nutrition information · per scheduled whole pack, not consumed intake."
                )
                state.session.subscriptions
                    .filter { it.status == "Active" }
                    .forEach { sub ->
                        val product = state.products.first { it.id == sub.productId }
                        val nutrition = packNutrients(sub.productId, sub.unit, sub.qty)
                        HorizontalDivider(color = Line)
                        Text(
                            "${product.name} · ${sub.qty} × ${sub.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Caption(
                            if (nutrition == null) "Nutrition unavailable"
                            else
                                "Per delivery: ${grams(nutrition.protein)} protein · ${grams(nutrition.fiber)} fiber"
                        )
                        val deliveries =
                            week.days.count { scheduledOn(sub, LocalDate.parse(it.date)) }
                        Caption(
                            "Week: $deliveries deliveries · ${if(nutrition==null)"Nutrition unavailable" else "${grams(nutrition.protein*deliveries)} protein · ${grams(nutrition.fiber*deliveries)} fiber"}"
                        )
                    }
                if (state.session.subscriptions.none { it.status == "Active" })
                    Caption(
                        "No active subscriptions. Resume a plan to see its scheduled nutrients."
                    )
            }
        }
    }
}

@Composable
private fun OptionsInline(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Daily", "Weekly").forEach {
            FilterChip(selected == it, { onSelect(it) }, label = { Text(it, fontSize = 10.sp) })
        }
    }
}
