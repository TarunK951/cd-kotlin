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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun HomeScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    connectedFeedback: (@Composable () -> Unit)? = null,
) {
    val s = state.session
    val products = state.products.filter { matchesPreference(it, s.goal.preference) }
    var freshTab by rememberSaveable { mutableStateOf("All fresh") }
    var repeatTab by rememberSaveable { mutableStateOf("All items") }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = { go("shop") },
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF0F6F2),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(CountryIcons.Search, null, Modifier.size(19.dp), tint = Muted)
                        Text("Search fresh groceries", color = Muted, fontSize = 12.sp)
                    }
                }
                Surface(
                    onClick = { go("vip") },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Row(
                        Modifier.height(48.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFD8AD4A), Color(0xFFFFF2BE), Color(0xFFE8BB54))
                                ),
                                RoundedCornerShape(24.dp),
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            CountryIcons.Crown,
                            null,
                            Modifier.size(16.dp),
                            tint = Color(0xFF67430D),
                        )
                        Text(
                            "VIP",
                            color = Color(0xFF67430D),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        item { DeliveryOverview(state, vm, go) }
        item { CampaignCarousel(state.products, go) }
        item {
            if (connectedFeedback != null) {
                connectedFeedback()
                return@item
            }
            val request =
                feedbackRequests(s, state.products, immediate = true).firstOrNull()
                    ?.takeUnless { r -> s.feedback.any { it.request.id == r.id } }
            if (request != null) FeedbackPrompt(request, vm, go)
            else if (
                BuildConfig.DEBUG &&
                    !s.sampleFeedbackDismissed &&
                    s.orders.isEmpty() &&
                    s.subscriptions.isEmpty()
            )
                state.sampleRequests.firstOrNull { candidate ->
                    s.feedback.none { it.request.id == candidate.id }
                }?.let { FeedbackPrompt(it, vm, go) }
        }

        item {
            Text("Shop by category", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(categoryTiles) { (id, title) ->
                    Column(
                        Modifier.width(66.dp).clickable { go("shop?category=$id") },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AssetImage(
                            "assets/categories/$id.png",
                            title,
                            Modifier.size(66.dp, 56.dp).clip(RoundedCornerShape(16.dp)),
                            ContentScale.Crop,
                        )
                        Text(
                            title,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
            }
        }
        item { MilkRow(state, go) }
        item {
            SectionHeading("Price drops") { go("shop?category=deals") }
            ProductShelf(
                products
                    .filter { it.mrp > it.price }
                    .sortedByDescending { (it.mrp - it.price) / it.mrp }
                    .take(6),
                s,
                vm,
                go,
            )
        }
        item {
            SectionHeading("Fresh from the farm") { go("shop?category=produce") }
            Options(listOf("All fresh", "Vegetables", "Fruits"), freshTab) { freshTab = it }
            ProductShelf(
                products.filter {
                    it.category == "Fruits & Veg" &&
                        (freshTab == "All fresh" || it.subCategory == freshTab)
                },
                s,
                vm,
                go,
            )
        }
        item { SeasonalPanel(state, go) }
        item {
            SectionHeading("Buy again") { go("shop?category=buyAgain") }
            Options(listOf("All items", "Favourites", "Wishlist"), repeatTab) { repeatTab = it }
            val bought =
                s.orders
                    .filter { it.status != "Cancelled" }
                    .flatMap { it.items }
                    .map { it.line.productId }
                    .toSet()
            val picks =
                products.filter { p ->
                    when (repeatTab) {
                        "Wishlist" -> p.id in s.wishlist
                        "Favourites" -> p.id in bought && p.id in s.wishlist
                        else -> p.id in bought
                    }
                }
            if (picks.isEmpty())
                Caption(
                    when (repeatTab) {
                        "Wishlist" -> "Tap a product’s heart to add it to your wishlist."
                        "Favourites" -> "Tap the heart on a past purchase to keep it here."
                        else -> "Your ordered items will appear here."
                    }
                )
            else ProductShelf(picks, s, vm, go)
        }
        items(
            listOf(
                "recommended" to "Recommended for you",
                "pantryPicks" to "Pantry top-ups",
                "proteinPicks" to "Protein picks",
                "bakery" to "Breakfast, sorted",
            )
        ) { (id, title) ->
            SectionHeading(title) { go("shop?category=$id") }
            ProductShelf(categoryProducts(products, id, s), s, vm, go)
        }
        item {
            Caption(
                "Country Delight · Good mornings. Delivered.",
                Modifier.padding(vertical = 24.dp),
            )
        }
    }
}
