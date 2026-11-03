package com.countryd.app.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import java.time.LocalDate
import kotlin.math.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun disabled() =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduced by remember { mutableStateOf(disabled()) }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced = disabled()
                }
            }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

@Composable
fun VipCelebration(onDone: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val pass = remember { Animatable(if (reduced) 1f else 0f) }
    val burst = remember { Animatable(if (reduced) 1f else 0f) }
    val copy = remember { Animatable(if (reduced) 1f else 0f) }
    val spin = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            launch { pass.animateTo(1f, spring(.5f, 180f)) }
            launch {
                delay(120)
                burst.animateTo(1f, tween(2100, easing = LinearOutSlowInEasing))
            }
            launch {
                delay(420)
                copy.animateTo(1f, tween(420, easing = LinearOutSlowInEasing))
            }
            launch { spin.animateTo(40f, tween(3500, easing = LinearEasing)) }
        }
        delay(2900)
        onDone()
    }
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0C6B42), Color(0xFF074A2E), Color(0xFF05331F))
                )
            )
            .clickable(onClick = onDone)
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = max(size.width, size.height) * .63f
            drawCircle(
                Brush.radialGradient(listOf(Color(0x88F7D46B), Color.Transparent), center, radius),
                radius,
                center,
            )
            repeat(16) { i ->
                val angle = (i * 22.5f + spin.value) * PI.toFloat() / 180
                val path =
                    Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(
                            center.x + sin(angle - .07f) * radius,
                            center.y - cos(angle - .07f) * radius,
                        )
                        lineTo(
                            center.x + sin(angle + .07f) * radius,
                            center.y - cos(angle + .07f) * radius,
                        )
                        close()
                    }
                drawPath(path, Color(0xFFF7D46B).copy(alpha = .09f * pass.value))
            }
            val colors =
                listOf(
                    Color(0xFFF7D46B),
                    Color(0xFFEDB73E),
                    Color.White,
                    Color(0xFF9FE0B5),
                    Color(0xFFF9E7A6),
                    Color(0xFF5BC488),
                )
            if (!reduced)
                repeat(46) { i ->
                    val progress = burst.value
                    val angle = i / 46f * 2 * PI + (i * 37 % 10) / 20.0
                    val distance = 150 + (i * 53 % 120)
                    val dx = cos(angle).toFloat() * distance * 1.4f * progress * density
                    val dy = sin(angle).toFloat() * distance - 40
                    val y =
                        if (progress < .45f) -40 + (dy + 10) * progress / .45f
                        else dy - 30 + 250 * (progress - .45f) / .55f
                    val alpha =
                        when {
                            progress < .06f -> progress / .06f
                            progress > .82f -> (1 - progress) / .18f
                            else -> 1f
                        }
                    drawCircle(
                        colors[i % colors.size].copy(alpha = alpha.coerceIn(0f, 1f)),
                        4.dp.toPx(),
                        Offset(center.x + dx, center.y + y * density),
                    )
                }
        }
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.size(236.dp, 148.dp)
                    .graphicsLayer {
                        scaleX = .3f + .7f * pass.value
                        scaleY = scaleX
                        rotationZ = -14 + 10 * pass.value
                        alpha = pass.value.coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFBE08A), Color(0xFFE2A52C))))
                    .border(2.dp, Color(0xFFFCE7A2), RoundedCornerShape(18.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "COUNTRY DELIGHT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = Color(0xFF6B4A06),
                )
                Text(
                    "VIP",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = Color(0xFF5A3D04),
                )
                HorizontalDivider(Modifier.width(150.dp), color = Color(0x80B98318))
                Text(
                    "YOUR PRICE ADVANTAGE",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = Color(0xFF6B4A06),
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Column(
                Modifier.padding(top = 40.dp, start = 32.dp, end = 32.dp).graphicsLayer {
                    alpha = copy.value
                    translationY = (1 - copy.value) * 18 * density
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Free · No paid renewal",
                    Modifier.background(Gold, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B5A37),
                )
                Text(
                    "VIP unlocked!",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    "Better prices on your next 30 deliveries. Savings show up in every bill.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFFCFEBDB),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Text(
            "Tap to continue",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            fontSize = 13.sp,
            color = Color(0xFF9FCDB3),
        )
    }
}

@Composable
fun RewardDetails() {
    val reduced = LocalReducedMotion.current
    val badge = remember { Animatable(if (reduced) 1f else 0f) }
    val rest = remember { Animatable(if (reduced) 1f else 0f) }
    val tiles = remember { List(7) { Animatable(if (reduced) 1f else 0f) } }
    LaunchedEffect(Unit) {
        if (!reduced) {
            launch {
                delay(550)
                badge.animateTo(1f, spring(.4f, 230f))
            }
            tiles.forEachIndexed { i, value ->
                launch {
                    delay(950 + i * 110L)
                    value.animateTo(1f, spring(.5f, 260f))
                }
            }
            launch {
                delay(1700)
                rest.animateTo(1f, tween(400, easing = LinearOutSlowInEasing))
            }
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.widthIn(min = 196.dp)
                .height(70.dp)
                .graphicsLayer {
                    scaleX = .35f + .65f * badge.value
                    scaleY = scaleX
                    alpha = badge.value.coerceIn(0f, 1f)
                }
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFCE08A), Gold, Color(0xFFE2A52C))),
                    RoundedCornerShape(35.dp),
                )
                .border(2.dp, Color(0xFFFCE9A9), RoundedCornerShape(35.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("4+3", fontSize = 46.sp, fontWeight = FontWeight.Black, color = Color(0xFF7A5200))
        }
        Text(
            "4 paid dates + 3 gift dates",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tiles.forEachIndexed { i, animation ->
                val date = LocalDate.now().plusDays(i + 1L)
                val gift = i >= 4
                Column(
                    Modifier.weight(1f)
                        .graphicsLayer {
                            alpha = animation.value.coerceIn(0f, 1f)
                            translationY = (1 - animation.value) * 14 * density
                            scaleX = .6f + .4f * animation.value
                            scaleY = scaleX
                        }
                        .background(
                            if (gift) Color(0xFFFDF1C7) else Mint,
                            RoundedCornerShape(12.dp),
                        )
                        .border(
                            1.5.dp,
                            if (gift) Color(0xFFEFC75A) else Color(0xFFCFE8D7),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        date.dayOfWeek.name.take(3).lowercase().replaceFirstChar(Char::uppercase),
                        fontSize = 10.sp,
                        color = Muted,
                    )
                    Text("${date.dayOfMonth}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (gift) "✧" else "♧",
                        fontSize = 16.sp,
                        color = if (gift) Color(0xFF7A5200) else Green,
                    )
                    Text(
                        if (gift) "FREE" else "PAID",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gift) Color(0xFF7A5200) else Green,
                    )
                }
            }
        }
        Column(
            Modifier.fillMaxWidth()
                .padding(top = 12.dp)
                .graphicsLayer {
                    alpha = rest.value
                    translationY = (1 - rest.value) * 10 * density
                }
                .background(Mint, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf("1 pack free on each gift date", "Up to 2 eligible products").forEach {
                Text("✓  $it", fontSize = 15.sp)
            }
        }
        Caption("Extra packs are charged on gift dates.", Modifier.padding(top = 8.dp))
    }
}
