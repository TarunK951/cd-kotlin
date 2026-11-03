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
fun ProteinScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    ShopScreen(state, vm, go, "protein")
}
