package com.countryd.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import kotlinx.coroutines.delay

@Composable
fun QuickOffers(
    session: Session,
    catalogue: List<Product>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    val active = bundleOffers.filter { offerAvailable(it, session, now) }
    val unlocked = active.filter { offerQuantity(it, session.cart, catalogue) >= it.required }
    val next =
        active
            .filterNot { it in unlocked }
            .minByOrNull { it.required - offerQuantity(it, session.cart, catalogue) }
    val free = unlocked.sumOf { it.free }
    val subtotal =
        session.cart.sumOf { line ->
            catalogue
                .find { it.id == line.productId }
                ?.let {
                    ((line.variant?.price ?: it.price) +
                        line.addons.sumOf { addon -> addon.price }) * line.qty
                } ?: 0.0
        }
    val ratio =
        if (free > 0) 1f
        else
            next?.let {
                (offerQuantity(it, session.cart, catalogue).toFloat() / it.required).coerceIn(
                    0f,
                    1f,
                )
            } ?: (subtotal / 200).toFloat().coerceIn(0f, 1f)
    val title =
        when {
            free > 0 -> "$free free ${if(free==1)"pack" else "packs"} unlocked"
            next != null -> "Unlock ${next.free} free ${if(next.free==1)"pack" else "packs"}"
            else -> if (subtotal >= 200) "Free delivery unlocked" else "Unlock free delivery"
        }
    val subtitle =
        if (free > 0) "Added to your basket"
        else
            next?.let { offer ->
                "Add ${offer.required-offerQuantity(offer,session.cart,catalogue)} ${catalogue.first{it.id==offer.productId}.name.lowercase()} packs"
            } ?: if (subtotal >= 200) "You’re all set" else "Shop for ${money(200-subtotal)} more"
    Surface(
        onClick,
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = "Quick view offers" },
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxSize(),
                    color = Green,
                    trackColor = Line,
                    strokeWidth = 3.dp,
                )
                Icon(CountryIcons.Tag, null, tint = Green, modifier = Modifier.size(15.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Icon(CountryIcons.ChevronUp, null, Modifier.size(16.dp), tint = Green)
        }
    }
}
