package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.LocalDate

@Composable
fun SubscriptionsScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Active") }
    var end by remember { mutableStateOf<Subscription?>(null) }
    ScreenBody {
        Text("My deliveries", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        DeliveryTabs("My subscriptions") { if (it == "Orders") go("orders") }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Subscriptions",
                Modifier.weight(1f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Button(
                { go("shop") },
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text("+ Add", fontSize = 13.sp)
            }
        }
        DeliveryOverview(state, go)
        ScheduledNutrition(state, go)
        Row(
            Modifier.fillMaxWidth()
                .background(Color(0xFFF4F8F4), RoundedCornerShape(24.dp))
                .border(1.dp, Line, RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            listOf("Active", "Paused", "Past").forEach { status ->
                Box(
                    Modifier.weight(1f)
                        .heightIn(min = 36.dp)
                        .background(
                            if (filter == status) Green else Color.Transparent,
                            RoundedCornerShape(22.dp),
                        )
                        .selectable(
                            filter == status,
                            role = Role.Tab,
                            onClick = { filter = status },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$status (${state.session.subscriptions.count{it.status==status}})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (filter == status) Color.White else Muted,
                    )
                }
            }
        }
        val list = state.session.subscriptions.filter { it.status == filter }
        if (list.isEmpty())
            Column(
                Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(CountryIcons.Repeat, null, Modifier.size(44.dp), tint = Muted)
                Text(
                    "No ${filter.lowercase()} subscriptions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Subscribe to fresh milk, eggs, or paneer to get daily hassle-free delivery.",
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                PrimaryButton("Browse products") { go("shop") }
            }
        list.forEach { sub ->
            val p = state.products.first { it.id == sub.productId }
            CountryCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssetImage(productImage(p), p.name, Modifier.size(64.dp))
                    Column(Modifier.weight(1f)) {
                        Heading(p.name)
                        Caption("${sub.qty} × ${sub.unit} · ${sub.plan.frequency}")
                        Caption("${money(sub.price*sub.qty)} per delivery")
                    }
                }
                Caption("Next delivery: ${nextDelivery(sub) ?: "Not scheduled"}")
                if (sub.vacationStart.isNotEmpty())
                    Caption("Vacation: ${sub.vacationStart} – ${sub.vacationEnd}")
                if (sub.plan.offer != "none")
                    Caption(
                        "${sub.plan.offer.replaceFirstChar(Char::uppercase)} milk benefit · ${state.session.orders.filter{it.status=="Delivered"}.flatMap{it.items}.count{it.subscriptionId==sub.id}} delivered"
                    )
                if (sub.status != "Past") {
                    Row {
                        TextButton({
                            vm.editSubscription(
                                sub.copy(
                                    status = if (sub.status == "Paused") "Active" else "Paused"
                                )
                            )
                        }) {
                            Text(if (sub.status == "Paused") "Resume" else "Pause")
                        }
                        TextButton({ go("edit-sub/${sub.id}") }) { Text("Edit") }
                        TextButton({
                            nextDelivery(sub)?.let { date ->
                                vm.editSubscription(
                                    sub.copy(skippedDates = sub.skippedDates + date.toString())
                                )
                            }
                        }) {
                            Text("Skip next")
                        }
                    }
                    TextButton({ end = sub }) { Text("End subscription") }
                }
            }
        }
    }
    end?.let { sub ->
        AlertDialog(
            onDismissRequest = { end = null },
            title = { Text("End this subscription?") },
            text = {
                Text(
                    "This stops future scheduled deliveries. Existing orders stay in order history."
                )
            },
            confirmButton = {
                TextButton({
                    vm.editSubscription(sub.copy(status = "Past"))
                    end = null
                }) {
                    Text("End subscription")
                }
            },
            dismissButton = { TextButton({ end = null }) { Text("Keep plan") } },
        )
    }
}

@Composable
fun SubscriptionEditScreen(
    state: CountryState,
    vm: CountryViewModel,
    id: String,
    onDone: () -> Unit,
) {
    val sub = state.session.subscriptions.find { it.id == id } ?: return
    var qty by rememberSaveable { mutableIntStateOf(sub.qty) }
    var plan by remember { mutableStateOf(sub.plan) }
    var vacationStart by rememberSaveable { mutableStateOf(sub.vacationStart) }
    var vacationEnd by rememberSaveable { mutableStateOf(sub.vacationEnd) }
    var error by remember { mutableStateOf("") }
    ScreenBody {
        Heading(state.products.first { it.id == sub.productId }.name)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Packs per delivery", Modifier.weight(1f))
            Quantity(qty, { qty = it })
        }
        ScheduleEditor(plan) { plan = it }
        Heading("Vacation dates")
        Caption(
            "Scheduled deliveries are excluded during these dates. Leave both blank to clear vacation."
        )
        EntryField("Away from · YYYY-MM-DD", vacationStart, { vacationStart = it })
        EntryField("Back after · YYYY-MM-DD", vacationEnd, { vacationEnd = it })
        if (sub.skippedDates.isNotEmpty()) {
            Heading("Skipped deliveries")
            sub.skippedDates.sorted().forEach { date ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(date, Modifier.weight(1f))
                    TextButton({
                        vm.editSubscription(sub.copy(skippedDates = sub.skippedDates - date))
                    }) {
                        Text("Restore")
                    }
                }
            }
        }
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton(
            "Save changes",
            enabled = plan.frequency != "Custom" || plan.dates.isNotEmpty(),
        ) {
            val valid =
                vacationStart.isBlank() && vacationEnd.isBlank() ||
                    runCatching {
                            val from = LocalDate.parse(vacationStart)
                            val to = LocalDate.parse(vacationEnd)
                            from >= LocalDate.now() && to >= from
                        }
                        .getOrDefault(false)
            if (!valid) error = "Choose a valid vacation range, or clear both dates."
            else {
                vm.editSubscription(
                    sub.copy(
                        qty = qty,
                        plan = plan,
                        vacationStart = vacationStart,
                        vacationEnd = vacationEnd,
                    )
                )
                onDone()
            }
        }
    }
}
