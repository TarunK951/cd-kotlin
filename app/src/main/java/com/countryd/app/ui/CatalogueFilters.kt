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

val categoryTiles =
    listOf(
        "milk" to "Milk",
        "dairy" to "Dairy",
        "fruits" to "Fruits",
        "vegetables" to "Vegetables",
        "eggs" to "Eggs",
        "bakery" to "Bakery",
        "breakfast" to "Breakfast",
        "grains" to "Rice & Grains",
        "pulses" to "Dals & Pulses",
        "oils" to "Oils",
        "spices" to "Spices",
        "dryfruits" to "Dry Fruits",
    )

fun categoryProducts(products: List<Product>, category: String, session: Session): List<Product> {
    val ids =
        session.orders
            .filter { it.status != "Cancelled" }
            .flatMap { it.items }
            .map { it.line.productId }
            .toSet()
    return products.filter { p ->
        when (category) {
            "all" -> true
            "milk" -> p.name.contains("milk", true)
            "dairy" -> p.category == "Milk & Dairy" && !p.name.contains("milk", true)
            "produce" -> p.category == "Fruits & Veg"
            "fruits" -> p.subCategory == "Fruits"
            "vegetables" -> p.subCategory == "Vegetables"
            "eggs" -> p.category == "Farm Eggs"
            "bakery" -> p.category == "Bakery & Breakfast"
            "breakfast" -> p.category == "Bakery & Breakfast" && !p.name.contains("bread", true)
            "staples" -> p.category == "Staples"
            "grains" -> p.subCategory == "Rice & Grains"
            "pulses" -> p.subCategory == "Dals & Pulses"
            "oils" -> p.subCategory == "Oils"
            "spices" -> p.subCategory == "Spices"
            "dryfruits" -> p.subCategory == "Dry Fruits"
            "deals" -> p.mrp > p.price
            "buyAgain" -> p.id in ids
            "wishlist" -> p.id in session.wishlist
            "proteinPicks",
            "protein" -> p.highProtein
            "recommended" -> p.id in listOf("p15", "p11", "p6", "p10", "p2", "p7")
            "pantryPicks" -> p.id in listOf("p19", "p20", "p21", "p22", "p23", "p15", "p14", "p6")
            else -> true
        }
    }
}
