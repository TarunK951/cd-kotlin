package com.countryd.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
internal fun ConnectedSubscriptions(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    openOrders: () -> Unit,
    openShop: () -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("ACTIVE") }
    var editing by remember { mutableStateOf<JsonObject?>(null) }
    var cancelling by remember { mutableStateOf<JsonObject?>(null) }
    LaunchedEffect(model) {
        model.state.first { !it.busy }
        model.subscriptions()
    }
    val plans = state.subscriptions?.list("subscriptions").orEmpty()
    ScreenBody {
        Text("My deliveries", style = MaterialTheme.typography.headlineSmall)
        DeliveryTabs("My subscriptions") { if (it == "Orders") openOrders() }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Subscriptions", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = openShop) { Text("+ Add") }
        }
        state.subscriptions?.str("estimatedMonthlyTotal")?.takeIf(String::isNotBlank)?.let {
            Text("Estimated monthly total · ₹$it", color = Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("ACTIVE" to "Active", "PAUSED" to "Paused", "CANCELLED" to "Past").forEach {
                (status, label) ->
                FilterChip(
                    selected = filter == status,
                    onClick = { filter = status },
                    label = { Text("$label (${plans.count { it.str("status") == status }})") },
                )
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (editing == null && state.error.isNotBlank()) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = model::subscriptions, enabled = !state.busy) { Text("Refresh") }
        }
        if (state.message.isNotBlank() && state.error.isBlank()) {
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = Green)
        }
        val visible = plans.filter { it.str("status") == filter }
        if (state.subscriptions != null && visible.isEmpty()) {
            Text("No ${filter.lowercase()} subscriptions.", color = Muted)
        }
        visible.forEach { plan ->
            CountryCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FeedbackImage(plan.str("imageUrl"), plan.str("productName"))
                    Column(Modifier.weight(1f)) {
                        Text(plan.str("productName"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${plan.num("quantity")} × ${plan.str("unit")} · ${subscriptionFrequencyLabel(plan.str("frequency"))}",
                            color = Muted,
                        )
                    }
                }
                plan.str("nextDeliveryDate").takeIf(String::isNotBlank)?.let {
                    Text("Next delivery · $it")
                }
                plan.str("deliveryAddress").takeIf(String::isNotBlank)?.let {
                    Text(it, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                if (filter != "CANCELLED") {
                    plan.str("estimatedMonthlyCost").takeIf(String::isNotBlank)?.let {
                        Text("Estimated monthly cost · ₹$it", color = Muted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { editing = plan }, enabled = !state.busy) {
                            Text("Edit")
                        }
                        TextButton(
                            onClick = {
                                model.subscriptionAction(
                                    plan.num("subscriptionId"),
                                    if (filter == "PAUSED") "resume" else "pause",
                                )
                            },
                            enabled = !state.busy,
                        ) {
                            Text(if (filter == "PAUSED") "Resume" else "Pause")
                        }
                        TextButton(onClick = { cancelling = plan }, enabled = !state.busy) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
    editing?.let { plan ->
        SubscriptionForm(state, model, product = null, plan = plan, onDismiss = { editing = null })
    }
    cancelling?.let { plan ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            title = { Text("Cancel subscription?") },
            text = { Text("Future deliveries for ${plan.str("productName")} will stop.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cancelling = null
                        model.subscriptionAction(plan.num("subscriptionId"), "cancel")
                    }
                ) {
                    Text("Cancel subscription")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelling = null }) { Text("Keep subscription") }
            },
        )
    }
}

@Composable
internal fun SubscribeAction(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    product: JsonObject,
) {
    if (!product.flag("subscribable")) return
    var show by rememberSaveable(product.str("productId")) { mutableStateOf(false) }
    OutlinedButton(
        onClick = { show = true },
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Subscribe")
    }
    if (show) SubscriptionForm(state, model, product, plan = null, onDismiss = { show = false })
}

@Composable
private fun SubscriptionForm(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    product: JsonObject?,
    plan: JsonObject?,
    onDismiss: () -> Unit,
) {
    val editing = plan != null
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var date by rememberSaveable { mutableStateOf(today.plusDays(1).toString()) }
    var quantity by rememberSaveable {
        mutableIntStateOf(plan?.num("quantity")?.toInt()?.coerceIn(1, 10) ?: 1)
    }
    var frequency by rememberSaveable {
        mutableStateOf(plan?.str("frequency").orEmpty().ifBlank { "DAILY" })
    }
    var addressId by rememberSaveable { mutableLongStateOf(plan?.num("addressId") ?: 0L) }
    var addressMenu by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(model) {
        if (state.addresses == null) {
            model.state.first { !it.busy }
            model.addresses()
        }
    }
    LaunchedEffect(submitted) {
        if (submitted) {
            val response = model.state.first { !it.busy }
            if (response.error.isBlank()) onDismiss() else submitted = false
        }
    }
    val addresses = state.addresses?.list("addresses").orEmpty()
    val address =
        addresses.firstOrNull { it.num("addressId") == addressId }
            ?: if (addressId == 0L) addresses.firstOrNull { it.flag("isDefault") } else null
    AlertDialog(
        onDismissRequest = { if (!submitted) onDismiss() },
        title = {
            Text(
                if (editing) "Edit subscription"
                else "Subscribe to ${product?.str("name").orEmpty()}"
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantity", Modifier.weight(1f))
                    Quantity(quantity, { quantity = it }, maximum = 10)
                }
                Text("Delivery frequency")
                listOf("DAILY", "ALTERNATE_DAYS").forEach { value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            enabled = !submitted,
                        )
                        Text(subscriptionFrequencyLabel(value))
                    }
                }
                if (!editing) {
                    OutlinedButton(
                        onClick = {
                            val current = LocalDate.parse(date)
                            val zone = ZoneId.systemDefault()
                            DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        date = LocalDate.of(year, month + 1, day).toString()
                                    },
                                    current.year,
                                    current.monthValue - 1,
                                    current.dayOfMonth,
                                )
                                .apply {
                                    datePicker.minDate =
                                        today
                                            .plusDays(1)
                                            .atStartOfDay(zone)
                                            .toInstant()
                                            .toEpochMilli()
                                    datePicker.maxDate =
                                        today
                                            .plusDays(30)
                                            .atStartOfDay(zone)
                                            .toInstant()
                                            .toEpochMilli()
                                }
                                .show()
                        },
                        enabled = !submitted,
                    ) {
                        Text("Start date · $date")
                    }
                }
                if (addresses.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { addressMenu = true }, enabled = !submitted) {
                            Text(
                                "Deliver to · ${address?.str("label").orEmpty().ifBlank { "Choose address" }}"
                            )
                        }
                        DropdownMenu(
                            expanded = addressMenu,
                            onDismissRequest = { addressMenu = false },
                        ) {
                            addresses.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.str("label")) },
                                    onClick = {
                                        addressId = option.num("addressId")
                                        addressMenu = false
                                    },
                                )
                            }
                        }
                    }
                    address?.str("fullAddress")?.takeIf(String::isNotBlank)?.let {
                        Text(it, color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (state.error.isNotBlank())
                    Text(state.error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.busy && !submitted,
                onClick = {
                    val body = buildJsonObject {
                        if (product != null) {
                            put("productId", product.str("productId"))
                            put("startDate", date)
                        }
                        put("quantity", quantity)
                        put("frequency", frequency)
                        if (addressId > 0) put("addressId", addressId)
                    }
                    if (plan != null) model.updateSubscription(plan.num("subscriptionId"), body)
                    else model.createSubscription(body)
                    submitted = true
                },
            ) {
                Text(if (editing) "Save changes" else "Start subscription")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitted) { Text("Back") } },
    )
}

private fun subscriptionFrequencyLabel(value: String) =
    if (value == "ALTERNATE_DAYS") "Alternate days" else "Daily"
