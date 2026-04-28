package io.usewise.android

import android.content.Context
import io.usewise.android.models.*
import io.usewise.android.persistence.UsewiseStorage
import io.usewise.android.queue.EventQueue
import io.usewise.android.transport.UsewiseHttpClient
import io.usewise.android.utils.DeviceContext
import io.usewise.android.utils.IdGenerator
import kotlinx.coroutines.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object Usewise {
    private lateinit var config: UsewiseConfig
    private lateinit var httpClient: UsewiseHttpClient
    private lateinit var eventQueue: EventQueue
    private lateinit var storage: UsewiseStorage
    private lateinit var deviceContext: DeviceContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var _anonymousId: String = ""
    private var _userId: String? = null
    private var _optedOut: Boolean = false
    private var initialized = false

    val anonymousId: String get() = _anonymousId
    val userId: String? get() = _userId
    val isOptedOut: Boolean get() = _optedOut

    fun initialize(context: Context, config: UsewiseConfig) {
        this.config = config
        storage = UsewiseStorage(context.applicationContext)
        httpClient = UsewiseHttpClient(config)
        eventQueue = EventQueue(httpClient, config)
        deviceContext = DeviceContext.capture(context.applicationContext)

        _anonymousId = storage.getString("anonymous_id") ?: IdGenerator.uuid().also {
            storage.setString("anonymous_id", it)
        }
        _optedOut = storage.getBool("opted_out")

        eventQueue.start(scope)
        initialized = true
    }

    fun enableCrashReporting() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            trackError(
                message = throwable.message ?: "Unknown crash",
                type = "crash",
                severity = "critical",
                stackTrace = throwable.stackTraceToString(),
            )
            runBlocking { flush() }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    // ── Track ──

    fun track(
        event: String,
        properties: Map<String, Any>? = null,
        element: ElementData? = null,
        page: PageData? = null,
    ) {
        if (_optedOut || !initialized) return
        eventQueue.add(TrackEvent(
            event = event, anonymousId = _anonymousId, userId = _userId,
            eventUuid = IdGenerator.uuid(), timestamp = iso8601Now(),
            properties = properties, element = element, page = page,
            screen = ScreenData(deviceContext.screenWidth, deviceContext.screenHeight),
            context = DeviceContextData(
                deviceOs = deviceContext.deviceOs, deviceModel = deviceContext.deviceModel,
                appVersion = deviceContext.appVersion, isVpn = deviceContext.isVpn, isJailbroken = deviceContext.isRooted,
            ),
        ))
    }

    // ── Identify ──

    suspend fun identify(userId: String, traits: Map<String, Any>? = null) {
        if (_optedOut || !initialized) return
        flush()
        _userId = userId
        try {
            httpClient.post("/v1/identify", IdentifyPayload(_anonymousId, userId, traits).toJson())
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] identify failed: ${e.message}")
        }
    }

    // ── Process Tracking ──

    suspend fun startProcess(name: String, properties: Map<String, Any>? = null): String? {
        if (_optedOut || !initialized) return null
        return try {
            val resp = httpClient.post("/v1/process/start", ProcessStartPayload(name, _anonymousId, _userId, properties).toJson())
            ProcessStartResponse.fromJson(JSONObject(resp)).processId
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] startProcess failed: ${e.message}")
            null
        }
    }

    suspend fun startSubProcess(parentProcessId: String, name: String, properties: Map<String, Any>? = null): String? {
        if (_optedOut || !initialized) return null
        return try {
            val resp = httpClient.post("/v1/process/start-sub", ProcessSubStartPayload(parentProcessId, name, _anonymousId, _userId, properties).toJson())
            ProcessStartResponse.fromJson(JSONObject(resp)).processId
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] startSubProcess failed: ${e.message}")
            null
        }
    }

    suspend fun processStep(processId: String, stepName: String, properties: Map<String, Any>? = null): ProcessStepResponse? {
        if (_optedOut || !initialized) return null
        return try {
            val resp = httpClient.post("/v1/process/step", ProcessStepPayload(processId, stepName, properties).toJson())
            ProcessStepResponse.fromJson(JSONObject(resp))
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] processStep failed: ${e.message}")
            null
        }
    }

    suspend fun completeProcess(processId: String): ProcessCompleteResponse? {
        if (_optedOut || !initialized) return null
        return try {
            val resp = httpClient.post("/v1/process/complete", ProcessCompletePayload(processId).toJson())
            ProcessCompleteResponse.fromJson(JSONObject(resp))
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] completeProcess failed: ${e.message}")
            null
        }
    }

    suspend fun failProcess(processId: String, reason: String? = null): ProcessCompleteResponse? {
        if (_optedOut || !initialized) return null
        return try {
            val resp = httpClient.post("/v1/process/fail", ProcessFailPayload(processId, reason).toJson())
            ProcessCompleteResponse.fromJson(JSONObject(resp))
        } catch (e: Exception) {
            if (config.enableLogging) println("[Usewise] failProcess failed: ${e.message}")
            null
        }
    }

    // ── Error Tracking ──

    fun trackError(
        message: String, type: String = "exception", screen: String? = null,
        processId: String? = null, stepName: String? = null, code: String? = null,
        severity: String = "medium", stackTrace: String? = null, context: Map<String, Any>? = null,
    ) {
        if (_optedOut || !initialized) return
        scope.launch {
            try {
                val body = JSONObject().apply {
                    put("error_type", type); put("error_message", message)
                    put("anonymous_id", _anonymousId); put("severity", severity)
                    _userId?.let { put("user_id", it) }
                    screen?.let { put("screen", it) }
                    processId?.let { put("process_id", it) }
                    stepName?.let { put("step_name", it) }
                    code?.let { put("error_code", it) }
                    stackTrace?.let { put("stack_trace", it) }
                    context?.let { put("context", JSONObject(it)) }
                    put("device_context", JSONObject().apply {
                        put("device_os", deviceContext.deviceOs)
                        put("device_model", deviceContext.deviceModel)
                        put("app_version", deviceContext.appVersion)
                    })
                }
                httpClient.post("/v1/error", body)
            } catch (e: Exception) {
                if (config.enableLogging) println("[Usewise] trackError failed: ${e.message}")
            }
        }
    }

    // ── Queue / Privacy ──

    suspend fun flush() { if (!_optedOut && initialized) eventQueue.flush() }

    fun reset() {
        _userId = null
        _anonymousId = IdGenerator.uuid()
        storage.setString("anonymous_id", _anonymousId)
        eventQueue.clear()
    }

    fun optOut() {
        _optedOut = true
        storage.setBool("opted_out", true)
        eventQueue.clear()
    }

    fun optIn() {
        _optedOut = false
        storage.setBool("opted_out", false)
    }

    suspend fun shutdown() {
        flush()
        eventQueue.dispose()
        httpClient.shutdown()
        scope.cancel()
    }

    private fun iso8601Now(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
