package com.countryd.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.data.Order

@Composable
fun DeliveryProgressCard(order: Order) {
    val step =
        when (order.status) {
            "Delivered" -> 3
            "Out for Delivery" -> 2
            "Quality Tested" -> 1
            else -> 0
        }
    val titles = listOf("Confirmed", "Ready to go", "On the way", "Delivered")
    val locations =
        listOf(
            "Your order is with the store",
            "Packed at the store",
            "With your delivery partner",
            "Your order has reached your doorstep",
        )
    val next =
        listOf(
            "Next: packing your items",
            "Next: handover to your delivery partner",
            "Next: doorstep delivery",
            "Your delivery is complete",
        )
    CountryCard {
        BillRow("YOUR DELIVERY", "#${order.id}")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titles[step], fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Caption(locations[step])
            }
            AssetImage(
                "assets/generated/delivery-v1/van.png",
                "Delivery van",
                Modifier.size(92.dp, 80.dp),
            )
        }
        Text(order.slot, fontSize = 12.sp, color = Green)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Confirmed", "Packed", "On the way", "Delivered").forEachIndexed { index, label
                ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(32.dp)
                            .background(if (index <= step) Mint else Canvas, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index < step) Text("✓", color = Green)
                        else
                            Icon(
                                if (index == 3) CountryIcons.Home else CountryIcons.Package,
                                null,
                                Modifier.size(15.dp),
                                tint = if (index == step) Green else Muted,
                            )
                    }
                    Text(
                        label,
                        fontSize = 9.sp,
                        color = if (index == step) Green else Muted,
                        fontWeight = if (index == step) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Text(next[step], fontSize = 12.sp, color = Green)
        Caption("Demo status · live location is not connected")
    }
}
