package com.countryd.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.countryDataStore by preferencesDataStore("country-native-session-v1")

class CountryRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val key = stringPreferencesKey("session")
    val session: Flow<Session> =
        context.countryDataStore.data
            .map { prefs -> prefs[key]?.let { json.decodeFromString<Session>(it) } ?: Session() }
            .flowOn(Dispatchers.IO)

    suspend fun catalogue(): List<Product> =
        withContext(Dispatchers.IO) {
            context.assets.open("catalogue.json").bufferedReader().use {
                json.decodeFromString(it.readText())
            }
        }

    suspend fun save(session: Session) =
        withContext(Dispatchers.IO) {
            context.countryDataStore.edit { it[key] = json.encodeToString(session) }
        }

    suspend fun feedbackSamples(): List<FeedbackRequest> =
        withContext(Dispatchers.IO) {
            context.assets.open("data/product-feedback-demo.json").bufferedReader().use {
                json.decodeFromString<FeedbackSamplesFile>(it.readText()).requests
            }
        }
}
