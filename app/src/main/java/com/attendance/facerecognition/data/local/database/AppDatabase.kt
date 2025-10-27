package com.attendance.facerecognition.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.attendance.facerecognition.data.local.dao.AttendanceAuditDao
import com.attendance.facerecognition.data.local.dao.AttendanceDao
import com.attendance.facerecognition.data.local.dao.DeviceRegistrationDao
import com.attendance.facerecognition.data.local.dao.EmployeeDao
import com.attendance.facerecognition.data.local.dao.PendingAttendanceDao
import com.attendance.facerecognition.data.local.entities.AttendanceAudit
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import com.attendance.facerecognition.data.local.entities.DeviceRegistration
import com.attendance.facerecognition.data.local.entities.Employee
import com.attendance.facerecognition.data.local.entities.FloatListConverter
import com.attendance.facerecognition.data.local.entities.PendingAttendanceRecord
import com.attendance.facerecognition.data.local.entities.PendingReasonConverter
import com.attendance.facerecognition.data.local.entities.PendingStatusConverter
import com.attendance.facerecognition.data.local.entities.SyncStatusTypeConverter
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        Employee::class,
        AttendanceRecord::class,
        AttendanceAudit::class,
        com.attendance.facerecognition.data.local.entities.User::class,
        DeviceRegistration::class,
        PendingAttendanceRecord::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(
    FloatListConverter::class,
    com.attendance.facerecognition.data.local.entities.AuditActionConverter::class,
    SyncStatusTypeConverter::class,
    PendingReasonConverter::class,
    PendingStatusConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun attendanceAuditDao(): AttendanceAuditDao
    abstract fun userDao(): com.attendance.facerecognition.data.local.dao.UserDao
    abstract fun deviceRegistrationDao(): DeviceRegistrationDao
    abstract fun pendingAttendanceDao(): PendingAttendanceDao

    companion object {
        // Migración de versión 10 a 11
        // Agrega tabla pending_attendance_records
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_attendance_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        employeeId TEXT NOT NULL,
                        employeeName TEXT,
                        timestamp INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        photoPath TEXT NOT NULL,
                        deviceId TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        reviewedBy INTEGER,
                        reviewedAt INTEGER,
                        reviewNotes TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }

        // Migración de versión 9 a 10
        // Agrega campos de huella a User y los remueve de Employee
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Agregar columnas de huella a users
                database.execSQL("ALTER TABLE users ADD COLUMN hasFingerprintEnabled INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN fingerprintKeystoreAlias TEXT")

                // 2. Remover columnas de employees
                // SQLite no soporta DROP COLUMN directamente, hay que recrear la tabla
                database.execSQL("""
                    CREATE TABLE employees_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        employeeId TEXT NOT NULL,
                        fullName TEXT NOT NULL,
                        department TEXT NOT NULL,
                        position TEXT NOT NULL,
                        faceEmbeddings TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)

                // Copiar datos (sin las columnas de huella que ya no existen)
                database.execSQL("""
                    INSERT INTO employees_new (id, employeeId, fullName, department, position, faceEmbeddings, createdAt, isActive)
                    SELECT id, employeeId, fullName, department, position, faceEmbeddings, createdAt, isActive
                    FROM employees
                """)

                // Eliminar tabla vieja y renombrar
                database.execSQL("DROP TABLE employees")
                database.execSQL("ALTER TABLE employees_new RENAME TO employees")
            }
        }

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
                try {
                    // Cargar librerías nativas de SQLCipher con manejo de errores
                    System.loadLibrary("sqlcipher")
                } catch (e: UnsatisfiedLinkError) {
                    android.util.Log.e("AppDatabase", "Error loading SQLCipher library", e)
                    // Continuar sin SQLCipher - se usará Room estándar
                }

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
                        android.util.Log.w("AppDatabase", "Error checking database header", e)
                        // No eliminar si hay error, puede ser DB encriptada
                    }
                }

                val passphrase = getOrCreatePassphrase(context)

                val instance = try {
                    // Intentar con SQLCipher primero
                    val factory = SupportOpenHelperFactory(passphrase)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                        .openHelperFactory(factory)
                        .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error creating encrypted database, falling back to standard Room", e)
                    // Fallback a Room estándar sin encriptación
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(MIGRATION_9_10, MIGRATION_10_11)
                        .fallbackToDestructiveMigrationOnDowngrade()
                        .build()
                }

                INSTANCE = instance
                instance
            }
        }
    }
}
