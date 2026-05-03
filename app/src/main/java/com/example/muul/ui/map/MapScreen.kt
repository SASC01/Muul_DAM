package com.example.muul.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muul.ui.route.RouteBottomSheet
import com.example.muul.ui.route.RouteViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import kotlin.math.abs

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    routeViewModel: RouteViewModel = viewModel()
) {
    val pois by viewModel.filteredPois.collectAsState()
    val selectedPoi by viewModel.selectedPoi.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val ubicacion by viewModel.ubicacionUsuario.collectAsState()
    val mapReady by viewModel.mapReady.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val currentRoute by routeViewModel.currentRoute.collectAsState()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var poiManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var userManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var routeLineManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    var currentPoisRef by remember { mutableStateOf<List<com.example.muul.data.model.POI>>(emptyList()) }
    var showRouteSheet by remember { mutableStateOf(false) }

    currentPoisRef = pois

    Box(modifier = Modifier.fillMaxSize()) {
        if (mapReady) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapView = this

                        val center = ubicacion?.let {
                            Point.fromLngLat(it.second, it.first)
                        } ?: Point.fromLngLat(-90.5300, 19.8440)

                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(center)
                                .zoom(14.0)
                                .build()
                        )
                        mapboxMap.loadStyle(Style.LIGHT) {
                            // Estilo cargado
                        }

                        gestures.pitchEnabled = true
                        gestures.scrollEnabled = true
                        gestures.rotateEnabled = true
                        gestures.pinchToZoomEnabled = true
                        gestures.doubleTapToZoomInEnabled = true
                        gestures.doubleTouchToZoomOutEnabled = true

                        // Listener para cambios de cámara (Mapbox v11)
                        mapboxMap.subscribeCameraChanged {
                            viewModel.updateZoomLevel(mapboxMap.cameraState.zoom)
                        }

                        poiManager = annotations.createCircleAnnotationManager()
                        userManager = annotations.createCircleAnnotationManager()
                        routeLineManager = annotations.createPolylineAnnotationManager()

                        poiManager?.addClickListener { annotation ->
                            val clickedPoi = currentPoisRef.minByOrNull { poi ->
                                val latDiff = abs(poi.latitud - annotation.point.latitude())
                                val lngDiff = abs(poi.longitud - annotation.point.longitude())
                                latDiff + lngDiff
                            }
                            clickedPoi?.let {
                                val latDiff = abs(it.latitud - annotation.point.latitude())
                                val lngDiff = abs(it.longitud - annotation.point.longitude())
                                if (latDiff + lngDiff < 0.001) {
                                    viewModel.selectPoi(it)
                                }
                            }
                            true
                        }
                    }
                },
                update = { _ ->
                    poiManager?.let { manager ->
                        manager.deleteAll()
                        val annotations = pois.map { poi ->
                            val color = when (poi.categoria) {
                                "comida" -> "#FF6B35"
                                "cultural" -> "#7C3AED"
                                "deportes" -> "#10B981"
                                "tienda" -> "#F59E0B"
                                "servicio" -> "#6B7280"
                                "atraccion" -> "#EC4899"
                                else -> "#003E6F"
                            }
                            CircleAnnotationOptions()
                                .withPoint(Point.fromLngLat(poi.longitud, poi.latitud))
                                .withCircleRadius(8.0)
                                .withCircleColor(color)
                                .withCircleStrokeWidth(2.0)
                                .withCircleStrokeColor("#FFFFFF")
                        }
                        if (annotations.isNotEmpty()) {
                            manager.create(annotations)
                        }
                    }

                    userManager?.let { manager ->
                        manager.deleteAll()
                        ubicacion?.let { (lat, lng) ->
                            manager.create(
                                CircleAnnotationOptions()
                                    .withPoint(Point.fromLngLat(lng, lat))
                                    .withCircleRadius(10.0)
                                    .withCircleColor("#4285F4")
                                    .withCircleStrokeWidth(3.0)
                                    .withCircleStrokeColor("#FFFFFF")
                            )
                        }
                    }

                    routeLineManager?.let { manager ->
                        manager.deleteAll()
                        val route = currentRoute
                        if (route != null && route.lugares.size >= 2) {
                            val points = route.lugares.map {
                                Point.fromLngLat(it.longitud, it.latitud)
                            }
                            val polylineOptions = PolylineAnnotationOptions()
                                .withPoints(points)
                                .withLineColor("#003E6F")
                                .withLineWidth(3.0)
                            manager.create(polylineOptions)
                        }
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            MuulSearchBar(viewModel)
            FilterChips(viewModel)
        }

        FloatingActionButton(
            onClick = {
                viewModel.refreshLocation()
                ubicacion?.let { (lat, lng) ->
                    mapView?.mapboxMap?.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(lng, lat))
                            .zoom(15.0)
                            .build()
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Mi ubicación",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        FloatingActionButton(
            onClick = {
                ubicacion?.let { (lat, lng) ->
                    routeViewModel.surpriseMe(lat, lng, pois, maxDistanceKm = 5.0)
                    showRouteSheet = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 80.dp),
            containerColor = Color(0xFFFDD835),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Sorprendeme",
                tint = Color(0xFF003E6F)
            )
        }

        val route = currentRoute
        val hasRouteItems = route?.lugares?.isNotEmpty() == true
        FloatingActionButton(
            onClick = {
                if (isTracking) {
                    viewModel.stopRoute()
                } else {
                    if (route != null && hasRouteItems) {
                        viewModel.startRoute(route.id)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            containerColor = when {
                isTracking -> MaterialTheme.colorScheme.error
                hasRouteItems -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isTracking) "Detener ruta" else "Iniciar ruta",
                tint = if (hasRouteItems || isTracking) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!loading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .padding(bottom = 32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${pois.size} lugares",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "Buscando lugares cercanos...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        if (showRouteSheet) {
            RouteBottomSheet(
                routeViewModel = routeViewModel,
                onDismiss = { showRouteSheet = false },
                onStartTracking = { routeToTrack ->
                    viewModel.startRoute(routeToTrack.id)
                    showRouteSheet = false
                }
            )
        }

        selectedPoi?.let { poi ->
            POIBottomSheet(
                poi = poi,
                distanciaTexto = viewModel.formatDistance(poi.distanciaMetros),
                onDismiss = { viewModel.clearSelectedPoi() },
                onAddToRoute = {
                    routeViewModel.addToRoute(poi)
                    viewModel.clearSelectedPoi()
                    showRouteSheet = true
                }
            )
        }
    }
}
