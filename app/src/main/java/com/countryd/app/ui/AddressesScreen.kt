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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.countryd.app.CountryState
import com.countryd.app.CountryViewModel
import com.countryd.app.data.Address
import java.util.UUID

private val localDeliveryPin = Regex("[1-9][0-9]{5}")

internal fun Address.isCompleteDeliveryAddress() =
    house.trim().length >= 2 && localDeliveryPin.matches(pincode)

@Composable
fun AddressesScreen(state: CountryState, vm: CountryViewModel, onSelected: (() -> Unit)? = null) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(state.session.name) }
    var label by rememberSaveable { mutableStateOf("Home") }
    var customLabel by rememberSaveable { mutableStateOf("") }
    var house by rememberSaveable { mutableStateOf("") }
    var locality by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    fun edit(address: Address?) {
        editingId = address?.id
        name = address?.name ?: state.session.name
        label = address?.label?.takeIf { it in listOf("Home", "Work") }
            ?: if (address == null) "Home" else "Other"
        customLabel = if (label == "Other") address?.label.orEmpty() else ""
        house = address?.house.orEmpty()
        locality = address?.locality.orEmpty()
        pin = address?.pincode.orEmpty()
        error = ""
        showForm = true
    }

    BackHandler(enabled = showForm) { showForm = false }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!showForm) {
                if (onSelected != null)
                    Text("Choose where your next order should be delivered.", color = Muted)
                if (state.session.addresses.isEmpty()) Text("No saved addresses yet.")
                state.session.addresses.sortedByDescending { it.id == state.session.addressId }.forEach { address ->
                    val selected = state.session.addressId == address.id
                    val complete = address.isCompleteDeliveryAddress()
                    CountryCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(address.label, fontWeight = FontWeight.SemiBold)
                                if (selected && complete)
                                    Text("Current delivery address", color = Green, fontSize = 12.sp)
                            }
                            if (complete)
                                IconButton(onClick = { edit(address) }) {
                                    Icon(CountryIcons.Pencil, "Edit ${address.label} address", tint = Green)
                                }
                        }
                        Text(address.display, fontSize = 13.sp, color = Muted)
                        if (!complete) {
                            Text("Add the street address and PIN code to use this address.", color = Muted)
                            PrimaryButton("Complete address") { edit(address) }
                        } else if (onSelected != null || !selected) {
                            PrimaryButton(if (selected) "Use this address" else "Deliver here") {
                                vm.update { it.copy(addressId = address.id) }
                                onSelected?.invoke()
                            }
                        }
                        if (state.session.addresses.size > 1)
                            TextButton(onClick = { pendingDelete = address.id }) { Text("Delete") }
                    }
                }
            } else {
                TextButton(onClick = { showForm = false }) {
                    Icon(CountryIcons.ArrowLeft, null, Modifier.size(16.dp))
                    Text("Saved addresses", Modifier.padding(start = 6.dp))
                }
                Heading(if (editingId == null) "Add address" else "Edit address")
                EntryField("Full name", name, { name = it })
                EntryField("Flat / Street / Building", house, { house = it })
                EntryField("Area and city", locality, { locality = it })
                EntryField("PIN code", pin, { pin = it.filter { ch -> ch in '0'..'9' }.take(6) }, true)
                Text("Save as", fontWeight = FontWeight.SemiBold)
                Options(listOf("Home", "Work", "Other"), label) { label = it }
                if (label == "Other") EntryField("Address name", customLabel, { customLabel = it })
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                PrimaryButton(if (onSelected == null) "Save address" else "Save and deliver here") {
                    error = when {
                        pin == "000000" -> "Delivery is unavailable for this PIN code."
                        name.trim().length < 2 -> "Enter your full name."
                        house.trim().length < 2 -> "Add your flat, street and building."
                        locality.trim().length < 3 -> "Enter your area and city."
                        !localDeliveryPin.matches(pin) -> "Enter a valid 6-digit PIN code."
                        label == "Other" && customLabel.trim().isEmpty() -> "Give this address a name."
                        else -> ""
                    }
                    if (error.isEmpty()) {
                        vm.saveAddress(
                            Address(
                                editingId ?: UUID.randomUUID().toString(),
                                if (label == "Other") customLabel.trim() else label,
                                name.trim(),
                                house.trim(),
                                locality.trim(),
                                pin,
                            )
                        )
                        showForm = false
                        onSelected?.invoke()
                    }
                }
                TextButton(onClick = { showForm = false }) { Text("Cancel") }
            }
        }
        if (!showForm) {
            Surface(color = Canvas) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PrimaryButton("Add address") { edit(null) }
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
                TextButton(onClick = {
                    pendingDelete = null
                    vm.update { session ->
                        val rest = session.addresses.filterNot { it.id == id }
                        if (rest.isEmpty()) session
                        else session.copy(
                            addresses = rest,
                            addressId = if (session.addressId == id) {
                                (rest.firstOrNull { it.isCompleteDeliveryAddress() } ?: rest.first()).id
                            } else session.addressId,
                        )
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
