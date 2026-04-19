package com.example.muul.ui.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.model.POI
import com.example.muul.data.remote.POIRepository
import com.example.muul.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = POIRepository()
    val locationHelper = LocationHelper(application)

    private val _pois = MutableStateFlow<List<POI>>(emptyList())
    val pois: StateFlow<List<POI>> = _pois

    private val _filteredPois = MutableStateFlow<List<POI>>(emptyList())
    val filteredPois: StateFlow<List<POI>> = _filteredPois

    private val _selectedPoi = MutableStateFlow<POI?>(null)
    val selectedPoi: StateFlow<POI?> = _selectedPoi

    private val _activeFilter = MutableStateFlow("todos")
    val activeFilter: StateFlow<String> = _activeFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _ubicacionUsuario = MutableStateFlow<Pair<Double, Double>?>(null)
    val ubicacionUsuario: StateFlow<Pair<Double, Double>?> = _ubicacionUsuario

    private val _mapReady = MutableStateFlow(false)
    val mapReady: StateFlow<Boolean> = _mapReady

    val categorias = listOf("todos", "comida", "cultural", "deportes", "tienda", "servicio", "atraccion")

    val categoriaEmojis = mapOf(
        "todos" to "🗺️",
        "comida" to "🍽️",
        "cultural" to "🏛️",
        "deportes" to "⚽",
        "tienda" to "🛍️",
        "servicio" to "🔧",
        "atraccion" to "📸"
    )

    val categoriaLabels = mapOf(
        "todos" to "Todos",
        "comida" to "Comida",
        "cultural" to "Cultural",
        "deportes" to "Deportes",
        "tienda" to "Tiendas",
        "servicio" to "Servicios",
        "atraccion" to "Atracciones"
    )

    init {
        Log.d("MUUL", "ViewModel init - empezando")
        fetchUserLocation()
    }

    private fun fetchUserLocation() {
        viewModelScope.launch {
            try {
                Log.d("MUUL", "Buscando ubicación GPS...")
                val location = locationHelper.getCurrentLocation()
                Log.d("MUUL", "Ubicación obtenida: $location")
                _ubicacionUsuario.value = location
                _mapReady.value = true
                fetchPOIs()
            } catch (e: Exception) {
                Log.e("MUUL", "Error GPS: ${e.message}", e)
                _ubicacionUsuario.value = null
                _mapReady.value = true
                fetchPOIs()
            }
        }
    }

    private fun fetchPOIs() {
        viewModelScope.launch {
            try {
                val ubicacion = _ubicacionUsuario.value ?: Pair(19.8440, -90.5300)
                Log.d("MUUL", "Buscando POIs cerca de: ${ubicacion.first}, ${ubicacion.second}")

                val data = repository.fetchPOIsCercanos(ubicacion.first, ubicacion.second)
                Log.d("MUUL", "POIs encontrados: ${data.size}")

                if (data.isNotEmpty()) {
                    data.take(3).forEach {
                        Log.d("MUUL", "  POI: ${it.nombre} [${it.categoria}] (${it.latitud}, ${it.longitud})")
                    }
                } else {
                    Log.w("MUUL", "No se encontraron POIs!")
                }

                val dataConDistancia = data.map { poi ->
                    val dist = HaversineUtils.haversine(
                        ubicacion.first, ubicacion.second,
                        poi.latitud, poi.longitud
                    )
                    poi.copy(distanciaMetros = dist)
                }.sortedBy { it.distanciaMetros }

                _pois.value = dataConDistancia
                _filteredPois.value = dataConDistancia
                Log.d("MUUL", "filteredPois actualizado: ${_filteredPois.value.size}")
            } catch (e: Exception) {
                Log.e("MUUL", "Error cargando POIs: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                _ubicacionUsuario.value = location
                fetchPOIs()
            }
        }
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        var result = _pois.value
        val filter = _activeFilter.value
        val query = _searchQuery.value

        if (filter != "todos") {
            result = result.filter { it.categoria == filter }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.nombre.lowercase().contains(q) ||
                        (it.descripcion?.lowercase()?.contains(q) ?: false)
            }
        }
        _filteredPois.value = result
    }

    fun selectPoi(poi: POI) {
        _selectedPoi.value = poi
    }

    fun clearSelectedPoi() {
        _selectedPoi.value = null
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000)
        } else {
            "${meters.toInt()} m"
        }
    }
}