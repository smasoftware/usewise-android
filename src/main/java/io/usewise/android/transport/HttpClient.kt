package io.usewise.android.transport

import io.usewise.android.UsewiseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpClient(private val config: UsewiseConfig) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.httpTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.httpTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.httpTimeoutMs, TimeUnit.MILLISECONDS)
        .build()

    suspend inline fun <reified T> post(path: String, body: T): String {
        return withContext(Dispatchers.IO) {
            val jsonBody = json.encodeToString(body)
            val request = Request.Builder()
                .url("${config.baseUrl}$path")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-Key", config.apiKey)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            var lastException: Exception? = null
            repeat(config.maxRetries) {
                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) return@withContext responseBody
                    if (response.code in 400..499) throw IOException("HTTP ${response.code}: $responseBody")
                } catch (e: Exception) {
                    lastException = e
                    if (config.enableLogging) {
                        println("[Usewise] HTTP retry ${it + 1}/${config.maxRetries}: ${e.message}")
                    }
                }
            }
            throw lastException ?: IOException("Request failed after ${config.maxRetries} retries")
        }
    }

    suspend fun postBatch(path: String, jsonBody: String): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${config.baseUrl}$path")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-Key", config.apiKey)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        }
    }
}
