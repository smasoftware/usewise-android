package io.usewise.android.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class IdentifyPayload(
    val anonymous_id: String,
    val user_id: String,
    val traits: JsonObject? = null,
)
