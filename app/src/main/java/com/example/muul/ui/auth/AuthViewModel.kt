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
                // Después de un registro exitoso, iniciamos sesión automáticamente
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
            val savedUri = withContext(Dispatchers.IO) {
                copyProfilePhotoToAppStorage(sourceUri)
            }

            if (savedUri != null) {
                repo.updateProfilePhotoUri(savedUri)
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

    private fun copyProfilePhotoToAppStorage(sourceUri: Uri): String? {
        val application = getApplication<Application>()
        val user = repo.getCurrentUser() ?: return null
        val safeEmail = user.email
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
        val targetDirectory = File(application.filesDir, "profile_photos").apply {
            mkdirs()
        }
        val targetFile = File(targetDirectory, "${safeEmail}_${System.currentTimeMillis()}.jpg")

        return runCatching {
            application.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            targetFile.absolutePath
        }.getOrNull()
    }
}
