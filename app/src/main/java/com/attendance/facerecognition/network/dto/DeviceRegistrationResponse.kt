package com.attendance.facerecognition.network.dto

import com.google.gson.annotations.SerializedName

data class DeviceRegistrationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: DeviceData
)

data class DeviceData(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("tenant_id")
    val tenantId: String,

    @SerializedName("device_token")
    val deviceToken: String,

    @SerializedName("token_expires_at")
    val tokenExpiresAt: Long?,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("registered_at")
    val registeredAt: Long
)
