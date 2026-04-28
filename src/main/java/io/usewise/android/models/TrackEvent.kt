package io.usewise.android.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ElementData(
    val tag: String? = null,
    val text: String? = null,
    val id: String? = null,
)

@Serializable
data class PageData(
    val url: String? = null,
    val title: String? = null,
    val referrer: String? = null,
)

@Serializable
data class ScreenData(
    val width: Int,
    val height: Int,
)

@Serializable
data class DeviceContextData(
    val device_os: String? = null,
    val device_model: String? = null,
    val app_version: String? = null,
    val is_vpn: Boolean = false,
    val is_jailbroken: Boolean = false,
)

@Serializable
data class TrackEvent(
    val event: String,
    val anonymous_id: String? = null,
    val user_id: String? = null,
    val event_uuid: String,
    val timestamp: String,
    val properties: JsonObject? = null,
    val element: ElementData? = null,
    val page: PageData? = null,
    val screen: ScreenData? = null,
    val context: DeviceContextData? = null,
)
