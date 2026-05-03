package com.example.muul.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muul.ui.map.MapScreen

@Composable
fun AuthHost(
    authViewModel: AuthViewModel = viewModel()
) {
    val userState = authViewModel.currentUser.collectAsState()

    if (userState.value != null) {
        // User is logged in -> show map
        MapScreen()
    } else {
        // show login/register selection flow
        val showingRegister = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val vm = authViewModel
        if (!showingRegister.value) {
            LoginScreen(vm, onLoggedIn = {}, onShowRegister = { showingRegister.value = true })
        } else {
            RegisterScreen(vm, onRegistered = { /* registration sets current user */ }, onBackToLogin = { showingRegister.value = false })
        }
    }
}
