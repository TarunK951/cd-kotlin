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
fun ProfileScreen(
    state: CountryState,
    vm: CountryViewModel,
    go: (String) -> Unit,
    confirm: (String) -> Unit,
) {
    val session = state.session
    var editing by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(false) }
    ScreenBody {
        Text("Account", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(52.dp)
                    .background(Mint, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    session.name.take(1).ifBlank { "G" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Green,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    session.name.ifBlank { "Guest" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("+91 ${session.phone}", fontSize = 13.sp, color = Muted)
            }
            IconButton({ editing = true }) {
                Icon(CountryIcons.Settings, "Account settings", tint = Ink)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
        CountryCard {
            AccountRow(
                "Orders",
                if (session.orders.isEmpty()) "No orders yet"
                else "${session.orders.size} saved orders",
                CountryIcons.Package,
            ) {
                go("orders")
            }
            HorizontalDivider(color = Line)
            AccountRow(
                "Wallet & loyalty",
                "${money(session.wallet)} · ${session.points} points",
                CountryIcons.Wallet,
            ) {
                go("wallet")
            }
            HorizontalDivider(color = Line)
            AccountRow("Delight VIP pass", "View your membership & savings", CountryIcons.Crown) {
                go("vip")
            }
            HorizontalDivider(color = Line)
            AccountRow(
                "Delivery address",
                session.address?.display.orEmpty(),
                CountryIcons.MapPin,
            ) {
                go("addresses")
            }
        }
        Spacer(Modifier.height(2.dp))
        Text("More", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
        CountryCard {
            if (BuildConfig.DEBUG) {
                AccountRow(
                    "Try product feedback",
                    "Five sample delivered products",
                    CountryIcons.Heart,
                ) {
                    go("feedback/${state.sampleRequests.firstOrNull()?.productId ?: "p2"}")
                }
                HorizontalDivider(color = Line)
            }
            AccountRow("Protein & wellness", "", CountryIcons.BicepsFlexed) { go("protein") }
            HorizontalDivider(color = Line)
            AccountRow(
                "Plan with Delight",
                "Meal ideas, shopping and weekly nutrition",
                CountryIcons.Leaf,
            ) {
                go("assistant")
            }
            HorizontalDivider(color = Line)
            AccountRow("Wishlist", "", CountryIcons.Heart) { go("wishlist") }
            HorizontalDivider(color = Line)
            AccountRow("Subscriptions", "", CountryIcons.Repeat) { go("subs") }
            HorizontalDivider(color = Line)
            AccountRow("Delivery calendar", "", CountryIcons.Calendar) { go("calendar") }
            HorizontalDivider(color = Line)
            AccountRow("Food preferences & protein goal", "", CountryIcons.Settings) {
                go("routine")
            }
            HorizontalDivider(color = Line)
            AccountRow("My routine", "", CountryIcons.Leaf) { go("routine") }
            HorizontalDivider(color = Line)
            AccountRow("Notifications", "", CountryIcons.Bell) { notifications = true }
            HorizontalDivider(color = Line)
            AccountRow("Help & support", "", CountryIcons.CircleHelp) { go("support") }
            HorizontalDivider(color = Line)
            AccountRow("App & nutrition information", "", CountryIcons.CircleHelp) {
                go("information")
            }
        }
        if (BuildConfig.DEBUG) TextButton({ confirm("reset") }) { Text("Reset demo activity") }
        Caption("Country Delight · Live Better")
    }
    if (editing)
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Settings") },
            text = {
                Column {
                    Caption("Manage account options")
                    listOf(
                            "wishlist" to "Wishlist",
                            "subs" to "Subscriptions",
                            "calendar" to "Delivery calendar",
                        )
                        .forEach { (route, title) ->
                            TextButton({
                                editing = false
                                go(route)
                            }) {
                                Text(title)
                            }
                        }
                    TextButton({
                        editing = false
                        notifications = true
                    }) {
                        Text("Notifications")
                    }
                    TextButton({
                        editing = false
                        confirm("signout")
                    }) {
                        Text("Log out", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton({ editing = false }) { Text("Close") } },
        )
    if (notifications)
        AlertDialog(
            onDismissRequest = { notifications = false },
            title = { Text("Notifications") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Delivery updates", Modifier.weight(1f))
                        Switch(
                            session.deliveryAlerts,
                            { value -> vm.update { it.copy(deliveryAlerts = value) } },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Offers and updates", Modifier.weight(1f))
                        Switch(
                            session.offerAlerts,
                            { value -> vm.update { it.copy(offerAlerts = value) } },
                        )
                    }
                    Caption(
                        "Preferences are saved in this demo. Push notifications are not connected."
                    )
                }
            },
            confirmButton = { TextButton({ notifications = false }) { Text("Done") } },
        )
}

@Composable
private fun AccountRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = Green)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty())
                Text(subtitle, fontSize = 12.sp, color = Muted, lineHeight = 18.sp)
        }
        Text("›", fontSize = 22.sp, color = Muted)
    }
}
