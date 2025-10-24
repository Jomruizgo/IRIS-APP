package com.attendance.facerecognition.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "employees")
@TypeConverters(FloatListConverter::class)
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val employeeId: String, // ID del empleado en el sistema de la empresa
    val fullName: String,
    val department: String,
    val position: String,

    // Lista de embeddings (vectores de 192 dimensiones)
    // Múltiples embeddings para mejorar precisión (diferentes ángulos, iluminación)
    val faceEmbeddings: List<FloatArray>,

    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * Clase auxiliar para almacenar un embedding con metadatos
 */
data class FaceEmbedding(
    val vector: FloatArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEmbedding

        if (!vector.contentEquals(other.vector)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vector.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Type Converter para convertir List<FloatArray> a String y viceversa
 * Room no puede almacenar directamente listas de arrays
 */
class FloatListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArrayList(value: List<FloatArray>): String {
        // Convertir cada FloatArray a List<Float> para poder serializarlo con Gson
        val listOfLists = value.map { it.toList() }
        return gson.toJson(listOfLists)
    }

    @TypeConverter
    fun toFloatArrayList(value: String): List<FloatArray> {
        val type = object : TypeToken<List<List<Float>>>() {}.type
        val listOfLists: List<List<Float>> = gson.fromJson(value, type)
        return listOfLists.map { it.toFloatArray() }
    }
}
