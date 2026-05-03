package com.example.muul.data.model

data class User(
    val email: String,
    val password: String,
    val stepsByRoute: Map<String, Int> = emptyMap()
)
