package com.example.muul.data.local

import com.example.muul.data.model.POI
import kotlin.math.*

object HaversineUtils {

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun optimizarOrdenTSP(
        pois: List<POI>,
        ubicacionUsuario: Pair<Double, Double>? = null
    ): List<POI> {
        if (pois.size <= 2) return pois

        val visitados = mutableSetOf<Int>()
        val resultado = mutableListOf<POI>()

        var currentLat = ubicacionUsuario?.first ?: pois[0].latitud
        var currentLon = ubicacionUsuario?.second ?: pois[0].longitud

        val primerIdx = if (ubicacionUsuario != null) {
            pois.indices.minByOrNull { i ->
                haversine(currentLat, currentLon, pois[i].latitud, pois[i].longitud)
            } ?: 0
        } else 0

        visitados.add(primerIdx)
        resultado.add(pois[primerIdx])
        currentLat = pois[primerIdx].latitud
        currentLon = pois[primerIdx].longitud

        while (visitados.size < pois.size) {
            var mejorIdx = -1
            var mejorDist = Double.MAX_VALUE

            for (i in pois.indices) {
                if (i in visitados) continue
                val dist = haversine(currentLat, currentLon, pois[i].latitud, pois[i].longitud)
                if (dist < mejorDist) {
                    mejorDist = dist
                    mejorIdx = i
                }
            }

            if (mejorIdx == -1) break
            visitados.add(mejorIdx)
            resultado.add(pois[mejorIdx])
            currentLat = pois[mejorIdx].latitud
            currentLon = pois[mejorIdx].longitud
        }

        return resultado
    }
}