package com.example.muul.data.local

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import com.example.muul.data.model.POI
import com.example.muul.data.model.TransportMode
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class RouteResponse(
    val geometry: List<Pair<Double, Double>>, // list of (lat, lng)
    val distanceMeters: Double,
    val durationSeconds: Double
)

class RoutingRepository(private val application: Application) {
    private val accessToken: String? by lazy {
        // Prefer BuildConfig (set from gradle) which contains the token at build time
        try {
            val tokenField = Class.forName("com.example.muul.BuildConfig").getField("MAPBOX_ACCESS_TOKEN")
            tokenField.get(null) as? String
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Failed to read MAPBOX_ACCESS_TOKEN from BuildConfig: ${e.message}")
            null
        }
    }

    fun getRoute(startLat: Double, startLng: Double, waypoints: List<POI>, transportMode: TransportMode): RouteResponse? {
        val token = accessToken ?: return null

        // Map transport mode to Mapbox profile
        val profile = when (transportMode) {
            TransportMode.WALKING -> "walking"
            TransportMode.BIKING -> "cycling"
            TransportMode.CAR, TransportMode.TAXI -> "driving"
        }

        // Build coordinates string: lng,lat;lng,lat;...
        val coords = StringBuilder()
        coords.append("${startLng},${startLat}")
        for (poi in waypoints) {
            coords.append(";")
            coords.append("${poi.longitud},${poi.latitud}")
        }

        try {
            val base = "https://api.mapbox.com/directions/v5/mapbox/$profile/"
            val params = "geometries=geojson&overview=full&access_token=${URLEncoder.encode(token, "utf-8")}" 
            val url = URL(base + URLEncoder.encode(coords.toString(), "utf-8").replace("%3B", ";") + "?$params")

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val code = conn.responseCode
            if (code != 200) {
                Log.w("RoutingRepo", "Directions API returned $code")
                return null
            }

            val input = BufferedReader(InputStreamReader(conn.inputStream))
            val body = input.readText()
            input.close()

            val obj = JSONObject(body)
            val routes = obj.getJSONArray("routes")
            if (routes.length() == 0) return null
            val r0 = routes.getJSONObject(0)
            val distance = r0.optDouble("distance", 0.0)
            val duration = r0.optDouble("duration", 0.0)
            val geometry = r0.getJSONObject("geometry")
            val coordsArray = geometry.getJSONArray("coordinates")

            val points = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until coordsArray.length()) {
                val pair = coordsArray.getJSONArray(i)
                val lng = pair.optDouble(0)
                val lat = pair.optDouble(1)
                points.add(Pair(lat, lng))
            }

            return RouteResponse(points, distance, duration)
        } catch (e: Exception) {
            Log.w("RoutingRepo", "Error fetching route: ${e.message}")
            return null
        }
    }
}
