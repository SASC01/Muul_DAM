package com.example.muul.data.local

import com.example.muul.data.model.ItineraryStop
import com.example.muul.data.model.Ruta
import com.example.muul.data.model.TransportMode
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

object RoutePlanner {
    fun routeDistanceMeters(route: Ruta): Double {
        if (route.lugares.size < 2) return 0.0
        var total = 0.0
        for (index in 0 until route.lugares.size - 1) {
            val first = route.lugares[index]
            val second = route.lugares[index + 1]
            total += HaversineUtils.haversine(first.latitud, first.longitud, second.latitud, second.longitud)
        }
        return total
    }

    fun estimateMinutes(route: Ruta, transportMode: TransportMode): Int {
        if (route.lugares.size < 2) return 0
        val travelMinutes = routeDistanceMeters(route) / 1000.0 / transportMode.speedKmh * 60.0
        val visitMinutes = max(15, route.lugares.drop(1).size * 15)
        return ceil(travelMinutes + visitMinutes).toInt()
    }

    fun buildItinerary(
        route: Ruta,
        transportMode: TransportMode,
        totalAvailableMinutes: Int,
        startTimeMinutes: Int
    ): List<ItineraryStop> {
        val stops = route.lugares.drop(1)
        if (stops.isEmpty()) return emptyList()

        val travelMinutesPerKm = 60.0 / transportMode.speedKmh
        val routeDistanceKm = routeDistanceMeters(route) / 1000.0
        val estimatedTravelMinutes = ceil(routeDistanceKm * travelMinutesPerKm).toInt().coerceAtLeast(1)
        val availableVisitMinutes = max(totalAvailableMinutes - estimatedTravelMinutes, stops.size * 10)
        val stayPerStop = max(10, availableVisitMinutes / stops.size)

        var clock = startTimeMinutes
        val itinerary = mutableListOf<ItineraryStop>()
        var previous = route.lugares.first()

        stops.forEach { poi ->
            val segmentMinutes = ceil(
                HaversineUtils.haversine(previous.latitud, previous.longitud, poi.latitud, poi.longitud) / 1000.0 * travelMinutesPerKm
            ).toInt().coerceAtLeast(1)
            clock += segmentMinutes
            itinerary.add(
                ItineraryStop(
                    poi = poi,
                    arrivalLabel = formatMinutes(clock),
                    stayMinutes = stayPerStop,
                    travelMinutesFromPrevious = segmentMinutes
                )
            )
            clock += stayPerStop
            previous = poi
        }

        return itinerary
    }

    fun formatMinutes(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val hour = normalized / 60
        val minute = normalized % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }
}