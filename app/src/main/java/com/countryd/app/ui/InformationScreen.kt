package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun InformationScreen() {
    ScreenBody {
        listOf(
                "Nutrition rings" to
                    "Protein and fiber estimate nutrients in scheduled whole packs. The third ring counts deliveries. Rings are summaries, not health targets or food consumed. Day and week totals exclude paused, skipped and vacation deliveries.",
                "Product nutrition" to
                    "Details use per 100g for solids, per 100ml for liquids and per egg where edible weight is unavailable. Only protein and fiber are supplied in this demo. Energy, fat, carbohydrates, sugar, sodium and micronutrients need verified label data. Missing values are not zero.",
                "Pause, skip & vacation" to
                    "Pause plan stops the entire selected subscription, including every pack, until you resume. Skip delivery excludes its next scheduled delivery. Vacation dates exclude deliveries until the final away day. One-time orders are managed separately.",
                "Your routine" to
                    "Gym, diet, everyday groceries and milk essentials personalise suggestions. They do not prescribe a diet or create an order.",
                "This preview" to
                    "This app uses demo login and sample products. Your preferences, basket, orders and plan changes are saved on this device. Live authentication, billing, delivery and batch quality reports are not connected. Clearing app data removes saved demo activity.",
            )
            .forEach { (title, text) ->
                Heading(title)
                Text(text)
            }
    }
}
