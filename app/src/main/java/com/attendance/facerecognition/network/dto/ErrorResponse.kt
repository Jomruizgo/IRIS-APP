package com.attendance.facerecognition.network.dto

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("error")
    val error: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("code")
    val code: String?
)
