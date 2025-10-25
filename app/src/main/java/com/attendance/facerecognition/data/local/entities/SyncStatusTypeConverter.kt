package com.attendance.facerecognition.data.local.entities

import androidx.room.TypeConverter

class SyncStatusTypeConverter {
    @TypeConverter
    fun fromSyncStatusType(value: SyncStatusType): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatusType(value: String): SyncStatusType {
        return try {
            SyncStatusType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SyncStatusType.NEVER_SYNCED
        }
    }
}
