package com.example.muul.ui.map

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.model.POI
import com.example.muul.data.remote.PoiDescriptionInfo
import com.example.muul.data.remote.PoiDescriptionRepository
import com.example.muul.data.remote.POIRepository
import com.example.muul.util.LocationHelper
import com.example.muul.util.StepTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.hypot

data class MapSearchSuggestion(
    val title: String,
    val subtitle: String,
    val type: MapSearchSuggestionType,
    val poi: POI? = null
)

enum class MapSearchSuggestionType {
    POI,
    HISTORY,
    QUERY
}

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = POIRepository()
    val locationHelper = LocationHelper(application)

    private val _pois = MutableStateFlow<List<POI>>(emptyList())
    val pois: StateFlow<List<POI>> = _pois

    private val _filteredPois = MutableStateFlow<List<POI>>(emptyList())
    val filteredPois: StateFlow<List<POI>> = _filteredPois

    private val _selectedPoi = MutableStateFlow<POI?>(null)
    val selectedPoi: StateFlow<POI?> = _selectedPoi

    private val poiDescriptionRepository = PoiDescriptionRepository()
    private val descriptionCache = mutableMapOf<String, PoiDescriptionInfo?>()
    private var descriptionJob: Job? = null

    private val _selectedPoiDescription = MutableStateFlow<PoiDescriptionInfo?>(null)
    val selectedPoiDescription: StateFlow<PoiDescriptionInfo?> = _selectedPoiDescription

    private val _selectedPoiDescriptionLoading = MutableStateFlow(false)
    val selectedPoiDescriptionLoading: StateFlow<Boolean> = _selectedPoiDescriptionLoading

    private val _activeFilter = MutableStateFlow("todos")
    val activeFilter: StateFlow<String> = _activeFilter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val recentSearches = mutableListOf<String>()

    private val _searchSuggestions = MutableStateFlow<List<MapSearchSuggestion>>(emptyList())
    val searchSuggestions: StateFlow<List<MapSearchSuggestion>> = _searchSuggestions

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _ubicacionUsuario = MutableStateFlow<Pair<Double, Double>?>(null)
    val ubicacionUsuario: StateFlow<Pair<Double, Double>?> = _ubicacionUsuario

    private val _mapReady = MutableStateFlow(false)
    val mapReady: StateFlow<Boolean> = _mapReady

    private var lastPoiRefreshLocation: Pair<Double, Double>? = null
    private val minPoiRefreshDistanceMeters = 180.0

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

                val fallbackLocation = Pair(19.8440, -90.5300)
                _ubicacionUsuario.value = fallbackLocation
                lastPoiRefreshLocation = fallbackLocation
                _mapReady.value = true
                fetchPOIs(fallbackLocation)

                val initialLocation = locationHelper.getCurrentLocation()

                Log.d("MUUL", "Ubicación inicial obtenida: $initialLocation")

                if (initialLocation != null) {
                    _ubicacionUsuario.value = initialLocation

                    val movedFromFallback = HaversineUtils.haversine(
                        fallbackLocation.first,
                        fallbackLocation.second,
                        initialLocation.first,
                        initialLocation.second
                    )

                    if (movedFromFallback >= minPoiRefreshDistanceMeters) {
                        lastPoiRefreshLocation = initialLocation
                        fetchPOIs(initialLocation)
                    }
                }

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
                        fetchPOIs(location)
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

    private fun fetchPOIs(targetLocation: Pair<Double, Double>? = null) {
        viewModelScope.launch {
            try {
                val ubicacion = targetLocation ?: _ubicacionUsuario.value ?: Pair(19.8440, -90.5300)

                Log.d(
                    "MUUL",
                    "Buscando POIs cerca de: ${ubicacion.first}, ${ubicacion.second}"
                )

                val data = withTimeoutOrNull(15_000L) {
                    withContext(Dispatchers.IO) {
                        repository.fetchPOIsCercanos(
                            ubicacion.first,
                            ubicacion.second
                        )
                    }
                } ?: emptyList()

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
                updateSearchSuggestions()

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
        updateSearchSuggestions()
    }

    fun selectSearchSuggestion(suggestion: MapSearchSuggestion) {
        rememberSearch(suggestion.title)

        val poi = suggestion.poi
        if (poi != null) {
            _searchQuery.value = poi.nombre
            _searchSuggestions.value = emptyList()
            applyFilters()
            selectPoi(poi)
            return
        }

        _searchQuery.value = suggestion.title
        _searchSuggestions.value = emptyList()
        applyFilters()
    }

    fun hideSearchSuggestions() {
        _searchSuggestions.value = emptyList()
    }

    private fun applyFilters() {
        var result = _pois.value

        val filter = _activeFilter.value
        val query = _searchQuery.value
        val zoom = _zoomLevel.value

        val maxDistanceMeters = when {
            zoom >= 17 -> 700
            zoom >= 15 -> 1600
            zoom >= 13 -> 3500
            zoom >= 11 -> 8000
            else -> 15000
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

        if (query.isBlank() && filter == "todos") {
            val minImportance = when {
                zoom < 12 -> 45
                zoom < 13 -> 35
                zoom < 14.5 -> 24
                else -> 0
            }

            if (minImportance > 0) {
                result = result.filter { poiImportanceScore(it) >= minImportance }
            }
        }

        val maxVisiblePois = when {
            query.isNotBlank() -> 80
            filter != "todos" -> 90
            zoom >= 17 -> 120
            zoom >= 15 -> 90
            zoom >= 13 -> 60
            zoom >= 12 -> 36
            else -> 18
        }

        val selectedPoi = _selectedPoi.value
        _filteredPois.value = if (query.isBlank() && filter == "todos") {
            selectBalancedPois(
                pois = result,
                maxVisiblePois = maxVisiblePois,
                selectedPoi = selectedPoi
            )
        } else {
            result
                .sortedWith(
                    compareBy<POI> { selectedPoi?.id != it.id }
                        .thenByDescending { poiImportanceScore(it) }
                        .thenBy { it.distanciaMetros }
                )
                .take(maxVisiblePois)
        }
    }

    fun selectPoi(poi: POI) {
        _selectedPoi.value = poi
        if (poi.descripcion.isNullOrBlank()) {
            loadExternalDescription(poi)
        } else {
            descriptionJob?.cancel()
            _selectedPoiDescription.value = null
            _selectedPoiDescriptionLoading.value = false
        }
    }

    fun clearSelectedPoi() {
        _selectedPoi.value = null
        descriptionJob?.cancel()
        _selectedPoiDescription.value = null
        _selectedPoiDescriptionLoading.value = false
    }

    private fun loadExternalDescription(poi: POI) {
        descriptionJob?.cancel()
        _selectedPoiDescription.value = null

        val cacheKey = poiDescriptionCacheKey(poi)
        if (descriptionCache.containsKey(cacheKey)) {
            _selectedPoiDescription.value = descriptionCache[cacheKey]
            _selectedPoiDescriptionLoading.value = false
            return
        }

        _selectedPoiDescriptionLoading.value = true
        descriptionJob = viewModelScope.launch {
            val description = withContext(Dispatchers.IO) {
                poiDescriptionRepository.fetchDescription(poi)
            }

            val currentPoi = _selectedPoi.value
            if (currentPoi != null && poiDescriptionCacheKey(currentPoi) == cacheKey) {
                descriptionCache[cacheKey] = description
                _selectedPoiDescription.value = description
                _selectedPoiDescriptionLoading.value = false
            }
        }
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.1f km", meters / 1000)
        } else {
            "${meters.toInt()} m"
        }
    }

    private fun poiDescriptionCacheKey(poi: POI): String {
        return poi.id.ifBlank {
            "${poi.nombre}_${poi.latitud}_${poi.longitud}"
        }
    }

    private fun updateSearchSuggestions() {
        val query = _searchQuery.value.trim()

        if (query.isBlank()) {
            _searchSuggestions.value = recentSearches.take(4).map {
                MapSearchSuggestion(
                    title = it,
                    subtitle = "Busqueda reciente",
                    type = MapSearchSuggestionType.HISTORY
                )
            }
            return
        }

        val normalizedQuery = normalizeSearchText(query)
        val poiSuggestions = _pois.value
            .asSequence()
            .mapNotNull { poi ->
                val haystack = normalizeSearchText(
                    listOfNotNull(
                        poi.nombre,
                        poi.direccion,
                        categoriaLabels[poi.categoria],
                        poi.descripcion
                    ).joinToString(" ")
                )

                if (!haystack.contains(normalizedQuery)) return@mapNotNull null

                val startsWithScore = if (normalizeSearchText(poi.nombre).startsWith(normalizedQuery)) 1 else 0
                poi to startsWithScore
            }
            .sortedWith(
                compareByDescending<Pair<POI, Int>> { it.second }
                    .thenByDescending { poiImportanceScore(it.first) }
                    .thenBy { it.first.distanciaMetros }
            )
            .take(5)
            .map { (poi, _) ->
                MapSearchSuggestion(
                    title = poi.nombre,
                    subtitle = poi.direccion?.takeIf { it.isNotBlank() }
                        ?: categoriaLabels[poi.categoria]
                        ?: "Lugar cercano",
                    type = MapSearchSuggestionType.POI,
                    poi = poi
                )
            }
            .toList()

        val querySuggestion = MapSearchSuggestion(
            title = query,
            subtitle = "Buscar \"$query\"",
            type = MapSearchSuggestionType.QUERY
        )

        _searchSuggestions.value = (poiSuggestions + querySuggestion).take(6)
    }

    private fun rememberSearch(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        recentSearches.removeAll { it.equals(cleanText, ignoreCase = true) }
        recentSearches.add(0, cleanText)
        while (recentSearches.size > 6) {
            recentSearches.removeLast()
        }
    }

    private fun poiImportanceScore(poi: POI): Int {
        val categoryScore = when (poi.categoria) {
            "cultural", "atraccion" -> 50
            "comida", "tienda" -> 30
            "deportes" -> 24
            "servicio" -> 18
            else -> 10
        }

        val verificationScore = if (poi.verificado) 30 else 0
        val contentScore = listOf(
            poi.descripcion,
            poi.direccion,
            poi.horario_apertura,
            poi.precio_rango
        ).count { !it.isNullOrBlank() } * 6

        return categoryScore + verificationScore + contentScore
    }

    private fun selectBalancedPois(
        pois: List<POI>,
        maxVisiblePois: Int,
        selectedPoi: POI?
    ): List<POI> {
        if (pois.size <= maxVisiblePois) return pois.sortedForMap(selectedPoi)

        val grouped = pois
            .groupBy { it.categoria }
            .mapValues { (_, categoryPois) -> categoryPois.sortedForMap(selectedPoi) }
            .toMutableMap()

        val categoriesInOrder = categorias
            .filter { it != "todos" }
            .filter { grouped[it].orEmpty().isNotEmpty() }

        if (categoriesInOrder.isEmpty()) {
            return pois.sortedForMap(selectedPoi).take(maxVisiblePois)
        }

        val perCategoryCap = ceil(maxVisiblePois.toDouble() / categoriesInOrder.size.toDouble())
            .toInt()
            .coerceAtLeast(2)
        val selected = mutableListOf<POI>()

        categoriesInOrder.forEach { category ->
            grouped[category]
                .orEmpty()
                .take(perCategoryCap)
                .forEach { poi ->
                    if (selected.none { it.id == poi.id }) {
                        selected.add(poi)
                    }
                }
        }

        if (selected.size < maxVisiblePois) {
            pois.sortedForMap(selectedPoi).forEach { poi ->
                if (selected.size >= maxVisiblePois) return@forEach
                if (selected.none { it.id == poi.id }) {
                    selected.add(poi)
                }
            }
        }

        return selected
            .sortedWith(
                compareBy<POI> { selectedPoi?.id != it.id }
                    .thenBy { it.distanciaMetros }
            )
            .take(maxVisiblePois)
    }

    private fun List<POI>.sortedForMap(selectedPoi: POI?): List<POI> {
        return sortedWith(
            compareBy<POI> { selectedPoi?.id != it.id }
                .thenByDescending { poiImportanceScore(it) }
                .thenBy { it.distanciaMetros }
        )
    }

    private fun normalizeSearchText(text: String): String {
        return java.text.Normalizer.normalize(text.lowercase(Locale.getDefault()), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    override fun onCleared() {
        if (stepTracker.isRunning()) {
            stepTracker.stop()
        }

        super.onCleared()
    }
}
