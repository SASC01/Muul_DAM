package com.example.muul.ui.route

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.DataModule
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.local.RoutePlanner
import com.example.muul.data.local.RoutingRepository
import com.example.muul.data.local.RouteStorageRepository
import com.example.muul.data.model.POI
import com.example.muul.data.model.ItineraryStop
import com.example.muul.data.model.Ruta
import com.example.muul.data.model.TransportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RouteViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepo = DataModule.getUserRepository(application)
    private val routeRepo = RouteStorageRepository(application)

    private val _currentRoute = MutableStateFlow<Ruta?>(null)
    val currentRoute: StateFlow<Ruta?> = _currentRoute

    private val _currentRouteGeometry = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val currentRouteGeometry: StateFlow<List<Pair<Double, Double>>> = _currentRouteGeometry

    private val _routeHistory = MutableStateFlow<List<Ruta>>(emptyList())
    val routeHistory: StateFlow<List<Ruta>> = _routeHistory

    private val _savedRoutes = MutableStateFlow<List<Ruta>>(emptyList())
    val savedRoutes: StateFlow<List<Ruta>> = _savedRoutes

    private val _selectedSavedRoute = MutableStateFlow<Ruta?>(null)
    val selectedSavedRoute: StateFlow<Ruta?> = _selectedSavedRoute

    private val _selectedTransportMode = MutableStateFlow(TransportMode.WALKING)
    val selectedTransportMode: StateFlow<TransportMode> = _selectedTransportMode

    private val _itineraryMinutes = MutableStateFlow(240)
    val itineraryMinutes: StateFlow<Int> = _itineraryMinutes

    private val _startTimeMinutes = MutableStateFlow(9 * 60)
    val startTimeMinutes: StateFlow<Int> = _startTimeMinutes

    private val _maxPlaces = 10
    val maxPlaces: Int = _maxPlaces

    init {
        refreshSavedRoutes()
    }

    fun addToRoute(poi: POI) {
        val current = _currentRoute.value ?: Ruta()
        if (current.lugares.size < _maxPlaces) {
            val newRoute = current.copy(
                lugares = current.lugares + poi
            )
            _currentRoute.value = newRoute
            recomputeRouteForTransport()
        }
    }

    fun removeFromRoute(index: Int) {
        val current = _currentRoute.value ?: return
        if (index in current.lugares.indices) {
            val newRoute = current.copy(
                lugares = current.lugares.filterIndexed { i, _ -> i != index }
            )
            _currentRoute.value = newRoute
            recomputeRouteForTransport()
        }
    }

    fun clearRoute() {
        _currentRoute.value = null
        _currentRouteGeometry.value = emptyList()
    }

    fun setTransportMode(transportMode: TransportMode) {
        _selectedTransportMode.value = transportMode
        updateCurrentRouteTransport()
    }

    private fun recomputeRouteForTransport() {
        val route = _currentRoute.value ?: return
        if (route.lugares.size < 2) {
            _currentRouteGeometry.value = emptyList()
            _currentRoute.value = route.copy(
                distanciaTotal = 0.0,
                plannedDurationMinutes = 0
            )
            return
        }

        viewModelScope.launch {
            try {
                val start = route.lugares.first()
                val rest = route.lugares.drop(1)
                val routing = withContext(Dispatchers.IO) {
                    RoutingRepository(getApplication()).getRoute(
                        start.latitud,
                        start.longitud,
                        rest,
                        _selectedTransportMode.value
                    )
                }
                if (routing != null) {
                    _currentRouteGeometry.value = routing.geometry
                    _currentRoute.value = route.copy(
                        distanciaTotal = routing.distanceMeters,
                        plannedDurationMinutes = (routing.durationSeconds / 60.0).toInt()
                    )
                    lastRecomputeLocation = Pair(start.latitud, start.longitud)
                    lastRecomputeTimestamp = System.currentTimeMillis()
                } else {
                    _currentRouteGeometry.value = emptyList()
                    _currentRoute.value = route.copy(
                        distanciaTotal = RoutePlanner.routeDistanceMeters(route),
                        plannedDurationMinutes = RoutePlanner.estimateMinutes(route, _selectedTransportMode.value)
                    )
                }
            } catch (e: Exception) {
                lastRecomputeTimestamp = System.currentTimeMillis()
            }
        }
    }

    private var lastRecomputeLocation: Pair<Double, Double>? = null
    private var lastRecomputeTimestamp: Long = 0L
    private val MIN_RECOMPUTE_INTERVAL_MS = 15_000L 
    private val MIN_MOVE_METERS = 30.0

    private fun shouldRecomputeForLocation(lat: Double, lng: Double): Boolean {
        val now = System.currentTimeMillis()
        val lastLoc = lastRecomputeLocation
        if (now - lastRecomputeTimestamp > MIN_RECOMPUTE_INTERVAL_MS) return true
        if (lastLoc == null) return true
        val moved = HaversineUtils.haversine(lastLoc.first, lastLoc.second, lat, lng)
        return moved >= MIN_MOVE_METERS
    }

    fun setItineraryMinutes(minutes: Int) {
        _itineraryMinutes.value = minutes.coerceAtLeast(60)
    }

    fun setStartTimeMinutes(minutes: Int) {
        _startTimeMinutes.value = minutes.coerceIn(0, 1439)
    }

    fun saveCurrentRoute(routeName: String): Ruta? {
        val route = _currentRoute.value ?: return null
        val routeWithLocation = if (route.lugares.isNotEmpty() && route.lugares[0].id == "user_location") {
            route
        } else {
            route
        }

        val visiblePois = routeWithLocation.lugares.filterNot { it.id == "user_location" }
        if (visiblePois.isEmpty()) return null

        val transportMode = _selectedTransportMode.value
        val persistedRoute = routeWithLocation.copy(lugares = visiblePois)
        val savedRoute = persistedRoute.copy(
            nombre = routeName.ifBlank { routeWithLocation.nombre },
            transportMode = transportMode.name,
            plannedDurationMinutes = routeWithLocation.plannedDurationMinutes
                .takeIf { it > 0 }
                ?: RoutePlanner.estimateMinutes(persistedRoute, transportMode),
            distanciaTotal = routeWithLocation.distanciaTotal
                .takeIf { it > 0.0 }
                ?: RoutePlanner.routeDistanceMeters(persistedRoute),
            startTimeMinutes = _startTimeMinutes.value
        )

        viewModelScope.launch {
            routeRepo.saveRoute(savedRoute)
            refreshSavedRoutes()
        }

        _currentRoute.value = routeWithLocation
        return savedRoute
    }

    fun refreshSavedRoutes() {
        _savedRoutes.value = routeRepo.getSavedRoutes()
    }

    fun deleteSavedRoute(routeId: String) {
        viewModelScope.launch {
            routeRepo.deleteRoute(routeId)
            refreshSavedRoutes()
        }
    }

    fun selectSavedRoute(route: Ruta?) {
        _selectedSavedRoute.value = route
    }

    fun buildItinerary(route: Ruta): List<ItineraryStop> {
        val transport = runCatching { TransportMode.valueOf(route.transportMode) }.getOrDefault(TransportMode.WALKING)
        return RoutePlanner.buildItinerary(
            route = route,
            transportMode = transport,
            totalAvailableMinutes = if (route.plannedDurationMinutes > 0) route.plannedDurationMinutes else _itineraryMinutes.value,
            startTimeMinutes = route.startTimeMinutes
        )
    }

    fun estimateCurrentRouteMinutes(): Int {
        val route = _currentRoute.value ?: return 0
        return RoutePlanner.estimateMinutes(route, _selectedTransportMode.value)
    }

    fun saveRoute(steps: Int) {
        val route = _currentRoute.value ?: return
        if (route.lugares.isEmpty()) return
        val routeWithSteps = route.copy(pasosTotales = steps)
        viewModelScope.launch {
            val currentHistory = _routeHistory.value.toMutableList()
            currentHistory.add(routeWithSteps)
            _routeHistory.value = currentHistory
        }
        clearRoute()
    }

    fun prependUserLocation(userLat: Double, userLng: Double) {
        val current = _currentRoute.value ?: Ruta()
        val userLocationPoi = POI(
            id = "user_location",
            nombre = "Tu ubicación",
            categoria = "inicio",
            latitud = userLat,
            longitud = userLng,
            descripcion = "Punto de inicio de la ruta"
        )

        if (current.lugares.isEmpty()) {
            val newRoute = current.copy(lugares = listOf(userLocationPoi))
            _currentRoute.value = newRoute
            if (shouldRecomputeForLocation(userLat, userLng)) {
                recomputeRouteForTransport()
            }
            return
        }

        val first = current.lugares[0]
        if (first.id == "user_location") {
            val updatedFirst = first.copy(latitud = userLat, longitud = userLng)
            val newPlaces = current.lugares.toMutableList()
            newPlaces[0] = updatedFirst
            _currentRoute.value = current.copy(lugares = newPlaces)
            if (shouldRecomputeForLocation(userLat, userLng)) {
                recomputeRouteForTransport()
            }
            return
        }

        if (HaversineUtils.haversine(current.lugares[0].latitud, current.lugares[0].longitud, userLat, userLng) > 100.0) {
            val newRoute = current.copy(lugares = listOf(userLocationPoi) + current.lugares)
            _currentRoute.value = newRoute
            if (shouldRecomputeForLocation(userLat, userLng)) {
                recomputeRouteForTransport()
            }
        }
    }

    fun surpriseMe(
        userLat: Double,
        userLng: Double,
        availablePois: List<POI>,
        maxDistanceKm: Double = 5.0
    ) {
        val maxDistanceM = maxDistanceKm * 1000
        val nearbyPois = availablePois.filter { poi ->
            val distance = HaversineUtils.haversine(userLat, userLng, poi.latitud, poi.longitud)
            distance <= maxDistanceM
        }
        if (nearbyPois.isEmpty()) return
        val selectedCount = (3..5).random().coerceAtMost(nearbyPois.size)
        val randomPois = nearbyPois.shuffled().take(selectedCount)
        val userLocationPoi = POI(
            id = "user_location",
            nombre = "Tu ubicación",
            categoria = "inicio",
            latitud = userLat,
            longitud = userLng,
            descripcion = "Punto de inicio de la ruta"
        )
        val routePois = listOf(userLocationPoi) + randomPois
        val provisionalRoute = Ruta(
            nombre = "Ruta Sorpresa",
            lugares = routePois,
            transportMode = _selectedTransportMode.value.name,
            plannedDurationMinutes = RoutePlanner.estimateMinutes(Ruta(lugares = routePois.filterNot { it.id == "user_location" }), _selectedTransportMode.value),
            distanciaTotal = RoutePlanner.routeDistanceMeters(Ruta(lugares = routePois.filterNot { it.id == "user_location" })),
            startTimeMinutes = _startTimeMinutes.value
        )
        _currentRoute.value = provisionalRoute
        lastRecomputeTimestamp = 0L
        recomputeRouteForTransport()
    }

    fun calculateTotalDistance(): Double {
        val route = _currentRoute.value ?: return 0.0
        if (route.lugares.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until route.lugares.size - 1) {
            val p1 = route.lugares[i]
            val p2 = route.lugares[i + 1]
            total += HaversineUtils.haversine(p1.latitud, p1.longitud, p2.latitud, p2.longitud)
        }
        return total
    }

    private fun updateCurrentRouteTransport() {
        val current = _currentRoute.value ?: return
        val transportMode = _selectedTransportMode.value
        _currentRoute.value = current.copy(
            transportMode = transportMode.name
        )
        recomputeRouteForTransport()
    }
}
