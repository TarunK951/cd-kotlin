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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.countryd.app.api.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    val prompt = state.prompt ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FeedbackRatingCard(prompt, state.busy) { rating -> model.answer(rating) }
        if (state.error.isNotBlank()) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun FeedbackRatingCard(prompt: JsonObject, busy: Boolean, answer: (Int?) -> Unit) {

    val name = prompt.str("productName")
    val deliveryDate = prompt.str("deliveryDate")
    val deliveryLabel =
        prompt.str("subtitle").ifBlank {
            if (deliveryDate.isBlank()) "" else "Delivered ${feedbackDate(deliveryDate)}"
        }
    val subtitle =
        listOf(deliveryLabel, prompt.str("productUnit"))
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
                TextButton(
                    onClick = { answer(null) },
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(
                        prompt.str("skipLabel").ifBlank { "Skip" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                prompt.str("title").ifBlank { "How was your $name?" },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 248.dp).fillMaxWidth()) {
                for (rating in 1..5) {
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
        }
    }
}

@Composable
internal fun FeedbackProductImage(prompt: JsonObject) {
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
fun ConnectedFeedbackSheet(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    openCart: () -> Unit,
    chooseAddress: () -> Unit,
    visible: Boolean = true,
) {
    val result = state.result
    val detail = state.detail
    if (detail != null) {
        if (!visible) return
        FeedbackConfirmation("Feedback saved", model::closeResult) {
            Text("Thank you for telling us.", style = MaterialTheme.typography.titleLarge)
            Text("Your feedback has been submitted to our quality team.", color = Muted)
            CountryCard {
                FeedbackProductSummary(detail)
                Text("Your saved rating · ${detail.num("rating")} of 5")
                FeedbackStars(detail.num("rating"))
                detail.list("issues").forEach { Text(it.str("label")) }
                detail.str("note").takeIf(String::isNotBlank)?.let { Text(it) }
                if (detail.str("photoUrl").isBlank()) Text("No photo attached", color = Muted)
                else FeedbackImage(detail.str("photoUrl"), "Submitted photo")
            }
        }
        return
    }
    if (result == null) return
    val issueDraft = rememberSaveableStateHolder()
    var openReorderAfterSave by rememberSaveable { mutableStateOf(false) }
    val form = result.obj("issueForm").takeIf { result.num("rating") in 1L..3L }
    if (form != null) {
        if (!visible) return
        Dialog(
            onDismissRequest = { if (!state.busy) model.closeResult() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = Color.White) {
                issueDraft.SaveableStateProvider(result.obj("ratedProduct")?.num("promptId") ?: 0L) {
                    IssueFields(result, state.issueReceipt?.obj("issueForm") ?: form, state, model)
                }
            }
        }
        return
    }
    if (result.flag("ratingDraft")) {
        if (!visible) return
        val rating = result.num("rating").toInt()
        FeedbackConfirmation(
            title = "Your rating",
            close = { if (!state.busy) model.savePositiveRating { model.closeResult() } },
            buyAgain = {
                if (!state.busy) model.savePositiveRating { saved ->
                    // The receipt opens the reorder sheet only when the server returns an offer.
                    if (saved.obj("buyAgain") != null) openReorderAfterSave = true
                }
            },
            successMessage = "Tap a star to change your rating.",
            draft = true,
        ) {
            Text("Your rating · $rating of 5", style = MaterialTheme.typography.titleMedium)
            RatingChoices(rating, !state.busy, model::editIssueRating)
            result.obj("ratedProduct")?.let { FeedbackProductSummary(it) }
            ConnectionStatus(state.copy(message = ""))
        }
        return
    }
    val offer = result.obj("buyAgain").takeIf { result.num("rating") in 4L..5L }
    var reorderOpen by rememberSaveable(result.num("feedbackId")) { mutableStateOf(false) }
    LaunchedEffect(openReorderAfterSave) {
        if (openReorderAfterSave) {
            reorderOpen = offer != null
            openReorderAfterSave = false
        }
    }
    val draft = rememberSaveableStateHolder()
    var subscriptionDate by rememberSaveable(result.num("feedbackId")) { mutableStateOf("") }
    var subscriptionError by rememberSaveable(result.num("feedbackId")) { mutableStateOf("") }
    var subscriptionQuantity by rememberSaveable(result.num("feedbackId")) { mutableIntStateOf(1) }
    if (!visible) return
    val subscriptionFailed = subscriptionError.isNotBlank()
    if (subscriptionDate.isNotEmpty()) {
        FeedbackConfirmation(
            title = if (subscriptionFailed) "Subscription failed" else "Subscription confirmed",
            close = model::closeResult,
            successMessage =
                if (subscriptionFailed) "Your feedback remains saved. You can try again."
                else "Your feedback remains saved",
            failed = subscriptionFailed,
            backToChoice = {
                subscriptionDate = ""
                reorderOpen = true
            },
        ) {
            Text(
                if (subscriptionFailed) "Subscription failed" else "Thank you for subscribing!",
                style = MaterialTheme.typography.titleLarge,
            )
            if (subscriptionFailed) Text(subscriptionError, color = Muted)
            else {
                Text("Starts ${feedbackDate(subscriptionDate)}", color = Muted)
                result.obj("ratedProduct")?.let { product ->
                    CountryCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FeedbackProductImage(product)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.str("productName"),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Caption("$subscriptionQuantity × ${product.str("productUnit")}")
                            }
                        }
                        state.activeDeliveryAddress()?.let { address ->
                            Caption(
                                listOf(
                                        address.str("line1"),
                                        address.str("area"),
                                        address.str("city"),
                                    )
                                    .filter(String::isNotBlank)
                                    .joinToString(", ")
                            )
                        }
                    }
                }
            }
        }
        return
    }
    FeedbackConfirmation(
        "Feedback saved",
        close = {
            if (reorderOpen) {
                if (!state.busy) reorderOpen = false
            } else model.closeResult()
        },
        buyAgain = if (offer != null) ({ reorderOpen = true }) else null,
        sheet =
            if (reorderOpen && offer != null)
                ({
                    draft.SaveableStateProvider("reorder") {
                        BuyAgainFields(
                            result,
                            offer,
                            state,
                            model,
                            close = { reorderOpen = false },
                            openCart = openCart,
                            chooseAddress = chooseAddress,
                            subscriptionResult = { date, quantity, error ->
                                subscriptionDate = date
                                subscriptionQuantity = quantity
                                subscriptionError = error
                                reorderOpen = false
                            },
                        )
                    }
                    ConnectionStatus(state.copy(message = ""))
                })
            else null,
    ) {
        val name = state.customer?.str("name").orEmpty()
        Text(
            if (name.isBlank()) "Thanks for sharing!" else "Thanks for sharing, $name!",
            style = MaterialTheme.typography.titleLarge,
        )
        val rating = result.num("rating")
        val product = result.obj("ratedProduct")
        val productName = product?.str("productName").orEmpty()
        val thanks =
            if (rating >= 4 && productName.isNotBlank()) {
                " We’re glad you enjoyed your $productName."
            } else {
                ""
            }
        Text("Your ${rating}-star rating is saved.$thanks", color = Muted)
        FeedbackStars(rating)
        product?.let {
            CountryCard {
                FeedbackProductSummary(product)
                product.str("area").takeIf(String::isNotBlank)?.let { Caption(it) }
            }
        }
    }
}

@Composable
internal fun FeedbackConfirmation(
    title: String,
    close: () -> Unit,
    buyAgain: (() -> Unit)? = null,
    successMessage: String = "Feedback saved successfully",
    viewOrders: (() -> Unit)? = null,
    failed: Boolean = false,
    draft: Boolean = false,
    backToChoice: (() -> Unit)? = null,
    sheet: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accent = if (failed) MaterialTheme.colorScheme.error else Green
    val badge = if (failed) MaterialTheme.colorScheme.errorContainer else Mint
    val statusIcon = if (failed) CountryIcons.Close else if (draft) CountryIcons.Star else CountryIcons.CircleCheck
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxSheetHeight = maxHeight * 0.88f
            Surface(Modifier.fillMaxSize(), color = Color.White) {
                val receipt = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                Column(if (sheet == null) receipt else receipt.clearAndSetSemantics {}) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = close) { Icon(CountryIcons.ChevronLeft, "Back") }
                        Text(
                            title,
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = close) { Icon(CountryIcons.Close, "Close") }
                    }
                    Column(
                        Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = badge,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(statusIcon, null, Modifier.size(15.dp), tint = accent)
                                Text(
                                    successMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = accent,
                                )
                            }
                        }
                        Surface(
                            modifier =
                                Modifier.align(Alignment.CenterHorizontally)
                                    .padding(top = 10.dp)
                                    .size(62.dp),
                            color = badge,
                            shape = RoundedCornerShape(50),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(statusIcon, null, Modifier.size(36.dp), tint = accent)
                            }
                        }
                        content()
                    }
                    Column(
                        Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (viewOrders != null)
                            OutlinedButton(viewOrders, Modifier.fillMaxWidth()) {
                                Text("View orders")
                            }
                        if (buyAgain != null) {
                            PrimaryButton("Buy again", onClick = buyAgain)
                            OutlinedButton(
                                onClick = close,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Continue shopping")
                            }
                        } else {
                            PrimaryButton("Continue shopping", onClick = close)
                        }
                        if (backToChoice != null) {
                            OutlinedButton(backToChoice, Modifier.fillMaxWidth()) {
                                Text("Back to subscription choice")
                            }
                        }
                    }
                }
            }
            if (sheet != null) {
                Box(
                    Modifier.matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(onClick = close)
                        .clearAndSetSemantics {}
                )
                Surface(
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .heightIn(max = maxSheetHeight),
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    color = Color.White,
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            Modifier.align(Alignment.CenterHorizontally)
                                .size(34.dp, 4.dp)
                                .background(Line, RoundedCornerShape(4.dp))
                        )
                        sheet()
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackStars(rating: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (star in 1..5) {
            Icon(
                if (star <= rating) CountryIcons.StarFilled else CountryIcons.Star,
                null,
                Modifier.size(24.dp),
                tint = if (star <= rating) Green else Muted,
            )
        }
    }
}

@Composable
private fun RatingChoices(rating: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (1..5).forEach { stars ->
            IconButton(onClick = { onSelect(stars) }, enabled = enabled) {
                Icon(CountryIcons.Star, "Select $stars stars", tint = if (stars <= rating) Green else Muted)
            }
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
    var editedRating by rememberSaveable { mutableStateOf<Int?>(null) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var note by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<Uri?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) { photo = it; model.removeIssuePhoto() }
        }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = model::closeResult, enabled = !busy) {
                Icon(CountryIcons.ChevronLeft, "Back")
            }
            Text(
                "Share feedback",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = model::closeResult, enabled = !busy) {
                Icon(CountryIcons.Close, "Close feedback")
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            result.obj("ratedProduct")?.let { product ->
                FeedbackProductSummary(product)
                Text(
                    "Feedback stays with this delivery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
            Text(
                "Your rating · ${result.num("rating")} of 5",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { FeedbackStars(result.num("rating")) }
                if (state.issueReceipt == null) {
                    TextButton(onClick = { editedRating = result.num("rating").toInt() }, enabled = !busy) {
                        Text("Edit rating")
                    }
                }
            }
            editedRating?.let { draftRating ->
                RatingChoices(draftRating, !busy) { stars ->
                    editedRating = null
                    model.editIssueRating(stars)
                }
                TextButton(onClick = { editedRating = null }) { Text("Cancel") }
            }
            val threeStars = result.num("rating") == 3L
            Text(
                when (result.num("rating")) {
                    1L -> "We’re really sorry about this"
                    2L -> "Sorry, this fell short."
                    else -> "Good, but could be better."
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (threeStars) "What would have made it a 4 or 5? Pick all that apply."
                else form.str("question"),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
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
                        { Text(option.str("label"), style = MaterialTheme.typography.labelMedium) },
                        leadingIcon =
                            if (checked) {
                                {
                                    Icon(
                                        CountryIcons.CircleCheck,
                                        null,
                                        Modifier.size(14.dp),
                                        tint = Green,
                                    )
                                }
                            } else null,
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Mint,
                                selectedLabelColor = Green,
                            ),
                        shape = RoundedCornerShape(50),
                        enabled = !busy,
                    )
                }
            }
            if (form.flag("noteAllowed")) {
                Text("Anything else? (optional)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(form.num("noteMaxLength").toInt().takeIf { limit -> limit > 0 } ?: 500) },
                    placeholder = { Text("Tell us a little more…") },
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F8F4),
                            unfocusedContainerColor = Color(0xFFF5F8F4),
                        ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
            }
            if (form.flag("photoAllowed") || photo != null) {
                if (photo == null) {
                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Add a photo (optional)") }
                    Caption("One JPEG, PNG, WebP or HEIC photo, up to 5 MB.")
                } else {
                    val progress = state.photoProgress
                    Text(
                        when {
                            state.photoFailed -> "Photo · Upload failed"
                            progress != null && progress >= 1f -> "Photo · Finishing upload"
                            progress != null -> "Photo · Uploading ${(progress * 100).toInt()}%"
                            else -> "Photo attached"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (progress != null) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                    if (state.photoFailed) {
                        Text(
                            "Photo couldn’t upload. Your rating and note are safe.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.photoFailed) {
                            Button(onClick = { model.issues(selected, note, photo) }, enabled = !busy) {
                                Text("Retry photo")
                            }
                        }
                        OutlinedButton(
                            onClick = { photo = null; model.removeIssuePhoto() },
                            enabled = !busy,
                        ) { Text("Remove") }
                    }
                }
            }
            if (busy && state.photoProgress == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Caption("Saving feedback… Please wait.")
            }
        }
        if (state.error.isNotBlank() && !state.photoFailed) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(
                        "Couldn’t save your feedback. Your draft is still here.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        state.error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Button(
            onClick = { model.issues(selected, note, photo) },
            enabled = !busy && editedRating == null && !state.photoFailed && selected.size >= form.num("minSelections"),
            colors =
                ButtonDefaults.buttonColors(
                    disabledContentColor = Muted,
                    disabledContainerColor = Line,
                ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 48.dp),
        ) {
            Text(if (busy) "Submitting…" else if (state.error.isBlank()) "Submit feedback" else "Retry Submit")
        }
        Text(
            "Your note and photo are optional.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

@Composable
internal fun FeedbackProductSummary(prompt: JsonObject) {
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
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    close: () -> Unit,
    openCart: () -> Unit,
    chooseAddress: () -> Unit,
    subscriptionResult: (String, Int, String) -> Unit,
) {
    val busy = state.busy
    val productId = offer.str("productId")
    val product =
        state.products.firstOrNull { it.str("productId") == productId }
            ?: state.product?.takeIf { it.str("productId") == productId }
    LaunchedEffect(productId) {
        if (product == null) {
            model.state.first { !it.busy }
            model.product(productId)
        }
    }
    var quantity by rememberSaveable {
        mutableIntStateOf(offer.num("defaultQuantity").toInt().coerceIn(1, 20))
    }
    var date by rememberSaveable { mutableStateOf(offer.str("suggestedDate")) }
    var subscription by rememberSaveable { mutableStateOf(false) }
    var submissionStart by rememberSaveable { mutableStateOf<Int?>(null) }
    val pending =
        state.subscriptionPending && state.failedSubscription?.str("productId") == productId
    val editable = !busy && !pending && submissionStart == null
    val address = state.activeDeliveryAddress()
    LaunchedEffect(submissionStart) {
        val before = submissionStart ?: return@LaunchedEffect
        val response = model.state.first { !it.busy }
        submissionStart = null
        if (response.subscriptionCreationCount > before) subscriptionResult(date, quantity, "")
        else if (!response.subscriptionPending)
            subscriptionResult(
                date,
                quantity,
                response.subscriptionFailure ?: response.error.ifBlank { "Try again." },
            )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Buy again", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = close, enabled = !busy) { Icon(CountryIcons.Close, "Close Buy again") }
    }
    Text(
        "Feedback saved. This optional order is separate.",
        style = MaterialTheme.typography.bodySmall,
        color = Muted,
    )
    result.obj("ratedProduct")?.let { FeedbackProductSummary(it) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Quantity · ₹${offer.str("price")} / pack",
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
        )
        Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { quantity-- },
                    enabled = editable && quantity > 1,
                    modifier = Modifier.width(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("−")
                }
                Text("$quantity", style = MaterialTheme.typography.titleSmall)
                TextButton(
                    onClick = { quantity++ },
                    enabled = editable && quantity < if (subscription) 10 else 20,
                    modifier = Modifier.width(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("+")
                }
            }
        }
    }
    if (product?.flag("subscribable") == true) {
        Text("How do you want it?", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = subscription,
                onClick = {
                    subscription = true
                    quantity = quantity.coerceAtMost(10)
                },
                label = { Text("Subscription") },
                enabled = editable,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = !subscription,
                onClick = { subscription = false },
                label = { Text("Buy Once") },
                enabled = editable,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Text("Deliver on", style = MaterialTheme.typography.titleSmall)
    val suggested = offer.str("suggestedDate")
    val quickLabel =
        offer
            .list("quickDates")
            .firstOrNull { it.str("date") == suggested }
            ?.str("label")
            .orEmpty()
            .ifBlank { "Suggested date" }
    FeedbackDateChoices(
        suggested = suggested,
        suggestedLabel = quickLabel,
        minimum = offer.str("minDate"),
        maximum = offer.str("maxDate"),
        date = date,
        enabled = editable,
        onDate = { date = it },
    )
    if (subscription && !pending && address?.isCompleteDeliveryAddress() != true) {
        TextButton(onClick = chooseAddress, enabled = editable) { Text("Choose delivery address") }
    }
    PrimaryButton(
        if (pending) "Check subscription status"
        else if (subscription) "Start subscription" else "Continue to checkout",
        enabled =
            !busy &&
                submissionStart == null &&
                date.isNotBlank() &&
                (pending || !subscription || address?.isCompleteDeliveryAddress() == true),
    ) {
        if (pending) {
            submissionStart = state.subscriptionCreationCount
            model.retrySubscription()
        } else if (subscription) {
            submissionStart = state.subscriptionCreationCount
            model.createSubscription(
                buildJsonObject {
                    put("productId", productId)
                    put("quantity", quantity)
                    put("startDate", date)
                }
            )
        } else {
            model.add(productId, quantity, date, result.num("feedbackId"), onAdded = openCart)
        }
    }
}

@Composable
fun ConnectedAccount(
    state: FeedbackConnection,
    model: FeedbackConnectionModel,
    chooseAddress: () -> Unit,
    openOrders: () -> Unit,
) {
    LaunchedEffect(Unit) {
        model.state.first { !it.busy }
        model.cart()
    }

    val cart = state.cart
    val items = cart?.list("items").orEmpty()
    val orderTotal = "₹${cart?.str("totalAmount").orEmpty()}"
    val address = state.activeDeliveryAddress()
    val complete = address?.isCompleteDeliveryAddress() == true
    val canEditCart = !state.busy && !state.checkoutPending
    val context = LocalContext.current
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (items.isNotEmpty()) {
                CountryCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Deliver to", style = MaterialTheme.typography.titleSmall)
                            Text(
                                address?.str("fullAddress")?.ifBlank { "Choose a delivery address" }
                                    ?: "Choose a delivery address",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        }
                        TextButton(onClick = chooseAddress, enabled = canEditCart) {
                            Text(if (address == null) "Choose" else "Change")
                        }
                    }
                }
                TextButton(
                    onClick = { confirmClear = true },
                    enabled = canEditCart,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Clear cart")
                }
            }
            if (cart != null && items.isEmpty()) {
                Text("Your cart is empty.", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = openOrders) { Text("My orders") }
            }

            items.forEach { item ->
                val quantity = item.num("quantity").toInt()
                val id = item.num("cartItemId")
                CountryCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                            FeedbackProductImage(item)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.str("productName"),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                item.str("unit"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        }
                        Text(
                            "₹${item.str("lineTotal")}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            enabled = canEditCart,
                            onClick = {
                                val today = LocalDate.now()
                                val min = today.plusDays(1)
                                val max = today.plusDays(30)
                                val initial =
                                    runCatching { LocalDate.parse(item.str("deliveryDate")) }
                                        .getOrDefault(min)
                                        .coerceIn(min, max)
                                val zone = java.time.ZoneId.systemDefault()
                                DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected =
                                                LocalDate.of(year, month + 1, day).toString()
                                            if (selected != item.str("deliveryDate"))
                                                model.updateCartItem(id, date = selected)
                                        },
                                        initial.year,
                                        initial.monthValue - 1,
                                        initial.dayOfMonth,
                                    )
                                    .apply {
                                        datePicker.minDate =
                                            min.atStartOfDay(zone).toInstant().toEpochMilli()
                                        datePicker.maxDate =
                                            max.atStartOfDay(zone).toInstant().toEpochMilli()
                                    }
                                    .show()
                            },
                        ) {
                            Text(feedbackDate(item.str("deliveryDate")))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                enabled = canEditCart,
                                onClick = {
                                    if (quantity <= 1) model.remove(id)
                                    else model.updateCartItem(id, quantity = quantity - 1)
                                },
                            ) {
                                Text("−")
                            }
                            Text(quantity.toString())
                            TextButton(
                                enabled = canEditCart && quantity < 20,
                                onClick = { model.updateCartItem(id, quantity = quantity + 1) },
                            ) {
                                Text("+")
                            }
                        }
                    }
                }
            }

            if (items.isNotEmpty() && state.cartRecommendations.isNotEmpty()) {
                Text("Goes well with your cart", style = MaterialTheme.typography.titleMedium)
                state.cartRecommendations.forEach { recommendation ->
                    CountryCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                FeedbackProductImage(recommendation)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    recommendation.str("productName"),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "${recommendation.str("unit")} · ₹${recommendation.str("price")}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    recommendation.str("reason"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                )
                            }
                            TextButton(
                                enabled = canEditCart,
                                onClick = {
                                    model.add(
                                        recommendation.str("productId"),
                                        1,
                                        recommendation.str("suggestedDate").ifBlank { null },
                                    )
                                },
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            if (items.isNotEmpty()) {
                CountryCard {
                    Text("Bill summary", style = MaterialTheme.typography.titleMedium)
                    items.forEach { item ->
                        BillRow(item.str("productName"), "₹${item.str("lineTotal")}")
                    }
                    HorizontalDivider()
                    BillRow("Order total", orderTotal, bold = true)
                }
                val dates = items.map { it.str("deliveryDate") }.distinct()
                if (dates.size > 1) {
                    dates.forEach { date ->
                        TextButton(
                            onClick = { if (complete) model.placeOrder(date) else chooseAddress() },
                            enabled = canEditCart,
                        ) {
                            Text("Order items for ${feedbackDate(date)}")
                        }
                    }
                }
            }
            if (items.isEmpty() && state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (items.isEmpty() && state.error.isNotBlank()) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = model::cart, enabled = canEditCart) { Text("Retry") }
            }
        }
        if (items.isNotEmpty() || state.checkoutPending) {
            Surface(color = Color.White) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (state.error.isNotBlank()) {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                    }
                    BillRow("Order total", orderTotal, bold = true)
                    PrimaryButton(
                        if (state.checkoutPending) "Check order status"
                        else if (complete) "Place order" else "Complete delivery address",
                        enabled = !state.busy,
                    ) {
                        if (state.checkoutPending) model.retryCheckout()
                        else if (complete) model.placeOrder() else chooseAddress()
                    }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear cart?") },
            text = { Text("All items will be removed from your cart.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        model.clearCart()
                    }
                ) {
                    Text("Clear cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Keep items") }
            },
        )
    }
}

@Composable
internal fun FeedbackImage(
    path: String,
    description: String,
    fallback: String? = null,
    imageModifier: Modifier = Modifier.size(68.dp),
    scale: ContentScale = ContentScale.Crop,
) {
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
    val modifier = imageModifier.clip(RoundedCornerShape(10.dp))
    val image = bitmap
    if (image != null) Image(image, description, modifier, contentScale = scale)
    else if (fallback != null) AssetImage(fallback, description, modifier, scale)
}
