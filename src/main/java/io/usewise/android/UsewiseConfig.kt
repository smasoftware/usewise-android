package io.usewise.android

data class UsewiseConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.usewise.io/api/v1",
    val flushIntervalMs: Long = 30_000L,
    val flushAt: Int = 20,
    val maxQueueSize: Int = 1000,
    val maxRetries: Int = 3,
    val httpTimeoutMs: Long = 10_000L,
    val enableLogging: Boolean = false,
)
