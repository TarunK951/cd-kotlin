package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.Instant

@Composable
fun VipScreen(state: CountryState) {
    val s = state.session
    var details by remember { mutableStateOf(false) }
    val days =
        ((s.vipExpiresAt - System.currentTimeMillis()) / 86400000.0).let {
            kotlin.math.ceil(it).toInt().coerceAtLeast(0)
        }
    ScreenBody {
        Column(
            Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF143E30), Color(0xFF246951))),
                    RoundedCornerShape(20.dp),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                CountryIcons.Crown,
                null,
                tint = Color(0xFFF4D48D),
                modifier = Modifier.size(26.dp),
            )
            Text(
                if (days > 0) "You’re a VIP member" else "Your VIP membership",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                if (days > 0) "$days days left · Complimentary pass"
                else if (s.vipExpiresAt > 0) "Your complimentary pass has ended"
                else "Add an item to unlock your complimentary pass",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = Color(0xFFE1EEE7),
            )
            Text("₹0 membership fee · No auto-renewal", fontSize = 12.sp, color = Color(0xFFF4D48D))
        }
        CountryCard {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Saved with VIP",
                        fontSize = 14.sp,
                        color = Color(0xFF805B12),
                        fontWeight = FontWeight.Medium,
                    )
                    Caption("Completed orders")
                }
                Text(
                    money(s.vipSaved),
                    fontSize = 30.sp,
                    color = Color(0xFF805B12),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        CountryCard {
            Text("Included with your pass", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            listOf(
                    "VIP pricing" to "Your basket shows the VIP discount before you pay.",
                    "A complimentary pass" to "No membership charge. No automatic renewal.",
                    "Clear savings" to "Every discount appears in your bill summary.",
                )
                .forEach { (title, body) ->
                    Row(
                        Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            if (title == "VIP pricing") CountryIcons.FileText
                            else CountryIcons.Crown,
                            null,
                            tint = Color(0xFF816535),
                            modifier = Modifier.size(21.dp),
                        )
                        Column {
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                body,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = Muted,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        }
                    }
                }
        }
        TextButton({ details = !details }) {
            Text(if (details) "Hide pass details −" else "Pass details +")
        }
        if (details)
            Caption(
                "This is a local demo pass. Savings follow the existing demo checkout rules. Delivery fees are separate. " +
                    if (s.vipExpiresAt > 0)
                        "Valid until ${Instant.ofEpochMilli(s.vipExpiresAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}."
                    else "Add an item to unlock your pass."
            )
    }
}
