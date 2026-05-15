package com.example.muul.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.DataModule
import com.example.muul.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = DataModule.getUserRepository(application)

    private val _currentUser = MutableStateFlow<User?>(repo.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser

    fun register(email: String, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.register(email, password)
            _currentUser.value = repo.getCurrentUser()
            callback(ok)
        }
    }

    fun login(email: String, password: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.login(email, password)
            _currentUser.value = repo.getCurrentUser()
            callback(ok)
        }
    }

    fun logout() {
        repo.logout()
        _currentUser.value = null
    }
}
