package com.countryd.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun ShopScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    initialCategory: String,
) {
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf("Recommended") }
    var under100 by rememberSaveable { mutableStateOf(false) }
    var rated by rememberSaveable { mutableStateOf(false) }
    var bestseller by rememberSaveable { mutableStateOf(false) }
    var onOffer by rememberSaveable { mutableStateOf(false) }
    val catalogueScroll = rememberLazyListState()
    val sidebar = categoryTiles.any { it.first == category } && query.isBlank()
    val continuous =
        sidebar && !under100 && !rated && !bestseller && !onOffer && sort == "Recommended"
    LaunchedEffect(category) {
        if (continuous && !catalogueScroll.isScrollInProgress)
            catalogueScroll.scrollToItem(
                categoryTiles.indexOfFirst { it.first == category }.coerceAtLeast(0)
            )
    }
    LaunchedEffect(continuous) {
        if (continuous)
            snapshotFlow {
                    catalogueScroll.firstVisibleItemIndex to catalogueScroll.isScrollInProgress
                }
                .collect { (index, moving) ->
                    if (moving)
                        category = categoryTiles[index.coerceIn(categoryTiles.indices)].first
                }
    }
    val session = state.session
    val all =
        if (query.isBlank()) categoryProducts(state.products, category, session)
        else searchProducts(state.products, query)
    val filtered =
        all.filter {
            (!under100 || it.price < 100) &&
                (!rated || it.rating >= 4) &&
                (!bestseller || it.tag == "Bestseller") &&
                (!onOffer || it.mrp > it.price)
        }
    val results =
        when (sort) {
            "Price: low to high" -> filtered.sortedBy { it.price }
            "Price: high to low" -> filtered.sortedByDescending { it.price }
            "Top rated" -> filtered.sortedByDescending { it.rating }
            else -> filtered
        }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (category != "all")
            Row(
                Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton({
                    category = "all"
                    searching = false
                    query = ""
                }) {
                    Icon(CountryIcons.ChevronLeft, "All categories")
                }
                Text(
                    if (category == "protein") "High protein"
                    else
                        categoryTiles.find { it.first == category }?.second
                            ?: category.replaceFirstChar(Char::uppercase),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        BasicTextField(
            query,
            {
                query = it
                searching = true
            },
            singleLine = true,
            textStyle = TextStyle(fontFamily = Onest, fontSize = 12.5.sp, color = Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .semantics { contentDescription = "Search products" }
                    .onFocusChanged { if (it.isFocused) searching = true },
            decorationBox = { field ->
                Row(
                    Modifier.fillMaxWidth()
                        .height(36.dp)
                        .background(Color(0xFFF6F8F4), RoundedCornerShape(18.dp))
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(CountryIcons.Search, null, Modifier.size(18.dp), tint = Muted)
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty())
                            Text(
                                "Search milk, paneer, eggs…",
                                fontSize = 12.5.sp,
                                color = Color(0xFF9CA3AF),
                            )
                        field()
                    }
                    if (query.isNotEmpty())
                        IconButton({ query = "" }, Modifier.size(32.dp)) {
                            Icon(CountryIcons.Close, "Clear search", Modifier.size(16.dp))
                        }
                }
            },
        )
        if (category == "all" && query.isBlank()) {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Text("Shop by category", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                items(categoryTiles.chunked(3)) { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        row.forEach { (id, title) ->
                            Column(
                                Modifier.weight(1f).clickable { category = id },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                AssetImage(
                                    "assets/categories/$id.png",
                                    title,
                                    Modifier.size(56.dp, 52.dp).clip(RoundedCornerShape(20.dp)),
                                    ContentScale.Crop,
                                )
                                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                item { ShopBento { category = it } }
            }
        } else {
            if (query.isBlank() && searching && session.recentSearches.isNotEmpty()) {
                Caption("Recent searches", Modifier.padding(horizontal = 16.dp))
                Options(session.recentSearches, "") { query = it }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    {
                        sort =
                            when (sort) {
                                "Recommended" -> "Price: low to high"
                                "Price: low to high" -> "Price: high to low"
                                "Price: high to low" -> "Top rated"
                                else -> "Recommended"
                            }
                    },
                    label = { Text(if (sort == "Recommended") "↕ Sort" else sort) },
                    shape = RoundedCornerShape(22.dp),
                )
                FilterChip(
                    under100,
                    { under100 = !under100 },
                    label = { Text("Under ₹100") },
                    shape = RoundedCornerShape(22.dp),
                )
                FilterChip(
                    rated,
                    { rated = !rated },
                    label = { Text("4★ & above") },
                    shape = RoundedCornerShape(22.dp),
                )
                FilterChip(
                    bestseller,
                    { bestseller = !bestseller },
                    label = { Text("Bestseller") },
                    shape = RoundedCornerShape(22.dp),
                )
                FilterChip(
                    onOffer,
                    { onOffer = !onOffer },
                    label = { Text("On offer") },
                    shape = RoundedCornerShape(22.dp),
                )
                if (under100 || rated || bestseller || onOffer || sort != "Recommended")
                    TextButton({
                        under100 = false
                        rated = false
                        bestseller = false
                        onOffer = false
                        sort = "Recommended"
                    }) {
                        Text("Clear all")
                    }
            }
            if (results.isEmpty())
                EmptyState(
                    "No products found",
                    "Try another product name or filter.",
                    "Clear filters",
                ) {
                    query = ""
                    under100 = false
                    rated = false
                    bestseller = false
                }
            Row(Modifier.weight(1f)) {
                if (sidebar)
                    LazyColumn(Modifier.width(84.dp).fillMaxHeight().border(1.dp, Line)) {
                        items(categoryTiles) { (id, title) ->
                            Column(
                                Modifier.fillMaxWidth()
                                    .background(if (category == id) Mint else Color.Transparent)
                                    .clickable { category = id }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AssetImage(
                                    "assets/categories/$id.png",
                                    title,
                                    Modifier.size(52.dp, 45.dp).clip(RoundedCornerShape(14.dp)),
                                    ContentScale.Crop,
                                )
                                Text(
                                    title,
                                    fontSize = 10.5.sp,
                                    fontWeight =
                                        if (category == id) FontWeight.ExtraBold
                                        else FontWeight.SemiBold,
                                    color = if (category == id) Green else Muted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                            }
                        }
                    }
                if (continuous)
                    LazyColumn(
                        state = catalogueScroll,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        items(categoryTiles, key = { it.first }) { (id, title) ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                categoryProducts(state.products, id, session).chunked(2).forEach {
                                    row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach {
                                            ProductTile(it, session, vm, go, Modifier.weight(1f))
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                else
                    LazyVerticalGrid(
                        GridCells.Fixed(if (sidebar) 2 else 3),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        contentPadding = PaddingValues(if (sidebar) 10.dp else 14.dp),
                    ) {
                        items(results, key = { it.id }) { product ->
                            ProductTile(
                                product,
                                session,
                                vm,
                                {
                                    if (query.isNotBlank()) vm.search(query)
                                    go(it)
                                },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun ShopBento(onCategory: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .height(224.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF124D3D), Color(0xFF34866B), Color(0xFF86BCA6))
                    )
                )
                .clickable { onCategory("milk") }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    "✧  EVERYDAY GOODNESS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFE7BA),
                )
                Text(
                    "Morning, made easy.",
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text("Explore essentials ↗", fontSize = 12.sp, color = Color(0xFFE0F0E7))
            }
            AssetImage(
                "assets/generated/commerce-2026-v5/pantry-cutout.png",
                "Breakfast groceries",
                Modifier.size(106.dp, 126.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("fruits" to "Fruit bowl", "breakfast" to "Breakfast").forEach { (id, title) ->
                Column(
                    Modifier.weight(1f)
                        .height(105.dp)
                        .background(
                            if (id == "fruits") Color(0xFFFFD7B2) else Color(0xFFF1D0E8),
                            RoundedCornerShape(18.dp),
                        )
                        .clickable { onCategory(id) }
                        .padding(12.dp)
                ) {
                    Text("$title ↗", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    AssetImage(
                        if (id == "fruits") "assets/generated/commerce-minimal-v1/fruit.png"
                        else "assets/generated/commerce-2026-v5/pantry-cutout.png",
                        null,
                        Modifier.align(Alignment.End).size(52.dp, 48.dp),
                    )
                }
            }
        }
    }
}
