package com.countryd.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.countryd.app.api.FeedbackConnection
import com.countryd.app.api.FeedbackConnectionModel
import com.countryd.app.api.flag
import com.countryd.app.api.list
import com.countryd.app.api.num
import com.countryd.app.api.str
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
internal fun ConnectedAddresses(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    onSelected: (() -> Unit)? = null,
) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val addresses = state.addresses?.list("addresses").orEmpty()

    LaunchedEffect(Unit) {
        model.state.first { !it.busy }
        model.addresses()
    }
    LaunchedEffect(saving) {
        if (saving) {
            val completed = model.state.first { !it.busy }
            if (completed.error.isBlank()) showForm = false
            saving = false
        }
    }

    ScreenBody {
        if (showForm) {
            val address = addresses.firstOrNull { it.num("addressId") == editingId }
            key(editingId) {
                AddressEditor(
                    address,
                    state.customer,
                    state.busy,
                    cancel = { showForm = false },
                    save = { body ->
                        model.saveAddress(editingId, body)
                        saving = true
                    },
                )
            }
        } else {
            Heading(if (onSelected == null) "Saved addresses" else "Choose delivery address")
            if (state.addresses != null && addresses.isEmpty()) Text("No saved addresses.")
            addresses.forEach { address ->
                val id = address.num("addressId")
                val isDefault = address.flag("isDefault")
                val selected = state.selectedAddressId?.let { it == id } ?: isDefault
                val complete =
                    address.str("line1").isNotBlank() &&
                        address.str("pincode").matches(Regex("[0-9]{6}"))
                CountryCard {
                    Text(
                        address.str("label").ifBlank { "Address" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (isDefault)
                        Text(
                            "Default address",
                            color = Green,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    Text(address.str("fullAddress"), color = Muted)
                    if (!complete)
                        Text(
                            "Complete the street address and PIN code before delivery.",
                            color = Muted,
                        )
                    if (onSelected != null) {
                        PrimaryButton(
                            if (selected) "Use selected address" else "Deliver here",
                            enabled = complete && !state.busy,
                        ) {
                            model.selectAddress(id)
                            onSelected()
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            enabled = !state.busy,
                            onClick = {
                                editingId = id
                                showForm = true
                            },
                        ) {
                            Text("Edit")
                        }
                        if (!isDefault) {
                            TextButton(
                                enabled = !state.busy,
                                onClick = { model.defaultAddress(id) },
                            ) {
                                Text("Set as default")
                            }
                        }
                        if (addresses.size > 1) {
                            TextButton(enabled = !state.busy, onClick = { pendingDelete = id }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
            PrimaryButton("Add address", enabled = !state.busy && addresses.size < 10) {
                editingId = null
                showForm = true
            }
            if (addresses.size >= 10) Text("You can save up to 10 addresses.", color = Muted)
            if (state.error.isNotBlank()) {
                TextButton(enabled = !state.busy, onClick = model::addresses) {
                    Text("Refresh addresses")
                }
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete address?") },
            text = { Text("This address will be removed from your saved addresses.") },
            confirmButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = {
                        pendingDelete = null
                        model.deleteAddress(id)
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AddressEditor(
    address: JsonObject?,
    customer: JsonObject?,
    busy: Boolean,
    cancel: () -> Unit,
    save: (JsonObject) -> Unit,
) {
    var label by rememberSaveable { mutableStateOf(address?.str("label") ?: "Home") }
    var line1 by rememberSaveable { mutableStateOf(address?.str("line1").orEmpty()) }
    var line2 by rememberSaveable { mutableStateOf(address?.str("line2").orEmpty()) }
    var landmark by rememberSaveable { mutableStateOf(address?.str("landmark").orEmpty()) }
    var area by rememberSaveable {
        mutableStateOf(address?.str("area") ?: customer?.str("area").orEmpty())
    }
    var city by rememberSaveable {
        mutableStateOf(address?.str("city") ?: customer?.str("city").orEmpty())
    }
    var pincode by rememberSaveable { mutableStateOf(address?.str("pincode").orEmpty()) }
    var makeDefault by rememberSaveable { mutableStateOf(address?.flag("isDefault") == true) }

    Heading(if (address == null) "Add address" else "Edit address")
    EntryField("Address label", label, { label = it.take(30) })
    EntryField("Flat, house and building", line1, { line1 = it.take(120) })
    EntryField("Street or road (optional)", line2, { line2 = it.take(120) })
    EntryField("Landmark (optional)", landmark, { landmark = it.take(80) })
    EntryField("Area", area, { area = it.take(80) })
    EntryField("City", city, { city = it.take(60) })
    EntryField(
        "PIN code",
        pincode,
        { pincode = it.filter { char -> char in '0'..'9' }.take(6) },
        true,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            makeDefault,
            { makeDefault = it },
            enabled = !busy && address?.flag("isDefault") != true,
        )
        Text("Use as default")
    }
    val valid = listOf(label, line1, area, city).all { it.isNotBlank() } && pincode.length == 6
    PrimaryButton("Save address", enabled = valid && !busy) {
        save(
            buildJsonObject {
                put("label", label.trim())
                put("line1", line1.trim())
                put("line2", line2.trim())
                put("landmark", landmark.trim())
                put("area", area.trim())
                put("city", city.trim())
                put("pincode", pincode)
                put("makeDefault", makeDefault)
            }
        )
    }
    TextButton(enabled = !busy, onClick = cancel) { Text("Cancel") }
}
