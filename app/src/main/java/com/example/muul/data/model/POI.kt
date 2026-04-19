package com.example.muul.data.model

import kotlinx.serialization.Serializable

@Serializable
data class POI(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String? = null,
    val categoria: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val emoji: String? = null,
    val horario_apertura: String? = null,
    val horario_cierre: String? = null,
    val precio_rango: String? = null,
    val verificado: Boolean = false,
    val direccion: String? = null,
    val created_at: String? = null,
    val distanciaMetros: Double = 0.0
)