package com.example.muul.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: String? = null,

    @SerialName("nombre")
    val nombre: String,

    @SerialName("apellido")
    val apellido: String,

    @SerialName("username")
    val username: String,

    @SerialName("email")
    val email: String,

    @SerialName("password")
    val password: String,

    @SerialName("num_pasos")
    val numPasos: Int? = 0,

    @SerialName("total_steps")
    val totalSteps: Int? = 0,

    @SerialName("steps_by_route")
    val stepsByRoute: Map<String, Int>? = emptyMap(),

    @SerialName("profile_photo_uri")
    val profilePhotoUri: String? = null
)
