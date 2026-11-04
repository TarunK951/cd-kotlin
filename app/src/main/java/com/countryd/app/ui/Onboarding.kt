package com.countryd.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import com.countryd.app.CountryViewModel
import com.countryd.app.data.Address
import java.util.UUID
import kotlinx.coroutines.delay

@Composable
fun Onboarding(vm: CountryViewModel, onComplete: () -> Unit = {}) {
    var step by rememberSaveable { mutableStateOf("welcome") }
    var phone by rememberSaveable { mutableStateOf("9000000000") }
    var code by rememberSaveable { mutableStateOf("123456") }
    var name by rememberSaveable { mutableStateOf("Demo Tester") }
    var house by rememberSaveable { mutableStateOf("Flat 402, Test Building, Banjara Hills") }
    var pin by rememberSaveable { mutableStateOf("500034") }
    var error by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableIntStateOf(25) }
    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(step, seconds) {
        if (step == "otp" && seconds > 0) {
            delay(1000)
            seconds--
        }
    }
    fun go(next: String) {
        error = ""
        step = next
    }
    BackHandler(step != "welcome") {
        go(
            when (step) {
                "mobile" -> "welcome"
                "otp" -> "mobile"
                "address",
                "unavailable" -> "otp"
                "vip" -> "address"
                else -> "vip"
            }
        )
    }
    val artHeight =
        mapOf(
            "welcome" to 330,
            "mobile" to 262,
            "otp" to 296,
            "address" to 236,
            "vip" to 232,
            "reward" to 352,
        )[step]
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color(0xFFFCFEFC)).safeDrawingPadding().imePadding()
    ) {
        val width = minOf(maxWidth, 430.dp)
        AssetImage(
            "native-art/footer-hills.png",
            null,
            Modifier.align(Alignment.BottomCenter).width(width).height(width * 70 / 390),
        )
        Column(Modifier.width(width).fillMaxHeight().align(Alignment.TopCenter)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (artHeight != null)
                    AssetImage(
                        "native-art/$step.png",
                        null,
                        Modifier.fillMaxWidth().height(width * artHeight / 390),
                    )
                Column(
                    Modifier.padding(horizontal = if (step == "vip") 16.dp else 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (step) {
                        "welcome" -> {
                            Text(
                                buildAnnotatedString {
                                    append("Good mornings")
                                    withStyle(SpanStyle(color = Green)) { append(".") }
                                    append("\nDelivered")
                                    withStyle(SpanStyle(color = Green)) { append(".") }
                                },
                                style = MaterialTheme.typography.displaySmall,
                            )
                            Text(
                                "Your essentials. Your schedule.",
                                fontSize = 17.sp,
                                lineHeight = 24.sp,
                                color = Muted,
                            )
                            Column(
                                Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                listOf(
                                        CountryIcons.Leaf to "Fresh essentials at your doorstep",
                                        CountryIcons.Calendar to
                                            "Deliveries on the days you choose",
                                        CountryIcons.Settings to "Manage everything in one place",
                                    )
                                    .forEach { (icon, text) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            Box(
                                                Modifier.size(40.dp)
                                                    .background(Color(0xFFDDF3E4), CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    icon,
                                                    null,
                                                    tint = Green,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                            Text(text, fontSize = 15.sp)
                                        }
                                    }
                            }
                        }
                        "mobile" -> {
                            Text(
                                "Welcome to\nCountry Delight.",
                                style = MaterialTheme.typography.displaySmall,
                                fontSize = 32.sp,
                            )
                            Text(
                                "Sign in with your mobile number to continue.",
                                fontSize = 17.sp,
                                lineHeight = 24.sp,
                                color = Muted,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Mobile number", fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                phone,
                                {
                                    phone = it.filter(Char::isDigit).take(10)
                                    error = ""
                                },
                                prefix = { Text("+91  │ ") },
                                placeholder = { Text("9000000000") },
                                singleLine = true,
                                keyboardOptions =
                                    KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Caption("We’ll send a verification code.")
                        }
                        "otp" -> {
                            Text(
                                "Enter verification code",
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = 30.sp,
                            )
                            Text(
                                "We’ve sent a 6-digit code to\n+91 $phone",
                                fontSize = 17.sp,
                                color = Muted,
                            )
                            OtpInput(code, error.isNotBlank()) {
                                code = it
                                error = ""
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Caption("Didn’t receive the code?", Modifier.weight(1f))
                                TextButton(
                                    {
                                        seconds = 25
                                        code = ""
                                    },
                                    enabled = seconds == 0,
                                ) {
                                    Text(
                                        if (seconds > 0)
                                            "Resend code (00:${seconds.toString().padStart(2,'0')})"
                                        else "Resend code",
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                            TextButton(
                                { go("mobile") },
                                Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text("Change number")
                            }
                            Caption("Demo verification · no SMS is sent.")
                        }
                        "address" -> {
                            EntryField(
                                "Full name",
                                name,
                                {
                                    name = it
                                    error = ""
                                },
                            )
                            EntryField(
                                "Flat / Street / Building",
                                house,
                                {
                                    house = it
                                    error = ""
                                },
                            )
                            EntryField(
                                "Postcode",
                                pin,
                                {
                                    pin = it.filter(Char::isDigit).take(6)
                                    error = ""
                                },
                                true,
                            )
                            Caption("We will check delivery availability when you continue.")
                        }
                        "unavailable" -> {
                            Spacer(Modifier.height(24.dp))
                            Icon(CountryIcons.MapPin, null, Modifier.size(80.dp), tint = Gold)
                            Heading("Freshness is on its way")
                            Text(
                                "Delivery is unavailable for postcode $pin. Try another postcode to find your nearest service area."
                            )
                        }
                        "vip" -> {
                            Text(
                                "YOUR VIP PRICE ADVANTAGE",
                                color = Color(0xFF8A5A00),
                                fontSize = 10.sp,
                            )
                            Text(
                                "Everyday essentials,\nbetter prices",
                                style = MaterialTheme.typography.headlineLarge,
                                fontSize = 27.sp,
                            )
                            Caption("Same fresh products. More value with VIP.")
                            listOf(
                                    Triple("Creamy Cow Milk", "450 ml", 45 to 50),
                                    Triple("Ghar Jaisa Dahi", "400 g", 62 to 69),
                                    Triple("Taaza Paneer", "180 g", 83 to 92),
                                    Triple("Protein White Eggs", "6 pieces", 65 to 72),
                                )
                                .forEachIndexed { i, (title, size, prices) ->
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .background(
                                                Color(0xFFF3F8F4),
                                                RoundedCornerShape(14.dp),
                                            )
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        AssetImage(
                                            "assets/ftue/${listOf("milk","dahi","paneer","eggs")[i]}-cutout.png",
                                            title,
                                            Modifier.size(52.dp, 62.dp),
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Caption(size)
                                        }
                                        Column {
                                            Caption("Regular")
                                            Text("₹${prices.second}")
                                        }
                                        Column(
                                            Modifier.background(
                                                    Color(0xFFFBE8A6),
                                                    RoundedCornerShape(12.dp),
                                                )
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Caption("VIP")
                                            Text(
                                                "₹${prices.first}",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                            )
                                        }
                                    }
                                }
                        }
                        "reward" -> RewardDetails()
                    }
                    if (error.isNotBlank())
                        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(20.dp))
            }
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val label =
                    mapOf(
                        "welcome" to "Get started",
                        "mobile" to "Continue",
                        "otp" to "Verify & continue",
                        "address" to "Continue",
                        "unavailable" to "Try another postcode",
                        "vip" to "Unlock my free VIP",
                        "reward" to "Explore milk products",
                    )[step]!!
                PrimaryButton(label, gold = step == "vip", arrow = true) {
                    when (step) {
                        "welcome" -> go("mobile")
                        "mobile" ->
                            if (!Regex("[6-9][0-9]{9}").matches(phone))
                                error = "Enter a valid 10-digit mobile number."
                            else {
                                seconds = 25
                                go("otp")
                            }
                        "otp" ->
                            if (code.length != 6) error = "Enter the 6-digit code."
                            else go("address")
                        "address" ->
                            when {
                                pin == "000000" -> go("unavailable")
                                name.trim().length < 2 -> error = "Enter your full name."
                                house.trim().length < 2 ->
                                    error = "Add your flat, street and building."
                                !Regex("[1-9][0-9]{5}").matches(pin) ->
                                    error = "Enter a valid 6-digit postcode."
                                else -> {
                                    vm.saveAddress(
                                        Address(
                                            UUID.randomUUID().toString(),
                                            "Home",
                                            name.trim(),
                                            house.trim(),
                                            "",
                                            pin,
                                        )
                                    )
                                    go("vip")
                                }
                            }
                        "unavailable" -> go("address")
                        "vip" -> {
                            vm.update {
                                it.copy(
                                    vipExpiresAt =
                                        if (it.vipExpiresAt == 0L)
                                            System.currentTimeMillis() + 30L * 86400000
                                        else it.vipExpiresAt
                                )
                            }
                            celebrating = true
                        }
                        "reward" -> {
                            vm.update { it.copy(loggedIn = true, phone = phone, name = name) }
                            onComplete()
                        }
                    }
                }
                if (step == "welcome")
                    TextButton({ go("mobile") }) { Text("Sign in", fontSize = 16.sp) }
                if (step == "mobile")
                    Caption(
                        "By continuing you agree to our Terms and Privacy Policy.",
                        Modifier.padding(top = 10.dp),
                    )
                if (step == "address")
                    Caption("Your address is safe with us", Modifier.padding(top = 8.dp))
            }
        }
        if (celebrating)
            VipCelebration {
                celebrating = false
                step = "reward"
            }
    }
}

@Composable
fun EntryField(label: String, value: String, onChange: (String) -> Unit, numeric: Boolean = false) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value,
            onChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            keyboardOptions =
                KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                ),
        )
    }
}
