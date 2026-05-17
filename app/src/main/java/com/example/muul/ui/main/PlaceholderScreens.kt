package com.example.muul.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.muul.data.model.ItineraryStop
import com.example.muul.data.model.Ruta
import com.example.muul.data.model.TransportMode
import com.example.muul.ui.route.RouteViewModel

private val ItineraryNavy = Color(0xFF001C43)
private val ItineraryBlue = Color(0xFF1A73E8)
private val ItineraryYellow = Color(0xFFFFCC00)
private val ItineraryBorder = Color(0xFFC7CEDA)

@Composable
fun ItinerariesScreen(routeViewModel: RouteViewModel = viewModel()) {
    val savedRoutes by routeViewModel.savedRoutes.collectAsState()
    val selectedRoute by routeViewModel.selectedSavedRoute.collectAsState()

    var selectedTransport by remember { mutableStateOf(TransportMode.WALKING) }
    var availableMinutes by remember { mutableStateOf("240") }
    var startTime by remember { mutableStateOf("09:00") }

    LaunchedEffect(Unit) {
        routeViewModel.refreshSavedRoutes()
    }

    LaunchedEffect(savedRoutes) {
        if (selectedRoute == null && savedRoutes.isNotEmpty()) {
            routeViewModel.selectSavedRoute(savedRoutes.first())
        }
    }

    val routeForItinerary = selectedRoute
    val itineraryMinutes = availableMinutes.toIntOrNull()?.coerceAtLeast(60) ?: 240
    val itinerary = routeForItinerary?.let { route ->
        routeViewModel.buildItinerary(
            route.copy(
                transportMode = selectedTransport.name,
                plannedDurationMinutes = itineraryMinutes,
                startTimeMinutes = parseTimeToMinutes(startTime)
            )
        )
    }.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFB)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Itinerarios",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = ItineraryNavy,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Elige una ruta guardada y conviértela en un plan por hora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionTitle(
                icon = Icons.Default.Route,
                title = "Rutas guardadas",
                trailing = "${savedRoutes.size}"
            )
        }

        if (savedRoutes.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            items(savedRoutes, key = { it.id }) { route ->
                SavedRouteCard(
                    route = route,
                    selected = selectedRoute?.id == route.id,
                    onSelect = {
                        routeViewModel.selectSavedRoute(route)
                        selectedTransport = runCatching {
                            TransportMode.valueOf(route.transportMode)
                        }.getOrDefault(TransportMode.WALKING)
                    },
                    onDelete = {
                        routeViewModel.deleteSavedRoute(route.id)
                    }
                )
            }

            item {
                ItineraryBuilderCard(
                    route = routeForItinerary,
                    selectedTransport = selectedTransport,
                    onTransportChange = { selectedTransport = it },
                    availableMinutes = availableMinutes,
                    onAvailableMinutesChange = {
                        availableMinutes = it.filter(Char::isDigit).take(4)
                    },
                    startTime = startTime,
                    onStartTimeChange = { startTime = it.take(5) },
                    itineraryMinutes = itineraryMinutes
                )
            }

            item {
                SectionTitle(
                    icon = Icons.Default.Schedule,
                    title = "Plan del día",
                    trailing = "${itinerary.size} paradas"
                )
            }

            if (routeForItinerary == null) {
                item {
                    EmptyState(message = "Selecciona una ruta guardada para crear un itinerario.")
                }
            } else if (itinerary.isEmpty()) {
                item {
                    EmptyState(message = "Esta ruta necesita al menos un destino para generar el plan.")
                }
            } else {
                items(itinerary) { stop ->
                    TimelineStop(stop = stop)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
    trailing: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ItineraryNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ItineraryNavy,
                fontWeight = FontWeight.Bold
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = ItineraryYellow,
            contentColor = ItineraryNavy
        ) {
            Text(
                text = trailing,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SavedRouteCard(
    route: Ruta,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) Color(0xFFE8F0FE) else Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (selected) ItineraryBlue else ItineraryBorder
        ),
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (selected) ItineraryBlue else Color(0xFFF0F1F3), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = if (selected) Color.White else ItineraryNavy
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(route.nombre, style = MaterialTheme.typography.titleMedium, color = ItineraryNavy, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${route.lugares.size} lugares · ${formatDistance(route.distanciaTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar ruta",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(
                    icon = Icons.Default.AccessTime,
                    text = formatMinutes(route.plannedDurationMinutes)
                )
                InfoPill(
                    icon = transportIcon(route.transportMode),
                    text = transportLabel(route.transportMode)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) ItineraryBlue else ItineraryNavy,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (selected) "Ruta seleccionada" else "Crear itinerario")
            }
        }
    }
}

@Composable
private fun ItineraryBuilderCard(
    route: Ruta?,
    selectedTransport: TransportMode,
    onTransportChange: (TransportMode) -> Unit,
    availableMinutes: String,
    onAvailableMinutesChange: (String) -> Unit,
    startTime: String,
    onStartTimeChange: (String) -> Unit,
    itineraryMinutes: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ItineraryBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = route?.nombre ?: "Selecciona una ruta",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    color = ItineraryNavy
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text("Transporte", style = MaterialTheme.typography.labelLarge, color = ItineraryNavy)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransportMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedTransport == mode,
                        onClick = { onTransportChange(mode) },
                        label = { Text(shortTransportLabel(mode)) },
                        leadingIcon = {
                            Icon(
                                imageVector = transportIcon(mode.name),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ItineraryBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = availableMinutes,
                    onValueChange = onAvailableMinutesChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Minutos") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = startTime,
                    onValueChange = onStartTimeChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Inicio") },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoPill(
                icon = Icons.Default.Schedule,
                text = "Plan de ${formatMinutes(itineraryMinutes)} desde $startTime"
            )
        }
    }
}

@Composable
private fun TimelineStop(stop: ItineraryStop) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(ItineraryBlue, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(72.dp)
                    .background(ItineraryBorder)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ItineraryBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stop.arrivalLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = ItineraryBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stop.poi.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = ItineraryNavy,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stop.travelMinutesFromPrevious} min de trayecto · ${stop.stayMinutes} min de visita",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFF0F1F3),
        contentColor = ItineraryNavy
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState(message: String = "Aún no hay rutas guardadas. Crea una desde el mapa y vuelve aquí.") {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ItineraryBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000.0) {
        "%.1f km".format(meters / 1000.0)
    } else {
        "${meters.toInt()} m"
    }
}

private fun formatMinutes(minutes: Int): String {
    return if (minutes >= 60) {
        "${minutes / 60}h ${minutes % 60}m"
    } else {
        "$minutes min"
    }
}

private fun transportIcon(modeName: String): ImageVector {
    return when (modeName) {
        TransportMode.BIKING.name -> Icons.Default.DirectionsBike
        TransportMode.CAR.name -> Icons.Default.DirectionsCar
        TransportMode.TAXI.name -> Icons.Default.DirectionsCar
        else -> Icons.Default.DirectionsWalk
    }
}

private fun transportLabel(modeName: String): String {
    return runCatching { TransportMode.valueOf(modeName).label }
        .getOrDefault(TransportMode.WALKING.label)
}

private fun shortTransportLabel(mode: TransportMode): String {
    return when (mode) {
        TransportMode.WALKING -> "Pie"
        TransportMode.BIKING -> "Bici"
        TransportMode.CAR -> "Auto"
        TransportMode.TAXI -> "Taxi"
    }
}
