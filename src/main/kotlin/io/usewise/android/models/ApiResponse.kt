package io.usewise.android.models

import org.json.JSONObject

data class ProcessStartResponse(val processId: String, val processName: String, val startedAt: String) {
    companion object {
        fun fromJson(json: JSONObject) = ProcessStartResponse(
            processId = json.getString("process_id"),
            processName = json.getString("process_name"),
            startedAt = json.getString("started_at"),
        )
    }
}

data class ProcessStepResponse(val stepId: String, val stepName: String, val stepOrder: Int, val durationMs: Int) {
    companion object {
        fun fromJson(json: JSONObject) = ProcessStepResponse(
            stepId = json.getString("step_id"), stepName = json.getString("step_name"),
            stepOrder = json.getInt("step_order"), durationMs = json.getInt("duration_ms"),
        )
    }
}

data class ProcessCompleteResponse(val totalSteps: Int, val totalDurationMs: Int) {
    companion object {
        fun fromJson(json: JSONObject) = ProcessCompleteResponse(
            totalSteps = json.getInt("total_steps"), totalDurationMs = json.getInt("total_duration_ms"),
        )
    }
}
