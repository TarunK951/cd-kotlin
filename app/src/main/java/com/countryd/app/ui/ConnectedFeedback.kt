package com.countryd.app.ui

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
fun ConnectedSignIn(state: FeedbackConnection, model: FeedbackConnectionModel, demo: () -> Unit) {
    val focus = LocalFocusManager.current
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var resend by rememberSaveable { mutableIntStateOf(0) }
    val otp = state.otp
    val length = otp?.num("otpLength")?.toInt() ?: 6
    LaunchedEffect(state.otpRequestCount) { resend = otp?.num("resendAfterSeconds")?.toInt() ?: 0 }
    LaunchedEffect(otp, resend) {
        if (otp != null && resend > 0) {
            delay(1000)
            resend--
        }
    }
    ScreenBody {
        Text("Welcome to Country Delight", style = MaterialTheme.typography.headlineSmall)
        if (state.restoring) {
            CircularProgressIndicator()
        } else if (otp == null) {
            EntryField("Mobile number", phone, { phone = it.filter(Char::isDigit).take(10) }, true)
            PrimaryButton("Continue", enabled = phone.length == 10 && !state.busy) {
                resend = 0
                focus.clearFocus()
                model.requestOtp(phone)
            }
            TextButton(
                onClick = model::restore,
                enabled = !state.busy && state.error.isNotBlank(),
            ) {
                Text("Retry saved sign-in")
            }
            TextButton(onClick = demo, enabled = !state.busy) { Text("Use local demo") }
        } else {
            Text("Verification code for ${otp.str("phone")}")
            EntryField(
                "Verification code",
                code,
                { code = it.filter(Char::isDigit).take(length) },
                true,
            )
            if (!otp.flag("registered")) {
                EntryField("Name", name, { name = it })
                EntryField("Area", area, { area = it })
                EntryField("City", city, { city = it })
            }
            val valid = otp.flag("registered") || listOf(name, area, city).all { it.isNotBlank() }
            PrimaryButton(
                "Verify & continue",
                enabled = valid && code.length == length && !state.busy,
            ) {
                focus.clearFocus()
                model.authenticate(code, name, area, city)
            }
            TextButton(
                enabled = !state.busy && resend == 0,
                onClick = { model.requestOtp(phone) },
            ) {
                Text(if (resend > 0) "Resend in ${resend}s" else "Resend code")
            }
            TextButton(
                enabled = !state.busy,
                onClick = {
                    code = ""
                    model.changePhone()
                },
            ) {
                Text("Change number")
            }
        }
        ConnectionStatus(state)
    }
}

@Composable
private fun ConnectionStatus(state: FeedbackConnection) {
    if (state.busy && !state.restoring) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (state.message.isNotBlank()) Text(state.message)
    if (state.error.isNotBlank()) Text(state.error, color = MaterialTheme.colorScheme.error)
}

@Composable
fun ConnectedHomeFeedback(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    open: () -> Unit,
) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.prompt?.let { prompt ->
            val name = prompt.str("productName")
            val subtitle = listOf(prompt.str("subtitle"), prompt.str("productUnit"))
                .filter(String::isNotBlank).joinToString(" · ")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF5F8F4),
                border = BorderStroke(1.dp, Line),
            ) {
                Column(
                    Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val photo = when (prompt.str("productId")) {
                            "GHEE" -> "products/photos/ghee.jpg"
                            "PANEER" -> "reference/paneer.jpg"
                            "MILK" -> "products/photos/cow-milk.jpg"
                            "DAHI" -> "products/photos/curd.jpg"
                            "BREAD" -> "products/photos/bread.jpg"
                            else -> null
                        }
                        FeedbackImage(prompt.str("imageUrl"), name, photo?.let { "assets/$it" })
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        TextButton(
                            onClick = { model.answer(null) },
                            enabled = !state.busy,
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(prompt.str("skipLabel").ifBlank { "Skip" }.uppercase(), maxLines = 1)
                        }
                    }
                    Text(
                        prompt.str("title"),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Row(Modifier.widthIn(max = 280.dp).fillMaxWidth()) {
                        for (rating in prompt.num("ratingMin").toInt()..prompt.num("ratingMax").toInt()) {
                            IconButton(
                                onClick = { model.answer(rating) },
                                enabled = !state.busy,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(CountryIcons.Star, "Rate $rating stars", Modifier.size(30.dp), tint = Muted)
                            }
                        }
                    }
                }
            }
        }
        ConnectionStatus(state)
        if (state.error.isNotBlank()) {
            TextButton(onClick = model::next, enabled = !state.busy) { Text("Retry feedback") }
        }
        TextButton(onClick = open) { Text("Buy again cart", maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedFeedbackSheet(state: FeedbackConnection, model: FeedbackConnectionModel) {
    val result = state.result
    val detail = state.detail
    if (result == null && detail == null) return
    ModalBottomSheet(onDismissRequest = { if (!state.busy) model.closeResult() }) {
        ScreenBody {
            when {
                result != null ->
                    key(result.num("feedbackId")) {
                        result.obj("issueForm")?.let { IssueFields(result, it, state.busy, model) }
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

@Composable
private fun IssueFields(
    result: JsonObject,
    form: JsonObject,
    busy: Boolean,
    model: FeedbackConnectionModel,
) {
    val id = result.num("feedbackId")
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) model.upload(id, it)
        }
    Text(form.str("question"), style = MaterialTheme.typography.titleLarge)
    form.list("options").forEach { option ->
        val code = option.str("code")
        val checked = code in selected
        val toggle = {
            selected =
                when {
                    checked -> selected - code
                    form.flag("multiSelect") -> selected + code
                    else -> setOf(code)
                }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (form.flag("multiSelect")) Checkbox(checked, { toggle() }, enabled = !busy)
            else RadioButton(checked, toggle, enabled = !busy)
            Text(option.str("label"))
        }
    }
    if (form.flag("noteAllowed")) {
        EntryField("Note (optional)", note, { note = it.take(form.num("noteMaxLength").toInt()) })
    }
    if (form.flag("photoAllowed")) {
        TextButton(onClick = { picker.launch("image/*") }, enabled = !busy) {
            Text(if (result.str("photoUrl").isBlank()) "Attach photo" else "Replace attached photo")
        }
    }
    PrimaryButton(
        "Submit feedback",
        enabled = !busy && selected.size >= form.num("minSelections"),
    ) {
        model.issues(id, selected.toList(), note)
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
    Text("Buy again", style = MaterialTheme.typography.titleLarge)
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
fun ConnectedAccount(state: FeedbackConnection, model: FeedbackConnectionModel) {
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
        ConnectionStatus(state)
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
