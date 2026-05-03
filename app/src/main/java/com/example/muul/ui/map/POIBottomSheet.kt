package com.example.muul.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muul.data.model.POI
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
    onDismiss: () -> Unit,
    onAddToRoute: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryColor = when (poi.categoria) {
        "comida" -> CategoryComida
        "cultural" -> CategoryCultural
        "deportes" -> CategoryDeportes
        "tienda" -> CategoryTienda
        "servicio" -> CategoryServicio
        "atraccion" -> androidx.compose.ui.graphics.Color(0xFFEC4899)
        else -> MaterialTheme.colorScheme.primary
    }

    val categoryEmoji = when (poi.categoria) {
        "comida" -> "🍽️"
        "cultural" -> "🏛️"
        "deportes" -> "⚽"
        "tienda" -> "🛍️"
        "servicio" -> "🔧"
        "atraccion" -> "📸"
        else -> "📍"
    }

    val categoryLabel = when (poi.categoria) {
        "comida" -> "Restaurante / Comida"
        "cultural" -> "Cultural"
        "deportes" -> "Deportes"
        "tienda" -> "Tienda / Mercado"
        "servicio" -> "Servicio"
        "atraccion" -> "Atracción turística"
        else -> poi.categoria
    }

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
            // Header: Emoji + Nombre + Categoría
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Emoji grande
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = poi.emoji ?: categoryEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poi.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Chip de categoría
                        Box(
                            modifier = Modifier
                                .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = categoryLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Distancia
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = distanciaTexto,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Descripción
            if (!poi.descripcion.isNullOrEmpty()) {
                Text(
                    text = poi.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Info rows
            // Dirección
            if (!poi.direccion.isNullOrEmpty()) {
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Dirección",
                    value = poi.direccion
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Horario
            if (!poi.horario_apertura.isNullOrEmpty()) {
                InfoRow(
                    icon = Icons.Default.AccessTime,
                    label = "Horario",
                    value = "${poi.horario_apertura} - ${poi.horario_cierre ?: "N/A"}"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Precio
            if (!poi.precio_rango.isNullOrEmpty()) {
                InfoRow(
                    icon = Icons.Default.AttachMoney,
                    label = "Rango de precio",
                    value = poi.precio_rango
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Coordenadas
            InfoRow(
                icon = Icons.Default.Directions,
                label = "Coordenadas",
                value = "${String.format("%.4f", poi.latitud)}, ${String.format("%.4f", poi.longitud)}"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddToRoute(); onDismiss() },
                    modifier = Modifier
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Agregar a Ruta",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cerrar",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
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