package io.usewise.android.models

import org.json.JSONObject

data class ElementData(val tag: String? = null, val text: String? = null, val id: String? = null)
data class PageData(val url: String? = null, val title: String? = null, val referrer: String? = null)
data class ScreenData(val width: Int? = null, val height: Int? = null)
data class DeviceContextData(
    val deviceOs: String? = null, val deviceModel: String? = null, val appVersion: String? = null,
    val isVpn: Boolean? = null, val isJailbroken: Boolean? = null,
)

data class TrackEvent(
    val event: String, val anonymousId: String?, val userId: String?,
    val eventUuid: String, val timestamp: String,
    val properties: Map<String, Any>? = null, val element: ElementData? = null,
    val page: PageData? = null, val screen: ScreenData? = null, val context: DeviceContextData? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("event", event); put("event_uuid", eventUuid); put("timestamp", timestamp)
        anonymousId?.let { put("anonymous_id", it) }
        userId?.let { put("user_id", it) }
        properties?.let { put("properties", JSONObject(it)) }
        element?.let { put("element", JSONObject().apply { it.tag?.let { put("tag", it) }; it.text?.let { put("text", it) }; it.id?.let { put("id", it) } }) }
        page?.let { put("page", JSONObject().apply { it.url?.let { put("url", it) }; it.title?.let { put("title", it) }; it.referrer?.let { put("referrer", it) } }) }
        screen?.let { put("screen", JSONObject().apply { it.width?.let { put("width", it) }; it.height?.let { put("height", it) } }) }
        context?.let { put("context", JSONObject().apply {
            it.deviceOs?.let { put("device_os", it) }; it.deviceModel?.let { put("device_model", it) }
            it.appVersion?.let { put("app_version", it) }; it.isVpn?.let { put("is_vpn", it) }; it.isJailbroken?.let { put("is_jailbroken", it) }
        }) }
    }
}

data class BatchPayload(val batch: List<TrackEvent>) {
    fun toJson(): JSONObject = JSONObject().put("batch", batch.map { it.toJson() }.let { org.json.JSONArray(it) })
}
