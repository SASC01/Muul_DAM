package com.example.muul.ui.route

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.local.RoutePlanner
import com.example.muul.data.local.RoutingRepository
import com.example.muul.data.local.RouteStorageRepository
import com.example.muul.data.local.UserRepository
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
    private val userRepo = UserRepository(application)
    private val routeRepo = RouteStorageRepository(application)

    private val _currentRoute = MutableStateFlow<Ruta?>(null)
    val currentRoute: StateFlow<Ruta?> = _currentRoute

    // Geometry for the current route (list of lat,lng pairs following streets)
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

    // Agregar un lugar a la ruta actual
    fun addToRoute(poi: POI) {
        val current = _currentRoute.value ?: Ruta()
        if (current.lugares.size < _maxPlaces) {
            val newRoute = current.copy(
                lugares = current.lugares + poi
            )
            _currentRoute.value = newRoute
        }
    }

    // Remover un lugar de la ruta
    fun removeFromRoute(index: Int) {
        val current = _currentRoute.value ?: return
        if (index in current.lugares.indices) {
            val newRoute = current.copy(
                lugares = current.lugares.filterIndexed { i, _ -> i != index }
            )
            _currentRoute.value = newRoute
        }
    }

    // Limpiar ruta actual
    fun clearRoute() {
        _currentRoute.value = null
    }

    fun setTransportMode(transportMode: TransportMode) {
        _selectedTransportMode.value = transportMode
        updateCurrentRouteTransport()
    }

    // Recompute route geometry and metrics using routing API
    private fun recomputeRouteForTransport() {
        val route = _currentRoute.value ?: return
        if (route.lugares.size < 2) return

        viewModelScope.launch {
            // throttle/recompute guard handled by lastRecompute* checks in caller
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
                    // Update persisted route metrics in memory
                    _currentRoute.value = route.copy(
                        distanciaTotal = routing.distanceMeters,
                        plannedDurationMinutes = (routing.durationSeconds / 60.0).toInt()
                    )
                    // record last recompute
                    lastRecomputeLocation = Pair(start.latitud, start.longitud)
                    lastRecomputeTimestamp = System.currentTimeMillis()
                } else {
                    // Fallback to Haversine if routing failed
                    _currentRouteGeometry.value = emptyList()
                    _currentRoute.value = route.copy(
                        distanciaTotal = RoutePlanner.routeDistanceMeters(route),
                        plannedDurationMinutes = RoutePlanner.estimateMinutes(route, _selectedTransportMode.value)
                    )
                }
            } catch (e: Exception) {
                // Keep previous geometry on failure
                lastRecomputeTimestamp = System.currentTimeMillis()
            }
        }
    }

    // Throttle state to avoid frequent Directions API calls
    private var lastRecomputeLocation: Pair<Double, Double>? = null
    private var lastRecomputeTimestamp: Long = 0L
    private val MIN_RECOMPUTE_INTERVAL_MS = 15_000L // 15s
    private val MIN_MOVE_METERS = 30.0 // 30 meters

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

        // Ensure the route keeps the user's location as first point for display,
        // but persist the route WITHOUT the user_location as visible stops.
        val routeWithLocation = if (route.lugares.isNotEmpty() && route.lugares[0].id == "user_location") {
            route
        } else {
            Log.w("MUUL_ROUTE", "User location not present as first point when saving route")
            route
        }

        val visiblePois = routeWithLocation.lugares.filterNot { it.id == "user_location" }
        if (visiblePois.isEmpty()) {
            Log.w("MUUL_ROUTE", "No visible POIs to save")
            return null
        }

        val transportMode = _selectedTransportMode.value
        val persistedRoute = routeWithLocation.copy(lugares = visiblePois)
        val savedRoute = persistedRoute.copy(
            nombre = routeName.ifBlank { routeWithLocation.nombre },
            transportMode = transportMode.name,
            plannedDurationMinutes = RoutePlanner.estimateMinutes(persistedRoute, transportMode),
            distanciaTotal = RoutePlanner.routeDistanceMeters(persistedRoute),
            startTimeMinutes = _startTimeMinutes.value
        )

        viewModelScope.launch {
            Log.d("MUUL_ROUTE", "Saving route: ${savedRoute.nombre} with ID: ${savedRoute.id}")
            routeRepo.saveRoute(savedRoute)
            refreshSavedRoutes()
        }

        // Keep _currentRoute including the user_location so the UI shows the start point
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

    // Guardar ruta actual (al terminar de rastrear pasos)
    fun saveRoute(steps: Int) {
        val route = _currentRoute.value ?: return
        if (route.lugares.isEmpty()) return

        val routeWithSteps = route.copy(pasosTotales = steps)

        viewModelScope.launch {
            // Guardar en historial local
            val currentHistory = _routeHistory.value.toMutableList()
            currentHistory.add(routeWithSteps)
            _routeHistory.value = currentHistory

            // Guardar en perfil de usuario
            // (aquí podrías persistir en UserRepository si lo necesitas)
        }

        clearRoute()
    }

    // Prepend user location as first point in route
    fun prependUserLocation(userLat: Double, userLng: Double) {
        // If there's no current route, create an empty one so we can prepend the user location
        val current = _currentRoute.value ?: Ruta()

        // Create a POI representing user's current position
        val userLocationPoi = POI(
            id = "user_location",
            nombre = "Tu ubicación",
            categoria = "inicio",
            latitud = userLat,
            longitud = userLng,
            descripcion = "Punto de inicio de la ruta"
        )

        if (current.lugares.isEmpty()) {
            // If no places yet, just set the user location as the only point
            val newRoute = current.copy(lugares = listOf(userLocationPoi))
            _currentRoute.value = newRoute
            Log.d("MUUL_ROUTE", "Posición del usuario agregada como punto inicial (ruta vacía)")
            // Recompute route geometry (no-op if not enough points)
            if (shouldRecomputeForLocation(userLat, userLng)) {
                recomputeRouteForTransport()
            }
            return
        }

        // If first place is already user_location, update its coordinates
        val first = current.lugares[0]
        if (first.id == "user_location") {
            val updatedFirst = first.copy(latitud = userLat, longitud = userLng)
            val newPlaces = current.lugares.toMutableList()
            newPlaces[0] = updatedFirst
            _currentRoute.value = current.copy(lugares = newPlaces)
            Log.d("MUUL_ROUTE", "Posición del usuario actualizada en la ruta")
            if (shouldRecomputeForLocation(userLat, userLng)) {
                recomputeRouteForTransport()
            }
            return
        }

        // Otherwise, prepend only if far enough from current first point
        if (HaversineUtils.haversine(current.lugares[0].latitud, current.lugares[0].longitud, userLat, userLng) > 100.0) {
            val newRoute = current.copy(lugares = listOf(userLocationPoi) + current.lugares)
            _currentRoute.value = newRoute
            Log.d("MUUL_ROUTE", "Posición del usuario agregada como punto inicial")
            if (shouldRecomputeForLocation(userLat, userLng)) {
                recomputeRouteForTransport()
            }
        }
    }

    // Sorprendeme: seleccionar lugares aleatorios dentro de distancia máxima
    fun surpriseMe(
        userLat: Double,
        userLng: Double,
        availablePois: List<POI>,
        maxDistanceKm: Double = 5.0
    ) {
        val maxDistanceM = maxDistanceKm * 1000

        // Filtrar POIs dentro de la distancia
        val nearbyPois = availablePois.filter { poi ->
            val distance = HaversineUtils.haversine(userLat, userLng, poi.latitud, poi.longitud)
            distance <= maxDistanceM
        }

        if (nearbyPois.isEmpty()) return

        // Seleccionar 3-5 lugares aleatorios
        val selectedCount = (3..5).random().coerceAtMost(nearbyPois.size)
        val randomPois = nearbyPois.shuffled().take(selectedCount)

        // Crear POI para posición del usuario como primer punto
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
        Log.d("MUUL_ROUTE", "Ruta sorpresa creada con posición del usuario como inicio")
        lastRecomputeTimestamp = 0L
        recomputeRouteForTransport()
    }

    // Calcular distancia total de la ruta
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

        // Recompute using routing API
        recomputeRouteForTransport()
    }
}
