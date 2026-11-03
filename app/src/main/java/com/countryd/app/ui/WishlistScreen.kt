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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun WishlistScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    ScreenBody {
        val list = state.products.filter { it.id in state.session.wishlist }
        if (list.isEmpty())
            EmptyState(
                "Save your favourites",
                "Tap a product’s heart to keep it here.",
                "Explore products",
            ) {
                go("shop")
            }
        else
            list.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { ProductTile(it, state.session, vm, go, Modifier.weight(1f)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
    }
}
