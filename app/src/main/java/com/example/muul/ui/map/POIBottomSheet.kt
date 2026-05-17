package com.example.muul.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muul.data.model.POI
import com.example.muul.data.model.TransportMode
import com.example.muul.ui.theme.CategoryComida
import com.example.muul.ui.theme.CategoryCultural
import com.example.muul.ui.theme.CategoryDeportes
import com.example.muul.ui.theme.CategoryServicio
import com.example.muul.ui.theme.CategoryTienda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POIBottomSheet(
    poi: POI,
    distanciaTexto: String,
    travelTimeText: String,
    selectedTransportMode: TransportMode,
    onTransportSelected: (TransportMode) -> Unit,
    onDismiss: () -> Unit,
    onAddToRoute: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val categoryColor = categoryColor(poi.categoria)
    val categoryEmoji = poi.emoji ?: categoryEmoji(poi.categoria)
    val categoryLabel = categoryLabel(poi.categoria)
    val horario = buildHorarioText(poi)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(categoryColor.copy(alpha = 0.14f))
                        .border(
                            BorderStroke(1.dp, categoryColor.copy(alpha = 0.28f)),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poi.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryChip(
                            text = categoryLabel,
                            color = categoryColor
                        )
                        if (poi.verificado) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF188038)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Verificado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF188038),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.NearMe,
                    label = "Distancia",
                    value = distanciaTexto,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = transportIcon(selectedTransportMode),
                    label = "Llegas en",
                    value = travelTimeText,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = Icons.Default.AttachMoney,
                    label = "Precio",
                    value = poi.precio_rango ?: "N/D",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Elige cómo llegar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransportMode.entries.forEach { mode ->
                    TransportOption(
                        mode = mode,
                        selected = mode == selectedTransportMode,
                        onClick = { onTransportSelected(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            if (!poi.descripcion.isNullOrBlank()) {
                Text(
                    text = poi.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!poi.direccion.isNullOrBlank()) {
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Dirección",
                    value = poi.direccion
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (horario != null) {
                InfoRow(
                    icon = Icons.Default.AccessTime,
                    label = "Horario",
                    value = horario
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            InfoRow(
                icon = Icons.Default.Directions,
                label = "Coordenadas",
                value = "${String.format("%.4f", poi.latitud)}, ${String.format("%.4f", poi.longitud)}"
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddToRoute,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ir")
                }
                FilledTonalButton(
                    onClick = onAddToRoute,
                    modifier = Modifier.weight(1.25f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar a ruta")
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.13f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransportOption(
    mode: TransportMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (selected) Color(0xFFE8F0FE) else MaterialTheme.colorScheme.surface
    val content = if (selected) Color(0xFF1A73E8) else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF1A73E8) else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = transportIcon(mode),
                    contentDescription = mode.label,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = shortTransportLabel(mode),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun categoryColor(categoria: String): Color {
    return when (categoria) {
        "comida" -> CategoryComida
        "cultural" -> CategoryCultural
        "deportes" -> CategoryDeportes
        "tienda" -> CategoryTienda
        "servicio" -> CategoryServicio
        "atraccion" -> Color(0xFFEC4899)
        else -> Color(0xFF1A73E8)
    }
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

private fun categoryLabel(categoria: String): String {
    return when (categoria) {
        "comida" -> "Restaurante / Comida"
        "cultural" -> "Cultural"
        "deportes" -> "Deportes"
        "tienda" -> "Tienda / Mercado"
        "servicio" -> "Servicio"
        "atraccion" -> "Atracción turística"
        else -> categoria
    }
}

private fun buildHorarioText(poi: POI): String? {
    val opening = poi.horario_apertura
    val closing = poi.horario_cierre
    return when {
        !opening.isNullOrBlank() && !closing.isNullOrBlank() -> "$opening - $closing"
        !opening.isNullOrBlank() -> "Abre $opening"
        !closing.isNullOrBlank() -> "Cierra $closing"
        else -> null
    }
}

private fun transportIcon(mode: TransportMode): ImageVector {
    return when (mode) {
        TransportMode.WALKING -> Icons.Default.DirectionsWalk
        TransportMode.BIKING -> Icons.Default.DirectionsBike
        TransportMode.CAR -> Icons.Default.DirectionsCar
        TransportMode.TAXI -> Icons.Default.DirectionsCar
    }
}

private fun shortTransportLabel(mode: TransportMode): String {
    return when (mode) {
        TransportMode.WALKING -> "Pie"
        TransportMode.BIKING -> "Bici"
        TransportMode.CAR -> "Auto"
        TransportMode.TAXI -> "Taxi"
    }
}
