package com.attendance.facerecognition.data.repository

import com.attendance.facerecognition.data.local.dao.UserDao
import com.attendance.facerecognition.data.local.entities.User
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest

class UserRepository(private val userDao: UserDao) {

    fun getAllActiveUsers(): Flow<List<User>> = userDao.getAllActiveUsers()

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)

    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    suspend fun getAnyAdmin(): User? = userDao.getAnyAdmin()

    suspend fun getAdminCount(): Int = userDao.getAdminCount()

    suspend fun insertUser(user: User): Long = userDao.insertUser(user)

    suspend fun updateUser(user: User) = userDao.updateUser(user)

    suspend fun deleteUser(user: User) = userDao.deleteUser(user)

    suspend fun updateLastLogin(userId: Long) {
        userDao.updateLastLogin(userId, System.currentTimeMillis())
    }

    /**
     * Valida las credenciales del usuario
     */
    suspend fun validateCredentials(username: String, pin: String): User? {
        val user = getUserByUsername(username) ?: return null
        val pinHash = hashPin(pin)

        return if (user.pinHash == pinHash && user.isActive) {
            updateLastLogin(user.id)
            user
        } else {
            null
        }
    }

    /**
     * Crea el hash SHA-256 del PIN
     */
    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifica si existe al menos un admin
     */
    suspend fun hasAdmin(): Boolean {
        return getAdminCount() > 0
    }
}
