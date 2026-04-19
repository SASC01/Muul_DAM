package com.example.muul.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MuulColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimary = OnPrimary,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    onSecondary = OnSecondary,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiary = OnTertiary,
    surface = Surface,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLowest = SurfaceContainerLowest,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = Error,
)

@Composable
fun MuulTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuulColorScheme,
        typography = MuulTypography,
        content = content
    )
}