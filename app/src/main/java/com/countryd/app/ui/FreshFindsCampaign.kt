package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.data.Product
import kotlin.math.floor

@Composable
fun FreshFindsCampaign(catalogue: List<Product>, onClick: () -> Unit) {
    val saving =
        catalogue
            .filter { it.subCategory == "Fruits" }
            .maxOfOrNull { floor((it.mrp - it.price) / it.mrp * 100).toInt() } ?: 0
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFD5A6), Color(0xFFFFAC99))))
            .clickable(onClick = onClick)
    ) {
        val narrow = maxWidth < 340.dp
        Box(
            Modifier.align(Alignment.TopEnd)
                .offset(x = 8.dp, y = 25.dp)
                .size(maxWidth * .54f)
                .background(Color(0xFFFFE6B8), CircleShape)
                .border(8.dp, Color.White.copy(alpha = .2f), CircleShape)
        )
        AssetImage(
            "assets/generated/commerce-minimal-v1/fruit.png",
            null,
            Modifier.align(Alignment.TopEnd)
                .offset(y = 44.dp)
                .fillMaxWidth(.47f)
                .height(140.dp)
                .graphicsLayer { rotationZ = -7f },
        )
        Column(Modifier.padding(start = 18.dp, top = 17.dp).fillMaxWidth(.56f)) {
            Text(
                "FRESH FINDS /",
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
                color = Color(0xFF68434F),
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Fresh picks.\nHappy bites.",
                fontSize = if (narrow) 22.sp else 26.sp,
                lineHeight = if (narrow) 25.sp else 28.sp,
                color = Color(0xFF492C47),
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 7.dp),
            )
            Column(
                Modifier.padding(top = 10.dp)
                    .graphicsLayer { rotationZ = -4f }
                    .background(Color(0xFF603451), RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF75415C), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "UP TO",
                    fontSize = 7.sp,
                    color = Color(0xFFFFE6BA),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "$saving% OFF",
                    fontSize = if (narrow) 27.sp else 31.sp,
                    color = Color(0xFFFFF2CB),
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Text(
                "Selected fruit picks.",
                fontSize = 10.5.sp,
                color = Color(0xFF653E49),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Text(
            "Shop fruits ↗",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF53334E),
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 10.dp)
                    .background(Color(0xFFFFFBEF), RoundedCornerShape(7.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
