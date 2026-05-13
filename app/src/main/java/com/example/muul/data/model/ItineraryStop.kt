package com.example.muul.data.model

data class ItineraryStop(
    val poi: POI,
    val arrivalLabel: String,
    val stayMinutes: Int,
    val travelMinutesFromPrevious: Int
)