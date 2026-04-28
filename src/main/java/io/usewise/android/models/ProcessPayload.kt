package io.usewise.android.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ProcessStartPayload(
    val process_name: String,
    val anonymous_id: String? = null,
    val user_id: String? = null,
    val properties: JsonObject? = null,
)

@Serializable
data class ProcessSubStartPayload(
    val parent_process_id: String,
    val process_name: String,
    val anonymous_id: String? = null,
    val user_id: String? = null,
    val properties: JsonObject? = null,
)

@Serializable
data class ProcessStepPayload(
    val process_id: String,
    val step_name: String,
    val properties: JsonObject? = null,
)

@Serializable
data class ProcessCompletePayload(
    val process_id: String,
)

@Serializable
data class ProcessStartResponse(
    val status: String,
    val process_id: String,
)
