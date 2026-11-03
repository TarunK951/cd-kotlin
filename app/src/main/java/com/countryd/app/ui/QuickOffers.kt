package com.countryd.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import kotlinx.coroutines.delay

@Composable
fun QuickOffers(session: Session, catalogue: List<Product>, onClick: () -> Unit) {
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
    val title =
        when {
            free > 0 -> "$free free ${if(free==1)"pack" else "packs"} unlocked"
            next != null -> "Unlock ${next.free} free ${if(next.free==1)"pack" else "packs"}"
            else -> "Offers for your basket"
        }
    val subtitle =
        if (free > 0) "Added to your basket"
        else
            next?.let { offer ->
                "Add ${offer.required-offerQuantity(offer,session.cart,catalogue)} ${catalogue.first{it.id==offer.productId}.name.lowercase()} packs"
            } ?: "View your available offers"
    Surface(
        onClick,
        shape = RoundedCornerShape(26.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    ) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(CountryIcons.Tag, null, tint = Green, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 11.sp, color = Muted)
            }
            Text("⌃", color = Green)
        }
    }
}
