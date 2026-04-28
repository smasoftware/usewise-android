package io.usewise.android.models

import org.json.JSONObject

data class IdentifyPayload(val anonymousId: String, val userId: String, val traits: Map<String, Any>? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("anonymous_id", anonymousId); put("user_id", userId)
        traits?.let { put("traits", JSONObject(it)) }
    }
}
