package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val today = LocalDate.now()
    var selected by rememberSaveable { mutableStateOf(today.plusDays(1).toString()) }
    var monthText by rememberSaveable {
        mutableStateOf(YearMonth.from(LocalDate.parse(selected)).toString())
    }
    var expanded by rememberSaveable { mutableStateOf("") }
    var reportOpen by rememberSaveable { mutableStateOf(false) }
    var delivered by rememberSaveable { mutableStateOf(true) }
    val month = YearMonth.parse(monthText)
    val date = LocalDate.parse(selected)
    val session = state.session
    val orders = session.orders.filter { it.deliveryDate == selected && it.status != "Cancelled" }
    val subscriptions =
        if (date > today)
            session.subscriptions.filter { sub ->
                scheduledOn(sub, date) &&
                    orders.none { order -> order.items.any { it.line.productId == sub.productId } }
            }
        else emptyList()
    fun move(offset: Long) {
        val next = month.plusMonths(offset)
        monthText = next.toString()
        selected = next.atDay(1).toString()
        expanded = ""
    }
    ScreenBody {
        CountryCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ move(-1) }) { Icon(CountryIcons.ChevronLeft, "Previous month") }
                Text(
                    month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton({ move(1) }) {
                    Icon(
                        CountryIcons.ChevronLeft,
                        "Next month",
                        Modifier.graphicsLayer { rotationZ = 180f },
                    )
                }
            }
            Row {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = Muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            val leading = month.atDay(1).dayOfWeek.value % 7
            val cells = ((month.lengthOfMonth() + leading + 6) / 7) * 7
            (0 until cells).toList().chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        val number = cell - leading + 1
                        if (number !in 1..month.lengthOfMonth())
                            Spacer(Modifier.weight(1f).height(44.dp))
                        else {
                            val day = month.atDay(number)
                            val key = day.toString()
                            val dated =
                                session.orders.filter {
                                    it.deliveryDate == key && it.status != "Cancelled"
                                }
                            val hasDelivered = dated.any { it.status == "Delivered" }
                            val upcoming =
                                dated.any { it.status != "Delivered" } ||
                                    day > today &&
                                        session.subscriptions.any { scheduledOn(it, day) }
                            val skipped = session.subscriptions.any { key in it.skippedDates }
                            val vacation =
                                session.subscriptions.any {
                                    it.status != "Past" &&
                                        it.vacationStart.isNotEmpty() &&
                                        key >= it.vacationStart &&
                                        key <= it.vacationEnd
                                }
                            Column(
                                Modifier.weight(1f)
                                    .height(44.dp)
                                    .clickable { selected = key }
                                    .background(
                                        if (selected == key) Mint else Color.Transparent,
                                        CircleShape,
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    number.toString(),
                                    fontSize = 13.sp,
                                    color = if (!hasDelivered && !upcoming) Muted else Green,
                                    fontWeight =
                                        if (selected == key) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(
                                    when {
                                        vacation -> "✈"
                                        skipped -> "–"
                                        hasDelivered -> "●"
                                        upcoming -> "○"
                                        else -> ""
                                    },
                                    fontSize = 9.sp,
                                    color = Green,
                                )
                            }
                        }
                    }
                }
            }
            Caption("● Delivered  ○ Upcoming  – Skipped  ✈ Vacation")
        }
        CountryCard {
            Row(
                Modifier.fillMaxWidth().clickable { reportOpen = !reportOpen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Monthly nutrition comparison",
                    Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(if (reportOpen) CountryIcons.ChevronUp else CountryIcons.ChevronDown, null)
            }
            androidx.compose.animation.AnimatedVisibility(reportOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Options(
                        listOf("Delivered", "Upcoming"),
                        if (delivered) "Delivered" else "Upcoming",
                    ) {
                        delivered = it == "Delivered"
                    }
                    val report = monthlyNutrition(session, state.products, month, today, delivered)
                    BillRow("Protein", "${report.protein.toInt()}g")
                    BillRow("Fiber", "${report.fiber.toInt()}g")
                    Caption(
                        "Whole-pack estimates · ${report.known}/${report.packs} entries covered · not consumed intake."
                    )
                    if (report.packs == 0)
                        Caption(
                            "No ${if(delivered)"delivered orders" else "upcoming deliveries"} this month."
                        )
                    TextButton({ go("assistant") }) { Text("Plan with Delight") }
                }
            }
        }
        Text(
            date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            fontWeight = FontWeight.SemiBold,
            color = Green,
        )
        if (date > today)
            CountryCard(color = Color(0xFFF4F7F2)) {
                Heading("Make this day work for you")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ go("subs") }, Modifier.weight(1f)) {
                        Text("Change", fontSize = 11.sp)
                    }
                    val active = session.subscriptions.filter { scheduledOn(it, date) }
                    val skipped =
                        session.subscriptions.filter {
                            it.status == "Active" && selected in it.skippedDates
                        }
                    val restore = active.isEmpty() && skipped.isNotEmpty()
                    OutlinedButton(
                        {
                            val ids = (if (restore) skipped else active).map { it.id }.toSet()
                            vm.update {
                                it.copy(
                                    subscriptions =
                                        it.subscriptions.map { sub ->
                                            if (sub.id !in ids) sub
                                            else
                                                sub.copy(
                                                    skippedDates =
                                                        if (restore) sub.skippedDates - selected
                                                        else sub.skippedDates + selected
                                                )
                                        }
                                )
                            }
                        },
                        Modifier.weight(1f),
                        enabled = active.isNotEmpty() || restore,
                    ) {
                        Text(if (restore) "Restore" else "Skip day", fontSize = 11.sp)
                    }
                    OutlinedButton({ go("vacation") }, Modifier.weight(1f)) {
                        Text("Vacation", fontSize = 11.sp)
                    }
                }
            }
        orders.forEach { order ->
            CountryCard {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        expanded = if (expanded == order.id) "" else order.id
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "#${order.id} · ${order.status}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Caption("${order.slot} · ${order.items.sumOf{it.line.qty}} items")
                    }
                    Text("›")
                }
                if (expanded == order.id) {
                    order.items.forEach { item ->
                        val product = state.products.first { it.id == item.line.productId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetImage(
                                productImage(product),
                                product.name,
                                Modifier.size(44.dp, 52.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(product.name, fontSize = 13.sp)
                                Caption(
                                    "${item.line.qty} × ${item.line.variant?.unit ?: product.unit}"
                                )
                            }
                            Text(money(lineTotal(item.line, product)), fontSize = 13.sp)
                        }
                    }
                    BillRow("Order total", money(order.totals.total))
                    Caption(order.address)
                    TextButton({ go("order/${order.id}") }) { Text("View full order") }
                }
            }
        }
        if (subscriptions.isNotEmpty())
            CountryCard {
                Heading("Scheduled subscriptions")
                subscriptions.forEach { sub ->
                    val product = state.products.first { it.id == sub.productId }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssetImage(productImage(product), product.name, Modifier.size(44.dp, 52.dp))
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontSize = 13.sp)
                            Caption("${sub.qty} × ${sub.unit} · ${sub.plan.slot}")
                        }
                        Text(money(sub.price * sub.qty), fontSize = 13.sp)
                    }
                }
                TextButton({ go("subs") }) { Text("Manage subscriptions") }
            }
        if (orders.isEmpty() && subscriptions.isEmpty())
            EmptyState(
                "No deliveries on this date.",
                "Your active plans and orders appear on their delivery days.",
            )
    }
}
