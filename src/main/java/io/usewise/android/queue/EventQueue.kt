package io.usewise.android.queue

import io.usewise.android.UsewiseConfig
import io.usewise.android.models.TrackEvent
import io.usewise.android.transport.HttpClient
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EventQueue(
    private val httpClient: HttpClient,
    private val config: UsewiseConfig,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val queue = mutableListOf<TrackEvent>()
    private var flushJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        flushJob = scope.launch {
            while (isActive) {
                delay(config.flushIntervalMs)
                flush()
            }
        }
    }

    fun add(event: TrackEvent) {
        synchronized(queue) {
            if (queue.size >= config.maxQueueSize) {
                queue.removeAt(0)
            }
            queue.add(event)
        }
        if (queue.size >= config.flushAt) {
            scope.launch { flush() }
        }
    }

    suspend fun flush() {
        val batch: List<TrackEvent>
        synchronized(queue) {
            if (queue.isEmpty()) return
            batch = queue.toList()
            queue.clear()
        }

        try {
            val body = json.encodeToString(mapOf("batch" to batch))
            httpClient.postBatch("/batch", body)
            if (config.enableLogging) {
                println("[Usewise] Flushed ${batch.size} events")
            }
        } catch (e: Exception) {
            if (config.enableLogging) {
                println("[Usewise] Flush failed: ${e.message}")
            }
            synchronized(queue) {
                queue.addAll(0, batch)
            }
        }
    }

    fun clear() {
        synchronized(queue) { queue.clear() }
    }

    fun dispose() {
        flushJob?.cancel()
        scope.cancel()
    }
}
