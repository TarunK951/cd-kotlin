package com.countryd.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun ProductScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    id: String,
    initialMode: String = "Once",
) {
    val product = state.products.find { it.id == id } ?: return
    var variant by remember { mutableStateOf(product.defaultVariant) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var mode by rememberSaveable {
        mutableStateOf(if (initialMode == "Subscribe") "Subscribe" else "Buy once")
    }
    var addons by remember { mutableStateOf(setOf<String>()) }
    var information by rememberSaveable { mutableStateOf(false) }
    var nutrition by rememberSaveable { mutableStateOf(false) }
    val unit = variant?.unit ?: product.unit
    val price = variant?.price ?: product.price
    val nutrients = packNutrients(product.id, unit, quantity)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFE6F1E6), Color(0xFFFFE7CB)))
                    )
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssetImage(
                    productImage(product),
                    product.name,
                    Modifier.weight(.42f)
                        .fillMaxHeight()
                        .background(Color.White, RoundedCornerShape(18.dp)),
                )
                Column(
                    Modifier.weight(.58f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("YOUR SELECTED PACK", fontSize = 10.sp, color = Muted)
                    Text(unit, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        money(price),
                        fontSize = 25.sp,
                        color = Green,
                        fontWeight = FontWeight.Bold,
                    )
                    Caption("per pack")
                }
            }
            if (product.variants.isNotEmpty()) {
                Text("Choose your pack", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.variants.forEach { pack ->
                        Column(
                            Modifier.weight(1f)
                                .border(
                                    1.dp,
                                    if (variant?.id == pack.id) Green else Line,
                                    RoundedCornerShape(14.dp),
                                )
                                .background(
                                    if (variant?.id == pack.id) Color(0xFFE7F3EB) else Color.White,
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable { variant = pack }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(pack.unit, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(money(pack.price), fontSize = 13.sp)
                        }
                    }
                }
            }
            Text("How do you want it?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth()
                    .background(Color(0xFFEDF7F1), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                listOf("Subscribe", "Buy once").forEach { choice ->
                    Box(
                        Modifier.weight(1f)
                            .height(40.dp)
                            .background(
                                if (mode == choice) Green else Color.Transparent,
                                RoundedCornerShape(24.dp),
                            )
                            .clickable { mode = choice },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            choice,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mode == choice) Color.White else Muted,
                        )
                    }
                }
            }
            if (mode == "Subscribe")
                CountryCard(color = Mint) {
                    Caption("Choose your delivery days and compare milk offers.")
                    PrimaryButton("Set up my plan") { go("milk-plan/${product.id}?offer=none") }
                }
            HorizontalDivider(color = Line)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable {
                    information = !information
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Product information",
                    Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (information) CountryIcons.ChevronUp else CountryIcons.ChevronDown,
                    null,
                    tint = Green,
                )
            }
            androidx.compose.animation.AnimatedVisibility(information) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(product.description, fontSize = 13.sp)
                    Caption(
                        if (product.rating > 0)
                            "★ ${product.rating} · ${product.reviewsCount} reviews"
                        else "Sample catalogue"
                    )
                    product.addons.forEach { addon ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                addon.id in addons,
                                { addons = if (it) addons + addon.id else addons - addon.id },
                            )
                            Text(addon.name, Modifier.weight(1f), fontSize = 12.sp)
                            Text("+${money(addon.price)}", fontSize = 12.sp)
                        }
                    }
                }
            }
            HorizontalDivider(color = Line)
            ProductQualityCard(product.name, unit)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { nutrition = !nutrition },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Nutrition", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Caption(
                        if (nutrients == null) "Values unavailable · per pack"
                        else
                            "${nutrients.protein.toInt()} g protein · ${nutrients.fiber.toInt()} g fibre · per pack"
                    )
                }
                Text(
                    if (nutrition) "Less" else "Details",
                    fontSize = 12.sp,
                    color = Green,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            androidx.compose.animation.AnimatedVisibility(nutrition) {
                Caption(
                    "Estimated whole-pack values. Check the pack label for exact nutrition. Missing values are not zero."
                )
            }
            if (information) {
                Text("Pairs well with", fontWeight = FontWeight.SemiBold)
                ProductShelf(
                    state.products
                        .filter { it.category == product.category && it.id != product.id }
                        .take(4),
                    state.session,
                    vm,
                    go,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().border(1.dp, Line).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Caption(unit)
                Text(money(price), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (quantity > 1) Caption("$quantity packs")
            }
            Quantity(quantity, { quantity = it })
            Button(
                {
                    if (mode == "Subscribe") go("milk-plan/${product.id}?offer=none")
                    else {
                        vm.add(
                            CartLine(
                                product.id,
                                quantity,
                                variant = variant,
                                addons = product.addons.filter { it.id in addons },
                            )
                        )
                        go("cart")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.width(116.dp).heightIn(min = 44.dp),
            ) {
                Text(
                    if (mode == "Subscribe") "Set up plan" else "Add",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
