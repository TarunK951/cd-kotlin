package com.countryd.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import kotlinx.coroutines.flow.first

@Composable
internal fun ConnectedShop(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    category: String,
    viewCart: () -> Unit,
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
    val products =
        state.products.filter {
            (matchingCategory == null || it.str("category") == matchingCategory) &&
                (category != "milk" || it.str("productId") == "MILK") &&
                (it.str("name").contains(query.trim(), ignoreCase = true) ||
                    it.str("category").contains(query.trim(), ignoreCase = true))
        }
    ScreenBody {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Shop fresh groceries",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = viewCart) { Text("View cart") }
        }
        OutlinedTextField(
            query,
            { query = it },
            label = { Text("Search products") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ConnectedCatalogueStatus(state, model::products)
        if (!state.busy && state.error.isBlank() && products.isEmpty()) Text("No products found.")
        products.forEach { product ->
            val id = product.str("productId")
            val name = product.str("name")
            Card(onClick = { openProduct(Uri.encode(id)) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FeedbackImage(product.str("imageUrl"), name)
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text("${product.str("unit")} · ₹${product.str("price")}", color = Muted)
                    }
                    TextButton(onClick = { model.add(id, 1) }, enabled = !state.busy) { Text("Add") }
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
    viewCart: () -> Unit,
) {
    var quantity by rememberSaveable(id) { mutableIntStateOf(1) }
    LaunchedEffect(id) {
        model.state.first { !it.busy }
        model.product(id)
    }
    val product = state.product?.takeIf { it.str("productId").equals(id, ignoreCase = true) }
    ScreenBody {
        TextButton(onClick = viewCart) { Text("View cart") }
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
