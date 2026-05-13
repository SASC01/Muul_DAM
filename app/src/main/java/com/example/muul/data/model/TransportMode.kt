package com.example.muul.data.model

enum class TransportMode(val label: String, val speedKmh: Double) {
    WALKING("Caminando", 4.5),
    BIKING("Bicicleta", 15.0),
    CAR("Auto", 28.0),
    TAXI("Taxi", 24.0)
}