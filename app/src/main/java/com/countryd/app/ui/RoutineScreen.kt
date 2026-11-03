package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun RoutineScreen(state: CountryState, vm: CountryViewModel, onDone: () -> Unit = {}) {
    var goal by remember { mutableStateOf(state.session.goal) }
    var target by rememberSaveable { mutableStateOf(goal.protein.toString()) }
    var error by remember { mutableStateOf("") }
    ScreenBody {
        Heading("Personalise your routine")
        Caption(
            "Optional preferences help us suggest better combinations. You can change these anytime."
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Compare with my protein goal", Modifier.weight(1f))
            Switch(goal.enabled, { goal = goal.copy(enabled = it) })
        }
        EntryField(
            "Daily protein goal (grams)",
            target,
            {
                target = it.filter(Char::isDigit).take(3)
                error = ""
            },
            true,
        )
        Heading("Food preference")
        Options(listOf("Everything", "Vegetarian", "Plant-based"), goal.preference) {
            goal = goal.copy(preference = it)
        }
        Heading("What matters to you?")
        listOf(
                Triple(
                    "More protein",
                    "Daily protein",
                    "Discover protein-rich breakfast ingredients",
                ),
                Triple(
                    "Balanced meals",
                    "Everyday balance",
                    "Mix familiar staples with fresh produce",
                ),
                Triple(
                    "More plants",
                    "More vegetables & fiber",
                    "Add variety with plant ingredients",
                ),
            )
            .forEach { (id, title, copy) ->
                CountryCard(color = if (goal.focus == id) Mint else Color.White) {
                    Row(
                        Modifier.fillMaxWidth().clickable { goal = goal.copy(focus = id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(goal.focus == id, { goal = goal.copy(focus = id) })
                        Column {
                            Text(title, fontWeight = FontWeight.SemiBold)
                            Caption(copy)
                        }
                    }
                }
            }
        Heading("Household size")
        Caption("Starting pack suggestions for your household. Your protein goal stays personal.")
        Quantity(goal.household, { goal = goal.copy(household = it) }, maximum = 8)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryButton("Save my preferences") {
            val number = target.toIntOrNull()
            if (goal.enabled && (number == null || number !in 1..300))
                error = "Enter a whole number from 1 to 300."
            else {
                vm.update {
                    it.copy(
                        goal =
                            goal.copy(
                                protein = if (goal.enabled) number!! else goal.protein,
                                configured = true,
                            )
                    )
                }
                onDone()
            }
        }
        OutlinedButton(
            {
                vm.update { it.copy(goal = it.goal.copy(enabled = false)) }
                onDone()
            },
            Modifier.fillMaxWidth(),
        ) {
            Text("Skip nutrition tracking")
        }
        Caption(
            "Saved for this demo session. Choose your own goal; the app does not prescribe a target."
        )
    }
}
