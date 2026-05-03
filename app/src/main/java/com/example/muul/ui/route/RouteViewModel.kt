package com.example.muul.ui.route

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.muul.data.local.HaversineUtils
import com.example.muul.data.local.UserRepository
import com.example.muul.data.model.POI
import com.example.muul.data.model.Ruta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RouteViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepo = UserRepository(application)

    private val _currentRoute = MutableStateFlow<Ruta?>(null)
    val currentRoute: StateFlow<Ruta?> = _currentRoute

    private val _routeHistory = MutableStateFlow<List<Ruta>>(emptyList())
    val routeHistory: StateFlow<List<Ruta>> = _routeHistory

    private val _maxPlaces = 10
    val maxPlaces: Int = _maxPlaces

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

        // Crear nueva ruta con estos lugares
        val newRoute = Ruta(
            nombre = "Ruta Sorpresa",
            lugares = randomPois
        )
        _currentRoute.value = newRoute
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
}
