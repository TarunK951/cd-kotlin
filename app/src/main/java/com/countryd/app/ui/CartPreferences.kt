package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

@Composable
fun CartPreferenceTabs(selected: String, onSelect: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 300.dp
        val stackIcons = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
        Row(
            Modifier.fillMaxWidth()
                .background(Color(0xFFF2F5F2), RoundedCornerShape(13.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            listOf(
                    "Delivery" to CountryIcons.Calendar,
                    "Tip" to CountryIcons.Heart,
                    "Instructions" to CountryIcons.CircleHelp,
                )
                .forEach { (label, icon) ->
                    val active = selected == label
                    val shape = RoundedCornerShape(10.dp)
                    val color = if (active) Green else Muted
                    val widthWeight =
                        if (!compact) 1f
                        else
                            when (label) {
                                "Instructions" -> 1.35f
                                "Tip" -> .7f
                                else -> 1f
                            }
                    Box(
                        Modifier.weight(widthWeight)
                            .heightIn(min = 44.dp)
                            .background(if (active) Color.White else Color.Transparent, shape)
                            .border(
                                1.dp,
                                if (active) Color(0xFFC9DECF) else Color.Transparent,
                                shape,
                            )
                            .selectable(active, role = Role.Tab) { onSelect(label) }
                            .padding(horizontal = 2.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val labelContent: @Composable () -> Unit = {
                            Icon(icon, null, Modifier.size(14.dp), tint = color)
                            Text(
                                label,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = color,
                                textAlign = TextAlign.Center,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                        if (stackIcons)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                labelContent()
                            }
                        else
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                labelContent()
                            }
                    }
                }
        }
    }
}

@Composable
fun CartInstructionCards(selected: List<String>, onToggle: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Delivery Instructions",
            Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("For this order", fontSize = 10.sp, color = Muted)
    }
    listOf(
            "Avoid ringing bell" to CountryIcons.BellOff,
            "Leave at the door" to CountryIcons.DoorClosed,
            "Meet at building entrance" to CountryIcons.Mic,
            "Avoid calling" to CountryIcons.VolumeX,
            "Leave with security" to CountryIcons.ShieldCheck,
            "Hand it to me" to CountryIcons.Person,
        )
        .chunked(2)
        .forEach { pair ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { (label, icon) ->
                    val active = label in selected
                    val shape = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.weight(1f)
                            .fillMaxHeight()
                            .heightIn(min = 92.dp)
                            .clip(shape)
                            .background(if (active) Color(0xFFEDF6F0) else Color(0xFFF8FAF8), shape)
                            .border(1.dp, if (active) Color(0xFF92BDA6) else Line, shape)
                            .toggleable(active, role = Role.Checkbox) { onToggle(label) }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(28.dp)
                                    .background(
                                        if (active) Color.White else Color(0xFFEBF0EC),
                                        RoundedCornerShape(9.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    icon,
                                    null,
                                    Modifier.size(20.dp),
                                    tint = if (active) Green else Ink,
                                )
                            }
                            if (active)
                                Icon(
                                    CountryIcons.CircleCheck,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = Green,
                                )
                        }
                        Text(
                            label,
                            Modifier.padding(top = 6.dp),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            color = if (active) Green else Ink,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
}

@Composable
fun CartTipOptions(tip: Double, onSelect: (Double) -> Unit) {
    var custom by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier.fillMaxWidth()
            .background(Color(0xFFF3F6F1), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Add an optional tip", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Caption("Optional demo tip. No money is transferred.")
        }
        AssetImage("assets/generated/assistant-2026/delight-guide.png", null, Modifier.size(58.dp))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        listOf(10, 20, 40).forEach { value ->
            val active = tip == value.toDouble()
            Surface(
                onClick = { onSelect(if (active) 0.0 else value.toDouble()) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (active) Mint else Canvas,
                border = BorderStroke(1.dp, if (active) Green else Line),
            ) {
                Column(
                    Modifier.heightIn(min = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(CountryIcons.Heart, null, Modifier.size(16.dp), tint = Green)
                    Text("₹$value", fontSize = 12.sp)
                }
            }
        }
        Surface(
            onClick = { custom = !custom },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = if (custom) Mint else Canvas,
            border = BorderStroke(1.dp, Line),
        ) {
            Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
                Text("Custom", fontSize = 11.sp)
            }
        }
    }
    if (custom)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                amount,
                { amount = it.filter(Char::isDigit).take(4) },
                Modifier.weight(1f),
                label = { Text("Custom tip") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            TextButton({
                onSelect(amount.toDoubleOrNull() ?: 0.0)
                custom = false
            }) {
                Text("Apply")
            }
        }
}
