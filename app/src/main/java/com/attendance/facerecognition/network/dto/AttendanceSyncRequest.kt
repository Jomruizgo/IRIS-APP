package com.attendance.facerecognition.network.dto

import com.google.gson.annotations.SerializedName

data class AttendanceSyncRequest(
    @SerializedName("records")
    val records: List<AttendanceRecordDto>
)

data class AttendanceRecordDto(
    @SerializedName("local_id")
    val localId: Long,

    @SerializedName("employee_id")
    val employeeId: String,

    @SerializedName("type")
    val type: String, // "ENTRY" o "EXIT"

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("confidence")
    val confidence: Float,

    @SerializedName("liveness_passed")
    val livenessPassed: Boolean,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("created_at")
    val createdAt: Long
)
