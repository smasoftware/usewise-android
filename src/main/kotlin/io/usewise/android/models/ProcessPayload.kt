package io.usewise.android.models

import org.json.JSONObject

data class ProcessStartPayload(val processName: String, val anonymousId: String?, val userId: String?, val properties: Map<String, Any>? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("process_name", processName)
        anonymousId?.let { put("anonymous_id", it) }; userId?.let { put("user_id", it) }
        properties?.let { put("properties", JSONObject(it)) }
    }
}

data class ProcessSubStartPayload(val parentProcessId: String, val processName: String, val anonymousId: String?, val userId: String?, val properties: Map<String, Any>? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("parent_process_id", parentProcessId); put("process_name", processName)
        anonymousId?.let { put("anonymous_id", it) }; userId?.let { put("user_id", it) }
        properties?.let { put("properties", JSONObject(it)) }
    }
}

data class ProcessStepPayload(val processId: String, val stepName: String, val properties: Map<String, Any>? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("process_id", processId); put("step_name", stepName)
        properties?.let { put("properties", JSONObject(it)) }
    }
}

data class ProcessCompletePayload(val processId: String) {
    fun toJson(): JSONObject = JSONObject().put("process_id", processId)
}

data class ProcessFailPayload(val processId: String, val reason: String? = null) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("process_id", processId); reason?.let { put("reason", it) }
    }
}
