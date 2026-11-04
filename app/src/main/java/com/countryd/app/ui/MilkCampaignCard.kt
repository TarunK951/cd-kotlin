package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun MilkCampaignCard(onClick: () -> Unit) {
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        val narrow = maxWidth < 340.dp
        AssetImage(
            "assets/generated/premium-offers-2026-10-22/milk-hamper-scene-v1.jpg",
            null,
            Modifier.fillMaxSize(),
            ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xD9062F25),
                        .42f to Color(0xBF062F25),
                        .72f to Color.Transparent,
                    )
                )
        )
        Column(Modifier.fillMaxWidth(.54f).padding(start = 18.dp, top = 18.dp)) {
            Text(
                "WELCOME OFFER",
                fontSize = 8.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = .86f),
            )
            AssetImage(
                "assets/generated/premium-offers-2026-10-22/offer-4-plus-3-gold.png",
                null,
                Modifier.padding(top = 13.dp)
                    .offset(x = (-8).dp)
                    .size(if (narrow) 146.dp else 164.dp, 90.dp),
            )
            Text(
                "3 days on us.",
                fontSize = if (narrow) 17.sp else 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "With 4 paid days of milk.",
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = Color(0xFFE5EFE8),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Text(
            "Explore offer ↗",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF25472F),
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 10.dp)
                    .background(Color(0xFFFFE7A3), RoundedCornerShape(7.dp))
                    .border(1.dp, Color(0xFFFFF1C6), RoundedCornerShape(7.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
