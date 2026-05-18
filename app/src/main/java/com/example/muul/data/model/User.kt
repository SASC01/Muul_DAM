package com.example.muul.data.model

data class User(
    val email: String,
    val password: String,
    val totalSteps: Int = 0,
    val stepsByRoute: Map<String, Int> = emptyMap(), // Mantener para compatibilidad
    val profilePhotoUri: String? = null
)
