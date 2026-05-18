package com.example.muul.ui.main

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.muul.ui.auth.AuthViewModel
import com.example.muul.ui.map.MapScreen
import com.example.muul.ui.route.RouteViewModel
import com.example.muul.ui.profile.ProfileScreen
import java.io.File

@Composable
fun MuulApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser = authViewModel.currentUser.collectAsState()

    if (currentUser.value != null) {
        MuulMainScreen(authViewModel = authViewModel)
    } else {
        // Show auth (login/register)
        com.example.muul.ui.auth.AuthHost(authViewModel = authViewModel)
    }
}

@Composable
fun MuulMainScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val currentUser = authViewModel.currentUser.collectAsState()
    val selectedTab = remember { mutableIntStateOf(0) }
    val routeViewModel: RouteViewModel = viewModel()

    val tabs = listOf(
        NavTab("map", "MAPA", Icons.Default.Map),
        NavTab("itineraries", "ITINERARIOS", Icons.Default.CalendarMonth),
        NavTab("profile", "PERFIL", Icons.Default.Person)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        MuulTopHeader(
            userEmail = currentUser.value?.email ?: "Usuario",
            profilePhotoUri = currentUser.value?.profilePhotoUri
        )

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            NavHost(
                navController = navController,
                startDestination = "map"
            ) {
                composable("map") {
                    MapScreen(routeViewModel = routeViewModel)
                }
                composable("itineraries") {
                    ItinerariesScreen(routeViewModel = routeViewModel)
                }
                composable("profile") {
                    ProfileScreen(authViewModel = authViewModel, onDismiss = {})
                }
            }
        }

        // Bottom Navigation
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    selected = selectedTab.intValue == index,
                    onClick = {
                        selectedTab.intValue = index
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selectedTab.intValue == index)
                                Color(0xFF003E6F)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF003E6F),
                        selectedTextColor = Color(0xFF003E6F),
                        indicatorColor = Color(0xFFFFCC00),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun MuulTopHeader(
    userEmail: String,
    profilePhotoUri: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color(0xFF003E6F)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "MUUL",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF003E6F),
            modifier = Modifier.weight(1f)
        )
        HeaderAvatar(userEmail = userEmail, profilePhotoUri = profilePhotoUri)
    }
}

@Composable
private fun HeaderAvatar(
    userEmail: String,
    profilePhotoUri: String?
) {
    val context = LocalContext.current
    val imageBitmap = remember(profilePhotoUri) {
        if (profilePhotoUri.isNullOrBlank()) {
            null
        } else {
            runCatching {
                decodeHeaderProfileBitmap(context, profilePhotoUri)?.asImageBitmap()
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(Color(0xFF003E6F), CircleShape)
            .border(2.dp, Color(0xFFFFCC00), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = userInitials(userEmail),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun decodeHeaderProfileBitmap(
    context: android.content.Context,
    profilePhotoUri: String
): android.graphics.Bitmap? {
    val parsedUri = runCatching { Uri.parse(profilePhotoUri) }.getOrNull()

    if (parsedUri?.scheme == "file") {
        return BitmapFactory.decodeFile(parsedUri.path)
    }

    val file = File(profilePhotoUri)
    if (file.exists()) {
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    return parsedUri?.let { uri ->
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    }
}

data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private fun userInitials(email: String): String {
    val name = email.substringBefore("@").replace(".", " ").replace("_", " ").trim()
    if (name.isBlank()) return "M"

    return name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
}
