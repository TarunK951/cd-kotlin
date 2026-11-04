package com.countryd.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import kotlinx.coroutines.delay

@Composable
fun WalletScreen(state: CountryState, vm: CountryViewModel) {
    val session = state.session
    val firstPayment = session.transactions.none { it.method.startsWith("Demo top-up") }
    var amount by rememberSaveable { mutableStateOf("0") }
    var method by rememberSaveable { mutableStateOf("Google Pay") }
    var payment by rememberSaveable { mutableStateOf("idle") }
    var pendingAmount by rememberSaveable { mutableDoubleStateOf(0.0) }
    var pendingTransactionId by rememberSaveable { mutableStateOf("") }
    var credited by rememberSaveable { mutableIntStateOf(0) }
    var setupAutoPay by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var activity by rememberSaveable { mutableStateOf("Recharges") }
    val quote = rechargeQuote(if (firstPayment) "500" else amount, firstPayment)
    LaunchedEffect(payment) {
        if (payment == "processing") {
            delay(1800)
            val paid = rechargeQuote(pendingAmount.toInt().toString(), firstPayment)
            vm.topUp(pendingAmount, method, pendingTransactionId)
            if (setupAutoPay)
                vm.update {
                    it.copy(
                        autoPay =
                            it.autoPay.copy(
                                enabled = true,
                                amount = pendingAmount,
                                threshold = 100.0,
                                method = "$method UPI",
                            )
                    )
                }
            credited = paid.credit
            message =
                "₹${paid.credit} added${if(paid.bonus>0)" including ₹${paid.bonus} bonus" else ""}.${if(setupAutoPay)" Demo AutoPay preference saved." else ""}"
            amount = "0"
            setupAutoPay = false
            payment = "complete"
        }
    }
    ScreenBody {
        Column(
            Modifier.fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color(0xFF075945), Color(0xFF03A46C))
                    ),
                    RoundedCornerShape(26.dp),
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "DELIGHT WALLET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
            )
            Text("Available balance", fontSize = 12.sp, color = Color(0xFFD0E8DE))
            Text(
                money(session.wallet),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        CountryCard {
            Text("Recharge wallet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (firstPayment) {
                Caption("₹500 first recharge · fixed amount")
                Text("₹ 500", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            } else EntryField("Amount", amount, { amount = it.filter(Char::isDigit).take(5) }, true)
            HorizontalDivider(color = Line)
            Text(
                if (quote.bonus > 0) "₹50 bonus unlocked. You get ₹${quote.credit}."
                else "Recharge ₹500 or more to get ₹50 bonus.",
                fontSize = 12.sp,
                color = Green,
            )
            CountryCard(color = Mint) {
                Text(
                    "Recharge ₹500, get ₹50 bonus",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Caption("Pay ₹500. Wallet gets ₹550.")
            }
            Text("Pay with", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Options(listOf("Google Pay", "PhonePe", "Paytm"), method) { method = it }
            HorizontalDivider(color = Line)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AutoPay / e-mandate",
                    Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    session.autoPay.enabled || setupAutoPay,
                    { value ->
                        if (value) setupAutoPay = true
                        else {
                            setupAutoPay = false
                            vm.update { it.copy(autoPay = it.autoPay.copy(enabled = false)) }
                        }
                    },
                )
            }
            PrimaryButton("Recharge wallet", enabled = quote.valid) {
                pendingAmount = quote.amount.toDouble()
                pendingTransactionId = java.util.UUID.randomUUID().toString()
                payment = "processing"
            }
            if (message.isNotEmpty()) Text(message, color = Green)
            Caption(
                "Demo recharge - no money charged. Fee ₹0. One ₹50 bonus per recharge of ₹500 or more."
            )
        }
        CountryCard {
            Heading("Recent activity")
            Options(listOf("Recharges", "Spends"), activity) { activity = it }
            val entries =
                session.transactions.filter {
                    if (activity == "Recharges") it.amount >= 0 else it.amount < 0
                }
            if (entries.isEmpty())
                Caption(
                    if (activity == "Recharges") "Your recharges and refunds will appear here."
                    else "Your wallet payments will appear here."
                )
            entries.forEach { entry ->
                BillRow(
                    entry.method,
                    if (entry.amount >= 0) "+${money(entry.amount+entry.bonus)}"
                    else "−${money(-entry.amount)}",
                )
                if (entry.bonus > 0) Caption("Includes ${money(entry.bonus)} bonus")
                Caption(entry.date.take(10))
            }
        }
    }
    if (payment != "idle")
        Dialog(onDismissRequest = { if (payment == "complete") payment = "idle" }) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (payment == "processing") {
                    CircularProgressIndicator(color = Green)
                    Heading("Processing payment")
                    Caption("Please wait while we confirm your demo recharge.")
                } else {
                    Box(contentAlignment = Alignment.TopCenter) {
                        PaymentBurst(Modifier.matchParentSize())
                        Box(
                            Modifier.size(64.dp).background(Mint, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✓", fontSize = 32.sp, color = Green)
                        }
                    }
                    Heading("Payment done")
                    Caption("₹$credited added to your wallet, including any bonus.")
                    CountryCard(color = Color(0xFFF3F8F4)) {
                        Caption("Wallet balance")
                        Text(
                            money(session.wallet),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    PrimaryButton("Done") { payment = "idle" }
                }
            }
        }
}
