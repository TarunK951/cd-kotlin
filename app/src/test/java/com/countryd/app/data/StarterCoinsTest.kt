package com.countryd.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class StarterCoinsTest {
    @Test
    fun freshAndLegacySessionsStartWithStarterCoins() {
        assertEquals(1_500, Session().withStarterCoins().points)
        val legacy = Json.decodeFromString<Session>("""{"points":0}""")
        assertEquals(1_500, legacy.withStarterCoins().points)
        assertEquals(1_750, legacy.copy(points = 1_750).withStarterCoins().points)
    }

    @Test
    fun reloadingASpentBalanceDoesNotGrantCoinsAgain() {
        val spent = Session().withStarterCoins().copy(points = 0)
        val restored =
            Json.decodeFromString<Session>(Json.encodeToString(Session.serializer(), spent))
        assertEquals(0, restored.withStarterCoins().points)
    }
}
