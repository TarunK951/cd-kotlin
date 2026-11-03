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
import java.util.UUID

@Composable
fun AddressesScreen(state: CountryState, vm: CountryViewModel) {
    var editing by remember { mutableStateOf<Address?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf(state.session.name) }
    var label by rememberSaveable { mutableStateOf("Home") }
    var house by rememberSaveable { mutableStateOf("") }
    var locality by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    ScreenBody {
        state.session.addresses.forEach { a ->
            CountryCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        state.session.addressId == a.id,
                        { vm.update { it.copy(addressId = a.id) } },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(a.label, fontWeight = FontWeight.SemiBold)
                        Text(a.display, fontSize = 13.sp)
                    }
                }
                Row {
                    TextButton({
                        editing = a
                        name = a.name
                        label = a.label
                        house = a.house
                        locality = a.locality
                        pin = a.pincode
                        showForm = true
                    }) {
                        Text("Edit")
                    }
                    if (state.session.addresses.size > 1)
                        TextButton({
                            vm.update {
                                val rest = it.addresses.filterNot { old -> old.id == a.id }
                                it.copy(
                                    addresses = rest,
                                    addressId =
                                        if (it.addressId == a.id) rest.first().id else it.addressId,
                                )
                            }
                        }) {
                            Text("Delete")
                        }
                }
            }
        }
        if (!showForm)
            PrimaryButton("Add address") {
                editing = null
                house = ""
                locality = ""
                pin = ""
                showForm = true
            }
        if (showForm) {
            Heading(if (editing == null) "Add address" else "Edit address")
            Options(listOf("Home", "Work", "Other"), label) { label = it }
            EntryField("Full name", name, { name = it })
            EntryField("Flat / Street / Building", house, { house = it })
            EntryField("Locality", locality, { locality = it })
            EntryField("Postcode", pin, { pin = it.filter(Char::isDigit).take(6) }, true)
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            PrimaryButton("Save address") {
                error =
                    when {
                        pin == "000000" -> "Delivery is unavailable for this postcode."
                        name.trim().length < 2 -> "Enter your full name."
                        house.trim().length < 2 -> "Add your flat, street and building."
                        !Regex("[1-9][0-9]{5}").matches(pin) -> "Enter a valid 6-digit postcode."
                        else -> ""
                    }
                if (error.isEmpty()) {
                    vm.saveAddress(
                        Address(
                            editing?.id ?: UUID.randomUUID().toString(),
                            label,
                            name.trim(),
                            house.trim(),
                            locality.trim(),
                            pin,
                        )
                    )
                    showForm = false
                }
            }
            TextButton({ showForm = false }) { Text("Cancel") }
        }
    }
}
