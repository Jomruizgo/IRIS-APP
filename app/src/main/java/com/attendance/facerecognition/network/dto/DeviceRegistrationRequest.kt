package com.attendance.facerecognition.network.dto

import com.google.gson.annotations.SerializedName

data class DeviceRegistrationRequest(
    @SerializedName("activation_code")
    val activationCode: String,

    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("device_name")
    val deviceName: String,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("device_manufacturer")
    val deviceManufacturer: String,

    @SerializedName("android_version")
    val androidVersion: String
)
