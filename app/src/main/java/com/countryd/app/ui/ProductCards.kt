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
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import kotlin.math.roundToInt

@Composable
fun ProductShelf(
    products: List<Product>,
    session: Session,
    vm: CountryViewModel,
    go: (String) -> Unit,
) {
    val width = (LocalConfiguration.current.screenWidthDp.coerceAtMost(600) - 58) / 3.25f
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(products, key = { it.id }) { p ->
            ProductTile(p, session, vm, go, Modifier.width(width.dp))
        }
    }
}

@Composable
fun ProductTile(
    p: Product,
    s: Session,
    vm: CountryViewModel,
    go: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val line =
        s.cart.find {
            it.productId == p.id &&
                it.mode == "Once" &&
                (it.variant?.id ?: p.defaultVariant?.id) == p.defaultVariant?.id &&
                it.addons.isEmpty()
        }
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F7F4))
        ) {
            AssetImage(
                productImage(p),
                p.name,
                Modifier.fillMaxSize().clickable { go("product/${p.id}") },
            )
            IconButton({ vm.favorite(p.id) }, Modifier.align(Alignment.TopEnd).size(44.dp)) {
                Icon(
                    if (p.id in s.wishlist) CountryIcons.HeartFilled else CountryIcons.Heart,
                    "Save ${p.name}",
                    tint = if (p.id in s.wishlist) Green else Muted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 8.dp).height(44.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (line == null)
                OutlinedButton(
                    { vm.add(CartLine(p.id, variant = p.defaultVariant)) },
                    contentPadding = PaddingValues(8.dp, 0.dp),
                    modifier = Modifier.width(76.dp).height(36.dp),
                    shape = RoundedCornerShape(9.dp),
                    border = BorderStroke(1.dp, Green),
                ) {
                    Text("Add", fontSize = 11.sp, color = Green, fontWeight = FontWeight.Bold)
                }
            else
                Row(
                    Modifier.height(36.dp).border(1.dp, Green, RoundedCornerShape(9.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        { vm.quantity(line.key, -1) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.width(28.dp),
                    ) {
                        Text("−")
                    }
                    Text("${line.qty}", fontSize = 11.sp)
                    TextButton(
                        { vm.quantity(line.key, 1) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.width(28.dp),
                    ) {
                        Text("+")
                    }
                }
        }
        Column(Modifier.clickable { go("product/${p.id}") }) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(money(p.price), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (p.mrp > p.price)
                    Text(
                        money(p.mrp),
                        fontSize = 10.sp,
                        color = Muted,
                        textDecoration = TextDecoration.LineThrough,
                    )
            }
            Text(
                if (p.mrp > p.price) "${((p.mrp-p.price)/p.mrp*100).roundToInt()}% OFF"
                else if (p.highProtein) "Protein pick" else "",
                fontSize = 10.sp,
                color = Green,
                modifier = Modifier.height(18.dp),
            )
            Text(
                p.name,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                modifier = Modifier.heightIn(min = 34.dp),
            )
            Text(p.unit, fontSize = 11.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
