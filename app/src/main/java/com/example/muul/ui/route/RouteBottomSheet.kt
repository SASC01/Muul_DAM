package com.example.muul.ui.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muul.data.local.RoutePlanner
import com.example.muul.data.model.Ruta
import com.example.muul.data.model.TransportMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteBottomSheet(
    routeViewModel: RouteViewModel = viewModel(),
    onDismiss: () -> Unit,
    onStartTracking: (Ruta) -> Unit
) {
    val currentRoute = routeViewModel.currentRoute.collectAsState()
    val route = currentRoute.value ?: return
    val transportMode by routeViewModel.selectedTransportMode.collectAsState()
    val displayRoute = route.copy(lugares = route.lugares.filterNot { it.id == "user_location" })
    val estimatedMinutes = route.plannedDurationMinutes
        .takeIf { it > 0 }
        ?: RoutePlanner.estimateMinutes(route, transportMode)
    val distanceMeters = route.distanciaTotal
        .takeIf { it > 0.0 }
        ?: RoutePlanner.routeDistanceMeters(route)
    var routeName by remember(route.id) { mutableStateOf(route.nombre) }
    var expandedStops by remember(route.id) { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(route.id) {
        routeName = route.nombre
        expandedStops = false
    }

    val visibleStops = displayRoute.lugares

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    routeViewModel.saveCurrentRoute(routeName)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar ruta")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = routeName,
                onValueChange = { routeName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de la ruta") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Medio de transporte",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportMode.entries.forEach { mode ->
                    val selected = mode == transportMode
                    Button(
                        onClick = { routeViewModel.setTransportMode(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        TransportIcon(mode = mode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info de la ruta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${visibleStops.size}/${routeViewModel.maxPlaces} lugares")
                Text(formatDistance(distanceMeters))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tiempo estimado: ${estimatedMinutes / 60}h ${estimatedMinutes % 60}m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de lugares
            Text(
                text = "Lugares en la ruta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (visibleStops.isEmpty()) {
                Text(
                    text = "No hay lugares. Selecciona lugares del mapa o usa 'Sorprendeme'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    val shownStops = if (expandedStops) visibleStops else visibleStops.take(3)
                    itemsIndexed(shownStops) { index, poi ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${index + 1}. ${poi.nombre}", style = MaterialTheme.typography.labelLarge)
                                Text("${poi.categoria}", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(
                                onClick = {
                                    val realIndex = if (route.lugares.firstOrNull()?.id == "user_location") index + 1 else index
                                    routeViewModel.removeFromRoute(realIndex)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (visibleStops.size > 3) {
                        item {
                            Button(
                                onClick = { expandedStops = !expandedStops },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(if (expandedStops) "Mostrar menos" else "Mostrar ${visibleStops.size - 3} lugares más")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón iniciar rastreo
            if (visibleStops.isNotEmpty()) {
                Button(
                    onClick = {
                        val routeToTrack = route.copy()
                        onStartTracking(routeToTrack)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calcular e iniciar seguimiento")
                }
            }
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000.0) {
        String.format("%.1f km", meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}

@Composable
private fun TransportIcon(mode: TransportMode) {
    val icon = when (mode) {
        TransportMode.WALKING -> Icons.Default.DirectionsWalk
        TransportMode.BIKING -> Icons.Default.DirectionsBike
        TransportMode.CAR -> Icons.Default.DirectionsCar
        TransportMode.TAXI -> Icons.Default.DirectionsCar
    }
    Icon(imageVector = icon, contentDescription = mode.label)
}
