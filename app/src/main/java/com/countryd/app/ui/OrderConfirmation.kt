package com.countryd.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog

@Composable
fun OrderConfirmation(onHome: () -> Unit, onViewOrder: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val scale = remember { Animatable(if (reduced) 1f else .9f) }
    LaunchedEffect(Unit) { if (!reduced) scale.animateTo(1f, tween(220)) }
    Dialog(onDismissRequest = onHome) {
        Box(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp))) {
            IconButton(onHome, Modifier.align(Alignment.TopEnd)) {
                Icon(CountryIcons.Close, "Close order confirmation")
            }
            Column(
                Modifier.fillMaxWidth().padding(24.dp).padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(76.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                        .background(Color(0xFFE9F3EB), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(54.dp).background(Green, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", fontSize = 30.sp, color = Color.White)
                    }
                }
                Text(
                    "Order placed",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                )
                Button(
                    onHome,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Done", fontSize = 15.sp)
                }
                TextButton(onViewOrder) { Text("View order", fontSize = 14.sp) }
            }
        }
    }
}
