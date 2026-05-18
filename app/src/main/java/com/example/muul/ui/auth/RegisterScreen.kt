package com.example.muul.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.muul.data.model.User

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val nombre = remember { mutableStateOf("") }
    val apellido = remember { mutableStateOf("") }
    val username = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val isLoading = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.headlineLarge)
        
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre.value,
            onValueChange = { nombre.value = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apellido.value,
            onValueChange = { apellido.value = it },
            label = { Text("Apellido") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = username.value,
            onValueChange = { username.value = it },
            label = { Text("Nombre de usuario") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        )

        if (errorMessage.value != null) {
            Text(
                text = errorMessage.value!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (nombre.value.isNotBlank() && email.value.isNotBlank() && password.value.isNotBlank()) {
                    isLoading.value = true
                    errorMessage.value = null
                    
                    val newUser = User(
                        nombre = nombre.value.trim(),
                        apellido = apellido.value.trim(),
                        username = username.value.trim(),
                        email = email.value.trim(),
                        password = password.value
                    )
                    
                    viewModel.register(newUser) { success ->
                        isLoading.value = false
                        if (success) {
                            onRegistered()
                        } else {
                            errorMessage.value = "Error de conexión o datos duplicados. Revisa la fecha de tu dispositivo."
                        }
                    }
                } else {
                    errorMessage.value = "Por favor completa todos los campos"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Registrarse")
            }
        }

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading.value
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}
