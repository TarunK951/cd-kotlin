package com.countryd.app.domain

import com.countryd.app.data.*
import org.junit.Assert.*
import org.junit.Test

class AssistantTest {
    private val catalogue =
        listOf(
            Product(
                "p1",
                "Cow Milk",
                "Milk & Dairy",
                unit = "1 L",
                price = 68.0,
                mrp = 75.0,
                description = "Milk",
            ),
            Product(
                "p15",
                "Rolled Oats",
                "Bakery & Breakfast",
                unit = "500 g",
                price = 115.0,
                mrp = 135.0,
                description = "Oats",
            ),
            Product(
                "p10",
                "Robusta Bananas",
                "Fruits & Veg",
                unit = "500 g",
                price = 45.0,
                mrp = 50.0,
                description = "Bananas",
            ),
        )

    @Test
    fun suggestionsRespectPlantPreference() {
        assertFalse(
            assistantReply(
                    "breakfast ideas",
                    Session(goal = Goal(preference = "Plant-based")),
                    catalogue,
                )
                .products
                .any { it.id == "p1" }
        )
    }

    @Test
    fun pairsExcludeProductsAlreadyInBasket() {
        assertEquals(
            listOf("p15", "p10"),
            complementaryProducts(listOf(CartLine("p1")), catalogue, "Everything").map { it.id },
        )
    }

    @Test
    fun missingOrdersDoNotInventDeliveryStatus() {
        assertTrue(
            assistantReply("delivery help", Session(), catalogue)
                .text
                .startsWith("No orders saved yet")
        )
    }

    @Test
    fun quantityIsOnlyASuggestionAndIsCapped() {
        val session = Session()
        val reply = assistantReply("99 packs milk", session, catalogue)
        assertEquals(20, reply.quantity)
        assertTrue(session.cart.isEmpty())
    }

    @Test
    fun proteinGoalIsOptionalAndExplicit() {
        assertTrue(assistantReply("protein", Session(), catalogue).openGoal)
    }
}
