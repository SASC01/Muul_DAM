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
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.example.muul.ui.route.RouteBottomSheet
import com.example.muul.ui.route.RouteViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ClickInteraction
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
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.has
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.dsl.generated.not
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.toString
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextJustify
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.GeoJSONSourceData
import com.mapbox.geojson.LineString
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import kotlin.math.abs

private const val POI_SOURCE_ID = "pois-source"
private const val POI_CLUSTER_LAYER_ID = "pois-clusters"
private const val POI_CLUSTER_COUNT_LAYER_ID = "pois-cluster-count"
private const val POI_ICON_LAYER_ID = "pois-icons"
private const val POI_UNCLUSTERED_LAYER_ID = "pois-unclustered"
private const val POI_CLUSTER_MAX_ZOOM = 13.0
private const val POI_UNCLUSTERED_MIN_ZOOM = 13.0
private const val ROUTE_SOURCE_ID = "route-source"
private const val ROUTE_LAYER_ID = "route-layer"

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
    val currentRoute by routeViewModel.currentRoute.collectAsState()
    val currentRouteGeometry by routeViewModel.currentRouteGeometry.collectAsState()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var poiManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var userManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var routeLineManager by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    var currentPoisRef by remember { mutableStateOf<List<com.example.muul.data.model.POI>>(emptyList()) }
    var showRouteSheet by remember { mutableStateOf(false) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

    // Keep route start updated when user location changes
    androidx.compose.runtime.LaunchedEffect(ubicacion) {
        ubicacion?.let { (lat, lng) ->
            routeViewModel.prependUserLocation(lat, lng)
        }
    }

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
                        mapboxMap.loadStyle(Style.LIGHT) { style ->
                            loadedStyle = style
                            setupClusteredPoiLayers(style)
                            // Setup route layer/source so we can render Directions geometry exactly on the vector map
                            setupRouteLayer(style)
                            updateClusteredPoiSource(style, pois)

                            mapboxMap.addInteraction(
                                ClickInteraction.layer(POI_CLUSTER_LAYER_ID) { feature, _ ->
                                        val clusterPoint = feature.geometry as? Point
                                        ?: return@layer true
                                    mapboxMap.setCamera(
                                        CameraOptions.Builder()
                                            .center(clusterPoint)
                                            .zoom((mapboxMap.cameraState.zoom + 2.0).coerceAtMost(18.0))
                                            .build()
                                    )
                                    true
                                }
                            )

                            mapboxMap.addInteraction(
                                ClickInteraction.layer(POI_UNCLUSTERED_LAYER_ID) { feature, _ ->
                                        val clickedPoint = feature.geometry as? Point
                                        ?: return@layer true
                                    val clickedPoi = currentPoisRef.minByOrNull { poi ->
                                        val latDiff = abs(poi.latitud - clickedPoint.latitude())
                                        val lngDiff = abs(poi.longitud - clickedPoint.longitude())
                                        latDiff + lngDiff
                                    }
                                    clickedPoi?.let { viewModel.selectPoi(it) }
                                    true
                                }
                            )
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
                            val center = mapboxMap.cameraState.center
                            val metersPerPixel = mapboxMap.getMetersPerPixelAtLatitude(
                                center.latitude(),
                                mapboxMap.cameraState.zoom
                            )
                            viewModel.updateVisibleViewport(
                                centerLat = center.latitude(),
                                centerLng = center.longitude(),
                                metersPerPixelAtLatitude = metersPerPixel,
                                viewportWidthPx = mapView?.width ?: 0,
                                viewportHeightPx = mapView?.height ?: 0
                            )
                        }

                        poiManager = annotations.createCircleAnnotationManager()
                        userManager = annotations.createCircleAnnotationManager()
                        routeLineManager = annotations.createPolylineAnnotationManager()
                    }
                },
                update = { _ ->
                    loadedStyle?.let { style ->
                        updateClusteredPoiSource(style, pois)
                        // update route source if geometry available
                        updateRouteSource(style, currentRouteGeometry)
                    }

                    poiManager?.let { manager ->
                        manager.deleteAll()
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
                        // Prefer rendered street geometry when available
                        if (currentRouteGeometry.isNotEmpty()) {
                            val points = currentRouteGeometry.map {
                                Point.fromLngLat(it.second, it.first)
                            }
                            val polylineOptions = PolylineAnnotationOptions()
                                .withPoints(points)
                                .withLineColor("#003E6F")
                                .withLineWidth(3.0)
                            manager.create(polylineOptions)
                        } else {
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
                    // 1) Add user location if available
                    ubicacion?.let { (lat, lng) ->
                        routeViewModel.prependUserLocation(lat, lng)
                    }
                    
                    // 2) Save the route (now with user location as first point)
                    val routeName = routeToTrack.nombre
                    val saved = routeViewModel.saveCurrentRoute(routeName)

                    // 3) Start tracking using the persisted route ID (if saved)
                    if (saved != null) {
                        viewModel.startRoute(saved.id)
                    }

                    // Do NOT clear the current route: keep it visible on the map
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
                    // Ensure the user's current position is prepended as the route start
                    ubicacion?.let { (lat, lng) ->
                        routeViewModel.prependUserLocation(lat, lng)
                    }

                    routeViewModel.addToRoute(poi)
                    viewModel.clearSelectedPoi()
                    showRouteSheet = true
                }
            )
        }
    }
}

private fun setupRouteLayer(style: Style) {
    if (!style.styleSourceExists(ROUTE_SOURCE_ID)) {
        style.addSource(
            geoJsonSource(ROUTE_SOURCE_ID) {
                featureCollection(FeatureCollection.fromFeatures(emptyList()))
            }
        )
    }

    if (!style.styleLayerExists(ROUTE_LAYER_ID)) {
        style.addLayer(
            lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                lineColor("#003E6F")
                lineWidth(4.0)
                lineOpacity(0.9)
                lineJoin(LineJoin.ROUND)
                lineCap(LineCap.ROUND)
            }
        )
    }
}

private fun updateRouteSource(style: Style, geometry: List<Pair<Double, Double>>) {
    if (!style.styleSourceExists(ROUTE_SOURCE_ID)) return
    if (geometry.isEmpty()) {
        style.setStyleGeoJSONSourceData(
            ROUTE_SOURCE_ID,
            "",
            GeoJSONSourceData.valueOf(FeatureCollection.fromFeatures(emptyList()).features()!!)
        )
        return
    }

    val coords = geometry.map { Point.fromLngLat(it.second, it.first) }
    val line = LineString.fromLngLats(coords)
    val feature = Feature.fromGeometry(line)
    val fc = FeatureCollection.fromFeatures(listOf(feature))
    style.setStyleGeoJSONSourceData(
        ROUTE_SOURCE_ID,
        "",
        GeoJSONSourceData.valueOf(fc.features()!!)
    )
}

private fun setupClusteredPoiLayers(style: Style) {
    if (!style.styleSourceExists(POI_SOURCE_ID)) {
        style.addSource(
            geoJsonSource(POI_SOURCE_ID) {
                featureCollection(FeatureCollection.fromFeatures(emptyList()))
                cluster(true)
                clusterRadius(80)
                clusterMaxZoom(POI_CLUSTER_MAX_ZOOM.toLong())
                maxzoom(18)
            }
        )
    }

    if (!style.styleLayerExists(POI_CLUSTER_LAYER_ID)) {
        style.addLayer(
            circleLayer(POI_CLUSTER_LAYER_ID, POI_SOURCE_ID) {
                filter(has("point_count"))
                circleColor("#003E6F")
                circleRadius(22.0)
                circleOpacity(0.88)
                circleStrokeWidth(2.0)
                circleStrokeColor("#FFFFFF")
                maxZoom(POI_CLUSTER_MAX_ZOOM)
            }
        )
    }

    if (!style.styleLayerExists(POI_CLUSTER_COUNT_LAYER_ID)) {
        style.addLayer(
            symbolLayer(POI_CLUSTER_COUNT_LAYER_ID, POI_SOURCE_ID) {
                filter(has("point_count"))
                textField(
                    toString {
                        get {
                            literal("point_count_abbreviated")
                        }
                    }
                )
                textSize(12.0)
                textColor("#FFFFFF")
                textIgnorePlacement(true)
                textAllowOverlap(true)
                textAnchor(TextAnchor.CENTER)
                textJustify(TextJustify.CENTER)
                maxZoom(POI_CLUSTER_MAX_ZOOM)
            }
        )
    }

    if (!style.styleLayerExists(POI_ICON_LAYER_ID)) {
        style.addLayer(
            symbolLayer(POI_ICON_LAYER_ID, POI_SOURCE_ID) {
                filter(not { has("point_count") })
                textField(
                    toString {
                        get {
                            literal("emoji")
                        }
                    }
                )
                textSize(14.0)
                textIgnorePlacement(true)
                textAllowOverlap(true)
                textAnchor(TextAnchor.CENTER)
                textJustify(TextJustify.CENTER)
                minZoom(POI_UNCLUSTERED_MIN_ZOOM)
            }
        )
    }

    if (!style.styleLayerExists(POI_UNCLUSTERED_LAYER_ID)) {
        style.addLayer(
            circleLayer(POI_UNCLUSTERED_LAYER_ID, POI_SOURCE_ID) {
                filter(not { has("point_count") })
                circleColor(
                    match {
                        get {
                            literal("categoria")
                        }
                        literal("comida")
                        literal("#FF6B35")
                        literal("cultural")
                        literal("#7C3AED")
                        literal("deportes")
                        literal("#10B981")
                        literal("tienda")
                        literal("#F59E0B")
                        literal("servicio")
                        literal("#6B7280")
                        literal("atraccion")
                        literal("#EC4899")
                        literal("#003E6F")
                    }
                )
                circleRadius(8.5)
                circleStrokeWidth(2.0)
                circleStrokeColor("#FFFFFF")
                minZoom(POI_UNCLUSTERED_MIN_ZOOM)
            }
        )
    }
}

private fun updateClusteredPoiSource(style: Style, pois: List<com.example.muul.data.model.POI>) {
    val features = pois.map { poi ->
        Feature.fromGeometry(Point.fromLngLat(poi.longitud, poi.latitud)).apply {
            addStringProperty("categoria", poi.categoria)
            addStringProperty("emoji", poi.emoji ?: categoryEmoji(poi.categoria))
        }
    }
    val featureCollection = FeatureCollection.fromFeatures(features)
    style.setStyleGeoJSONSourceData(
        POI_SOURCE_ID,
        "",
        GeoJSONSourceData.valueOf(featureCollection.features()!!)
    )
}

private fun categoryEmoji(categoria: String): String {
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
