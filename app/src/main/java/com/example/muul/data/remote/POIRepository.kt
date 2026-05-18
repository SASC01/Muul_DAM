package com.example.muul.data.remote

import android.util.Log
import com.example.muul.BuildConfig
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.model.POI
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class POIRepository {

    private val accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    private val MAX_DISTANCE_METERS = 25_000.0
    private val memoryCache = mutableMapOf<String, List<POI>>()

    // Categorías de Mapbox SearchBox Category API
    // https://docs.mapbox.com/api/search/search-box/#category-search
    private val poiCategories = mapOf(
        "comida" to listOf(
            "restaurant", "cafe", "fast_food", "bar"
        ),
        "cultural" to listOf(
            "museum", "church", "historic_site", "theater"
        ),
        "deportes" to listOf(
            "park", "gym", "stadium", "sports"
        ),
        "tienda" to listOf(
            "supermarket", "shopping_mall", "market", "shop"
        ),
        "servicio" to listOf(
            "hospital", "pharmacy", "hotel", "gas_station"
        ),
        "atraccion" to listOf(
            "tourist_attraction", "landmark", "viewpoint", "archaeological_site"
        )
    )

    // Búsquedas de texto para cosas específicas
    private val textQueries = mapOf(
        "atraccion" to listOf(
            "zona arqueológica", "cenote", "malecón",
            "fuerte", "muralla", "ruinas", "hacienda"
        ),
        "cultural" to listOf(
            "catedral", "convento", "palacio", "centro cultural"
        ),
        "comida" to listOf(
            "mariscos", "taquería", "cochinita"
        )
    )

    suspend fun fetchPOIsCercanos(
        latitud: Double,
        longitud: Double
    ): List<POI> {
        val cacheKey = cacheKey(latitud, longitud)
        memoryCache[cacheKey]?.let {
            Log.d("MUUL", "POIs desde cache para $cacheKey: ${it.size}")
            return it
        }

        val allPois = coroutineScope {
            val categoryTasks = poiCategories.flatMap { (categoria, categories) ->
                categories.map { category ->
                    async(Dispatchers.IO) {
                        try {
                            val results = searchByCategory(category, latitud, longitud)
                            val pois = results.mapNotNull { json ->
                                parsePOI(json, categoria, latitud, longitud)
                            }

                            if (pois.isNotEmpty()) {
                                Log.d("MUUL", "Category '$category': ${pois.size} POIs")
                            }

                            pois
                        } catch (e: Exception) {
                            Log.e("MUUL", "Error category '$category': ${e.message}")
                            emptyList()
                        }
                    }
                }
            }

            val textTasks = textQueries.flatMap { (categoria, queries) ->
                queries.map { query ->
                    async(Dispatchers.IO) {
                        try {
                            val results = searchByText(query, latitud, longitud)
                            val pois = results.mapNotNull { json ->
                                parsePOI(json, categoria, latitud, longitud)
                            }

                            if (pois.isNotEmpty()) {
                                Log.d("MUUL", "Text '$query': ${pois.size} POIs")
                            }

                            pois
                        } catch (e: Exception) {
                            Log.e("MUUL", "Error text '$query': ${e.message}")
                            emptyList()
                        }
                    }
                }
            }

            (categoryTasks + textTasks).awaitAll().flatten()
        }

        val resultado = allPois.distinctBy { it.nombre.lowercase().trim() }
        Log.d("MUUL", "═══ POIs TOTALES: ${resultado.size} ═══")
        resultado.forEach {
            Log.d("MUUL", "  ✓ ${it.emoji} ${it.nombre} [${it.categoria}]")
        }

        memoryCache[cacheKey] = resultado

        return resultado
    }

    // ── SearchBox Category API ──
    private suspend fun searchByCategory(
        category: String,
        lat: Double,
        lng: Double
    ): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.mapbox.com/search/searchbox/v1/category/${category}" +
                    "?proximity=${lng},${lat}" +
                    "&origin=${lng},${lat}" +
                    "&limit=12" +
                    "&language=es" +
                    "&access_token=${accessToken}"

            val response = requestText(url)
            val json = JSONObject(response)
            val features = json.getJSONArray("features")

            val results = mutableListOf<JSONObject>()
            for (i in 0 until features.length()) {
                results.add(features.getJSONObject(i))
            }
            results
        } catch (e: Exception) {
            Log.e("MUUL", "  HTTP error category '$category': ${e.message}")
            emptyList()
        }
    }

    // ── SearchBox Text/Forward API ──
    private suspend fun searchByText(
        query: String,
        lat: Double,
        lng: Double
    ): List<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val bbox = "${lng - 0.25},${lat - 0.25},${lng + 0.25},${lat + 0.25}"
            val url = "https://api.mapbox.com/search/searchbox/v1/forward" +
                    "?q=${URLEncoder.encode(query, "UTF-8")}" +
                    "&proximity=${lng},${lat}" +
                    "&origin=${lng},${lat}" +
                    "&bbox=${bbox}" +
                    "&types=poi" +
                    "&limit=8" +
                    "&language=es" +
                    "&access_token=${accessToken}"

            val response = requestText(url)
            val json = JSONObject(response)
            val features = json.getJSONArray("features")

            val results = mutableListOf<JSONObject>()
            for (i in 0 until features.length()) {
                results.add(features.getJSONObject(i))
            }
            results
        } catch (e: Exception) {
            Log.e("MUUL", "  HTTP error text '$query': ${e.message}")
            emptyList()
        }
    }

    // ── Parsear JSON a POI ──
    private fun parsePOI(
        feature: JSONObject,
        categoria: String,
        userLat: Double,
        userLng: Double
    ): POI? {
        try {
            val properties = feature.getJSONObject("properties")
            val nombre = properties.optString("name", "")
            if (nombre.isBlank()) return null

            // Coordenadas
            val geometry = feature.getJSONObject("geometry")
            val coordinates = geometry.getJSONArray("coordinates")
            val poiLng = coordinates.getDouble(0)
            val poiLat = coordinates.getDouble(1)

            // Filtrar por distancia
            val distancia = HaversineUtils.haversine(userLat, userLng, poiLat, poiLng)
            if (distancia > MAX_DISTANCE_METERS) return null

            // Descartar calles/carreteras/colonias
            val descartarPrefijos = listOf(
                "Calle ", "Carretera ", "Avenida ", "Av. ",
                "Colonia ", "Col. ", "Fraccionamiento ",
                "Boulevard ", "Blvd. "
            )
            if (descartarPrefijos.any { nombre.startsWith(it, ignoreCase = true) }) return null

            val nombresGenericos = listOf("campeche", "mérida", "méxico")
            if (nombre.lowercase().trim() in nombresGenericos) return null

            // Dirección
            val fullAddress = properties.optString("full_address", "")
            val placeFormatted = properties.optString("place_formatted", "")
            val address = properties.optString("address", "")
            val direccion = fullAddress.ifEmpty { placeFormatted }.ifEmpty { address }.ifEmpty { null }

            val mapboxId = properties.optString("mapbox_id", "poi_${poiLat}_${poiLng}")

            return POI(
                id = mapboxId,
                nombre = nombre,
                descripcion = null,
                categoria = categoria,
                latitud = poiLat,
                longitud = poiLng,
                emoji = getEmoji(categoria),
                direccion = direccion,
                verificado = true
            )
        } catch (e: Exception) {
            Log.e("MUUL", "  Error parseando POI: ${e.message}")
            return null
        }
    }

    private fun getEmoji(categoria: String): String {
        return when (categoria) {
            "comida" -> "🍽️"
            "cultural" -> "🏛️"
            "deportes" -> "⚽"
            "tienda" -> "🛍️"
            "servicio" -> "🔧"
            "atraccion" -> "📸"
            else -> "📍"
        }
    }

    private fun cacheKey(latitud: Double, longitud: Double): String {
        return "%.2f,%.2f".format(latitud, longitud)
    }

    private fun requestText(urlText: String): String {
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        return try {
            if (connection.responseCode !in 200..299) {
                Log.e("MUUL", "HTTP ${connection.responseCode}: $urlText")
                return "{}"
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
