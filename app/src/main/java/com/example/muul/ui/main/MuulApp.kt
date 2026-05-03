package com.example.muul.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.muul.ui.auth.AuthViewModel
import com.example.muul.ui.map.MapScreen
import com.example.muul.ui.profile.ProfileScreen

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

    val tabs = listOf(
        NavTab("map", "MAP", Icons.Default.Map),
        NavTab("explore", "EXPLORE", Icons.Default.Explore),
        NavTab("community", "COMMUNITY", Icons.Default.Groups),
        NavTab("profile", "PROFILE", Icons.Default.Person)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        MuulTopHeader(currentUser.value?.email ?: "Usuario")

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
                    MapScreen()
                }
                composable("explore") {
                    ExploreScreen()
                }
                composable("community") {
                    CommunityScreen()
                }
                composable("profile") {
                    ProfileScreen(authViewModel = authViewModel, onDismiss = {})
                }
            }
        }

        // Bottom Navigation
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
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
                                Color(0xFFFDD835) // Yellow
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = { Text(tab.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFDD835),
                        selectedTextColor = Color(0xFFFDD835),
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
fun MuulTopHeader(userEmail: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "MUUL",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF003E6F)
        )
    }
}

data class NavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)
