package com.countryd.app.api

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class FeedbackApi(private val context: Context) {
    companion object {
        const val BASE = "https://cd-feedback-backend-production.up.railway.app"
    }

    suspend fun request(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: JsonObject? = null,
    ): JsonElement = send(path, method, token, body?.toString()?.toByteArray(), "application/json")

    suspend fun photo(id: Long, token: String, uri: Uri): JsonObject =
        withContext(Dispatchers.IO) {
            val mime = context.contentResolver.getType(uri)
            val ext =
                mapOf(
                    "image/jpeg" to "jpg",
                    "image/png" to "png",
                    "image/webp" to "webp",
                    "image/heic" to "heic",
                )[mime] ?: error("Choose a JPEG, PNG, WebP or HEIC image.")
            val limit = 5 * 1024 * 1024
            val stream =
                requireNotNull(context.contentResolver.openInputStream(uri)) {
                    "Could not open this photo."
                }
            val bytes =
                stream.use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        require(out.size() + n <= limit) { "Choose a photo smaller than 5 MB." }
                        out.write(buffer, 0, n)
                    }
                    out.toByteArray()
                }
            require(bytes.isNotEmpty()) { "This photo is empty." }
            val boundary = "Country-${java.util.UUID.randomUUID()}"
            val header =
                "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo.$ext\"\r\nContent-Type: $mime\r\n\r\n"
            send(
                    "/api/feedback/$id/photo",
                    "POST",
                    token,
                    header.toByteArray() + bytes + "\r\n--$boundary--\r\n".toByteArray(),
                    "multipart/form-data; boundary=$boundary",
                )
                .jsonObject
        }

    private suspend fun send(
        path: String,
        method: String,
        token: String?,
        bytes: ByteArray?,
        type: String,
    ): JsonElement =
        withContext(Dispatchers.IO) {
            val conn = URL(BASE + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/json")
                if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")
                if (bytes != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", type)
                    conn.setFixedLengthStreamingMode(bytes.size)
                    conn.outputStream.use { it.write(bytes) }
                }
                val status = conn.responseCode
                val raw =
                    (if (status in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                if (status !in 200..299) {
                    val error = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrNull()
                    throw ApiFailure(
                        status,
                        error?.str("code").orEmpty(),
                        error?.str("message")?.takeIf { it.isNotBlank() }
                            ?: "Request failed ($status). Try again.",
                    )
                }
                Json.parseToJsonElement(raw)
            } finally {
                conn.disconnect()
            }
        }
}

class ApiFailure(val status: Int, val code: String, message: String) : Exception(message)

internal fun JsonObject.str(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull.orEmpty()

internal fun JsonObject.obj(key: String) = get(key) as? JsonObject

internal fun JsonObject.list(key: String) =
    (get(key) as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

internal fun JsonObject.num(key: String) = (get(key) as? JsonPrimitive)?.longOrNull ?: 0L

internal fun JsonObject.flag(key: String) = (get(key) as? JsonPrimitive)?.booleanOrNull == true

internal class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("feedback-api-auth", Context.MODE_PRIVATE)
    private val alias = "country-feedback-token"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey(alias, null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(
                    KeyGenParameterSpec.Builder(
                            alias,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
                generateKey()
            }
    }

    fun read(): String? =
        runCatching {
                val encrypted = prefs.getString("token", null) ?: return null
                val parts =
                    encrypted.split(':', limit = 2).map { Base64.decode(it, Base64.NO_WRAP) }
                Cipher.getInstance("AES/GCM/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, parts[0]))
                    doFinal(parts[1]).decodeToString()
                }
            }
            .getOrElse {
                clear()
                null
            }

    fun save(token: String) {
        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val data =
            listOf(cipher.iv, cipher.doFinal(token.toByteArray())).joinToString(":") {
                Base64.encodeToString(it, Base64.NO_WRAP)
            }
        check(prefs.edit().putString("token", data).commit()) { "Could not save your sign-in." }
    }

    fun clear() {
        prefs.edit().clear().commit()
    }
}
