package com.example.muul.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muul.data.local.RoutePlanner
import com.example.muul.data.model.Ruta
import com.example.muul.data.model.TransportMode
import com.example.muul.ui.route.RouteViewModel

@Composable
fun ExploreScreen(routeViewModel: RouteViewModel = viewModel()) {
    val savedRoutes by routeViewModel.savedRoutes.collectAsState()
    val selectedRoute by routeViewModel.selectedSavedRoute.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var selectedTransport by remember { mutableStateOf(TransportMode.WALKING) }
    var availableMinutes by remember { mutableStateOf("240") }
    var startTime by remember { mutableStateOf("09:00") }

    LaunchedEffect(savedRoutes) {
        if (selectedRoute == null && savedRoutes.isNotEmpty()) {
            routeViewModel.selectSavedRoute(savedRoutes.first())
        }
    }

    val itineraryRoute = selectedRoute
    val itineraryMinutes = availableMinutes.toIntOrNull()?.coerceAtLeast(60) ?: 240
    val itinerary = itineraryRoute?.let {
        routeViewModel.buildItinerary(
            it.copy(
                transportMode = selectedTransport.name,
                plannedDurationMinutes = itineraryMinutes,
                startTimeMinutes = parseTimeToMinutes(startTime)
            )
        )
    }.orEmpty()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Explore",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Guarda rutas, ponles nombre y genera itinerarios según el tiempo y transporte.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { currentTab = 0 },
                modifier = Modifier.weight(1f)
            ) { Text("Rutas guardadas") }
            Button(
                onClick = { currentTab = 1 },
                modifier = Modifier.weight(1f)
            ) { Text("Itinerario") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentTab == 0) {
            if (savedRoutes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no hay rutas guardadas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = spacedBy(12.dp)) {
                    items(savedRoutes) { route ->
                        RouteCard(
                            route = route,
                            onOpen = {
                                routeViewModel.selectSavedRoute(route)
                                currentTab = 1
                            },
                            onDelete = {
                                routeViewModel.deleteSavedRoute(route.id)
                            }
                        )
                    }
                }
            }
        } else {
            if (itineraryRoute == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Selecciona una ruta guardada para generar el itinerario.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(itineraryRoute.nombre, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${itineraryRoute.lugares.size} lugares · ${itineraryRoute.transportMode}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Transporte", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TransportMode.entries.forEach { mode ->
                                val selected = selectedTransport == mode
                                TextButton(onClick = { selectedTransport = mode }) {
                                    Text(if (selected) "${mode.label} ✓" else mode.label)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = availableMinutes,
                            onValueChange = { availableMinutes = it.filter(Char::isDigit) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tiempo disponible en minutos") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Hora de inicio HH:MM") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val plannedMinutes = itineraryMinutes
                        Text(
                            text = "Duración estimada: ${plannedMinutes / 60}h ${plannedMinutes % 60}m",
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        itinerary.forEach { stop ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Text(stop.arrivalLabel, style = MaterialTheme.typography.labelLarge)
                                    Text(stop.poi.nombre, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${stop.travelMinutesFromPrevious} min de trayecto · ${stop.stayMinutes} min de visita",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: Ruta,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(route.nombre, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${route.lugares.size} lugares", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${route.transportMode} · ${route.plannedDurationMinutes / 60}h ${route.plannedDurationMinutes % 60}m",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Ver itinerario") }
                TextButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Text("Eliminar")
                }
            }
        }
    }
}

private fun parseTimeToMinutes(input: String): Int {
    val parts = input.split(":")
    if (parts.size != 2) return 9 * 60
    val hours = parts[0].toIntOrNull() ?: return 9 * 60
    val minutes = parts[1].toIntOrNull() ?: return 9 * 60
    return (hours * 60 + minutes).coerceIn(0, 1439)
}

@Composable
fun CommunityScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Comunidad",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
