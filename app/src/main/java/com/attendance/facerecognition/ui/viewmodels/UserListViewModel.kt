package com.attendance.facerecognition.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.facerecognition.data.local.database.AppDatabase
import com.attendance.facerecognition.data.local.entities.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserListViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val userDao = database.userDao()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUsers()
    }

    /**
     * Carga todos los usuarios
     */
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                userDao.getAllUsers().collect { users ->
                    _users.value = users
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _users.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    /**
     * Elimina un usuario
     */
    fun deleteUser(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                userDao.deleteUser(user)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error al eliminar usuario")
            }
        }
    }

    /**
     * Cambia el estado activo de un usuario
     */
    fun toggleUserActive(user: User, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val updatedUser = user.copy(isActive = !user.isActive)
                userDao.updateUser(updatedUser)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error al actualizar usuario")
            }
        }
    }
}
