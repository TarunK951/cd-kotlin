package com.countryd.app.ui

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.Instant

@Composable
fun RatingStars(value: Int, onChange: (Int) -> Unit) {
    Row {
        (1..5).forEach { n ->
            IconButton({ onChange(n) }) {
                Icon(
                    if (n <= value) CountryIcons.StarFilled else CountryIcons.Star,
                    "$n star${if(n==1)"" else "s"}",
                    tint = if (n <= value) Color(0xFFE3A82F) else Muted,
                )
            }
        }
    }
}

@Composable
fun FeedbackPrompt(request: FeedbackRequest, vm: CountryViewModel, go: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(request.id) { if (!request.demo) vm.markFeedbackShown(request) }
    CountryCard(color = Color(0xFFEFF4E8)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.products
                .find { it.id == request.productId }
                ?.let {
                    AssetImage(
                        productImage(it),
                        it.name,
                        Modifier.size(46.dp)
                            .background(
                                Color.White,
                                androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            ),
                    )
                }
            Column(Modifier.weight(1f)) {
                Text(request.productName, fontWeight = FontWeight.SemiBold)
                Caption("Rate your product")
            }
            IconButton({ vm.dismissFeedback(request) }) {
                Icon(CountryIcons.Close, "Skip feedback reminder")
            }
        }
        PrimaryButton("Rate product") { go("feedback/${request.productId}") }
    }
}

@Composable
fun FeedbackSamplesScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    ScreenBody {
        Heading("Sample deliveries")
        Caption(
            "Try product feedback with these five delivered items. Samples never populate your order history."
        )
        state.sampleRequests.forEach { request ->
            val p = state.products.first { it.id == request.productId }
            val response =
                state.session.feedback.find { it.request.productId == p.id && it.request.demo }
            val subscribed =
                state.session.subscriptions.any { it.productId == p.id && it.status != "Past" }
            CountryCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssetImage(productImage(p), p.name, Modifier.size(57.dp, 64.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.SemiBold)
                        Caption("1 × ${request.unit} · ${money(p.price)}")
                        Caption(
                            "${request.deliveryDate} · ${request.area.removeSuffix(" · sample area")}"
                        )
                    }
                }
                if (subscribed) Caption("Subscribed product · no feedback requested.")
                else {
                    Caption(if (response == null) "Rate this product" else "Your product rating")
                    RatingStars(response?.rating ?: 0) { rating ->
                        vm.saveFeedback(
                            FeedbackResponse(
                                request,
                                rating,
                                emptyList(),
                                "",
                                Instant.now().toString(),
                                rating >= 4,
                            )
                        )
                        go("feedback/${p.id}?from=orders")
                    }
                    TextButton({ go("feedback/${p.id}?from=orders") }) {
                        Text(
                            if (response?.rating != null && response.rating >= 4) "Buy again"
                            else if (response != null) "View feedback" else "Rate product"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    id: String,
    from: String = "",
) {
    val p = state.products.find { it.id == id } ?: return
    val real =
        state.feedbackPrompt?.takeIf { it.productId == id }
            ?: feedbackRequests(state.session, state.products).find { it.productId == id }
    val stored = state.session.feedback.lastOrNull { it.request.productId == id }
    val request =
        real
            ?: stored?.request
            ?: if (BuildConfig.DEBUG) state.sampleRequests.find { it.productId == id } else null
    val existing = stored?.takeIf { it.request.id == request?.id }
    if (request == null) {
        ScreenBody {
            EmptyState("No feedback requested", "This product has no eligible delivered order.")
        }
        return
    }
    var rating by rememberSaveable { mutableIntStateOf(existing?.rating ?: 0) }
    var issues by remember { mutableStateOf(existing?.issues?.toSet() ?: emptySet()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var photo by remember { mutableStateOf(existing?.photo) }
    var saved by remember { mutableStateOf(existing != null) }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                        var name = "feedback-photo"
                        var size: Long? = null
                        context.contentResolver
                            .query(
                                uri,
                                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                                null,
                                null,
                                null,
                            )
                            ?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    name = cursor.getString(0)
                                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                                }
                            }
                        if ((size ?: 0L) > 10 * 1024 * 1024)
                            message = "Choose a photo smaller than 10 MB."
                        else {
                            photo =
                                FeedbackPhoto(
                                    uri.toString(),
                                    name,
                                    context.contentResolver.getType(uri) ?: "image/jpeg",
                                    size,
                                )
                            saved = false
                        }
                    }
                    .onFailure {
                        message = "Unable to attach this photo. Please choose another image."
                    }
            }
        }
    val subscribed = state.session.subscriptions.any { it.productId == id && it.status != "Past" }
    ScreenBody {
        if (request.demo && from != "orders")
            Options(
                state.sampleRequests.map { it.label.ifBlank { it.productName } },
                request.label.ifBlank { request.productName },
            ) { name ->
                go(
                    "feedback/${state.sampleRequests.first{it.label.ifBlank{it.productName}==name}.productId}"
                )
            }
        if (request.demo) Caption("SAMPLE DELIVERY · no real purchase")
        CountryCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssetImage(productImage(p), p.name, Modifier.size(64.dp))
                Column {
                    Heading(p.name)
                    Caption("${request.unit} · ${request.deliveryDate}")
                    Caption(request.area)
                }
            }
        }
        if (subscribed) {
            EmptyState(
                "Subscribed product",
                "Feedback is not requested for your active subscription.",
            )
            return@ScreenBody
        }
        Heading("How was your product?")
        RatingStars(rating) {
            rating = it
            issues = emptySet()
            note = ""
            photo = null
            vm.saveFeedback(
                FeedbackResponse(
                    request,
                    rating,
                    emptyList(),
                    "",
                    Instant.now().toString(),
                    rating >= 4,
                )
            )
            vm.clearFeedbackPrompt()
            saved = true
        }
        if (rating in 1..3) {
            Heading("What could be better?")
            request.issues.forEach { issue ->
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(
                            value = issue in issues,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                issues = if (checked) issues + issue else issues - issue
                                saved = false
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(issue in issues, null)
                    Text(issue)
                }
            }
        }
        OutlinedTextField(
            note,
            {
                note = it.take(1000)
                saved = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tell us more (optional)") },
            minLines = 3,
        )
        OutlinedButton({ picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) {
            Text(if (photo == null) "Attach a product photo" else "Change photo")
        }
        photo?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Caption(it.fileName, Modifier.weight(1f))
                TextButton({
                    photo = null
                    saved = false
                }) {
                    Text("Remove")
                }
            }
        }
        if (message.isNotEmpty()) Caption(message)
        PrimaryButton(
            if (saved) "Update feedback" else "Save feedback",
            enabled = rating in 1..5 && (rating >= 4 || issues.isNotEmpty()),
        ) {
            val response =
                FeedbackResponse(
                    request,
                    rating,
                    issues.toList(),
                    note.trim(),
                    Instant.now().toString(),
                    true,
                    photo,
                )
            vm.saveFeedback(response)
            vm.clearFeedbackPrompt()
            saved = true
            message =
                "Feedback saved on this device. Submission is pending until a backend is connected."
        }
        if (saved && rating >= 4 && request.canReorder) {
            Heading("Glad you enjoyed it")
            PrimaryButton("Buy again") {
                vm.add(CartLine(p.id, variant = p.defaultVariant))
                go("cart")
            }
        }
        TextButton({
            vm.dismissFeedback(request)
            go(if (from == "orders") "orders" else "home")
        }) {
            Text("Not now")
        }
        if (request.demo && from != "orders")
            TextButton({
                vm.update {
                    it.copy(
                        feedback = it.feedback.filterNot { old -> old.request.id == request.id }
                    )
                }
                rating = 0
                issues = emptySet()
                note = ""
                photo = null
                saved = false
                message = ""
            }) {
                Text("Try this sample again")
            }
        Caption("Sample data only. Cart actions are simulated and do not place an order.")
    }
}
