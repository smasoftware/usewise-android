package io.usewise.android

import android.content.Context
import io.usewise.android.models.*
import io.usewise.android.persistence.UsewiseStorage
import io.usewise.android.queue.EventQueue
import io.usewise.android.transport.HttpClient
import io.usewise.android.utils.DeviceContext
import io.usewise.android.utils.DeviceInfo
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

class Usewise private constructor(
    private val config: UsewiseConfig,
    context: Context,
) {
    private val httpClient = HttpClient(config)
    private val eventQueue = EventQueue(httpClient, config)
    private val storage = UsewiseStorage(context)
    private val deviceInfo: DeviceInfo = DeviceContext.capture(context)
    private var anonymousId: String
    private var userId: String? = null
    private var optedOut: Boolean

    init {
        anonymousId = storage.getString("anonymous_id") ?: run {
            val id = UUID.randomUUID().toString()
            storage.setString("anonymous_id", id)
            id
        }
        optedOut = storage.getBool("opted_out")
        eventQueue.start()
    }

    companion object {
        @Volatile
        var instance: Usewise? = null
            private set

        fun initialize(context: Context, config: UsewiseConfig) {
            instance = Usewise(config, context.applicationContext)
        }
    }

    // ── Track ──

    fun track(
        event: String,
        properties: Map<String, Any?>? = null,
        element: ElementData? = null,
        page: PageData? = null,
    ) {
        if (optedOut) return

        val trackEvent = TrackEvent(
            event = event,
            anonymous_id = anonymousId,
            user_id = userId,
            event_uuid = UUID.randomUUID().toString(),
            timestamp = iso8601Now(),
            properties = properties?.toJsonObject(),
            element = element,
            page = page,
            screen = ScreenData(width = deviceInfo.screenWidth, height = deviceInfo.screenHeight),
            context = DeviceContextData(
                device_os = deviceInfo.deviceOs,
                device_model = deviceInfo.deviceModel,
                app_version = deviceInfo.appVersion,
                is_vpn = deviceInfo.isVpn,
                is_jailbroken = deviceInfo.isRooted,
            ),
        )

        eventQueue.add(trackEvent)
    }

    // ── Identify ──

    suspend fun identify(userId: String, traits: Map<String, Any?>? = null) {
        if (optedOut) return
        eventQueue.flush()
        this.userId = userId

        val payload = IdentifyPayload(
            anonymous_id = anonymousId,
            user_id = userId,
            traits = traits?.toJsonObject(),
        )

        try {
            httpClient.post("/identify", payload)
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] Identify failed: ${e.message}")
        }
    }

    // ── Process Tracking ──

    suspend fun startProcess(name: String, properties: Map<String, Any?>? = null): String? {
        if (optedOut) return null

        val payload = ProcessStartPayload(
            process_name = name,
            anonymous_id = anonymousId,
            user_id = userId,
            properties = properties?.toJsonObject(),
        )

        return try {
            val response = httpClient.post("/process/start", payload)
            Json.decodeFromString<ProcessStartResponse>(response).process_id
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] startProcess failed: ${e.message}")
            null
        }
    }

    suspend fun startSubProcess(parentProcessId: String, name: String, properties: Map<String, Any?>? = null): String? {
        if (optedOut) return null

        val payload = ProcessSubStartPayload(
            parent_process_id = parentProcessId,
            process_name = name,
            anonymous_id = anonymousId,
            user_id = userId,
            properties = properties?.toJsonObject(),
        )

        return try {
            val response = httpClient.post("/process/start-sub", payload)
            Json.decodeFromString<ProcessStartResponse>(response).process_id
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] startSubProcess failed: ${e.message}")
            null
        }
    }

    suspend fun processStep(processId: String, stepName: String, properties: Map<String, Any?>? = null) {
        if (optedOut) return
        val payload = ProcessStepPayload(
            process_id = processId,
            step_name = stepName,
            properties = properties?.toJsonObject(),
        )
        try {
            httpClient.post("/process/step", payload)
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] processStep failed: ${e.message}")
        }
    }

    suspend fun completeProcess(processId: String) {
        if (optedOut) return
        try {
            httpClient.post("/process/complete", ProcessCompletePayload(process_id = processId))
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] completeProcess failed: ${e.message}")
        }
    }

    // ── Flush / Reset / Privacy ──

    suspend fun flush() {
        if (!optedOut) eventQueue.flush()
    }

    fun reset() {
        userId = null
        anonymousId = UUID.randomUUID().toString()
        storage.setString("anonymous_id", anonymousId)
        eventQueue.clear()
    }

    fun optOut() {
        optedOut = true
        storage.setBool("opted_out", true)
        eventQueue.clear()
    }

    fun optIn() {
        optedOut = false
        storage.setBool("opted_out", false)
    }

    suspend fun shutdown() {
        flush()
        eventQueue.dispose()
        instance = null
    }

    // ── Getters ──

    val currentAnonymousId: String get() = anonymousId
    val currentUserId: String? get() = userId
    val isOptedOut: Boolean get() = optedOut

    // ── Helpers ──

    private fun iso8601Now(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun Map<String, Any?>.toJsonObject(): JsonObject {
        return buildJsonObject {
            this@toJsonObject.forEach { (key, value) ->
                when (value) {
                    null -> put(key, JsonNull)
                    is String -> put(key, JsonPrimitive(value))
                    is Number -> put(key, JsonPrimitive(value))
                    is Boolean -> put(key, JsonPrimitive(value))
                    else -> put(key, JsonPrimitive(value.toString()))
                }
            }
        }
    }
}
