package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
    ScreenBody {
        CountryCard(color = Color(0xFFFBE8A6)) {
            Text("COUNTRY DELIGHT VIP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (s.vipActive) "Your VIP is active" else "Your VIP trial has ended",
                style = MaterialTheme.typography.headlineLarge,
            )
            Caption(
                if (s.vipExpiresAt > 0)
                    "Valid until ${Instant.ofEpochMilli(s.vipExpiresAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}"
                else "Your membership preview"
            )
            Text("${money(s.vipSaved)} saved", style = MaterialTheme.typography.headlineSmall)
        }
        Heading("Everyday essentials, better prices")
        listOf("VIP price advantage", "Member savings at checkout", "Your savings in one place")
            .forEach { CountryCard { Text(it) } }
        Caption("Membership and savings are simulated. No paid renewal is connected.")
    }
}
