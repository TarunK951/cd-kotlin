package com.countryd.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun ProductQualityCard(name: String, unit: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    var example by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth()
            .background(Color(0xFFEFF7F2), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFBCD9CB), RoundedCornerShape(18.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).clickable { open = !open }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(38.dp, 42.dp)
                    .background(Color(0xFFD6EBDD), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CountryIcons.FileText, null, Modifier.size(20.dp), tint = Color(0xFF145C46))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Quality & test reports",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF173F31),
                )
                Text(
                    "Check the evidence for this product",
                    fontSize = 12.sp,
                    color = Color(0xFF52645B),
                )
            }
            Icon(
                if (open) CountryIcons.ChevronUp else CountryIcons.ChevronDown,
                "Test reports for $name",
                Modifier.size(18.dp),
                tint = Green,
            )
        }
        AnimatedVisibility(open) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("$name · $unit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Batch report not connected",
                    Modifier.background(Color(0xFFFFF3D8), RoundedCornerShape(8.dp)).padding(9.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF785419),
                )
                Text("No verified lab report is connected to this pack.", fontSize = 13.sp)
                TextButton({ example = !example }) {
                    Text(
                        if (example) "Hide example report" else "View example report →",
                        fontSize = 14.sp,
                    )
                }
                if (example)
                    CountryCard {
                        Text(
                            "DEMO DATA · NOT A CERTIFICATE",
                            fontSize = 12.sp,
                            color = Color(0xFF785419),
                        )
                        Heading("Product quality record")
                        listOf(
                                "Product" to name,
                                "Pack" to unit,
                                "Batch" to "DEMO-001",
                                "Laboratory" to "Example laboratory",
                                "Test date" to "13 Oct 2026 · illustrative",
                                "Composition" to "Result to be supplied",
                                "Microbiology" to "Result to be supplied",
                                "Report status" to "Not verified",
                            )
                            .forEach { (label, value) ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        label,
                                        Modifier.width(90.dp),
                                        fontSize = 12.sp,
                                        color = Muted,
                                    )
                                    Text(value, Modifier.weight(1f), fontSize = 13.sp)
                                }
                            }
                        Caption("Layout preview only. No safety, organic or certification claim.")
                    }
                HorizontalDivider(color = Color(0xFFCCDCD1))
                Text("Match it to your pack", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Check the product, batch number, lab, test date and results against the delivered label.",
                    fontSize = 13.sp,
                )
            }
        }
    }
}
