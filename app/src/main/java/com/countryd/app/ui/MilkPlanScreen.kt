package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MilkOffersScreen(state: CountryState, go: (String) -> Unit) {
    ScreenBody {
        AssetImage(
            "assets/generated/premium-offers-2026-10-22/milk-hamper-scene-v1.jpg",
            null,
            Modifier.fillMaxWidth().height(190.dp),
        )
        Text("Fresh milk.\nA little more on us.", style = MaterialTheme.typography.headlineLarge)
        Caption(
            "Choose your milk, packs and delivery days. Offer estimates are shown before checkout."
        )
        listOf(
                Triple("welcome", "4 paid + 3 free", "Your first milk week"),
                Triple("weekly", "7 paid + 3 free", "A little extra for your routine"),
                Triple("monthly", "3 weeks paid + 1 free", "Your monthly milk plan"),
                Triple("none", "Custom plan", "Choose your packs and delivery days"),
            )
            .forEach { (offer, title, subtitle) ->
                if (
                    offer != "welcome" ||
                        state.session.subscriptions.none { it.plan.offer == "welcome" }
                )
                    CountryCard(color = if (offer == "welcome") Mint else Color.White) {
                        Heading(title)
                        Caption(subtitle)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.products
                                .filter { it.id in listOf("p1", "p3", "p4") }
                                .forEach { p ->
                                    Column(
                                        Modifier.weight(1f).clickable {
                                            go("milk-plan/${p.id}?offer=$offer")
                                        },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        AssetImage(
                                            productImage(p),
                                            p.name,
                                            Modifier.fillMaxWidth().height(80.dp),
                                        )
                                        Text(p.name, fontSize = 11.sp)
                                        Text("Choose ›", color = Green, fontSize = 12.sp)
                                    }
                                }
                        }
                    }
            }
    }
}

@Composable
fun PlanScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    id: String,
    initialOffer: String,
) {
    val p = state.products.find { it.id == id } ?: return
    var variant by remember { mutableStateOf(p.defaultVariant) }
    var qty by rememberSaveable { mutableIntStateOf(2) }
    var plan by remember {
        mutableStateOf(
            Plan(
                slot = "7–8 AM",
                offer =
                    if (
                        initialOffer == "welcome" &&
                            state.session.subscriptions.any { it.plan.offer == "welcome" }
                    )
                        "none"
                    else initialOffer,
            )
        )
    }
    val line = CartLine(p.id, qty, "Subscribe", variant, plan = plan)
    val packs = packSummary(line)
    ScreenBody {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(productImage(p), p.name, Modifier.size(110.dp))
            Column {
                Heading(p.name)
                Caption(variant?.unit ?: p.unit)
                Text("${money(variant?.price ?: p.price)} per pack", color = Green)
            }
        }
        Heading("Choose your plan")
        val planLabels =
            linkedMapOf(
                    "welcome" to "Welcome 4 + 3",
                    "weekly" to "Weekly offer",
                    "monthly" to "Monthly offer",
                    "none" to "Custom plan",
                )
                .filterKeys {
                    it != "welcome" ||
                        state.session.subscriptions.none { sub -> sub.plan.offer == "welcome" }
                }
        Options(planLabels.values.toList(), planLabels[plan.offer].orEmpty()) { label ->
            plan = plan.copy(offer = planLabels.entries.first { it.value == label }.key)
        }
        if (p.variants.isNotEmpty())
            Options(p.variants.map { it.name }, variant?.name.orEmpty()) { name ->
                variant = p.variants.first { it.name == name }
            }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Packs per delivery", Modifier.weight(1f))
            Quantity(qty, { qty = it }, maximum = 9)
        }
        MilkScheduleEditor(plan) { plan = it }
        CountryCard(color = Mint) {
            Heading("Your plan estimate")
            BillRow("Scheduled deliveries", "${packs.deliveries}")
            BillRow("Free packs", "${packs.freePacks}")
            BillRow("Charged packs", "${packs.chargedPacks}")
            BillRow("Plan total", money(lineTotal(line, p)), true)
            Caption("Quantity is per delivery. Free packs apply once per selected plan period.")
        }
        PrimaryButton(
            "Add plan · ${money(lineTotal(line,p))}",
            enabled = plan.frequency != "Custom" || plan.dates.isNotEmpty(),
        ) {
            vm.add(line)
            go("cart")
        }
    }
}

@Composable
fun ScheduleEditor(plan: Plan, onChange: (Plan) -> Unit) {
    Heading("Delivery schedule")
    Options(listOf("Daily", "Alternate days", "Weekly", "Custom"), plan.frequency) {
        onChange(plan.copy(frequency = it))
    }
    Caption("Start date")
    DateOptions(plan.startDate) { onChange(plan.copy(startDate = it)) }
    if (plan.frequency == "Custom") {
        Caption("Choose delivery dates")
        val start = LocalDate.now().plusDays(1)
        (0..3).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (0..6).forEach { day ->
                    val date = start.plusDays((week * 7 + day).toLong()).toString()
                    FilterChip(
                        date in plan.dates,
                        {
                            val dates =
                                if (date in plan.dates) plan.dates - date else plan.dates + date
                            onChange(
                                plan.copy(
                                    dates = dates.sorted(),
                                    startDate = dates.minOrNull() ?: plan.startDate,
                                )
                            )
                        },
                        label = {
                            Text(LocalDate.parse(date).dayOfMonth.toString(), fontSize = 11.sp)
                        },
                        modifier = Modifier.widthIn(min = 40.dp),
                    )
                }
            }
        }
        if (plan.dates.isEmpty())
            Text(
                "Choose at least one delivery date.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
    }
    Caption("Morning slot")
    Options(listOf("Morning, 6:00 – 7:00 AM", "Morning, 7:00 – 8:00 AM"), plan.slot) {
        onChange(plan.copy(slot = it))
    }
}

@Composable
fun DateOptions(selected: String, onSelect: (String) -> Unit) {
    val dates = (1L..7L).map { LocalDate.now().plusDays(it) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        dates.forEach { date ->
            FilterChip(
                selected == date.toString(),
                { onSelect(date.toString()) },
                label = { Text(date.format(DateTimeFormatter.ofPattern("EEE d MMM"))) },
            )
        }
    }
}

@Composable
fun MilkScheduleEditor(plan: Plan, onChange: (Plan) -> Unit) {
    var weekdays by remember { mutableStateOf(setOf(1, 3, 5)) }
    fun schedule(
        frequency: String = plan.frequency,
        start: String = plan.startDate,
        days: Set<Int> = weekdays,
    ) {
        val dates =
            if (frequency == "Custom") customDeliveryDates(LocalDate.parse(start), days)
            else emptyList()
        onChange(
            plan.copy(
                frequency = frequency,
                startDate = dates.firstOrNull() ?: start,
                dates = dates,
                slot = "7–8 AM",
            )
        )
    }
    Heading("Delivery schedule")
    Options(listOf("Daily", "Alternate days", "Custom"), plan.frequency) {
        schedule(frequency = it)
    }
    Caption("Start date")
    DateOptions(plan.startDate) { schedule(start = it) }
    if (plan.frequency == "Custom") {
        Heading("Custom days")
        Caption(
            if (weekdays.isEmpty()) "Pick at least one day"
            else
                "Every " +
                    weekdays.sorted().joinToString(", ") {
                        java.time.DayOfWeek.of(it)
                            .name
                            .take(3)
                            .lowercase()
                            .replaceFirstChar(Char::uppercase)
                    }
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..7).forEach { day ->
                FilterChip(
                    day in weekdays,
                    {
                        weekdays = if (day in weekdays) weekdays - day else weekdays + day
                        schedule(days = weekdays)
                    },
                    label = {
                        Text(
                            java.time.DayOfWeek.of(day)
                                .name
                                .take(3)
                                .lowercase()
                                .replaceFirstChar(Char::uppercase)
                        )
                    },
                )
            }
        }
    }
    Caption("Morning slot · 7–8 AM")
}
