package com.example.muul.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.muul.ui.auth.AuthViewModel
import kotlin.math.roundToInt

private val ProfileNavy = Color(0xFF001C43)
private val ProfileYellow = Color(0xFFFFCC00)
private val ProfileBorder = Color(0xFFC7CEDA)
private val ProfileMuted = Color(0xFF6E7480)

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onDismiss: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        authViewModel.refreshCurrentUser()
    }

    val user by authViewModel.currentUser.collectAsState()
    val totalSteps = user?.totalSteps ?: 0
    val routesWithSteps = user?.stepsByRoute?.count { it.value > 0 } ?: 0
    val distanceKm = totalSteps * 0.00074
    val calories = (totalSteps * 0.04).roundToInt()
    val level = (totalSteps / 200).coerceAtLeast(1)
    
    val displayName = user?.username ?: "Viajero Muul"
    
    val progress = ((totalSteps % 10_000) / 10_000f).coerceIn(0.08f, 1f)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { authViewModel.updateProfilePhoto(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFB)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ProfileAvatar(
                    displayName = displayName,
                    photoUri = user?.profilePhotoUri,
                    onChangePhoto = {
                        photoPickerLauncher.launch("image/*")
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ProfileNavy
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ProfileYellow,
                    contentColor = ProfileNavy
                ) {
                    Text(
                        text = "Nivel $level · Explorador cultural",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cambiar foto")
                    }
                    if (!user?.profilePhotoUri.isNullOrBlank()) {
                        TextButton(onClick = { authViewModel.clearProfilePhoto() }) {
                            Text("Quitar")
                        }
                    }
                }
            }
        }

        item {
            ActivityCard(
                steps = totalSteps,
                distanceKm = distanceKm,
                calories = calories,
                progress = progress
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Insignias",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = ProfileNavy
                    )
                )
                Text(
                    text = "$routesWithSteps rutas activas",
                    style = MaterialTheme.typography.labelLarge,
                    color = ProfileNavy,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(profileBadges(totalSteps, routesWithSteps).chunked(2)) { rowBadges ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                rowBadges.forEach { badge ->
                    BadgeCard(
                        badge = badge,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowBadges.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ProfileNavy,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "RACHA ACTUAL",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFA9B8D6)
                        )
                        Text(
                            text = "${routesWithSteps.coerceAtLeast(if (totalSteps > 0) 1 else 0)} rutas",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.MilitaryTech,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = ProfileYellow.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    authViewModel.logout()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProfileYellow,
                    contentColor = ProfileNavy
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    displayName: String,
    photoUri: String?,
    onChangePhoto: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(ProfileNavy, CircleShape)
            .border(3.dp, ProfileYellow, CircleShape)
            .clickable(onClick = onChangePhoto),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(3.dp, ProfileYellow, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = displayName.initials(),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(30.dp)
                .background(ProfileYellow, CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = ProfileNavy
            )
        }
    }
}

@Composable
private fun ActivityCard(
    steps: Int,
    distanceKm: Double,
    calories: Int,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ProfileBorder),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        color = ProfileNavy
                    )
                )
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFF7A6200)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentAlignment = Alignment.Center
            ) {
                ActivityRing(progress = progress)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%,d".format(steps),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = FontFamily.Serif,
                            color = ProfileNavy,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "PASOS GUARDADOS",
                        style = MaterialTheme.typography.labelMedium,
                        color = ProfileMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MetricBox(
                    icon = Icons.Default.Place,
                    value = "%.1f km".format(distanceKm),
                    label = "DISTANCIA",
                    modifier = Modifier.weight(1f)
                )
                MetricBox(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "$calories kcal",
                    label = "QUEMADAS",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActivityRing(progress: Float) {
    Canvas(modifier = Modifier.size(180.dp)) {
        val strokeWidth = 22.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = Color(0xFFE8E8E8),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF003E7E),
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
    }
}

@Composable
private fun MetricBox(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFFF0F1F3), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ProfileNavy,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(value, style = MaterialTheme.typography.labelLarge, color = ProfileNavy, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ProfileMuted)
        }
    }
}

@Composable
private fun BadgeCard(
    badge: ProfileBadge,
    modifier: Modifier = Modifier
) {
    val contentColor = if (badge.unlocked) ProfileNavy else Color(0xFF7D8792)
    val bubbleColor = if (badge.unlocked) badge.color else Color(0xFFE8EAED)

    Surface(
        modifier = modifier.aspectRatio(0.95f),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProfileBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(bubbleColor, CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = badge.subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = ProfileMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class ProfileBadge(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val unlocked: Boolean
)

private fun profileBadges(totalSteps: Int, routesWithSteps: Int): List<ProfileBadge> {
    return listOf(
        ProfileBadge(
            title = "Primer Camino",
            subtitle = "${totalSteps.coerceAtMost(1_000)}/1000 pasos",
            icon = Icons.Default.Place,
            color = Color(0xFFD8E6FF),
            unlocked = totalSteps >= 1_000
        ),
        ProfileBadge(
            title = "Ruta Activa",
            subtitle = "$routesWithSteps rutas con pasos",
            icon = Icons.Default.Explore,
            color = Color(0xFFFFDF69),
            unlocked = routesWithSteps >= 1
        ),
        ProfileBadge(
            title = "Explorador Urbano",
            subtitle = "${totalSteps.coerceAtMost(5_000)}/5000 pasos",
            icon = Icons.Default.Museum,
            color = Color(0xFFE8EAED),
            unlocked = totalSteps >= 5_000
        ),
        ProfileBadge(
            title = "Nocturno",
            subtitle = "Guarda 3 rutas",
            icon = Icons.Default.NightsStay,
            color = Color(0xFFE4E9EF),
            unlocked = routesWithSteps >= 3
        ),
        ProfileBadge(
            title = "Sabor Local",
            subtitle = "Camina 10 000 pasos",
            icon = Icons.Default.LocalDining,
            color = Color(0xFFD8E6FF),
            unlocked = totalSteps >= 10_000
        ),
        ProfileBadge(
            title = "Constante",
            subtitle = "25 000 pasos",
            icon = Icons.Default.MilitaryTech,
            color = Color(0xFFFFDF69),
            unlocked = totalSteps >= 25_000
        )
    )
}

private fun String.initials(): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "M" }
}
