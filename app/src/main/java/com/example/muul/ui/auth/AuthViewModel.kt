package com.example.muul.ui.auth

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.DataModule
import com.example.muul.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = DataModule.getUserRepository(application)

    private val _currentUser = MutableStateFlow<User?>(repo.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser

    fun register(user: User, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val registeredOk = repo.register(user)
            if (registeredOk) {
                val loggedUser = repo.login(user.email, user.password)
                _currentUser.value = loggedUser
                callback(loggedUser != null)
            } else {
                callback(false)
            }
        }
    }

    fun login(emailOrUsername: String, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = repo.login(emailOrUsername, password)
            _currentUser.value = user
            callback(user != null)
        }
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
    }

    fun refreshCurrentUser() {
        _currentUser.value = repo.getCurrentUser()
    }

    fun updateProfilePhoto(sourceUri: Uri) {
        viewModelScope.launch {
            val user = repo.getCurrentUser() ?: return@launch
            
            // 1. Obtener los bytes de la imagen
            val bytes = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            } ?: return@launch

            // 2. Subir a Supabase Storage
            val fileName = "avatar_${user.id ?: user.username}_${System.currentTimeMillis()}.jpg"
            val publicUrl = repo.uploadProfilePhoto(bytes, fileName)

            // 3. Si la subida fue exitosa, guardar el link en la tabla de usuarios
            if (publicUrl != null) {
                repo.updateProfilePhotoUri(publicUrl)
                _currentUser.value = repo.getCurrentUser()
            }
        }
    }

    fun clearProfilePhoto() {
        viewModelScope.launch {
            repo.updateProfilePhotoUri(null)
            _currentUser.value = repo.getCurrentUser()
        }
    }
}
