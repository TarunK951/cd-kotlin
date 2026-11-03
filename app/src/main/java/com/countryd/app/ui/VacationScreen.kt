package com.countryd.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import com.countryd.app.*
import java.time.*

@Composable
fun VacationScreen(state: CountryState, vm: CountryViewModel, onDone: () -> Unit) {
    val eligible = state.session.subscriptions.filter { it.status == "Active" }
    var selected by remember { mutableStateOf(eligible.map { it.id }.toSet()) }
    var start by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var end by rememberSaveable { mutableStateOf(start) }
    ScreenBody {
        Heading("Pause while you’re away")
        Caption(
            "Choose the subscriptions to pause. Your usual schedule resumes after the last day away."
        )
        if (eligible.isEmpty())
            EmptyState(
                "No active subscriptions",
                "Start or resume a subscription before setting vacation dates.",
            )
        eligible.forEach { sub ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    sub.id in selected,
                    { selected = if (it) selected + sub.id else selected - sub.id },
                )
                Text("${state.products.first{it.id==sub.productId}.name} · ${sub.qty} packs")
            }
        }
        FutureDateField("Away from", start, LocalDate.now().plusDays(1)) {
            start = it
            if (end < start) end = start
        }
        FutureDateField("Last day away", end, LocalDate.parse(start)) { end = it }
        Caption("Usual schedule resumes from ${LocalDate.parse(end).plusDays(1)}.")
        PrimaryButton("Confirm vacation pause", enabled = selected.isNotEmpty()) {
            vm.update { session ->
                session.copy(
                    subscriptions =
                        session.subscriptions.map { sub ->
                            if (sub.id in selected && sub.status == "Active")
                                sub.copy(vacationStart = start, vacationEnd = end)
                            else sub
                        }
                )
            }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FutureDateField(
    label: String,
    value: String,
    minimum: LocalDate = LocalDate.now().plusDays(1),
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton({ open = true }, Modifier.fillMaxWidth()) { Text("$label $value") }
    if (open) {
        val picker =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val date =
                                Instant.ofEpochMilli(utcTimeMillis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            return date >= minimum && date <= LocalDate.now().plusDays(180)
                        }
                    },
            )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({
                    picker.selectedDateMillis?.let {
                        onSelect(
                            Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        )
                    }
                    open = false
                }) {
                    Text("Select date")
                }
            },
            dismissButton = { TextButton({ open = false }) { Text("Cancel") } },
        ) {
            DatePicker(picker)
        }
    }
}
