package com.countryd.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Same 22 particles, palette and 1400 ms lifecycle as the source payment burst. */
@Composable
fun PaymentBurst(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(reduced) { if (!reduced) progress.animateTo(1f, tween(1400)) }
    if (!reduced)
        Canvas(modifier.fillMaxWidth().height(180.dp)) {
            val p = progress.value
            val alpha =
                when {
                    p < .08f -> p / .08f
                    p < .7f -> 1f
                    else -> (1 - p) / .3f
                }.coerceIn(0f, 1f)
            val palette = listOf(Color(0xFFE7B438), Color(0xFF087443), Color(0xFF75C698))
            repeat(22) { i ->
                val angle = i * PI.toFloat() * 2 / 22
                val at =
                    Offset(
                        size.width / 2 + cos(angle) * 130.dp.toPx() * p,
                        56.dp.toPx() + (sin(angle) * 110.dp.toPx() + 40.dp.toPx()) * p,
                    )
                rotate((if (i % 2 == 1) 240 else -240) * p, at) {
                    drawRoundRect(
                        palette[i % 3].copy(alpha = alpha),
                        at,
                        Size(7.dp.toPx(), 11.dp.toPx()),
                        CornerRadius((if (i % 3 == 0) 6 else 2).dp.toPx()),
                    )
                }
            }
        }
}
