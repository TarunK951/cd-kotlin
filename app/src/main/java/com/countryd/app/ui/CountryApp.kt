package com.countryd.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.countryd.app.*
import com.countryd.app.api.FeedbackConnectionModel
import com.countryd.app.data.*
import com.countryd.app.domain.*
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryApp(
    vm: CountryViewModel = viewModel(),
    connection: FeedbackConnectionModel = viewModel(),
) {
    val connected by connection.state.collectAsStateWithLifecycle()
    var demo by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    var dialog by remember { mutableStateOf<String?>(null) }
    var overlay by remember { mutableStateOf<String?>(null) }
    var confirmedOrder by
        androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var returnToCart by remember { mutableStateOf(false) }
    val cartState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    fun closeOverlay() {
        overlay = if (returnToCart && state.session.cart.isNotEmpty()) "cart" else null
        returnToCart = false
    }
    var showMilkOffer by remember { mutableStateOf(false) }
    fun onOrderPlaced(id: String) {
        overlay = null
        returnToCart = false
        confirmedOrder = id
    }
    val motionDuration = if (LocalReducedMotion.current) 0 else 180
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val s = state.session
    if (state.loading) {
        Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.products.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(state.error ?: "Catalogue unavailable")
        }
        return
    }
    if (connected.customer == null && !demo) {
        ConnectedSignIn(connected, connection) {
            demo = true
            overlay = null
        }
        return
    }
    if (!s.loggedIn && connected.customer == null) {
        Onboarding(vm) {
            showMilkOffer = true
            overlay = null
        }
        return
    }
    fun go(destination: String) {
        if (
            destination in
                setOf("addresses", "wishlist", "offers", "vip", "routine", "cart", "vacation")
        ) {
            returnToCart = overlay == "cart" && destination in setOf("offers", "addresses")
            if (destination == "cart" && overlay != "cart") cartState.removeState("cart")
            overlay = destination
        } else {
            returnToCart = false
            overlay = null
            nav.navigate(destination) {
                launchSingleTop = true
                if (destination in setOf("home", "shop", "protein", "subs", "profile", "orders")) {
                    popUpTo(nav.graph.startDestinationId)
                    // A tab opens its root, rather than restoring a previous detail screen.
                    restoreState = false
                }
            }
        }
    }
    if (connected.customer != null) ConnectedFeedbackSheet(connected, connection)
    LaunchedEffect(connected.customer) {
        if (connected.customer != null) {
            demo = false
            go("home")
        }
    }
    LaunchedEffect(route, connected.customer) {
        if (route == "home" && connected.customer != null) {
            connection.state.first { !it.busy }
            connection.next()
        }
    }
    LaunchedEffect(showMilkOffer) {
        if (showMilkOffer) {
            nav.navigate("milk-offer") {
                launchSingleTop = true
                popUpTo(nav.graph.startDestinationId) { inclusive = false }
            }
            showMilkOffer = false
        }
    }
    val tabs =
        listOf(
            Triple("home", "Home", CountryIcons.Home),
            Triple("shop?category={category}", "Shop", CountryIcons.ShoppingBag),
            Triple("protein", "Protein", CountryIcons.BicepsFlexed),
            Triple("subs", "Subs", CountryIcons.Calendar),
            Triple("profile", "More", CountryIcons.More),
        )
    val main = route == "home" || route == "protein"
    Scaffold(
        containerColor = Canvas,
        topBar = {
            if (main)
                Row(
                    Modifier.statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssetImage(
                        "assets/brand-logo.png",
                        "Country Delight",
                        Modifier.size(38.dp, 35.dp),
                    )
                    Column(Modifier.weight(1f).clickable { go("addresses") }) {
                        Text(
                            "Deliver to ${s.address?.label ?: "Home"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            s.address?.display ?: "Add address",
                            fontSize = 10.sp,
                            color = Muted,
                            maxLines = 1,
                        )
                    }
                    OutlinedButton(
                        { go("wallet") },
                        contentPadding = PaddingValues(10.dp, 6.dp),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(CountryIcons.Wallet, null, Modifier.size(16.dp))
                        Text(money(s.wallet), fontSize = 11.sp)
                    }
                    IconButton({ go("wishlist") }) { Icon(CountryIcons.Heart, "Open wishlist") }
                }
            else if (
                route == "profile" ||
                    route.startsWith("shop") ||
                    route == "assistant" ||
                    route == "subs" ||
                    route == "orders"
            )
                Spacer(Modifier.statusBarsPadding())
            else
                TopAppBar(
                    title = {
                        val product =
                            if (route.startsWith("product/"))
                                state.products.find { it.id == entry?.arguments?.getString("id") }
                            else null
                        Column {
                            if (product != null) Caption(product.category)
                            Text(
                                product?.name ?: screenTitle(route),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton({ if (!nav.popBackStack()) go("home") }) {
                            Icon(CountryIcons.ArrowLeft, "Back")
                        }
                    },
                )
        },
        bottomBar = {
            if (
                !keyboardVisible &&
                    route !in listOf("checkout", "payment") &&
                    !route.startsWith("feedback/")
            ) {
                Column(
                    Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    val showOffers =
                        route == "home" || route.startsWith("shop") || route == "protein"
                    if (showOffers || s.cart.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (showOffers)
                                QuickOffers(s, state.products, Modifier.weight(1f)) { go("offers") }
                            else Spacer(Modifier.weight(1f))
                            if (s.cart.isNotEmpty()) {
                                val count = s.cart.sumOf { it.qty }
                                val subtotal =
                                    s.cart.sumOf { line ->
                                        lineTotal(
                                            line,
                                            state.products.first { it.id == line.productId },
                                        )
                                    }
                                Surface(
                                    onClick = { go("cart") },
                                    shape = RoundedCornerShape(24.dp),
                                    color = Green,
                                    contentColor = Color.White,
                                ) {
                                    Row(
                                        Modifier.widthIn(min = 76.dp).heightIn(min = 48.dp).padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            CountryIcons.ShoppingBag,
                                            "View cart",
                                            Modifier.size(18.dp),
                                        )
                                        Column {
                                            Text(
                                                money(subtotal),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                if (count == 1) "1 item" else "$count items",
                                                fontSize = 10.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Line),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(4.dp)) {
                                tabs.forEach { (r, label, icon) ->
                                    val selected =
                                        route == r ||
                                            (r.startsWith("shop") && route.startsWith("shop")) ||
                                            (r == "subs" &&
                                                (route == "orders" || route.startsWith("calendar")))
                                    Column(
                                        Modifier.weight(1f)
                                            .heightIn(min = 52.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(
                                                if (selected) Mint else Color.Transparent,
                                                RoundedCornerShape(22.dp),
                                            )
                                            .clickable {
                                                go(if (r.startsWith("shop")) "shop" else r)
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            icon,
                                            null,
                                            Modifier.size(21.dp),
                                            tint = if (selected) Green else Muted,
                                        )
                                        Text(
                                            label,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            color = Ink,
                                            fontWeight =
                                                if (selected) FontWeight.SemiBold
                                                else FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            onClick = { go("assistant") },
                            shape = RoundedCornerShape(28.dp),
                            color = Mint,
                            modifier = Modifier.size(48.dp),
                        ) {
                            AssetImage(
                                "assets/generated/assistant-2026/delight-guide.png",
                                "Open Delight assistant",
                                Modifier.padding(4.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            NavHost(
                nav,
                "home",
                enterTransition = { fadeIn(tween(motionDuration)) },
                exitTransition = { fadeOut(tween(motionDuration)) },
                popEnterTransition = { fadeIn(tween(motionDuration)) },
                popExitTransition = { fadeOut(tween(motionDuration)) },
            ) {
                composable("home") {
                    val feedback: (@Composable () -> Unit)? =
                        if (connected.customer != null) {
                            {
                                ConnectedHomeFeedback(connected, connection) {
                                    go("feedback-account")
                                }
                            }
                        } else null
                    HomeScreen(state, vm, ::go, feedback)
                }
                composable("feedback-account") { ConnectedAccount(connected, connection) }
                composable(
                    "shop?category={category}",
                    listOf(
                        navArgument("category") {
                            type = NavType.StringType
                            defaultValue = "all"
                        }
                    ),
                ) {
                    ShopScreen(state, vm, ::go, it.arguments?.getString("category") ?: "all")
                }
                composable("protein") { ProteinScreen(state, vm, ::go) }
                composable("subs") { SubscriptionsScreen(state, vm, ::go) }
                composable("orders") { OrdersScreen(state, vm, ::go) }
                composable("profile") { ProfileScreen(state, vm, ::go) { dialog = it } }
                composable(
                    "product/{id}?mode={mode}",
                    listOf(
                        navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = "Once"
                        }
                    ),
                ) {
                    val id = it.arguments?.getString("id").orEmpty()
                    val mode = it.arguments?.getString("mode").orEmpty()
                    key(id, mode) { ProductScreen(state, vm, ::go, id, mode) }
                }
                composable("milk-offer") { MilkOffersScreen(state, ::go) }
                composable(
                    "milk-plan/{id}?offer={offer}",
                    listOf(navArgument("offer") { defaultValue = "welcome" }),
                ) {
                    PlanScreen(
                        state,
                        vm,
                        ::go,
                        it.arguments?.getString("id").orEmpty(),
                        it.arguments?.getString("offer") ?: "welcome",
                    )
                }
                composable("cart") { CartScreen(state, vm, ::go, ::onOrderPlaced) }
                composable("checkout") { CheckoutScreen(state, vm, ::go, ::onOrderPlaced) }
                composable("order/{id}") {
                    OrderScreen(state, vm, ::go, it.arguments?.getString("id").orEmpty())
                }
                composable("wallet") { WalletScreen(state, vm) }
                composable(
                    "calendar?date={date}",
                    listOf(
                        navArgument("date") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    ),
                ) { entry ->
                    CalendarScreen(state, vm, ::go, entry.arguments?.getString("date").orEmpty())
                }
                composable("addresses") { AddressesScreen(state, vm) }
                composable("wishlist") { WishlistScreen(state, vm, ::go) }
                composable("offers") { OffersScreen(state, vm, ::go) }
                composable("vip") { VipScreen(state) }
                composable("nutrition") { NutritionScreen(state, ::go) }
                composable("routine") { RoutineScreen(state, vm) }
                composable(
                    "support?order={order}&view={view}",
                    listOf(
                        navArgument("order") { defaultValue = "" },
                        navArgument("view") { defaultValue = "help" },
                    ),
                ) {
                    SupportScreen(
                        state,
                        vm,
                        it.arguments?.getString("order").orEmpty(),
                        it.arguments?.getString("view") ?: "help",
                    )
                }
                composable("assistant") { AssistantScreen(state, vm, ::go) }
                composable("information") { InformationScreen() }
                composable("feedback-samples") { FeedbackSamplesScreen(state, vm, ::go) }
                composable(
                    "feedback/{id}?from={from}",
                    listOf(navArgument("from") { defaultValue = "" }),
                ) { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    key(id) {
                        FeedbackScreen(
                            state,
                            vm,
                            ::go,
                            id,
                            entry.arguments?.getString("from").orEmpty(),
                        )
                    }
                }
                composable("edit-sub/{id}") {
                    SubscriptionEditScreen(state, vm, it.arguments?.getString("id").orEmpty()) {
                        nav.popBackStack()
                    }
                }
            }
        }
    }
    overlay?.let { destination ->
        ModalBottomSheet(
            onDismissRequest = ::closeOverlay,
            containerColor = Canvas,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (destination == "cart") {
                        IconButton(::closeOverlay) { Icon(CountryIcons.ChevronLeft, "Close cart") }
                        Column(Modifier.weight(1f)) {
                            val count = s.cart.sumOf { it.qty }
                            Text(
                                "Your cart · $count ${if (count == 1) "item" else "items"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                s.address?.display ?: "Choose a delivery address",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Muted,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        IconButton({ go("addresses") }) {
                            Icon(CountryIcons.Pencil, "Change delivery address", tint = Green)
                        }
                    } else {
                        Text(
                            screenTitle(destination),
                            Modifier.weight(1f),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(::closeOverlay) {
                            Icon(CountryIcons.Close, "Close ${screenTitle(destination)}")
                        }
                    }
                }
                when (destination) {
                    "addresses" -> AddressesScreen(state, vm)
                    "wishlist" -> WishlistScreen(state, vm, ::go)
                    "offers" -> OffersScreen(state, vm, ::go)
                    "vip" -> VipScreen(state)
                    "routine" -> RoutineScreen(state, vm) { overlay = null }
                    "cart" ->
                        cartState.SaveableStateProvider("cart") {
                            CartScreen(state, vm, ::go, ::onOrderPlaced)
                        }
                    "vacation" -> VacationScreen(state, vm) { overlay = null }
                }
            }
        }
    }
    if (dialog != null)
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (dialog == "reset") "Reset demo activity?" else "Sign out?") },
            text = {
                Text(
                    if (dialog == "reset")
                        "This removes this native app’s saved orders, wallet, feedback and plans."
                    else "This clears the local demo session, as in the source app."
                )
            },
            confirmButton = {
                TextButton({
                    vm.reset()
                    dialog = null
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = { TextButton({ dialog = null }) { Text("Cancel") } },
        )
    confirmedOrder?.let { id ->
        OrderConfirmation(
            onHome = {
                confirmedOrder = null
                go("home")
            },
            onViewOrder = {
                confirmedOrder = null
                go("order/$id")
            },
        )
    }
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Unable to save") },
            text = { Text(message) },
            confirmButton = { TextButton(vm::retrySave) { Text("Try again") } },
        )
    }
}

private fun screenTitle(route: String): String =
    when {
        route.startsWith("product/") -> "Product details"
        route.startsWith("milk-plan/") -> "Your milk plan"
        route.startsWith("order/") -> "Your order"
        route.startsWith("edit-sub/") -> "Edit subscription"
        route.startsWith("feedback/") -> "Product feedback"
        route.startsWith("calendar") -> "Delivery calendar"
        route.startsWith("support") -> "Help & support"
        else ->
            mapOf(
                "subs" to "Your subscriptions",
                "orders" to "Your orders",
                "profile" to "More",
                "cart" to "Your basket",
                "checkout" to "Checkout",
                "wallet" to "Delight Wallet",
                "milk-offer" to "Milk offers",
                "calendar" to "Delivery calendar",
                "addresses" to "Your addresses",
                "wishlist" to "Your wishlist",
                "offers" to "Your offers",
                "vip" to "Your VIP pass",
                "nutrition" to "Your nutrition",
                "routine" to "Your routine",
                "assistant" to "Delight assistant",
                "information" to "About your nutrition & plans",
                "feedback-samples" to "Sample deliveries",
                "feedback-account" to "Buy again & feedback",
            )[route] ?: "Country Delight"
    }

@Composable
fun ScreenBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}
