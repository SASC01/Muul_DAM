package com.example.muul.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit,
    onShowRegister: () -> Unit
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(20.dp)) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(onClick = {
            viewModel.login(email.value.trim(), password.value) { ok ->
                if (ok) onLoggedIn()
            }
        }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("Iniciar sesión")
        }
        TextButton(onClick = onShowRegister, modifier = Modifier.padding(top = 8.dp)) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}
