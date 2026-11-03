package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun OffersScreen(state: CountryState, go: (String) -> Unit) {
    ScreenBody {
        Heading("A little more for your mornings")
        listOf(
                Triple("Fresh Curd", "Buy 1, get 1 free", "p5"),
                Triple("Cow Milk", "Buy 4, get 3 free", "p1"),
                Triple("Whole Wheat Bread", "Weekend breakfast bonus", "p13"),
            )
            .forEach { (name, title, id) ->
                CountryCard(color = Mint) {
                    Heading(title)
                    Caption(name)
                    TextButton({ go("product/$id") }) { Text("View product ›") }
                }
            }
        CountryCard {
            Heading("DELIGHT10")
            Caption("Save ₹10 on a basket of ₹200 or more. Apply at checkout.")
        }
        PrimaryButton("Explore milk plans") { go("milk-offer") }
    }
}
