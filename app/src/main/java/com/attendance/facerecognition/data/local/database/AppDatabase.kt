package com.attendance.facerecognition.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendance.facerecognition.data.local.dao.AttendanceAuditDao
import com.attendance.facerecognition.data.local.dao.AttendanceDao
import com.attendance.facerecognition.data.local.dao.DeviceRegistrationDao
import com.attendance.facerecognition.data.local.dao.EmployeeDao
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.DeviceRegistration
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.local.entities.FloatListConverter
import com.attendance.facerecognition.data.local.entities.SyncStatusTypeConverter
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        Employee::class,
        AttendanceRecord::class,
        AttendanceAudit::class,
        com.attendance.facerecognition.data.local.entities.User::class,
        DeviceRegistration::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(
    FloatListConverter::class,
    com.attendance.facerecognition.data.local.entities.AuditActionConverter::class,
    SyncStatusTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun attendanceAuditDao(): AttendanceAuditDao
    abstract fun userDao(): com.attendance.facerecognition.data.local.dao.UserDao
    abstract fun deviceRegistrationDao(): DeviceRegistrationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "face_recognition_attendance_db"
        private const val PREFS_NAME = "db_security"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"

        /**
         * Obtiene o genera una passphrase segura para la base de datos
         */
        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingPassphrase = prefs.getString(KEY_DB_PASSPHRASE, null)

            return if (existingPassphrase != null) {
                existingPassphrase.toByteArray(Charsets.ISO_8859_1)
            } else {
                // Generar nueva passphrase aleatoria de 32 bytes
                val passphrase = ByteArray(32)
                java.security.SecureRandom().nextBytes(passphrase)

                // Guardar en SharedPreferences
                prefs.edit()
                    .putString(KEY_DB_PASSPHRASE, String(passphrase, Charsets.ISO_8859_1))
                    .apply()

                passphrase
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Cargar librerías nativas de SQLCipher
                System.loadLibrary("sqlcipher")

                // Eliminar base de datos antigua no encriptada si existe
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                if (dbFile.exists()) {
                    // Verificar si es una DB no encriptada intentando leer el header
                    try {
                        val header = dbFile.inputStream().use {
                            val bytes = ByteArray(16)
                            it.read(bytes)
                            String(bytes, Charsets.UTF_8)
                        }
                        // Si empieza con "SQLite format", es una DB no encriptada
                        if (header.startsWith("SQLite format")) {
                            dbFile.delete()
                            context.getDatabasePath("$DATABASE_NAME-shm")?.delete()
                            context.getDatabasePath("$DATABASE_NAME-wal")?.delete()
                        }
                    } catch (e: Exception) {
                        // Si hay error leyendo, eliminar por seguridad
                        dbFile.delete()
                    }
                }

                val passphrase = getOrCreatePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
