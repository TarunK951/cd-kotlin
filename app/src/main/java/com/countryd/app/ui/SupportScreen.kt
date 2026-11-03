package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
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
    var topic by rememberSaveable { mutableStateOf("Missing delivery") }
    var issue by rememberSaveable { mutableStateOf(s.supportDraft) }
    var language by rememberSaveable { mutableStateOf(s.supportLanguage) }
    var active by rememberSaveable { mutableStateOf("") }
    var followup by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Open") }
    ScreenBody {
        Options(listOf("Help", "My requests"), view) { view = it }
        if (view == "Help") {
            Heading("How can we help?")
            if (orderId.isNotEmpty()) Caption("For order $orderId")
            Options(listOf("English", "Hindi", "Telugu", "Kannada"), language) {
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
                    issue = it.take(2000)
                    vm.update { old -> old.copy(supportDraft = issue) }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Describe your issue") },
                minLines = 4,
            )
            PrimaryButton("Create support request", enabled = issue.trim().length >= 5) {
                val request =
                    SupportRequest(
                        "REQ-${System.currentTimeMillis()}",
                        topic,
                        issue.trim(),
                        orderId,
                        language,
                        messages = listOf(SupportMessage(issue.trim(), Instant.now().toString())),
                    )
                vm.update { it.copy(requests = listOf(request) + it.requests, supportDraft = "") }
                issue = ""
                message = "Request saved locally. Live support is not connected."
                view = "My requests"
                active = request.id
            }
            Caption("This is a local preview. No support agent receives these messages.")
        } else {
            Options(listOf("Open", "Escalated", "Resolved", "Closed"), filter) { filter = it }
            val list = s.requests.filter { it.status == filter }
            if (list.isEmpty()) Caption("No ${filter.lowercase()} requests.")
            list.forEach { r ->
                CountryCard {
                    BillRow(r.topic, r.status, true)
                    Caption(r.id)
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
                        Options(listOf("Open", "Escalated", "Resolved", "Closed"), r.status) {
                            status ->
                            vm.update {
                                it.copy(
                                    requests =
                                        it.requests.map { old ->
                                            if (old.id == r.id) old.copy(status = status) else old
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
        if (message.isNotEmpty()) Caption(message)
    }
}
