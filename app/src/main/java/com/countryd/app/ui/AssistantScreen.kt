package com.countryd.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AssistantScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var reply by remember { mutableStateOf<AssistantReply?>(null) }
    var frequency by rememberSaveable { mutableStateOf("Weekly") }
    var quantities by remember { mutableStateOf(mapOf<String, Int>()) }
    var preview by remember { mutableStateOf<Product?>(null) }
    var savedRequest by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val session = state.session
    val scroll = rememberScrollState()
    LaunchedEffect(session.assistantMessages.size, typing, reply) {
        scroll.animateScrollTo(scroll.maxValue)
    }
    fun say(text: String) {
        if (typing || text.isBlank()) return
        val answer = assistantReply(text, session, state.products)
        reply = null
        input = ""
        typing = true
        savedRequest = ""
        vm.update {
            it.copy(
                assistantMessages =
                    it.assistantMessages.takeLast(10) +
                        SupportMessage(text, Instant.now().toString()),
                supportDraft = if (answer.action == "support") text else it.supportDraft,
            )
        }
        scope.launch {
            delay(650)
            vm.update {
                it.copy(
                    assistantMessages =
                        it.assistantMessages +
                            SupportMessage(answer.text, Instant.now().toString(), false)
                )
            }
            reply = answer
            typing = false
            if (answer.quantity != null)
                quantities = quantities + answer.products.associate { it.id to answer.quantity }
            if (answer.openGoal) go("routine")
        }
    }
    fun add(product: Product, subscribe: Boolean) {
        val qty = quantities[product.id] ?: 1
        vm.add(
            CartLine(
                product.id,
                qty,
                if (subscribe) "Subscribe" else "Once",
                product.defaultVariant,
                plan = if (subscribe) Plan(frequency = frequency) else null,
            )
        )
        vm.update {
            it.copy(
                assistantMessages =
                    it.assistantMessages +
                        SupportMessage(
                            "Added $qty × ${product.name} (${product.unit})${if(subscribe)" · $frequency from tomorrow" else ""}. Review your basket when you’re ready.",
                            Instant.now().toString(),
                            false,
                        )
            )
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssetImage(
                "assets/generated/assistant-2026/delight-guide.png",
                "Delight",
                Modifier.size(42.dp, 48.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("Delight", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Your shopping guide · demo", fontSize = 10.sp, color = Muted)
            }
            IconButton({ go("home") }) { Icon(CountryIcons.Close, "Close Delight") }
        }
        HorizontalDivider(color = Line)
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFA72C36), Color(0xFFC34338), Color(0xFFDC7951))
                        ),
                        RoundedCornerShape(22.dp),
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "YOUR EVERYDAY COMPANION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        "Good mornings\nstart here.",
                        fontSize = 24.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 9.dp),
                    )
                    Text(
                        "Meal ideas, basket picks and delivery help.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                AssetImage(
                    "assets/generated/assistant-2026/delight-guide.png",
                    null,
                    Modifier.size(84.dp, 112.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                        Triple("Customer care", "customer-care-executive-v4.png", "support"),
                        Triple("My requests", "requests.png", "support?view=requests"),
                    )
                    .forEach { (label, asset, route) ->
                        Column(
                            Modifier.weight(1f)
                                .heightIn(min = 90.dp)
                                .background(
                                    if (label == "Customer care") Color(0xFFFFF4E8)
                                    else Color(0xFFEEF2FC),
                                    RoundedCornerShape(18.dp),
                                )
                                .clickable { go(route) }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AssetImage(
                                "assets/generated/delight-support-2026-10-22/$asset",
                                null,
                                Modifier.size(44.dp),
                            )
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
            }
            if (session.assistantMessages.size <= 1) {
                Text("What shall we do today?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val prompts =
                    listOf(
                        Triple(
                            "Breakfast ideas",
                            "Morning ideas",
                            "generated/commerce-2026-v7/morning-ideas-3d.png",
                        ),
                        Triple(
                            "What goes with my basket",
                            "Pair my basket",
                            "generated/commerce-2026-v7/pair-basket-3d.png",
                        ),
                        Triple(
                            "Order help",
                            "Delivery help",
                            "generated/commerce-2026-v4/chilled.png",
                        ),
                        Triple(
                            "Quality and freshness",
                            "Quality guide",
                            "generated/commerce-2026-v4/milk-check.png",
                        ),
                    )
                prompts.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (text, label, asset) ->
                            Column(
                                Modifier.weight(1f)
                                    .heightIn(min = 132.dp)
                                    .background(
                                        if (label in listOf("Morning ideas", "Quality guide"))
                                            Color(0xFFFFF0E4)
                                        else Color(0xFFDDF1E9),
                                        RoundedCornerShape(16.dp),
                                    )
                                    .clickable { say(text) }
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                AssetImage(
                                    "assets/$asset",
                                    null,
                                    Modifier.fillMaxWidth().height(78.dp),
                                )
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Caption(
                                    when (label) {
                                        "Morning ideas" -> "Pick your ingredients"
                                        "Pair my basket" -> "Good things together"
                                        "Delivery help" -> "Order & support help"
                                        else -> "Know your everyday"
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton({ go("routine") }) {
                    Text(
                        if (session.goal.enabled) "${session.goal.protein}g goal"
                        else "Set my goal",
                        fontSize = 12.sp,
                    )
                }
                OutlinedButton({ say("Review my basket") }) { Text("My basket", fontSize = 12.sp) }
                OutlinedButton({ say("Review my weekly nutrition") }) {
                    Text("My week", fontSize = 12.sp)
                }
            }
            session.assistantMessages.takeLast(6).forEach { message ->
                Column(
                    Modifier.align(if (message.fromCustomer) Alignment.End else Alignment.Start)
                        .fillMaxWidth(if (message.fromCustomer) .86f else 1f)
                        .background(
                            if (message.fromCustomer) Color(0xFFE5F2EA) else Color(0xFFF7F9F6),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!message.fromCustomer)
                        Text("Delight", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(message.text, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
            if (typing) Caption("Delight is preparing a reply …")
            if (!typing)
                reply?.let { answer ->
                    when (answer.action) {
                        "quality" ->
                            CountryCard(color = Color(0xFFFFF9F4)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Heading("A little check, every delivery")
                                        Caption("Seal · pack condition · use-by date")
                                    }
                                    AssetImage(
                                        "assets/generated/commerce-2026-v3/delight-quality-check.png",
                                        null,
                                        Modifier.size(80.dp, 115.dp),
                                    )
                                }
                            }
                        "month" ->
                            OutlinedButton({ go("calendar") }, Modifier.fillMaxWidth()) {
                                Text("Monthly nutrition report")
                            }
                        "support" -> {
                            if (answer.orderId.isNotEmpty()) {
                                TextButton({ go("order/${answer.orderId}") }) {
                                    Text("View order ${answer.orderId} →")
                                }
                                PrimaryButton(
                                    if (savedRequest.isEmpty()) "Save help request"
                                    else "Request saved",
                                    enabled = savedRequest.isEmpty(),
                                ) {
                                    val id =
                                        "SR-${System.currentTimeMillis().toString(36).uppercase()}"
                                    vm.update {
                                        it.copy(
                                            requests =
                                                listOf(
                                                    SupportRequest(
                                                        id,
                                                        "Other",
                                                        it.supportDraft,
                                                        answer.orderId,
                                                    )
                                                ) + it.requests
                                        )
                                    }
                                    savedRequest = id
                                }
                            }
                            TextButton({ go("support?order=${answer.orderId}") }) {
                                Text("Customer care")
                            }
                        }
                    }
                    if (answer.detail == "basket")
                        CountryCard {
                            Heading("Your basket")
                            session.cart.forEach { line ->
                                val product = state.products.first { it.id == line.productId }
                                BillRow(
                                    "${line.qty} × ${product.name}",
                                    money(lineTotal(line, product)),
                                )
                            }
                            TextButton({ go("cart") }) { Text("Review basket") }
                            TextButton({ go("nutrition") }) { Text("Nutrition details") }
                        }
                    if (answer.detail == "week") ScheduledNutrition(state, go)
                    if (answer.products.isNotEmpty()) {
                        Heading("Your ingredients")
                        Caption("Subscribe from tomorrow. Manage plans after checkout.")
                        Options(listOf("Daily", "Weekly"), frequency) { frequency = it }
                        answer.products.forEach { product ->
                            CountryCard {
                                Row(
                                    Modifier.fillMaxWidth().clickable { preview = product },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AssetImage(
                                        productImage(product),
                                        product.name,
                                        Modifier.size(56.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            product.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                        )
                                        Caption(
                                            "${product.unit} · ${money(product.price)} per pack"
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Quantity(
                                        quantities[product.id] ?: 1,
                                        { quantities = quantities + (product.id to it) },
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton({ add(product, false) }) { Text("Add once") }
                                    TextButton({ add(product, true) }) { Text("Subscribe") }
                                }
                            }
                        }
                    }
                }
        }
        HorizontalDivider(color = Line)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BasicTextField(
                input,
                { input = it.take(2000) },
                textStyle =
                    TextStyle(
                        fontFamily = Onest,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Ink,
                    ),
                maxLines = 4,
                modifier =
                    Modifier.weight(1f)
                        .heightIn(min = 44.dp, max = 96.dp)
                        .border(1.dp, Line, RoundedCornerShape(22.dp))
                        .background(Color.White, RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                decorationBox = { field ->
                    Box {
                        if (input.isEmpty()) Text("Ask Delight…", fontSize = 14.sp, color = Muted)
                        field()
                    }
                },
            )
            Button(
                { say(input.trim()) },
                enabled = input.isNotBlank() && !typing,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("↑", fontSize = 24.sp)
            }
        }
    }
    preview?.let { product ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(product.name) },
            text = {
                Column {
                    AssetImage(
                        productImage(product),
                        product.name,
                        Modifier.fillMaxWidth().height(150.dp),
                    )
                    Caption("${product.unit} · ${money(product.price)} per pack")
                    val nutrition = packNutrients(product.id, product.unit)
                    Caption(
                        if (nutrition == null) "Nutrition unavailable for this pack"
                        else
                            "About ${nutrition.protein.toInt()}g protein · ${nutrition.fiber.toInt()}g fibre per whole pack"
                    )
                }
            },
            confirmButton = {
                TextButton({
                    add(product, false)
                    preview = null
                }) {
                    Text("Add to basket · ${quantities[product.id] ?: 1} pack(s)")
                }
            },
            dismissButton = { TextButton({ preview = null }) { Text("Close") } },
        )
    }
}
