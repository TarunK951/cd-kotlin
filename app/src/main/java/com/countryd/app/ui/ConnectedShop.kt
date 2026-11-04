package com.countryd.app.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectedShop(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    category: String,
    homeFeedback: (@Composable () -> Unit)? = null,
    refreshHome: (() -> Unit)? = null,
    viewAll: (() -> Unit)? = null,
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
                (category != "subscribe" || it.flag("subscribable")) &&
                (category != "protein" || it.str("category") in setOf("Dairy", "Eggs")) &&
                (category != "milk" || it.str("productId") == "MILK") &&
                (it.str("name").contains(query.trim(), ignoreCase = true) ||
                    it.str("category").contains(query.trim(), ignoreCase = true))
        }
    val cartQuantities =
        remember(state.cart) {
            state.cart
                ?.list("items")
                .orEmpty()
                .groupingBy { it.str("productId") }
                .fold(0L) { quantity, item -> quantity + item.num("quantity") }
        }
    val featured = homeFeedback != null && selectedCategory.isBlank() && query.isBlank()
    val featuredOrder = listOf("PANEER", "DAHI", "MILK", "BREAD")
    val catalogue: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (homeFeedback == null) {
                        Text(
                            if (category == "subscribe") "Choose a product to subscribe to"
                            else "Shop fresh groceries",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    OutlinedTextField(
                        query,
                        { query = it },
                        label = { Text("Search products") },
                        leadingIcon = { Icon(CountryIcons.Search, null, tint = Muted) },
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
                                .filter { category != "subscribe" || it.flag("subscribable") }
                                .map { it.str("category") }
                                .filter(String::isNotBlank)
                                .distinct()
                                .sorted()
                        val homeOrder =
                            listOf("Milk", "Dairy", "Eggs", "Bakery", "Staples", "Beverages")
                        val displayed =
                            if (homeFeedback == null) categories
                            else
                                homeOrder.filter { name ->
                                    name in categories ||
                                        (name == "Milk" &&
                                            state.products.any { it.str("productId") == "MILK" })
                                } + categories.filterNot { it in homeOrder }
                        item {
                            if (homeFeedback != null)
                                HomeCategoryTile("All", selectedCategory.isBlank()) {
                                    selectedCategory = ""
                                }
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
                                    HomeCategoryTile(name, selectedCategory == name) {
                                        selectedCategory = name
                                    }
                                else
                                    FilterChip(
                                        selectedCategory == name,
                                        { selectedCategory = name },
                                        { Text(name) },
                                    )
                            }
                        }
                    }
                    if (
                        homeFeedback == null &&
                            category != "subscribe" &&
                            state.buyAgainRecommendations.isNotEmpty() &&
                            query.isBlank()
                    ) {
                        Text("Order again", style = MaterialTheme.typography.titleMedium)
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.buyAgainRecommendations.forEach { item ->
                                item(key = item.str("productId")) {
                                    OutlinedCard(Modifier.width(155.dp)) {
                                        Column(
                                            Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            FeedbackImage(
                                                item.str("imageUrl"),
                                                item.str("productName"),
                                            )
                                            Text(
                                                item.str("productName"),
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                "${item.str("unit")} · ₹${item.str("price")}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            Text(
                                                item.str("reason"),
                                                color = Muted,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            TextButton(
                                                onClick = {
                                                    model.add(
                                                        item.str("productId"),
                                                        1,
                                                        item.str("suggestedDate"),
                                                        item.num("feedbackId"),
                                                    )
                                                },
                                                enabled = !state.busy,
                                            ) {
                                                Text("Order again")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ConnectedCatalogueStatus(state, model::products)
                    if (!state.busy && state.error.isBlank() && products.isEmpty())
                        Text("No products found.")
                }
            }
            if (featured && products.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Featured products",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (viewAll != null) TextButton(onClick = viewAll) { Text("View all") }
                    }
                }
                val ordered =
                    products.sortedBy {
                        featuredOrder.indexOf(it.str("productId")).takeIf { index -> index >= 0 }
                            ?: featuredOrder.size
                    }
                items(ordered.chunked(2), key = { row -> row.first().str("productId") }) { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { product ->
                            val id = product.str("productId")
                            ConnectedCatalogueCard(
                                product,
                                Modifier.weight(1f),
                                featured = true,
                                enabled = !state.busy && !state.checkoutPending,
                                chooseProduct = false,
                                quantity = cartQuantities[id] ?: 0,
                                open = { openProduct(Uri.encode(id)) },
                                changeQuantity = { model.adjustProductQuantity(id, it) },
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(products, key = { it.str("productId") }) { product ->
                    val id = product.str("productId")
                    val open = { openProduct(Uri.encode(id)) }
                    ConnectedCatalogueCard(
                        product,
                        Modifier.fillMaxWidth(),
                        featured = false,
                        enabled =
                            category == "subscribe" || (!state.busy && !state.checkoutPending),
                        chooseProduct = category == "subscribe",
                        quantity = cartQuantities[id] ?: 0,
                        open = open,
                        changeQuantity = { model.adjustProductQuantity(id, it) },
                    )
                }
            }
            if (homeFeedback != null && query.isBlank() && state.buyAgainRecommendations.isNotEmpty()) {
                item {
                    RatedRecommendations(state.buyAgainRecommendations, openProduct)
                }
            }
        }
    }
    PullToRefreshBox(
        isRefreshing = if (refreshHome == null) state.catalogueRefreshing else state.homeRefreshing,
        onRefresh = refreshHome ?: model::products,
        modifier = Modifier.fillMaxSize(),
    ) { catalogue() }
}

@Composable
private fun RatedRecommendations(items: List<JsonObject>, openProduct: (String) -> Unit) {
    val products = items.filter { it.str("productId").isNotBlank() }.distinctBy { it.str("productId") }
    if (products.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recommended for you", style = MaterialTheme.typography.titleMedium)
        Caption("Based on products you rated 4 or 5 stars.")
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products, key = { it.str("productId") }) { product ->
                val name = product.str("productName")
                OutlinedCard(
                    onClick = { openProduct(Uri.encode(product.str("productId"))) },
                    modifier = Modifier.width(150.dp),
                ) {
                    Column(
                        Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        FeedbackImage(product.str("imageUrl"), name)
                        Text(name, style = MaterialTheme.typography.titleSmall)
                        Caption("${product.str("unit")} · ₹${product.str("price")}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedCatalogueCard(
    product: JsonObject,
    modifier: Modifier,
    featured: Boolean,
    enabled: Boolean,
    chooseProduct: Boolean,
    quantity: Long,
    open: () -> Unit,
    changeQuantity: (Int) -> Unit,
) {
    val name = product.str("name")
    val price = "${product.str("unit")} · ₹${product.str("price")}"
    OutlinedCard(
        onClick = open,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Line),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (featured) {
            val photo =
                when (product.str("productId")) {
                    "PANEER" -> "assets/photo/paneer.jpg"
                    "DAHI" -> "assets/products/country-delight/ghar-jaisa-dahi.jpg"
                    else -> null
                }
            Box(
                Modifier.fillMaxWidth().height(106.dp).background(Mint),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null)
                    AssetImage(photo, name, Modifier.fillMaxSize(), ContentScale.Crop)
                else
                    FeedbackImage(
                        product.str("imageUrl"),
                        name,
                        imageModifier = Modifier.size(96.dp),
                        scale = ContentScale.Fit,
                    )
            }
            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Caption(price)
                CatalogueQuantity(name, quantity, enabled, changeQuantity)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FeedbackImage(product.str("imageUrl"), name)
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    Caption(price)
                }
                if (chooseProduct) {
                    OutlinedButton(onClick = open, enabled = enabled) { Text("Choose") }
                } else {
                    CatalogueQuantity(name, quantity, enabled, changeQuantity)
                }
            }
        }
    }
}

@Composable
private fun CatalogueQuantity(
    productName: String,
    quantity: Long,
    enabled: Boolean,
    change: (Int) -> Unit,
) {
    if (quantity == 0L) {
        OutlinedButton(
            onClick = { change(1) },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text("Add")
        }
    } else {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Mint,
            border = BorderStroke(1.dp, Line),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { change(-1) },
                    enabled = enabled,
                    modifier =
                        Modifier.size(44.dp).semantics {
                            contentDescription = "Remove one $productName"
                        },
                ) {
                    Text("−", color = Green, style = MaterialTheme.typography.titleMedium)
                }
                Text(quantity.toString(), style = MaterialTheme.typography.labelLarge)
                IconButton(
                    onClick = { change(1) },
                    enabled = enabled,
                    modifier =
                        Modifier.size(44.dp).semantics {
                            contentDescription = "Add one $productName"
                        },
                ) {
                    Text("+", color = Green, style = MaterialTheme.typography.titleMedium)
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
    startInSubscription: Boolean,
    openSubscriptions: () -> Unit,
    chooseAddress: () -> Unit,
    openProduct: (String) -> Unit,
) {
    var buyOpen by rememberSaveable(id) { mutableStateOf(false) }
    var buyQuantity by rememberSaveable(id) { mutableIntStateOf(1) }
    var buyDate by rememberSaveable(id) { mutableStateOf("") }
    LaunchedEffect(buyOpen) {
        if (buyOpen && (state.cart == null || state.addresses == null)) {
            model.state.first { !it.busy }
            model.cart()
        }
    }
    var requested by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        model.state.first { !it.busy }
        model.product(id)
        requested = true
    }
    val detail = state.product?.takeIf { it.str("productId").equals(id, ignoreCase = true) }
    val product =
        detail ?: state.products.firstOrNull { it.str("productId").equals(id, ignoreCase = true) }
    if (product == null) {
        ScreenBody {
            ConnectedCatalogueStatus(state) { model.product(id) }
            if (!state.busy && state.error.isBlank()) {
                if (requested) {
                    Text("Product details are unavailable.")
                    TextButton(onClick = { model.product(id) }) { Text("Retry") }
                } else {
                    Text("Loading product details…")
                }
            }
        }
        return
    }
    val name = product.str("name")
    val subscribable = product.flag("subscribable")
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        val heroHeight = minOf(maxWidth * .43f, maxHeight * .34f).coerceIn(96.dp, 146.dp)
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth()
                        .height(heroHeight)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Mint),
                    contentAlignment = Alignment.Center,
                ) {
                    FeedbackImage(
                        product.str("imageUrl"),
                        name,
                        imageModifier = Modifier.size(heroHeight - 10.dp),
                        scale = ContentScale.Fit,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Caption(product.str("category"))
                    Text(name, style = MaterialTheme.typography.headlineSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "₹${product.str("price")}",
                            color = Green,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Caption("${product.str("unit")} per pack")
                    }
                }
                HorizontalDivider(color = Line)
                Text("About this product", style = MaterialTheme.typography.titleMedium)
                Caption(
                    detail?.str("description").orEmpty().ifBlank {
                        "${product.str("unit")} · ${product.str("category")}"
                    }
                )
                ConnectedCatalogueStatus(state) { model.product(id) }
                RatedRecommendations(
                    state.buyAgainRecommendations.filterNot { it.str("productId").equals(id, ignoreCase = true) },
                    openProduct,
                )
            }
            Surface(shadowElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { buyOpen = true },
                        modifier = Modifier.weight(1f),
                        enabled = !state.busy && !state.checkoutPending,
                    ) {
                        Text("Buy")
                    }
                    if (subscribable) {
                        Box(Modifier.weight(1f)) {
                            SubscribeAction(
                                state,
                                model,
                                product,
                                openSubscriptions = openSubscriptions,
                                chooseAddress = chooseAddress,
                                startOpen = startInSubscription,
                            )
                        }
                    }
                }
            }
        }
    }
    if (buyOpen) {
        val tomorrow = LocalDate.now().plusDays(1)
        val latest = tomorrow.plusDays(29)
        val cartDates = state.cart?.list("items").orEmpty().map { it.str("deliveryDate") }.toSet()
        val suggested =
            (0L..29L)
                .asSequence()
                .map { tomorrow.plusDays(it).toString() }
                .firstOrNull { it !in cartDates }
                .orEmpty()
        val date = buyDate.ifBlank { suggested }
        val address = state.activeDeliveryAddress()
        val complete = address?.isCompleteDeliveryAddress() == true
        val canBuy =
            !state.busy &&
                !state.checkoutPending &&
                state.cart != null &&
                date.isNotBlank() &&
                date !in cartDates &&
                date >= tomorrow.toString() &&
                date <= latest.toString()
        AlertDialog(
            onDismissRequest = { buyOpen = false },
            title = { Text("Buy $name", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("${product.str("unit")} · ₹${product.str("price")} per pack")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantity", Modifier.weight(1f))
                        Quantity(buyQuantity, { buyQuantity = it }, maximum = 20)
                    }
                    Text("Delivery date", style = MaterialTheme.typography.titleSmall)
                    if (suggested.isNotBlank()) {
                        FeedbackDateChoices(
                            suggested = suggested,
                            suggestedLabel =
                                if (suggested == tomorrow.toString()) "Tomorrow"
                                else "Next available",
                            minimum = tomorrow.toString(),
                            maximum = latest.toString(),
                            date = date,
                            enabled = !state.busy,
                            onDate = { buyDate = it },
                            unavailable = cartDates,
                        )
                    } else
                        Caption(
                            "No dates are available for a separate purchase in the next 30 days."
                        )
                    if (cartDates.isNotEmpty())
                        Caption("Dates with cart items are reserved for your existing cart.")
                    Text("Deliver to", style = MaterialTheme.typography.titleSmall)
                    Caption(
                        address?.str("fullAddress").orEmpty().ifBlank {
                            "Choose a delivery address"
                        }
                    )
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!complete) chooseAddress()
                        else {
                            buyOpen = false
                            model.buyNow(id, buyQuantity, date)
                        }
                    },
                    enabled = if (complete) canBuy else !state.busy,
                ) {
                    Text(if (complete) "Place order" else "Choose address")
                }
            },
            dismissButton = { TextButton(onClick = { buyOpen = false }) { Text("Cancel") } },
        )
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
        if (name == "All") {
            Box(
                Modifier.size(64.dp, 53.dp).clip(RoundedCornerShape(12.dp)).background(Mint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CountryIcons.ShoppingBag, null, tint = Green)
            }
        } else {
            AssetImage(
                "assets/categories/$art.png",
                null,
                Modifier.size(64.dp, 53.dp).clip(RoundedCornerShape(12.dp)),
                ContentScale.Crop,
            )
        }
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Green else Ink,
        )
    }
}
