package com.attendance.facerecognition.export

import android.content.Context
import android.os.Environment
import com.attendance.facerecognition.data.local.entities.AttendanceRecord
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exportador de registros de asistencia a formato CSV
 */
class CsvExporter(private val context: Context) {

    /**
     * Exporta registros a CSV
     * Retorna la ruta del archivo generado
     */
    fun exportToCsv(
        records: List<AttendanceRecord>,
        fileName: String? = null
    ): Result<String> {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val finalFileName = fileName ?: "asistencia_$timestamp.csv"

            // Usar directorio de documentos de la app
            val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reportes")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, finalFileName)
            val writer = FileWriter(file)

            // Escribir encabezados
            writer.append("ID,Empleado,ID_Empleado,Tipo,Fecha,Hora,Confianza,Desafio_Liveness,Sincronizado\n")

            // Escribir datos
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            records.forEach { record ->
                val date = Date(record.timestamp)
                writer.append("${record.id},")
                writer.append("\"${record.employeeName}\",")
                writer.append("${record.employeeIdNumber},")
                writer.append("${record.type},")
                writer.append("${dateFormat.format(date)},")
                writer.append("${timeFormat.format(date)},")
                writer.append("${record.confidence},")
                writer.append("\"${record.livenessChallenge}\",")
                writer.append("${if (record.isSynced) "SI" else "NO"}\n")
            }

            writer.flush()
            writer.close()

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exporta registros con filtros personalizados
     */
    fun exportWithFilters(
        records: List<AttendanceRecord>,
        startDate: Date? = null,
        endDate: Date? = null,
        employeeId: String? = null
    ): Result<String> {
        val filteredRecords = records.filter { record ->
            val matchesDate = if (startDate != null && endDate != null) {
                record.timestamp >= startDate.time && record.timestamp <= endDate.time
            } else true

            val matchesEmployee = if (employeeId != null) {
                record.employeeIdNumber == employeeId
            } else true

            matchesDate && matchesEmployee
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val fileName = buildString {
            append("asistencia")
            if (employeeId != null) append("_$employeeId")
            if (startDate != null) append("_${dateFormat.format(startDate)}")
            if (endDate != null) append("_${dateFormat.format(endDate)}")
            append(".csv")
        }

        return exportToCsv(filteredRecords, fileName)
    }
}
