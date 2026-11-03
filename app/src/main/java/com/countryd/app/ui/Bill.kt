package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*

@Composable
fun BillRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            Modifier.weight(1f),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
        )
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
    }
}

@Composable
fun Bill(bill: Totals, tip: Double) {
    CountryCard {
        Heading("Bill details")
        BillRow("Items total", money(bill.items))
        if (bill.savings > 0) BillRow("VIP savings", "−${money(bill.savings)}")
        if (bill.coupon > 0) BillRow("Coupon", "−${money(bill.coupon)}")
        BillRow("Delivery", if (bill.delivery == 0.0) "FREE" else money(bill.delivery))
        if (tip > 0) BillRow("Delivery partner tip", money(tip))
        if (bill.pointsUsed > 0) BillRow("Reward points", "−${bill.pointsUsed}")
        HorizontalDivider(color = Line)
        BillRow("Total", money(bill.total), true)
        if (bill.walletUsed > 0) BillRow("Paid from wallet", "−${money(bill.walletUsed)}")
        BillRow("To pay", money(bill.toPay), true)
        Caption("Earn ${bill.pointsEarned} points with this order.")
    }
}
