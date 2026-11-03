package com.countryd.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun DeliveryOverview(state: CountryState, go: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    val selectedDate = LocalDate.parse(selected)
    val subs = state.session.subscriptions.filter { scheduledOn(it, selectedDate) }
    val orders =
        state.session.orders.filter { it.deliveryDate == selected && it.status != "Cancelled" }
    CountryCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { expanded = !expanded }) {
                Text("Next delivery", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Caption(
                    if (subs.isEmpty() && orders.isEmpty()) "Nothing scheduled yet"
                    else "${subs.size+orders.size} deliveries scheduled"
                )
            }
            IconButton({ expanded = !expanded }) {
                Icon(
                    if (expanded) CountryIcons.ChevronUp else CountryIcons.ChevronDown,
                    "Expand delivery dates",
                    Modifier.size(18.dp),
                    tint = Muted,
                )
            }
            IconButton({ go("calendar") }) {
                Icon(
                    CountryIcons.Calendar,
                    "Open delivery calendar",
                    Modifier.size(21.dp),
                    tint = Green,
                )
            }
        }
        androidx.compose.animation.AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(
                        selectedDate.format(
                            java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")
                        ),
                        Modifier.weight(1f),
                        fontSize = 10.sp,
                        color = Muted,
                    )
                    Text("Swipe for more dates", fontSize = 10.sp, color = Muted)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    items((1L..28L).map { LocalDate.now().plusDays(it) }) { date ->
                        val count =
                            state.session.subscriptions.count { scheduledOn(it, date) } +
                                state.session.orders.count {
                                    it.deliveryDate == date.toString() && it.status != "Cancelled"
                                }
                        Column(
                            Modifier.width(40.dp)
                                .background(
                                    if (selected == date.toString() && count > 0) Mint
                                    else Color(0xFFF3F3F3),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { selected = date.toString() }
                                .padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                date.dayOfWeek.name
                                    .take(3)
                                    .lowercase()
                                    .replaceFirstChar(Char::uppercase),
                                fontSize = 10.sp,
                                color = Muted,
                            )
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                date.month.name
                                    .take(3)
                                    .lowercase()
                                    .replaceFirstChar(Char::uppercase),
                                fontSize = 10.sp,
                                color = Muted,
                            )
                            Text(
                                if (count == 0) "–" else count.toString(),
                                fontSize = 11.sp,
                                color = Muted,
                            )
                        }
                    }
                }
                subs.forEach { sub ->
                    Caption(
                        "${state.products.first{it.id==sub.productId}.name} · ${sub.qty} × ${sub.unit}"
                    )
                }
                orders.forEach { order ->
                    Caption("${order.items.sumOf{it.line.qty}} items · ${order.slot}")
                }
            }
        }
    }
}

@Composable
fun CampaignCarousel(session: Session, go: (String) -> Unit) {
    val enrolled = session.subscriptions.any { it.plan.offer == "welcome" && it.status != "Past" }
    val slides = if (enrolled) listOf(1, 2, 3) else listOf(0, 1, 2, 3)
    val pager = rememberPagerState(pageCount = { slides.size })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    var direction by remember { mutableIntStateOf(1) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(dragging, slides.size, reduced) {
        if (!dragging && !reduced) {
            delay(7000)
            while (true) {
                if (pager.currentPage == slides.lastIndex) direction = -1
                if (pager.currentPage == 0) direction = 1
                pager.animateScrollToPage(pager.currentPage + direction)
                delay(4500)
            }
        }
    }
    HorizontalPager(
        pager,
        pageSpacing = 12.dp,
        modifier = Modifier.fillMaxWidth().height(224.dp),
    ) { page ->
        val index = slides[page]
        val title =
            listOf(
                "3 days on us.",
                "Fresh picks.\nHappy bites.",
                "Make mornings\ndelicious.",
                "Everyday picks.\nBetter prices.",
            )[index]
        Box(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        when (index) {
                            1 -> listOf(Color(0xFFA6342F), Color(0xFFFFAC83))
                            2 -> listOf(Color(0xFF83513A), Color(0xFFE3B48B))
                            3 -> listOf(Color(0xFF64345F), Color(0xFFCE88BE))
                            else -> listOf(Color(0xFF104D3E), Color(0xFF78B59D))
                        }
                    )
                )
                .clickable {
                    go(
                        when (index) {
                            0 -> "milk-offer"
                            1 -> "shop?category=fruits"
                            2 -> "shop?category=breakfast"
                            else -> "offers"
                        }
                    )
                }
        ) {
            if (index == 0) {
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
                                listOf(Color(0xD9062F25), Color(0xBF062F25), Color.Transparent)
                            )
                        )
                )
            } else
                AssetImage(
                    when (index) {
                        1 -> "assets/generated/commerce-minimal-v1/fruit.png"
                        2 -> "assets/generated/commerce-2026-v5/pantry-cutout.png"
                        else -> "assets/generated/commerce-2026-v5/produce-cutout.png"
                    },
                    null,
                    Modifier.align(Alignment.BottomEnd).width(175.dp).height(190.dp),
                )
            Column(
                Modifier.padding(16.dp).width(190.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    listOf(
                        "WELCOME OFFER",
                        "FRESH FINDS",
                        "MORNING FAVOURITES",
                        "EVERYDAY SAVINGS",
                    )[index],
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                if (index == 0)
                    AssetImage(
                        "assets/generated/premium-offers-2026-10-22/offer-4-plus-3-gold.png",
                        null,
                        Modifier.size(146.dp, 90.dp),
                    )
                Text(
                    title,
                    fontSize = if (index == 0) 19.sp else 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    listOf(
                        "With 4 paid days of milk.",
                        "Selected fruit picks.",
                        "Oats, honey & more.",
                        "Savings across your basket.",
                    )[index],
                    fontSize = 12.sp,
                    color = Color.White,
                )
                Text(
                    listOf(
                        "Explore offer ›",
                        "Shop fruits ›",
                        "Shop breakfast ›",
                        "Explore offers ›",
                    )[index],
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Row(
                Modifier.align(Alignment.BottomStart).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                slides.forEachIndexed { i, _ ->
                    Box(
                        Modifier.size(6.dp)
                            .background(
                                Color.White.copy(alpha = if (page == i) 1f else .4f),
                                CircleShape,
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun MilkRow(state: CountryState, go: (String) -> Unit) {
    CountryCard(color = Mint) {
        SectionHeading("Milk, your way") { go("milk-offer") }
        Caption("Fresh milk on the days you choose.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.products
                .filter { it.id in listOf("p1", "p3", "p4") }
                .forEach { p ->
                    Column(
                        Modifier.weight(1f).clickable { go("milk-plan/${p.id}?offer=welcome") },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AssetImage(productImage(p), p.name, Modifier.height(78.dp).fillMaxWidth())
                        Text(p.name, fontSize = 11.sp)
                        Caption(money(p.price))
                    }
                }
        }
    }
}

@Composable
fun SeasonalPanel(state: CountryState, go: (String) -> Unit) {
    CountryCard(color = Color(0xFF193F86)) {
        Text("THE FEEL-GOOD EDIT", fontSize = 10.sp, color = Color.White)
        Text("Fresh finds.", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Big colour. Little everyday joys.", color = Color.White, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("fruits" to "Fruits", "vegetables" to "Vegetables", "breakfast" to "Breakfast")
                .forEach { (id, title) ->
                    Column(
                        Modifier.weight(1f)
                            .background(Color.White, RoundedCornerShape(14.dp))
                            .clickable { go("shop?category=$id") }
                            .padding(8.dp)
                    ) {
                        AssetImage(
                            "assets/categories/$id.png",
                            title,
                            Modifier.fillMaxWidth().height(86.dp),
                        )
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
        }
    }
}
