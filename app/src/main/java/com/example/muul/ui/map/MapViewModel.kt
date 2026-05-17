package com.example.muul.ui.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.model.POI
import com.example.muul.data.remote.POIRepository
import com.example.muul.util.LocationHelper
import com.example.muul.util.StepTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.hypot

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

    private var lastPoiRefreshLocation: Pair<Double, Double>? = null
    private val minPoiRefreshDistanceMeters = 35.0

    private val _zoomLevel = MutableStateFlow(14.0)
    val zoomLevel: StateFlow<Double> = _zoomLevel

    private val _viewportCenter = MutableStateFlow<Pair<Double, Double>?>(null)
    val viewportCenter: StateFlow<Pair<Double, Double>?> = _viewportCenter

    private val _viewportRadiusMeters = MutableStateFlow<Double?>(null)
    val viewportRadiusMeters: StateFlow<Double?> = _viewportRadiusMeters

    private val stepTracker = StepTracker(application)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError

    val categorias = listOf(
        "todos",
        "comida",
        "cultural",
        "deportes",
        "tienda",
        "servicio",
        "atraccion"
    )

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
        observeUserLocation()
    }

    fun updateZoomLevel(newZoom: Double) {
        _zoomLevel.value = newZoom
        applyFilters()
    }

    fun updateVisibleViewport(
        centerLat: Double,
        centerLng: Double,
        metersPerPixelAtLatitude: Double,
        viewportWidthPx: Int,
        viewportHeightPx: Int
    ) {
        if (viewportWidthPx <= 0 || viewportHeightPx <= 0) return

        val diagonalPixels = hypot(
            viewportWidthPx.toDouble(),
            viewportHeightPx.toDouble()
        )

        val estimatedRadiusMeters =
            (metersPerPixelAtLatitude * diagonalPixels) / 2.0

        _viewportCenter.value = centerLat to centerLng
        _viewportRadiusMeters.value = estimatedRadiusMeters.takeIf { it > 0 }
        applyFilters()
    }

    fun startRoute(routeId: String): Boolean {
        if (_isTracking.value) {
            Log.w("MUUL", "La ruta ya se está trackeando")
            return true
        }

        Log.d("MUUL", "Starting route tracking for ID: $routeId")

        val started = stepTracker.start(routeId)

        if (started) {
            _isTracking.value = true
            _trackingError.value = null
            Log.d("MUUL", "Tracking iniciado correctamente")
        } else {
            _isTracking.value = false
            _trackingError.value =
                "No se pudo iniciar el contador de pasos. Revisa permisos o sensor del dispositivo."
            Log.e("MUUL", "No se pudo iniciar el tracking de pasos")
        }

        return started
    }

    fun stopRoute() {
        if (!_isTracking.value) {
            Log.w("MUUL", "stopRoute llamado pero no había tracking activo")
            return
        }

        stepTracker.stop()
        _isTracking.value = false

        Log.d("MUUL", "Tracking detenido")
    }

    fun clearTrackingError() {
        _trackingError.value = null
    }

    private fun observeUserLocation() {
        viewModelScope.launch {
            try {
                Log.d("MUUL", "Buscando ubicación GPS inicial...")

                val initialLocation = locationHelper.getCurrentLocation()

                Log.d("MUUL", "Ubicación inicial obtenida: $initialLocation")

                _ubicacionUsuario.value = initialLocation
                lastPoiRefreshLocation = initialLocation
                _mapReady.value = true

                fetchPOIs()

                locationHelper.getLocationUpdates().collect { location ->
                    val previousLocation = _ubicacionUsuario.value
                    _ubicacionUsuario.value = location

                    val shouldRefreshPois =
                        previousLocation == null ||
                                HaversineUtils.haversine(
                                    previousLocation.first,
                                    previousLocation.second,
                                    location.first,
                                    location.second
                                ) >= minPoiRefreshDistanceMeters

                    if (shouldRefreshPois && lastPoiRefreshLocation != location) {
                        lastPoiRefreshLocation = location
                        fetchPOIs()
                    }
                }
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

                Log.d(
                    "MUUL",
                    "Buscando POIs cerca de: ${ubicacion.first}, ${ubicacion.second}"
                )

                val data = repository.fetchPOIsCercanos(
                    ubicacion.first,
                    ubicacion.second
                )

                Log.d("MUUL", "POIs encontrados: ${data.size}")

                if (data.isNotEmpty()) {
                    data.take(3).forEach {
                        Log.d(
                            "MUUL",
                            "POI: ${it.nombre} [${it.categoria}] (${it.latitud}, ${it.longitud})"
                        )
                    }
                } else {
                    Log.w("MUUL", "No se encontraron POIs")
                }

                val dataConDistancia = data.map { poi ->
                    val distance = HaversineUtils.haversine(
                        ubicacion.first,
                        ubicacion.second,
                        poi.latitud,
                        poi.longitud
                    )

                    poi.copy(distanciaMetros = distance)
                }.sortedBy { it.distanciaMetros }

                _pois.value = dataConDistancia
                applyFilters()

                Log.d(
                    "MUUL",
                    "filteredPois actualizado: ${_filteredPois.value.size}"
                )
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
        val zoom = _zoomLevel.value

        val maxDistanceMeters = when {
            zoom >= 17 -> 400
            zoom >= 15 -> 1200
            zoom >= 13 -> 2500
            zoom >= 11 -> 4500
            else -> 7000
        }

        val userLocation = _ubicacionUsuario.value ?: Pair(19.8440, -90.5300)

        result = result.filter { poi ->
            val distance = HaversineUtils.haversine(
                userLocation.first,
                userLocation.second,
                poi.latitud,
                poi.longitud
            )

            distance <= maxDistanceMeters
        }

        val viewportCenter = _viewportCenter.value
        val viewportRadius = _viewportRadiusMeters.value

        if (viewportCenter != null && viewportRadius != null) {
            result = result.filter { poi ->
                val distance = HaversineUtils.haversine(
                    viewportCenter.first,
                    viewportCenter.second,
                    poi.latitud,
                    poi.longitud
                )

                distance <= viewportRadius
            }
        }

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

        val maxVisiblePois = when {
            query.isNotBlank() -> 80
            filter != "todos" -> 90
            zoom >= 17 -> 120
            zoom >= 15 -> 90
            zoom >= 13 -> 60
            else -> 35
        }

        val selectedPoi = _selectedPoi.value
        _filteredPois.value = result
            .sortedWith(
                compareBy<POI> { selectedPoi?.id != it.id }
                    .thenBy { it.distanciaMetros }
            )
            .take(maxVisiblePois)
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

    override fun onCleared() {
        if (stepTracker.isRunning()) {
            stepTracker.stop()
        }

        super.onCleared()
    }
}
