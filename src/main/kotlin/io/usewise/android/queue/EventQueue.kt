package io.usewise.android.queue

import io.usewise.android.UsewiseConfig
import io.usewise.android.models.BatchPayload
import io.usewise.android.models.TrackEvent
import io.usewise.android.transport.UsewiseHttpClient
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

class EventQueue(
    private val httpClient: UsewiseHttpClient,
    private val config: UsewiseConfig,
) {
    private val queue = CopyOnWriteArrayList<TrackEvent>()
    private var timerJob: Job? = null
    private var isFlushing = false

    fun start(scope: CoroutineScope) {
        timerJob = scope.launch {
            while (isActive) {
                delay(config.flushIntervalMs)
                flush()
            }
        }
    }

    fun add(event: TrackEvent) {
        queue.add(event)
        while (queue.size > config.maxQueueSize) queue.removeAt(0)
        if (queue.size >= config.flushAt) {
            CoroutineScope(Dispatchers.IO).launch { flush() }
        }
    }

    suspend fun flush() {
        if (isFlushing || queue.isEmpty()) return
        isFlushing = true
        try {
            while (queue.isNotEmpty()) {
                val batchSize = minOf(queue.size, 100)
                val batch = queue.take(batchSize)
                try {
                    httpClient.post("/v1/batch", BatchPayload(batch).toJson())
                    repeat(batchSize) { if (queue.isNotEmpty()) queue.removeAt(0) }
                } catch (e: Exception) {
                    if (config.enableLogging) println("[Usewise] Flush failed: ${e.message}")
                    break
                }
            }
        } finally { isFlushing = false }
    }

    fun clear() { queue.clear() }
    fun dispose() { timerJob?.cancel() }
}
