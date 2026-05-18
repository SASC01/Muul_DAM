package com.example.muul.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.muul.data.model.POI
import com.example.muul.data.model.Ruta
import org.json.JSONArray
import org.json.JSONObject

class LocalRouteRepositoryImpl(context: Context, private val userRepository: UserRepository) : RouteRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("muul_route_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROUTES = "saved_routes"
    }

    override suspend fun getSavedRoutes(): List<Ruta> {
        val raw = prefs.getString(namespacedKey(), "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(routeFromJson(array.getJSONObject(index)))
            }
        }
    }

    override suspend fun saveRoute(route: Ruta): Ruta {
        val routes = getSavedRoutes().toMutableList()
        val routeId = route.id ?: System.currentTimeMillis().toString()
        val routeToSave = if (route.id == null) route.copy(id = routeId) else route
        
        routes.removeAll { it.id == routeId }
        routes.add(routeToSave)
        prefs.edit().putString(namespacedKey(), JSONArray(routes.map { routeToJson(it) }).toString()).apply()
        return routeToSave
    }

    override suspend fun deleteRoute(routeId: String) {
        val routes = getSavedRoutes().filterNot { it.id == routeId }
        prefs.edit().putString(namespacedKey(), JSONArray(routes.map { routeToJson(it) }).toString()).apply()
    }

    private fun namespacedKey(): String {
        val email = userRepository.getCurrentUser()?.email?.lowercase()?.replace("@", "_")?.replace(".", "_")
        return if (email.isNullOrBlank()) KEY_ROUTES else "${KEY_ROUTES}_$email"
    }

    private fun routeToJson(route: Ruta): JSONObject {
        return JSONObject().apply {
            put("id", route.id)
            put("user_id", route.userId)
            put("nombre", route.nombre)
            put("pasos_totales", route.pasosTotales)
            put("distancia_total", route.distanciaTotal)
            put("transport_mode", route.transportMode)
            put("planned_duration_minutes", route.plannedDurationMinutes)
            put("start_time_minutes", route.startTimeMinutes)
            put("lugares", JSONArray(route.lugares.map { poiToJson(it) }))
        }
    }

    private fun routeFromJson(obj: JSONObject): Ruta {
        val places = obj.optJSONArray("lugares") ?: JSONArray()
        return Ruta(
            id = if (obj.isNull("id")) null else obj.optString("id"),
            userId = if (obj.isNull("user_id")) null else obj.optString("user_id"),
            nombre = obj.optString("nombre", "Nueva Ruta"),
            lugares = buildList {
                for (index in 0 until places.length()) {
                    add(poiFromJson(places.getJSONObject(index)))
                }
            },
            pasosTotales = obj.optInt("pasos_totales", 0),
            distanciaTotal = obj.optDouble("distancia_total", 0.0),
            transportMode = obj.optString("transport_mode", "WALKING"),
            plannedDurationMinutes = obj.optInt("planned_duration_minutes", 0),
            startTimeMinutes = obj.optInt("start_time_minutes", 9 * 60)
        )
    }

    private fun poiToJson(poi: POI): JSONObject {
        return JSONObject().apply {
            put("id", poi.id)
            put("nombre", poi.nombre)
            put("descripcion", poi.descripcion)
            put("categoria", poi.categoria)
            put("latitud", poi.latitud)
            put("longitud", poi.longitud)
            put("emoji", poi.emoji)
            put("horario_apertura", poi.horario_apertura)
            put("horario_cierre", poi.horario_cierre)
            put("precio_rango", poi.precio_rango)
            put("verificado", poi.verificado)
            put("direccion", poi.direccion)
            put("created_at", poi.created_at)
            put("distanciaMetros", poi.distanciaMetros)
        }
    }

    private fun poiFromJson(obj: JSONObject): POI {
        return POI(
            id = obj.optString("id"),
            nombre = obj.optString("nombre"),
            descripcion = if (obj.isNull("descripcion")) null else obj.optString("descripcion"),
            categoria = obj.optString("categoria"),
            latitud = obj.optDouble("latitud"),
            longitud = obj.optDouble("longitud"),
            emoji = if (obj.isNull("emoji")) null else obj.optString("emoji"),
            horario_apertura = if (obj.isNull("horario_apertura")) null else obj.optString("horario_apertura"),
            horario_cierre = if (obj.isNull("horario_cierre")) null else obj.optString("horario_cierre"),
            precio_rango = if (obj.isNull("precio_rango")) null else obj.optString("precio_rango"),
            verificado = obj.optBoolean("verificado", false),
            direccion = if (obj.isNull("direccion")) null else obj.optString("direccion"),
            created_at = if (obj.isNull("created_at")) null else obj.optString("created_at"),
            distanciaMetros = obj.optDouble("distanciaMetros", 0.0)
        )
    }
}
