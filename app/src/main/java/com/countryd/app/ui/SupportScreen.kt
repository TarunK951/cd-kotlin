package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.Instant

@Composable
fun SupportScreen(
    state: CountryState,
    vm: CountryViewModel,
    orderId: String,
    initialView: String = "help",
) {
    val s = state.session
    var view by
        rememberSaveable(initialView) {
            mutableStateOf(if (initialView == "requests") "My requests" else "Help")
        }
    val samples = remember(state.products) { sampleSupportOrders(state.products) }
    LaunchedEffect(Unit) {
        vm.update { old ->
            old.copy(
                requests =
                    old.requests +
                        sampleSupportTickets().filter { sample ->
                            old.requests.none { it.id == sample.id }
                        }
            )
        }
    }
    var requestOpen by rememberSaveable {
        mutableStateOf(orderId.isNotEmpty() || s.supportDraft.isNotEmpty())
    }
    var orderContext by rememberSaveable { mutableStateOf(orderId) }
    var previewOrder by rememberSaveable { mutableStateOf("") }
    val supportOrders = (s.orders + samples).sortedByDescending { it.createdAt }.take(6)
    var topic by rememberSaveable { mutableStateOf("Missing delivery") }
    var issue by rememberSaveable { mutableStateOf(s.supportDraft) }
    var language by rememberSaveable { mutableStateOf(s.supportLanguage) }
    var active by rememberSaveable { mutableStateOf("") }
    var followup by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Open") }
    ScreenBody {
        Row(Modifier.fillMaxWidth()) {
            listOf("Help", "My requests").forEach { tab ->
                Column(
                    Modifier.weight(1f).clickable { view = tab }.padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (tab == "Help") "Get help" else "My requests (${s.requests.size})",
                        color = if (view == tab) Green else Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier.fillMaxWidth()
                            .height(if (view == tab) 3.dp else 1.dp)
                            .background(if (view == tab) Green else Line)
                    )
                }
            }
        }
        if (view == "Help" && !requestOpen) {
            Heading("Choose an order")
            supportOrders.forEach { order ->
                CountryCard {
                    order.items.take(2).forEach { item ->
                        state.products
                            .find { it.id == item.line.productId }
                            ?.let { product ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AssetImage(
                                        productImage(product),
                                        product.name,
                                        Modifier.size(52.dp),
                                    )
                                    Column {
                                        Text(product.name)
                                        Caption(
                                            "${item.line.qty} � ${item.line.variant?.unit ?: product.unit}"
                                        )
                                    }
                                }
                            }
                    }
                    Caption("${order.status} � ${money(order.totals.total)}")
                    Caption("#${supportReference(order.id)} � ${order.deliveryDate}")
                    Row {
                        TextButton({
                            orderContext = order.id
                            requestOpen = true
                        }) {
                            Text("Get help")
                        }
                        TextButton({
                            previewOrder = if (previewOrder == order.id) "" else order.id
                        }) {
                            Text("View order")
                        }
                    }
                    if (previewOrder == order.id) {
                        order.items.forEach { item ->
                            state.products
                                .find { it.id == item.line.productId }
                                ?.let {
                                    Caption(
                                        "${item.line.qty} � ${it.name} � ${item.line.variant?.unit ?: it.unit}"
                                    )
                                }
                        }
                        Text("Total ${money(order.totals.total)}")
                    }
                }
            }
            PrimaryButton("Create a support request") {
                orderContext = ""
                requestOpen = true
            }
            CountryCard {
                Heading("Other help")
                TextButton({
                    topic = "Other"
                    issue = "Please arrange a callback about my order."
                    requestOpen = true
                }) {
                    Text("Save a callback request")
                }
            }
        } else if (view == "Help") {
            Heading("How can we help?")
            if (orderContext.isNotEmpty()) {
                Caption("For order #${supportReference(orderContext)}")
                TextButton({ orderContext = "" }) { Text("Change order") }
            } else {
                Caption("Order � Optional")
                Options(supportOrders.map { supportReference(it.id) }, "") { ref ->
                    orderContext = supportOrders.first { supportReference(it.id) == ref }.id
                }
            }
            Options(listOf("English", "Telugu", "Hindi"), language) {
                language = it
                vm.update { old -> old.copy(supportLanguage = it) }
            }
            listOf(
                    "Missing delivery",
                    "Wrong or damaged item",
                    "Payment or refund",
                    "Subscription",
                    "Product quality",
                    "Other",
                )
                .forEach { label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(topic == label, { topic = label })
                        Text(label)
                    }
                }
            OutlinedTextField(
                issue,
                {
                    issue = it.take(1000)
                    vm.update { old -> old.copy(supportDraft = issue) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Describe your issue") },
                minLines = 4,
            )
            PrimaryButton("Save support request", enabled = issue.trim().length >= 5) {
                val request =
                    SupportRequest(
                        "REQ-${System.currentTimeMillis()}",
                        topic,
                        issue.trim(),
                        orderContext,
                        language,
                        messages = listOf(SupportMessage(issue.trim(), Instant.now().toString())),
                    )
                vm.update { it.copy(requests = listOf(request) + it.requests, supportDraft = "") }
                issue = ""
                message = "Request saved locally. Live support is not connected."
                view = "My requests"
                active = request.id
                requestOpen = false
                filter = "Open"
            }
            Caption("This is a local preview. No support agent receives these messages.")
        } else {
            Options(listOf("Open", "Resolved", "Closed"), filter) { filter = it }
            val list = s.requests.filter { requestStatus(it) == filter }
            if (list.isEmpty()) Caption("No ${filter.lowercase()} requests.")
            list.forEach { r ->
                CountryCard {
                    BillRow(
                        r.topic,
                        if (r.status == "Escalated") "Open � escalation requested"
                        else requestStatus(r),
                        true,
                    )
                    Caption(supportReference(r.id))
                    Text(r.issue)
                    TextButton({ active = if (active == r.id) "" else r.id }) {
                        Text(if (active == r.id) "Hide details" else "View request")
                    }
                    if (active == r.id) {
                        r.messages.forEach { Caption("${it.at.take(10)} · ${it.text}") }
                        EntryField("Add a follow-up", followup, { followup = it })
                        TextButton({
                            if (followup.isNotBlank()) {
                                val text = followup.trim()
                                vm.update {
                                    it.copy(
                                        requests =
                                            it.requests.map { old ->
                                                if (old.id == r.id)
                                                    old.copy(
                                                        messages =
                                                            old.messages +
                                                                SupportMessage(
                                                                    text,
                                                                    Instant.now().toString(),
                                                                )
                                                    )
                                                else old
                                            }
                                    )
                                }
                                followup = ""
                            }
                        }) {
                            Text("Save follow-up")
                        }
                        listOf(
                                "Open" to "Reopen request",
                                "Escalated" to "Request escalation",
                                "Resolved" to "Mark resolved",
                                "Closed" to "Close request",
                            )
                            .filter { it.first != r.status }
                            .forEach { (status, label) ->
                                TextButton(
                                    onClick = {
                                        vm.update { session ->
                                            session.copy(
                                                requests =
                                                    session.requests.map {
                                                        if (it.id == r.id) it.copy(status = status)
                                                        else it
                                                    }
                                            )
                                        }
                                        filter = if (status == "Escalated") "Open" else status
                                    }
                                ) {
                                    Text(label)
                                }
                            }
                    }
                }
            }
        }
        if (message.isNotEmpty()) Caption(message)
    }
}
