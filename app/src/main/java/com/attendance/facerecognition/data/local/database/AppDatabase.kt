package com.attendance.facerecognition.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendance.facerecognition.data.local.dao.AttendanceAuditDao
import com.attendance.facerecognition.data.local.dao.AttendanceDao
import com.attendance.facerecognition.data.local.dao.EmployeeDao
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.local.entities.FloatListConverter

@Database(
    entities = [Employee::class, AttendanceRecord::class, AttendanceAudit::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(FloatListConverter::class, com.attendance.facerecognition.data.local.entities.AuditActionConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun attendanceAuditDao(): AttendanceAuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "face_recognition_attendance_db"
                )
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
