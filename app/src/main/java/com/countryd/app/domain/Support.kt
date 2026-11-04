package com.countryd.app.domain

import com.countryd.app.data.*
import java.time.LocalDate

fun requestStatus(request: SupportRequest): String =
    when (request.status) {
        "Resolved",
        "Closed" -> request.status
        else -> "Open"
    }

fun supportReference(id: String): String =
    when {
        id.startsWith("SUPPORT-DEMO-") -> "100${id.takeLast(1)}"
        id.startsWith("SUPPORT-TICKET-DEMO-") ->
            "SR-100${listOf("OPEN", "RESOLVED", "CLOSED").indexOf(id.removePrefix("SUPPORT-TICKET-DEMO-")) + 1}"
        else -> id.replace(Regex("^DEMO-"), "SR-")
    }

fun sampleSupportOrders(products: List<Product>, today: LocalDate = LocalDate.now()): List<Order> =
    listOf(listOf("p1", "p13"), listOf("p7", "p2"), listOf("p10", "p11")).mapIndexed { index, ids ->
        val lines =
            ids.mapNotNull { id -> products.find { it.id == id }?.let { OrderLine(CartLine(id)) } }
        val total = ids.sumOf { id -> products.find { it.id == id }?.price ?: 0.0 }
        val date = today.minusDays(index.toLong()).toString()
        Order(
            "SUPPORT-DEMO-${index + 1}",
            "${date}T06:00:00Z",
            date,
            "Morning",
            "Demo address",
            lines,
            Totals(total, 0.0, 0.0, 0.0, 0, total, 0.0, total, 0),
            status = if (index == 0) "Order Placed" else "Delivered",
        )
    }

fun sampleSupportTickets(): List<SupportRequest> =
    listOf("Open", "Resolved", "Closed").mapIndexed { index, status ->
        SupportRequest(
            "SUPPORT-TICKET-DEMO-${status.uppercase()}",
            listOf("Missing delivery", "Wrong or damaged item", "Product quality")[index],
            listOf(
                "Please check when my milk and bread will arrive.",
                "The damaged egg pack issue has been resolved.",
                "My question about the fruit and vegetable packs is complete.",
            )[index],
            "SUPPORT-DEMO-${index + 1}",
            status = status,
        )
    }
