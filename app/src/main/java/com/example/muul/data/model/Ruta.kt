package com.example.muul.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ruta(
    @SerialName("id")
    val id: String? = null,

    @SerialName("user_id")
    val userId: String? = null,

    @SerialName("nombre")
    val nombre: String = "Nueva Ruta",

    @SerialName("lugares")
    val lugares: List<POI> = emptyList(),

    @SerialName("pasos_totales")
    val pasosTotales: Int = 0,

    @SerialName("distancia_total")
    val distanciaTotal: Double = 0.0,

    @SerialName("transport_mode")
    val transportMode: String = "WALKING",

    @SerialName("planned_duration_minutes")
    val plannedDurationMinutes: Int = 0,

    @SerialName("start_time_minutes")
    val startTimeMinutes: Int = 9 * 60
)
