package com.countryd.app.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import kotlinx.coroutines.flow.first

@Composable
internal fun ConnectedShop(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    category: String,
    viewCart: () -> Unit,
    homeFeedback: (@Composable () -> Unit)? = null,
    openProduct: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        model.state.first { !it.busy }
        model.products()
    }
    val matchingCategory =
        when (category) {
            "bakery" -> "Bakery"
            "dairy" -> "Dairy"
            "eggs" -> "Eggs"
            "grains",
            "oils",
            "spices",
            "dals" -> "Staples"
            else -> null
        }
    var selectedCategory by
        rememberSaveable(category) { mutableStateOf(matchingCategory.orEmpty()) }
    val products =
        state.products.filter {
            (selectedCategory.isBlank() ||
                it.str("category") == selectedCategory ||
                (selectedCategory == "Milk" && it.str("productId") == "MILK")) &&
                (category != "protein" || it.str("category") in setOf("Dairy", "Eggs")) &&
                (category != "milk" || it.str("productId") == "MILK") &&
                (it.str("name").contains(query.trim(), ignoreCase = true) ||
                    it.str("category").contains(query.trim(), ignoreCase = true))
        }
    ScreenBody {
        if (homeFeedback == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Shop fresh groceries",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = viewCart) { Text("View cart") }
            }
        }
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search products") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        if (homeFeedback != null) {
            LiveHomeBanners()
            homeFeedback()
        }
        Text("Shop by category", style = MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories =
                state.products
                    .map { it.str("category") }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted()
            val homeOrder = listOf("Milk", "Dairy", "Eggs", "Bakery", "Staples", "Beverages")
            val displayed =
                if (homeFeedback == null) categories
                else
                    homeOrder.filter { name ->
                        name in categories ||
                            (name == "Milk" && state.products.any { it.str("productId") == "MILK" })
                    } + categories.filterNot { it in homeOrder }
            item {
                if (homeFeedback != null)
                    HomeCategoryTile("All", selectedCategory.isBlank()) { selectedCategory = "" }
                else
                    FilterChip(
                        selectedCategory.isBlank(),
                        { selectedCategory = "" },
                        { Text("All") },
                    )
            }
            displayed.forEach { name ->
                item(key = name) {
                    if (homeFeedback != null)
                        HomeCategoryTile(name, selectedCategory == name) { selectedCategory = name }
                    else
                        FilterChip(
                            selectedCategory == name,
                            { selectedCategory = name },
                            { Text(name) },
                        )
                }
            }
        }
        ConnectedCatalogueStatus(state, model::products)
        if (!state.busy && state.error.isBlank() && products.isEmpty()) Text("No products found.")
        products.forEach { product ->
            val id = product.str("productId")
            val name = product.str("name")
            OutlinedCard(
                onClick = { openProduct(Uri.encode(id)) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Line),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FeedbackImage(product.str("imageUrl"), name)
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${product.str("unit")} · ₹${product.str("price")}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(
                        onClick = { model.add(id, 1) },
                        enabled = !state.busy,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConnectedProduct(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    id: String,
) {
    var quantity by rememberSaveable(id) { mutableIntStateOf(1) }
    LaunchedEffect(id) {
        model.state.first { !it.busy }
        model.product(id)
    }
    val product = state.product?.takeIf { it.str("productId").equals(id, ignoreCase = true) }
    ScreenBody {
        ConnectedCatalogueStatus(state) { model.product(id) }
        if (product != null) {
            FeedbackImage(product.str("imageUrl"), product.str("name"))
            Text(product.str("name"), style = MaterialTheme.typography.titleLarge)
            Text("${product.str("category")} · ${product.str("unit")} · ₹${product.str("price")}")
            Text(product.str("description"), color = Muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quantity", Modifier.weight(1f))
                TextButton(onClick = { quantity-- }, enabled = quantity > 1 && !state.busy) {
                    Text("−")
                }
                Text(quantity.toString())
                TextButton(onClick = { quantity++ }, enabled = quantity < 20 && !state.busy) {
                    Text("+")
                }
            }
            PrimaryButton("Add to cart", enabled = !state.busy) { model.add(id, quantity) }
        }
    }
}

@Composable
private fun ConnectedCatalogueStatus(state: FeedbackConnection, retry: () -> Unit) {
    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (state.error.isNotBlank()) {
        Text(state.error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = retry, enabled = !state.busy) { Text("Retry") }
    }
}

@Composable
private fun LiveHomeBanners() {
    val banners =
        listOf(
            "banner-fresh-essentials-normalized.png" to "Fresh essentials",
            "banner-high-protein-clean.png" to "Everyday protein",
        )
    val pager = rememberPagerState { banners.size }
    HorizontalPager(pager, pageSpacing = 8.dp) { index ->
        val (art, title) = banners[index]
        Box(Modifier.fillMaxWidth().height(155.dp).clip(RoundedCornerShape(16.dp))) {
            AssetImage("assets/generated/$art", null, Modifier.fillMaxSize(), ContentScale.Crop)
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
            ) {
                Text(
                    title,
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
    ) {
        banners.indices.forEach { index ->
            Surface(
                Modifier.size(6.dp),
                shape = RoundedCornerShape(50),
                color = if (index == pager.currentPage) Green else Line,
            ) {}
        }
    }
}

@Composable
private fun HomeCategoryTile(name: String, selected: Boolean, onClick: () -> Unit) {
    val art =
        when (name) {
            "Bakery" -> "bakery"
            "Beverages" -> "fruits"
            "Milk" -> "milk"
            "Dairy" -> "dairy"
            "Eggs" -> "eggs"
            "Staples" -> "grains"
            else -> "milk"
        }
    Column(
        Modifier.width(68.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AssetImage(
            "assets/categories/$art.png",
            null,
            Modifier.size(64.dp, 53.dp).clip(RoundedCornerShape(12.dp)),
            ContentScale.Crop,
        )
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Green else Ink,
        )
    }
}
