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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.countryd.app.*
import com.countryd.app.data.*
import com.countryd.app.domain.*
import java.time.Instant

@Composable
fun RatingStars(value: Int, onChange: ((Int) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { n ->
            val description = "$n star${if (n == 1) "" else "s"}"
            val icon = if (n <= value) CountryIcons.StarFilled else CountryIcons.Star
            val tint = if (n <= value) Green else Muted
            if (onChange != null) {
                IconButton(onClick = { onChange(n) }, modifier = Modifier.weight(1f)) {
                    Icon(icon, description, Modifier.size(30.dp), tint = tint)
                }
            } else {
                Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        if (n == value) "$value out of 5 stars" else null,
                        Modifier.size(26.dp),
                        tint = tint,
                    )
                }
            }
        }
    }
}

internal fun feedbackRoute(request: FeedbackRequest, from: String = "") =
    "feedback/${request.productId}?from=$from&requestId=${android.net.Uri.encode(request.id)}"

internal fun feedbackLabel(request: FeedbackRequest) =
    request.label.ifBlank {
        request.productName.removePrefix("Fresh ").removePrefix("Whole Wheat ")
    }

@Composable
fun FeedbackPrompt(request: FeedbackRequest, vm: CountryViewModel, go: (String) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    CountryCard(color = Color(0xFFF5F8F4)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.products
                .find { it.id == request.productId }
                ?.let { AssetImage(productImage(it), it.name, Modifier.size(56.dp)) }
            Column(Modifier.weight(1f)) {
                Text(request.productName, fontWeight = FontWeight.SemiBold)
                Caption("Delivered ${feedbackDate(request.deliveryDate)} · ${request.unit}")
            }
            TextButton(
                { vm.dismissFeedback(request) },
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Text("SKIP", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            "How was your ${feedbackLabel(request)}?",
            Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        RatingStars(0) { rating ->
            vm.markFeedbackShown(request)
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
            go(feedbackRoute(request))
        }
    }
}

@Composable
fun FeedbackSamplesScreen(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    ScreenBody { FeedbackSampleCards(state, vm, go) }
}

@Composable
fun FeedbackSampleCards(state: CountryState, vm: CountryViewModel, go: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        go(feedbackRoute(request, "orders"))
                    }
                    TextButton({ go(feedbackRoute(request, "orders")) }) {
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
    requestId: String = "",
    onBack: () -> Unit,
) {
    val p = state.products.find { it.id == id } ?: return
    val stored =
        state.session.feedback.lastOrNull {
            it.request.productId == id && (requestId.isBlank() || it.request.id == requestId)
        }
    val exact =
        state.session.orders
            .asSequence()
            .flatMap { orderFeedbackRequests(it, state.session, state.products).asSequence() }
            .firstOrNull { it.id == requestId && it.productId == id }
    val request =
        if (requestId.isNotBlank()) {
            exact
                ?: stored?.request
                ?: state.sampleRequests.find { BuildConfig.DEBUG && it.id == requestId }
        } else {
            state.feedbackPrompt?.takeIf { it.productId == id }
                ?: if (BuildConfig.DEBUG) state.sampleRequests.find { it.productId == id } else null
        }
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
    var saved by rememberSaveable(request.id) { mutableStateOf(existing?.detailsComplete == true) }
    var reordering by rememberSaveable(request.id) { mutableStateOf(false) }
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
    val reset = {
        vm.update { session ->
            session.copy(feedback = session.feedback.filterNot { it.request.id == request.id })
        }
        rating = 0
        issues = emptySet()
        note = ""
        photo = null
        saved = false
        message = ""
    }
    fun useRating(newRating: Int) {
        if (rating == newRating) return
        rating = newRating
        val positive = rating >= 4
        vm.saveFeedback(
            FeedbackResponse(
                request,
                rating,
                if (positive) emptyList() else issues.toList(),
                if (positive) "" else note,
                Instant.now().toString(),
                positive,
                if (positive) null else photo,
            )
        )
        vm.clearFeedbackPrompt()
        saved = positive
    }
    if (saved && rating >= 4 && !subscribed) {
        FeedbackConfirmation(
            title = "Feedback saved",
            close = { if (reordering) reordering = false else onBack() },
            buyAgain = if (request.canReorder) ({ reordering = true }) else null,
            sheet =
                if (reordering)
                    ({
                        LocalFeedbackReorder(request, p, onBack = { reordering = false }) {
                            line,
                            date ->
                            vm.stageFeedbackReorder(line, date)
                            onBack()
                            go("cart")
                        }
                    })
                else null,
        ) {
            val name = state.session.name.trim().substringBefore(' ')
            Text(
                if (name.isBlank()) "Thanks for sharing!" else "Thanks for sharing, $name!",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Your $rating-star rating is saved. We’re glad you enjoyed your ${feedbackLabel(request)}.",
                color = Muted,
            )
            RatingStars(rating)
            CountryCard { LocalFeedbackSummary(request, p) }
            if (request.demo && from != "orders")
                TextButton(onClick = reset) { Text("Try this sample again") }
        }
        return
    }
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
        CountryCard { LocalFeedbackSummary(request, p) }
        if (subscribed) {
            EmptyState(
                "Subscribed product",
                "Feedback is not requested for your active subscription.",
            )
            return@ScreenBody
        }
        if (rating == 0) {
            Heading("How was your ${feedbackLabel(request)}?")
            RatingStars(rating, ::useRating)
            return@ScreenBody
        }
        if (saved) {
            Heading("Feedback saved")
            RatingStars(rating)
            issues.forEach { Text(it) }
            if (note.isNotBlank()) Text(note)
            photo?.let { Caption(it.fileName) }
            PrimaryButton("Continue shopping", onClick = onBack)
            if (request.demo && from != "orders")
                TextButton(onClick = reset) { Text("Try this sample again") }
            return@ScreenBody
        }
        Text("Your rating · $rating of 5", style = MaterialTheme.typography.titleSmall)
        RatingStars(rating, ::useRating)
        if (rating in 1..3) {
            Heading(
                when (rating) {
                    1 -> "We’re really sorry about this"
                    2 -> "Sorry, this fell short."
                    else -> "Good, but could be better."
                }
            )
            Caption("What wasn’t right? Pick all that apply.")
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
            Text(if (photo == null) "Add a photo (optional)" else "Replace attached photo")
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
        PrimaryButton("Submit feedback") {
            vm.saveFeedback(
                FeedbackResponse(
                    request,
                    rating,
                    issues.toList(),
                    note.trim(),
                    Instant.now().toString(),
                    true,
                    photo,
                )
            )
            vm.clearFeedbackPrompt()
            saved = true
        }
        TextButton(onClick = onBack) { Text("Not now") }
        if (request.demo && from != "orders")
            TextButton(onClick = reset) { Text("Try this sample again") }
    }
}

@Composable
private fun LocalFeedbackSummary(request: FeedbackRequest, product: Product) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AssetImage(productImage(product), product.name, Modifier.size(60.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            Caption("Delivered ${feedbackDate(request.deliveryDate)} · ${request.unit}")
            Caption(request.area)
        }
    }
}

@Composable
private fun LocalFeedbackReorder(
    request: FeedbackRequest,
    product: Product,
    onBack: () -> Unit,
    onContinue: (CartLine, String) -> Unit,
) {
    val tomorrow = java.time.LocalDate.now().plusDays(1)
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var date by rememberSaveable { mutableStateOf(tomorrow.toString()) }
    var subscription by rememberSaveable { mutableStateOf(false) }
    val variant = product.variants.firstOrNull { it.unit.equals(request.unit, ignoreCase = true) }
    val available = variant != null || product.unit.equals(request.unit, ignoreCase = true)
    val eligibleDate =
        runCatching { java.time.LocalDate.parse(date) >= tomorrow }.getOrDefault(false)
    TextButton(onClick = onBack) { Text("Back to feedback") }
    Heading("Buy again")
    Caption("Your feedback remains saved. This optional order is separate.")
    CountryCard { LocalFeedbackSummary(request, product) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Quantity · ${money(variant?.price ?: product.price)} / pack", Modifier.weight(1f))
        Quantity(qty, { qty = it }, maximum = if (subscription) 10 else 20)
    }
    if (product.frequency.isNotEmpty()) {
        Heading("How do you want it?")
        Options(
            listOf("Subscription", "Buy once"),
            if (subscription) "Subscription" else "Buy once",
        ) {
            subscription = it == "Subscription"
            if (subscription) qty = qty.coerceAtMost(10)
        }
    }
    Heading(if (subscription) "Start delivery on" else "Deliver on")
    FeedbackDateChoices(
        suggested = tomorrow.toString(),
        suggestedLabel = "Tomorrow",
        minimum = tomorrow.toString(),
        maximum = tomorrow.plusDays(30).toString(),
        date = date,
        enabled = true,
        onDate = { date = it },
    )
    if (subscription) Caption("Daily deliveries starting ${feedbackDate(date)}.")
    if (!available) Caption("This pack size is no longer available.")
    PrimaryButton("Continue to checkout", enabled = available && eligibleDate) {
        val plan = if (subscription) Plan(startDate = date) else null
        onContinue(
            CartLine(
                product.id,
                qty,
                if (subscription) "Subscribe" else "Once",
                variant,
                plan = plan,
            ),
            date,
        )
    }
}
