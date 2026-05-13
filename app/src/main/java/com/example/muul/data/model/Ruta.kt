package com.example.muul.data.model

data class Ruta(
    val id: String = System.currentTimeMillis().toString(),
    val nombre: String = "Ruta ${System.currentTimeMillis()}",
    val lugares: List<POI> = emptyList(),
    val pasosTotales: Int = 0,
    val distanciaTotal: Double = 0.0,
    val transportMode: String = "WALKING",
    val plannedDurationMinutes: Int = 0,
    val startTimeMinutes: Int = 9 * 60,
    val createdAt: Long = System.currentTimeMillis()
)
