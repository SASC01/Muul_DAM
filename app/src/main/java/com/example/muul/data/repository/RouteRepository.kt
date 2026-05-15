package com.example.muul.data.repository

import com.example.muul.data.model.Ruta

interface RouteRepository {
    fun getSavedRoutes(): List<Ruta>
    suspend fun saveRoute(route: Ruta): Ruta
    suspend fun deleteRoute(routeId: String)
}
