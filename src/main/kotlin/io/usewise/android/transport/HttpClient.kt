package io.usewise.android.transport

import io.usewise.android.UsewiseConfig
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

class UsewiseHttpClient(private val config: UsewiseConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.httpTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.httpTimeoutMs, TimeUnit.MILLISECONDS)
        .build()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun post(path: String, body: JSONObject): String {
        val url = "${config.baseUrl}$path"
        val requestBody = body.toString().toRequestBody(json)

        var lastException: Exception? = null

        for (attempt in 0..config.maxRetries) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-API-Key", config.apiKey)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) return responseBody

                if (response.code in 400..499 && response.code != 429) {
                    throw Exception("API error ${response.code}: $responseBody")
                }

                if (attempt < config.maxRetries) {
                    val delayMs = min(1000.0 * 2.0.pow(attempt), 30000.0).toLong() + (0..500).random()
                    if (config.enableLogging) println("[Usewise] Retry $attempt after ${delayMs}ms")
                    delay(delayMs)
                    continue
                }

                throw Exception("Server error after ${config.maxRetries} retries: ${response.code}")
            } catch (e: Exception) {
                if (e.message?.startsWith("API error") == true) throw e
                lastException = e
                if (attempt < config.maxRetries) {
                    delay(min(1000.0 * 2.0.pow(attempt), 30000.0).toLong())
                }
            }
        }
        throw lastException ?: Exception("Request failed")
    }

    fun shutdown() { client.dispatcher.executorService.shutdown() }
}
