package com.countryd.app.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.countryd.app.api.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

@Composable
fun ConnectedSignIn(state: FeedbackConnection, model: FeedbackConnectionModel) {
    val focus = LocalFocusManager.current
    var welcome by rememberSaveable { mutableStateOf(true) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var resend by rememberSaveable { mutableIntStateOf(0) }
    val otp = state.otp
    val length = otp?.num("otpLength")?.toInt() ?: 6
    val registering = otp != null && !otp.flag("registered")
    val step =
        when {
            welcome -> "welcome"
            otp == null -> "mobile"
            else -> "otp"
        }
    LaunchedEffect(state.otpRequestCount) { resend = otp?.num("resendAfterSeconds")?.toInt() ?: 0 }
    LaunchedEffect(otp, resend) {
        if (otp != null && resend > 0) {
            delay(1000)
            resend--
        }
    }
    fun changeNumber() {
        focus.clearFocus()
        code = ""
        model.changePhone()
    }
    BackHandler(!welcome && !state.busy) { if (otp != null) changeNumber() else welcome = true }
    Box(
        Modifier.fillMaxSize().background(Color(0xFFFCFEFC)).safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (state.restoring) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 20.dp)) {
                key(step) {
                    Column(
                        Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AssetImage(
                            "native-art/$step.png",
                            null,
                            Modifier.fillMaxWidth().aspectRatio(if (registering) 2f else 1.4f),
                        )
                        Text(
                            when (step) {
                                "welcome" -> "Good mornings.\nDelivered."
                                "mobile" -> "Welcome to Country Delight."
                                else ->
                                    if (registering) "Create your account"
                                    else "Enter verification code"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        when (step) {
                            "welcome" -> {
                                Text("Your essentials. Your schedule.", color = Muted)
                                Text("Fresh essentials at your doorstep", color = Muted)
                                Text("Deliveries on the days you choose", color = Muted)
                            }
                            "mobile" -> {
                                Text("Sign in with your mobile number to continue.", color = Muted)
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                                    label = { Text("Mobile number") },
                                    prefix = { Text("+91 ") },
                                    supportingText = { Text("We’ll send a verification code.") },
                                    keyboardOptions =
                                        androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType =
                                                androidx.compose.ui.text.input.KeyboardType.Phone
                                        ),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            else -> {
                                Text(
                                    "Verification code for ${otp?.str("phone").orEmpty()}",
                                    color = Muted,
                                )
                                EntryField(
                                    "Verification code",
                                    code,
                                    { code = it.filter(Char::isDigit).take(length) },
                                    true,
                                )
                                if (registering) {
                                    EntryField("Name", name, { name = it.take(80) })
                                    SignupAreaPicker(area, city) { selectedArea, selectedCity ->
                                        area = selectedArea
                                        city = selectedCity
                                    }
                                }
                                TextButton(
                                    enabled = !state.busy && resend == 0,
                                    onClick = { model.requestOtp(phone) },
                                ) {
                                    Text(if (resend > 0) "Resend in ${resend}s" else "Resend code")
                                }
                                TextButton(enabled = !state.busy, onClick = ::changeNumber) {
                                    Text("Change number")
                                }
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ConnectionStatus(state)
                    when (step) {
                        "welcome" -> {
                            PrimaryButton("Get started", enabled = !state.busy, arrow = true) {
                                welcome = false
                            }
                            TextButton(onClick = { welcome = false }, enabled = !state.busy) {
                                Text("Sign in")
                            }
                        }
                        "mobile" -> {
                            PrimaryButton(
                                "Continue",
                                enabled = phone.length == 10 && !state.busy,
                                arrow = true,
                            ) {
                                focus.clearFocus()
                                model.requestOtp(phone)
                            }
                            if (state.error.isNotBlank()) {
                                TextButton(onClick = model::restore, enabled = !state.busy) {
                                    Text("Retry saved sign-in")
                                }
                            }
                        }
                        else -> {
                            val valid = !registering || (name.isNotBlank() && area.isNotBlank())
                            PrimaryButton(
                                "Verify & continue",
                                enabled = valid && code.length == length && !state.busy,
                                arrow = true,
                            ) {
                                focus.clearFocus()
                                model.authenticate(code, name, area, city)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val signupAreas =
    listOf(
        "Banjara Hills" to "Hyderabad",
        "Jubilee Hills" to "Hyderabad",
        "HSR Layout" to "Bengaluru",
        "Whitefield" to "Bengaluru",
        "Sector 45" to "Gurugram",
        "Sohna Road" to "Gurugram",
    )

@Composable
private fun SignupAreaPicker(area: String, city: String, onSelect: (String, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (area.isBlank()) "Choose your area" else "$area, $city")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            signupAreas.forEach { (optionArea, optionCity) ->
                DropdownMenuItem(
                    text = { Text("$optionArea, $optionCity") },
                    onClick = {
                        onSelect(optionArea, optionCity)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: FeedbackConnection) {
    if (state.busy && !state.restoring) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (state.message.isNotBlank()) Text(state.message)
    if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
}

@Composable
fun ConnectedHomeFeedback(state: FeedbackConnection, model: FeedbackConnectionModel) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.prompt?.let { FeedbackRatingCard(it, state.busy) { rating -> model.answer(rating) } }
        ConnectionStatus(state)
        if (state.error.isNotBlank()) {
            TextButton(onClick = model::next, enabled = !state.busy) { Text("Retry feedback") }
        }
    }
}

@Composable
internal fun FeedbackRatingCard(prompt: JsonObject, busy: Boolean, answer: (Int?) -> Unit) {

    val name = prompt.str("productName")
    val subtitle =
        listOf(prompt.str("subtitle"), prompt.str("productUnit"))
            .filter(String::isNotBlank)
            .joinToString(" · ")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF5F8F4),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FeedbackProductImage(prompt)
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Text(
                prompt.str("title"),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Start,
            )
            Row(Modifier.widthIn(max = 280.dp).fillMaxWidth()) {
                for (rating in prompt.num("ratingMin").toInt()..prompt.num("ratingMax").toInt()) {
                    IconButton(
                        onClick = { answer(rating) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            CountryIcons.Star,
                            "Rate $rating stars",
                            Modifier.size(30.dp),
                            tint = Muted,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tap a star · 1–5",
                    Modifier.weight(1f),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = { answer(null) }, enabled = !busy) {
                    Text(prompt.str("skipLabel").ifBlank { "Skip" })
                }
            }
        }
    }
}

@Composable
private fun FeedbackProductImage(prompt: JsonObject) {
    val photo =
        when (prompt.str("productId")) {
            "GHEE" -> "products/photos/ghee.jpg"
            "PANEER" -> "reference/paneer.jpg"
            "MILK" -> "products/photos/cow-milk.jpg"
            "DAHI" -> "products/photos/curd.jpg"
            "BREAD" -> "products/photos/bread.jpg"
            "ATTA" -> "categories/grains.png"
            else -> null
        }
    FeedbackImage(prompt.str("imageUrl"), prompt.str("productName"), photo?.let { "assets/$it" })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedFeedbackSheet(state: FeedbackConnection, model: FeedbackConnectionModel) {
    val result = state.result
    val detail = state.detail
    if (result == null && detail == null) return
    ModalBottomSheet(onDismissRequest = { if (!state.busy) model.closeResult() }) {
        if (result != null && result.obj("issueForm") != null) {
            key(result.num("feedbackId")) {
                IssueFields(result, requireNotNull(result.obj("issueForm")), state, model)
            }
            return@ModalBottomSheet
        }
        ScreenBody {
            when {
                result != null ->
                    key(result.num("feedbackId")) {
                        result.obj("buyAgain")?.let {
                            BuyAgainFields(result, it, state.busy, model)
                        }
                    }
                detail != null -> {
                    Text("Feedback submitted", style = MaterialTheme.typography.titleLarge)
                    Text("${detail.str("productName")} · Rating: ${detail.num("rating")}")
                    detail.list("issues").forEach { Text(it.str("label")) }
                    FeedbackImage(detail.str("photoUrl"), "Submitted photo")
                    detail.str("note").takeIf(String::isNotBlank)?.let { Text(it) }
                    TextButton(
                        onClick = { model.feedback(detail.num("feedbackId")) },
                        enabled = !state.busy,
                    ) {
                        Text("Refresh submitted feedback")
                    }
                    PrimaryButton("Done", enabled = !state.busy) { model.closeResult() }
                }
            }
            ConnectionStatus(state)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueFields(
    result: JsonObject,
    form: JsonObject,
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
) {
    val busy = state.busy
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) photo = it
        }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.86f).imePadding().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Share feedback", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = model::closeResult, enabled = !busy) {
                Icon(CountryIcons.Close, "Close feedback")
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            result.obj("ratedProduct")?.let { FeedbackProductSummary(it) }
            Text("Your rating · ${result.num("rating")} of 5")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (star in 1..5) {
                    Icon(
                        if (star <= result.num("rating")) CountryIcons.StarFilled
                        else CountryIcons.Star,
                        null,
                        Modifier.size(24.dp),
                        tint = Green,
                    )
                }
            }
            Text(form.str("question"), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                form.list("options").forEach { option ->
                    val code = option.str("code")
                    val checked = code in selected
                    FilterChip(
                        checked,
                        {
                            selected =
                                when {
                                    checked -> selected - code
                                    form.flag("multiSelect") -> selected + code
                                    else -> listOf(code)
                                }
                        },
                        { Text(option.str("label")) },
                        enabled = !busy,
                    )
                }
            }
            if (form.flag("noteAllowed")) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(form.num("noteMaxLength").toInt()) },
                    label = { Text("Anything else? (optional)") },
                    placeholder = { Text("Tell us a little more…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
            }
            if (form.flag("photoAllowed")) {
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (photo == null) "Add a photo (optional)" else "Replace attached photo")
                }
                if (photo != null)
                    TextButton(onClick = { photo = null }, enabled = !busy) { Text("Remove photo") }
                Text(
                    "One photo only. You can submit without a photo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
        Button(
            onClick = { model.issues(result.num("feedbackId"), selected, note, photo) },
            enabled = !busy && selected.size >= form.num("minSelections"),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 48.dp),
        ) {
            Text("Submit feedback")
        }
    }
}

@Composable
private fun FeedbackProductSummary(prompt: JsonObject) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FeedbackProductImage(prompt)
        Column(Modifier.weight(1f)) {
            Text(prompt.str("productName"), style = MaterialTheme.typography.titleMedium)
            Text(
                listOf(prompt.str("subtitle"), prompt.str("productUnit"))
                    .filter(String::isNotBlank)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun BuyAgainFields(
    result: JsonObject,
    offer: JsonObject,
    busy: Boolean,
    model: FeedbackConnectionModel,
) {
    var quantity by rememberSaveable {
        mutableIntStateOf(offer.num("defaultQuantity").toInt().coerceIn(1, 20))
    }
    var date by rememberSaveable { mutableStateOf(offer.str("suggestedDate")) }
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Buy again", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = model::closeResult, enabled = !busy) {
            Icon(CountryIcons.Close, "Close Buy again")
        }
    }
    Text(
        "Feedback saved. Ordering again is optional.",
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
    )
    result.obj("ratedProduct")?.let { FeedbackProductSummary(it) }
    Text("${offer.str("productName")} · ${offer.str("unit")} · ₹${offer.str("price")}")
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton({ quantity-- }, enabled = !busy && quantity > 1) { Text("−") }
        Text("$quantity")
        TextButton({ quantity++ }, enabled = !busy && quantity < 20) { Text("+") }
    }
    Text("Delivery date: $date")
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        offer.list("quickDates").forEach { option ->
            FilterChip(
                date == option.str("date"),
                { date = option.str("date") },
                { Text(option.str("label")) },
                enabled = !busy,
            )
        }
    }
    TextButton(
        enabled = !busy,
        onClick = {
            val current = LocalDate.parse(date)
            DatePickerDialog(
                    context,
                    { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                    current.year,
                    current.monthValue - 1,
                    current.dayOfMonth,
                )
                .apply {
                    val zone = java.time.ZoneId.systemDefault()
                    datePicker.minDate =
                        LocalDate.parse(offer.str("minDate"))
                            .atStartOfDay(zone)
                            .toInstant()
                            .toEpochMilli()
                    datePicker.maxDate =
                        LocalDate.parse(offer.str("maxDate"))
                            .atStartOfDay(zone)
                            .toInstant()
                            .toEpochMilli()
                }
                .show()
        },
    ) {
        Text("Choose another date")
    }
    PrimaryButton("Add to Buy again cart", enabled = !busy && date.isNotBlank()) {
        model.add(offer.str("productId"), quantity, date, result.num("feedbackId"))
    }
}

@Composable
fun ConnectedAccount(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    openOrders: () -> Unit,
) {
    var product by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        model.state.first { !it.busy }
        model.cart()
    }
    ScreenBody {
        Text(state.customer?.str("name").orEmpty(), style = MaterialTheme.typography.titleLarge)
        Text(state.customer?.str("phone").orEmpty())
        Text("Buy again cart", style = MaterialTheme.typography.titleMedium)
        val cart = state.cart
        if (cart != null) {
            val items = cart.list("items")
            if (items.isEmpty()) Text("Your Buy again cart is empty.")
            items.forEach { item ->
                Text("${item.str("productName")} · ${item.num("quantity")} × ${item.str("unit")}")
                Text("${item.str("deliveryDate")} · ₹${item.str("lineTotal")}")
                TextButton(
                    onClick = { model.remove(item.num("cartItemId")) },
                    enabled = !state.busy,
                ) {
                    Text("Remove")
                }
            }
            if (items.isNotEmpty()) Text("Total ₹${cart.str("totalAmount")}")
        }
        if (cart != null && cart.list("items").isNotEmpty()) {
            val dates = cart.list("items").map { it.str("deliveryDate") }.distinct()
            PrimaryButton("Place order", enabled = !state.busy) { model.placeOrder() }
            if (dates.size > 1) {
                dates.forEach { date ->
                    TextButton(onClick = { model.placeOrder(date) }, enabled = !state.busy) {
                        Text("Order items for $date")
                    }
                }
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = openOrders) { Text("My orders") }
        TextButton(onClick = model::cart, enabled = !state.busy) { Text("Refresh cart") }
        TextButton(
            onClick = {
                product = ""
                model.products()
            },
            enabled = !state.busy,
        ) {
            Text("Load feedback products")
        }
        state.products.forEach { item ->
            TextButton(
                onClick = {
                    product = item.str("productId")
                    model.options(product)
                },
                enabled = !state.busy,
            ) {
                Text(item.str("name"))
            }
            if (product == item.str("productId") && !state.busy) {
                state.options.forEach { Text(it.str("label")) }
            }
        }
        TextButton(onClick = model::logout, enabled = !state.busy) { Text("Sign out") }
    }
}

@Composable
private fun FeedbackImage(path: String, description: String, fallback: String? = null) {
    if (path.isBlank() && fallback == null) return
    val bitmap by
        produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            val url =
                                URL(if (path.startsWith("/")) FeedbackApi.BASE + path else path)
                            require(url.protocol == "https")
                            val connection = url.openConnection() as HttpURLConnection
                            try {
                                connection.instanceFollowRedirects = false
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                if (connection.responseCode != 200) return@runCatching null
                                val bytes =
                                    connection.inputStream.use { input ->
                                        val output = java.io.ByteArrayOutputStream()
                                        val buffer = ByteArray(8192)
                                        while (true) {
                                            val read = input.read(buffer)
                                            if (read < 0) break
                                            require(output.size() + read <= 5 * 1024 * 1024)
                                            output.write(buffer, 0, read)
                                        }
                                        output.toByteArray()
                                    }
                                val options =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                options.inJustDecodeBounds = false
                                options.inSampleSize = 1
                                while (
                                    maxOf(options.outWidth, options.outHeight) /
                                        options.inSampleSize > 512
                                ) {
                                    options.inSampleSize *= 2
                                }
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                                    ?.asImageBitmap()
                            } finally {
                                connection.disconnect()
                            }
                        }
                        .getOrNull()
                }
        }
    val modifier = Modifier.size(68.dp).clip(RoundedCornerShape(10.dp))
    val image = bitmap
    if (image != null) Image(image, description, modifier, contentScale = ContentScale.Crop)
    else if (fallback != null) AssetImage(fallback, description, modifier, ContentScale.Crop)
}
