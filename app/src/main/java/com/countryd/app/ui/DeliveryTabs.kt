package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun DeliveryTabs(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color(0xFFF4F8F4), RoundedCornerShape(28.dp))
            .border(1.dp, Line, RoundedCornerShape(28.dp))
            .padding(4.dp)
    ) {
        listOf("My subscriptions", "Orders").forEach { label ->
            Box(
                Modifier.weight(1f)
                    .heightIn(min = 44.dp)
                    .background(
                        if (label == selected) Color(0xFF055F2C) else Color.Transparent,
                        RoundedCornerShape(24.dp),
                    )
                    .selectable(label == selected, role = Role.Tab, onClick = { onSelect(label) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (label == selected) Color.White else Muted,
                )
            }
        }
    }
}
