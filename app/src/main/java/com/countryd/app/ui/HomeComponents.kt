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
fun DeliveryOverview(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    val expanded = state.session.nextDeliveryExpanded
    fun toggleExpanded() = vm.update { it.copy(nextDeliveryExpanded = !it.nextDeliveryExpanded) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    val selectedDate = LocalDate.parse(selected)
    val subs = state.session.subscriptions.filter { scheduledOn(it, selectedDate) }
    val orders =
        state.session.orders.filter { it.deliveryDate == selected && it.status != "Cancelled" }
    val unsatisfiedSubs =
        subs.filter { sub ->
            orders.none { order -> order.items.any { it.subscriptionId == sub.id } }
        }
    val benefits =
        state.session.subscriptions.map {
            deliveryBenefits(it, state.session.orders, LocalDate.now())
        }
    CountryCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(CountryIcons.Bike, null, Modifier.padding(end = 10.dp).size(22.dp), tint = Green)
            Column(Modifier.weight(1f).clickable { toggleExpanded() }) {
                Text("Next delivery", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Caption(
                    if (subs.isEmpty() && orders.isEmpty()) "Nothing scheduled yet"
                    else "${unsatisfiedSubs.size+orders.size} deliveries scheduled"
                )
            }
            IconButton({ toggleExpanded() }) {
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
                    items((1L..14L).map { LocalDate.now().plusDays(it) }) { date ->
                        val count =
                            state.session.subscriptions.count { scheduledOn(it, date) } +
                                state.session.orders.count {
                                    it.deliveryDate == date.toString() && it.status != "Cancelled"
                                }
                        val free = benefits.any { week -> week.any { it.date == date && it.free } }
                        Column(
                            Modifier.width(40.dp)
                                .background(
                                    if (free) Color(0xFFFFF2C9)
                                    else if (count > 0) Mint else Color(0xFFF3F3F3),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { go("calendar?date=$date") }
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
fun CampaignCarousel(session: Session, catalogue: List<Product>, go: (String) -> Unit) {
    val enrolled = session.subscriptions.any { it.plan.offer != "none" && it.status != "Past" }
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
        if (index == 0) {
            MilkCampaignCard { go("milk-offer") }
            return@HorizontalPager
        }
        if (index == 1) {
            FreshFindsCampaign(catalogue) { go("shop?category=fruits") }
            return@HorizontalPager
        }
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
                Modifier.padding(start = 18.dp, top = 18.dp).fillMaxWidth(.57f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    listOf(
                        "WELCOME OFFER",
                        "FRESH FINDS",
                        "MORNING FAVOURITES",
                        "EVERYDAY SAVINGS",
                    )[index],
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
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
                if (index > 0)
                    Text(
                        if (index == 2) "Breakfast" else "Offers",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
            }
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 10.dp)
                    .background(
                        if (index == 0) Color(0xFFFFE7A3) else Color.White,
                        RoundedCornerShape(7.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    listOf("Explore offer", "Shop fruits", "Shop breakfast", "Explore offers")[
                        index] + " ↗",
                    color = if (index == 0) Color(0xFF25472F) else Color(0xFF174E39),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
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
    if (state.session.subscriptions.any { it.status != "Past" && it.plan.offer != "none" }) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MilkCampaignCard { go("milk-offer") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.products
                .filter { it.id in listOf("p1", "p3") }
                .forEach { p ->
                    Column(
                        Modifier.weight(1f)
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .border(1.dp, Line, RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                go("product/${p.id}?mode=Subscribe")
                            }
                        ) {
                            Column(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFAFCFA))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (p.id == "p1") Color(0xFF149BC0)
                                            else Color(0xFFD88938),
                                        ),
                                        RoundedCornerShape(12.dp),
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    if (p.id == "p1") "COW MILK" else "BUFFALO MILK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        if (p.id == "p1") Color(0xFF087E9B) else Color(0xFFA45C20),
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                AssetImage(
                                    productImage(p),
                                    p.name,
                                    Modifier.fillMaxWidth().height(142.dp).padding(vertical = 8.dp),
                                )
                            }
                            Text(
                                p.name.removePrefix("Pure "),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 12.dp),
                                maxLines = 1,
                            )
                            Text("${p.unit} per delivery", fontSize = 10.sp, color = Muted)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(money(p.price), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            TextButton({ go("product/${p.id}?mode=Subscribe") }) {
                                Text("+ Add", fontSize = 11.sp, color = Green)
                            }
                        }
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
