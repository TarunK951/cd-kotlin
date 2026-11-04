package com.countryd.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

private val deliveryPin = Regex("[1-9][0-9]{5}")

internal fun JsonObject.isCompleteDeliveryAddress() =
    str("line1").isNotBlank() && str("area").isNotBlank() && str("city").isNotBlank() &&
        deliveryPin.matches(str("pincode"))

internal fun FeedbackConnection.activeDeliveryAddress(): JsonObject? {
    val id = selectedAddressId ?: addresses?.num("defaultAddressId")
    return addresses?.list("addresses")?.firstOrNull { it.num("addressId") == id }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectedAddresses(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    onClose: () -> Unit,
    onSelected: (() -> Unit)? = null,
) {
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val addresses = state.addresses?.list("addresses").orEmpty()
    val activeAddress = state.activeDeliveryAddress()
    val orderedAddresses = addresses.sortedByDescending {
        it.num("addressId") == activeAddress?.num("addressId")
    }

    LaunchedEffect(Unit) {
        model.state.first { !it.busy }
        model.addresses()
    }
    val goBack = {
        if (showForm) showForm = false else onClose()
    }
    BackHandler(enabled = showForm) { if (!state.busy) goBack() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(if (showForm) {
                    if (editingId == null) "Add address" else "Edit address"
                } else "Saved addresses")
            },
            navigationIcon = {
                IconButton(onClick = goBack, enabled = !state.busy) {
                    Icon(CountryIcons.ArrowLeft, if (showForm) "Back to saved addresses" else "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        )
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showForm) {
                val address = addresses.firstOrNull { it.num("addressId") == editingId }
                key(editingId) {
                    AddressEditor(
                        address,
                        state.customer,
                        state.busy,
                        state.error,
                        onSelected != null,
                        cancel = { showForm = false },
                        save = { body ->
                            model.saveAddress(editingId, body, selectAfterSave = onSelected != null) {
                                showForm = false
                                onSelected?.invoke()
                            }
                        },
                    )
                }
            } else {
                if (onSelected != null) {
                    Text("Choose where your next order should be delivered.", color = Muted)
                }
                if (state.addresses != null && addresses.isEmpty()) Text("No saved addresses yet.")
                orderedAddresses.forEach { address ->
                    val id = address.num("addressId")
                    val isDefault = address.flag("isDefault")
                    val selected = activeAddress?.num("addressId") == id
                    val complete = address.isCompleteDeliveryAddress()
                    val edit = {
                        editingId = id
                        showForm = true
                    }
                    CountryCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    address.str("label").ifBlank { "Address" },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                val status = when {
                                    selected && complete -> "Current delivery address"
                                    isDefault -> "Default address"
                                    else -> null
                                }
                                status?.let {
                                    Text(it, color = Green, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (complete) {
                                IconButton(onClick = edit, enabled = !state.busy) {
                                    Icon(CountryIcons.Pencil, "Edit ${address.str("label")} address", tint = Green)
                                }
                            }
                        }
                        Text(address.str("fullAddress"), color = Muted)
                        if (!complete) {
                            Text(
                                "Add the street address and PIN code to use this address.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            PrimaryButton("Complete address", enabled = !state.busy, onClick = edit)
                        } else if (onSelected != null) {
                            PrimaryButton(
                                if (selected) "Use this address" else "Deliver here",
                                enabled = !state.busy,
                            ) {
                                model.selectAddress(id)
                                onSelected()
                            }
                        }
                        if (addresses.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!isDefault && complete) {
                                    TextButton(
                                        enabled = !state.busy,
                                        onClick = { model.defaultAddress(id) },
                                    ) { Text("Set as default") }
                                }
                                TextButton(enabled = !state.busy, onClick = { pendingDelete = id }) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
                if (addresses.size >= 10) Text("You can save up to 10 addresses.", color = Muted)
                if (state.error.isNotBlank()) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    TextButton(enabled = !state.busy, onClick = model::addresses) {
                        Text("Try again")
                    }
                }
            }
        }
        if (!showForm) {
            Surface(color = Canvas) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PrimaryButton("Add address", enabled = !state.busy && addresses.size < 10) {
                        editingId = null
                        showForm = true
                    }
                }
            }
        }
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
                ) { Text("Delete") }
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
    saveError: String,
    choose: Boolean,
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
    var triedSaving by rememberSaveable { mutableStateOf(false) }

    Text("Enter the address as it should appear on your delivery.", color = Muted)
    EntryField("Flat, house and building", line1, { line1 = it.take(120) })
    EntryField("Street or road (optional)", line2, { line2 = it.take(120) })
    EntryField("Area", area, { area = it.take(80) })
    EntryField("City", city, { city = it.take(60) })
    EntryField(
        "PIN code",
        pincode,
        { pincode = it.filter { char -> char in '0'..'9' }.take(6) },
        true,
    )
    EntryField("Landmark (optional)", landmark, { landmark = it.take(80) })
    EntryField("Save as", label, { label = it.take(30) })
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            makeDefault,
            { makeDefault = it },
            enabled = !busy && address?.flag("isDefault") != true,
        )
        Text("Set as default address")
    }
    val validation = when {
        line1.isBlank() -> "Enter your flat, house and building."
        area.isBlank() || city.isBlank() -> "Enter your area and city."
        !deliveryPin.matches(pincode) -> "Enter a valid 6-digit PIN code."
        label.isBlank() -> "Give this address a name."
        else -> null
    }
    if (triedSaving) {
        (validation ?: saveError.takeIf(String::isNotBlank))?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
    PrimaryButton(if (choose) "Save and deliver here" else "Save address", enabled = !busy) {
        triedSaving = true
        if (validation == null) {
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
    }
    TextButton(enabled = !busy, onClick = cancel) { Text("Cancel") }
}
