package com.countryd.app.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.countryd.app.R
import com.countryd.app.data.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val Green = Color(0xFF087443)
val Ink = Color(0xFF102018)
val Muted = Color(0xFF5B6A61)
val Canvas = Color(0xFFF9F9FB)
val Mint = Color(0xFFE9F8EF)
val Line = Color(0xFFE1E9E3)
val Gold = Color(0xFFF2BE45)
val Onest =
    FontFamily(
        Font(R.font.onest_400, FontWeight.Normal),
        Font(R.font.onest_500, FontWeight.Medium),
        Font(R.font.onest_600, FontWeight.SemiBold),
        Font(R.font.onest_700, FontWeight.Bold),
        Font(R.font.onest_800, FontWeight.ExtraBold),
        Font(R.font.onest_900, FontWeight.Black),
    )

@Composable
fun CountryTheme(content: @Composable () -> Unit) {
    val base = Typography()
    val reduced = rememberReducedMotion()
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Green,
                onPrimary = Color.White,
                primaryContainer = Mint,
                onPrimaryContainer = Green,
                background = Canvas,
                onBackground = Ink,
                surface = Color.White,
                onSurface = Ink,
                secondary = Green,
                secondaryContainer = Mint,
                onSecondaryContainer = Green,
                outline = Line,
                outlineVariant = Line,
                onSurfaceVariant = Muted,
                surfaceTint = Color.Transparent,
                surfaceContainerLow = Color.White,
                surfaceContainer = Color.White,
                surfaceContainerHigh = Color.White,
                surfaceContainerHighest = Mint,
                surfaceVariant = Mint,
            ),
        typography =
            base.copy(
                displaySmall =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                        letterSpacing = (-1).sp,
                    ),
                headlineLarge =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                    ),
                headlineSmall =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    ),
                titleLarge =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    ),
                titleMedium =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                titleSmall =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                bodyLarge = TextStyle(fontFamily = Onest, fontSize = 16.sp, lineHeight = 23.sp),
                bodyMedium = TextStyle(fontFamily = Onest, fontSize = 14.sp, lineHeight = 21.sp),
                bodySmall = TextStyle(fontFamily = Onest, fontSize = 12.sp, lineHeight = 18.sp),
                labelLarge =
                    TextStyle(
                        fontFamily = Onest,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                labelMedium =
                    TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                labelSmall =
                    TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 10.sp),
            ),
        content = {
            CompositionLocalProvider(LocalReducedMotion provides reduced) {
                Surface(Modifier.fillMaxSize(), color = Canvas, content = content)
            }
        },
    )
}

@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gold: Boolean = false,
    arrow: Boolean = false,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            if (pressed && !LocalReducedMotion.current) .98f else 1f,
            tween(if (pressed) 85 else 150, easing = LinearOutSlowInEasing),
            label = "source press scale",
        )
    Button(
        onClick,
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .background(
                Brush.linearGradient(
                    if (!enabled) listOf(Line, Line)
                    else if (gold) listOf(Color(0xFFF8D676), Color(0xFFEDB63A))
                    else listOf(Color(0xFF0A8456), Color(0xFF066B3F))
                ),
                RoundedCornerShape(30.dp),
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        enabled = enabled,
        shape = RoundedCornerShape(30.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = if (gold) Ink else Color.White,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = Muted,
            ),
        interactionSource = interactions,
    ) {
        Text(label, fontSize = 17.sp)
        if (arrow)
            Icon(
                CountryIcons.ChevronLeft,
                null,
                Modifier.size(20.dp).graphicsLayer { rotationZ = 180f },
            )
    }
}

@Composable
fun CountryCard(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(color, RoundedCornerShape(18.dp))
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = Muted)
}

@Composable
fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun SectionHeading(title: String, action: String = "See all ›", onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontSize = 19.sp,
        )
        TextButton(onClick) { Text(action, fontSize = 12.sp) }
    }
}

@Composable
fun EmptyState(title: String, text: String, action: String? = null, onClick: () -> Unit = {}) {
    CountryCard(color = Color(0xFFF3F7F2)) {
        Heading(title)
        Caption(text)
        if (action != null) PrimaryButton(action, onClick = onClick)
    }
}

// Cached by path, decoded off the main thread. Source files remain byte-for-byte identical.
private val imageCache =
    object : android.util.LruCache<String, ImageBitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

@Composable
fun AssetImage(
    path: String,
    description: String?,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val bitmap by
        key(path) {
            produceState<ImageBitmap?>(imageCache.get(path), path) {
                value = imageCache.get(path)
                if (value == null)
                    value =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                    context.assets.open(path).use {
                                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                                    }
                                }
                                .getOrNull()
                                ?.also { imageCache.put(path, it) }
                        }
            }
        }
    bitmap?.let { Image(BitmapPainter(it), description, modifier, contentScale = scale) }
        ?: Spacer(modifier)
}

fun productImage(p: Product): String =
    "assets/" +
        when (p.id) {
            "p1" -> "products/photos/cow-milk.jpg"
            "p2" -> "products/photos/paneer.jpg"
            "p3" -> "ftue/generated-2026-10-07/optimized/buffalo-milk-pack.png"
            "p4" -> "products/country-delight/a2-cow-milk.jpg"
            "p5" -> "products/photos/curd.jpg"
            "p6" -> "products/photos/ghee.jpg"
            "p7" -> "products/photos/eggs.jpg"
            "p8" -> "products/photos/white-eggs.jpg"
            "p9" -> "products/photos/coconut.jpg"
            "p10" -> "products/photos/banana.jpg"
            "p11" -> "products/photos/tomato.jpg"
            "p12" -> "products/photos/apple.jpg"
            "p13" -> "products/photos/bread.jpg"
            "p14" -> "products/photos/honey.jpg"
            "p15",
            "p24" -> "products/photos/oats.jpg"
            "p16" -> "generated/wellness-2026/broccoli.jpg"
            "p17" -> "generated/wellness-2026/spinach.jpg"
            "p18" -> "generated/catalogue-2026-10-22/carrots.jpg"
            "p19" -> "generated/catalogue-2026-10-22/rice.jpg"
            "p20" -> "generated/catalogue-2026-10-22/dal.jpg"
            "p21" -> "generated/catalogue-2026-10-22/oil.jpg"
            "p22" -> "generated/catalogue-2026-10-22/turmeric.jpg"
            "p23" -> "generated/catalogue-2026-10-22/almonds.jpg"
            else -> "products/placeholder.png"
        }

@Composable
fun Options(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { label ->
            FilterChip(
                selected == label,
                { onSelect(label) },
                label = { Text(label) },
                shape = RoundedCornerShape(14.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Mint,
                        labelColor = Ink,
                        selectedLabelColor = Green,
                    ),
            )
        }
    }
}

@Composable
fun Quantity(value: Int, onChange: (Int) -> Unit, minimum: Int = 1, maximum: Int = 20) {
    Row(
        Modifier.heightIn(min = 44.dp).border(1.dp, Green, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            { onChange(value - 1) },
            enabled = value > minimum,
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.widthIn(min = 44.dp),
        ) {
            Text("−")
        }
        Text("$value", fontWeight = FontWeight.SemiBold)
        TextButton(
            { onChange(value + 1) },
            enabled = value < maximum,
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.widthIn(min = 44.dp),
        ) {
            Text("+")
        }
    }
}
