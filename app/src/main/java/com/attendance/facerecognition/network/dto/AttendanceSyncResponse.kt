package com.attendance.facerecognition.network.dto

import com.google.gson.annotations.SerializedName

data class AttendanceSyncResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("synced_count")
    val syncedCount: Int,

    @SerializedName("synced_records")
    val syncedRecords: List<SyncedRecord>,

    @SerializedName("conflicts")
    val conflicts: List<SyncConflict>,

    @SerializedName("errors")
    val errors: List<SyncError>
)

data class SyncedRecord(
    @SerializedName("local_id")
    val localId: Long,

    @SerializedName("server_id")
    val serverId: Long,

    @SerializedName("synced_at")
    val syncedAt: Long
)

data class SyncConflict(
    @SerializedName("local_id")
    val localId: Long,

    @SerializedName("reason")
    val reason: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("existing_record")
    val existingRecord: ExistingRecord?
)

data class ExistingRecord(
    @SerializedName("server_id")
    val serverId: Long,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("device_id")
    val deviceId: String
)

data class SyncError(
    @SerializedName("local_id")
    val localId: Long,

    @SerializedName("error")
    val error: String
)
