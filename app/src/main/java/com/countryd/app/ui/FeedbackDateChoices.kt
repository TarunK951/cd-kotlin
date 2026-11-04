package com.countryd.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun feedbackDate(value: String): String =
    runCatching {
            LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
        }
        .getOrDefault(value)

@Composable
internal fun FeedbackDateChoices(
    suggested: String,
    suggestedLabel: String,
    minimum: String,
    maximum: String,
    date: String,
    enabled: Boolean,
    unavailable: Set<String> = emptySet(),
    onDate: (String) -> Unit,
) {
    val min = remember(minimum) { runCatching { LocalDate.parse(minimum) }.getOrNull() }
    val max = remember(maximum) { runCatching { LocalDate.parse(maximum) }.getOrNull() }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var monthKey by rememberSaveable { mutableStateOf(date.take(7)) }
    val month = runCatching { YearMonth.parse(monthKey) }.getOrNull() ?: min?.let(YearMonth::from)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                onDate(suggested)
                expanded = false
            },
            enabled = enabled && suggested.isNotBlank(),
            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (date == suggested) Green else Line),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = if (date == suggested) Mint else Canvas
                ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(suggestedLabel, style = MaterialTheme.typography.titleSmall)
                Text(feedbackDate(suggested), style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(
            onClick = {
                expanded = !expanded
                monthKey = date.take(7)
            },
            enabled = enabled && min != null && max != null && min <= max,
            modifier = Modifier.weight(1f).heightIn(min = 54.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (expanded || date != suggested) Green else Line),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    containerColor = if (date != suggested) Mint else Canvas
                ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (expanded) "Hide calendar" else "Choose date",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (date != suggested)
                    Text(feedbackDate(date), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (!expanded || month == null || min == null || max == null) return
    CountryCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { monthKey = month.minusMonths(1).toString() },
                enabled = enabled && month > YearMonth.from(min),
            ) {
                Icon(CountryIcons.ChevronLeft, "Previous month")
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
            TextButton(
                onClick = { monthKey = month.plusMonths(1).toString() },
                enabled = enabled && month < YearMonth.from(max),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Next")
            }
        }
        Row {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted)
            }
        }
        val leading = month.atDay(1).dayOfWeek.value % 7
        val weeks = (month.lengthOfMonth() + leading + 6) / 7
        repeat(weeks) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val number = week * 7 + column - leading + 1
                    val day = if (number in 1..month.lengthOfMonth()) month.atDay(number) else null
                    if (day == null) Spacer(Modifier.weight(1f).height(44.dp))
                    else {
                        val selected = date == day.toString()
                        val available = enabled && day in min..max && day.toString() !in unavailable
                        Surface(
                            modifier = Modifier.weight(1f).height(44.dp),
                            color = if (selected) Mint else Canvas,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Box(
                                Modifier.semantics {
                                        contentDescription = feedbackDate(day.toString())
                                    }
                                    .selectable(selected, available, Role.RadioButton) {
                                        onDate(day.toString())
                                        expanded = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(number.toString(), color = if (available) Ink else Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}
