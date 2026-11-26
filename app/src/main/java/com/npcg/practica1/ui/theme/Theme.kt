package com.npcg.practica1.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val WelliLightColorScheme = lightColorScheme(
    primary = WelliPrimaryLight,
    onPrimary = WelliOnPrimaryLight,
    primaryContainer = WelliPrimaryContainerLight,
    onPrimaryContainer = WelliOnPrimaryContainerLight,
    secondary = WelliSecondaryLight,
    onSecondary = WelliOnSecondaryLight,
    secondaryContainer = WelliSecondaryContainerLight,
    onSecondaryContainer = WelliOnSecondaryContainerLight,
    tertiary = WelliTertiaryLight,
    onTertiary = WelliOnTertiaryLight,
    tertiaryContainer = WelliTertiaryContainerLight,
    onTertiaryContainer = WelliOnTertiaryContainerLight,
    background = WelliBackgroundLight,
    onBackground = WelliOnBackgroundLight,
    surface = WelliSurfaceLight,
    onSurface = WelliOnSurfaceLight,
    surfaceVariant = WelliSurfaceVariantLight,
    onSurfaceVariant = WelliOnSurfaceVariantLight,
    error = WelliErrorLight,
    onError = WelliOnErrorLight
)

private val WelliDarkColorScheme = darkColorScheme(
    primary = WelliPrimaryDark,
    onPrimary = WelliOnPrimaryDark,
    primaryContainer = WelliPrimaryContainerDark,
    onPrimaryContainer = WelliOnPrimaryContainerDark,
    secondary = WelliSecondaryDark,
    onSecondary = WelliOnSecondaryDark,
    secondaryContainer = WelliSecondaryContainerDark,
    onSecondaryContainer = WelliOnSecondaryContainerDark,
    tertiary = WelliTertiaryDark,
    onTertiary = WelliOnTertiaryDark,
    tertiaryContainer = WelliTertiaryContainerDark,
    onTertiaryContainer = WelliOnTertiaryContainerDark,
    background = WelliBackgroundDark,
    onBackground = WelliOnBackgroundDark,
    surface = WelliSurfaceDark,
    onSurface = WelliOnSurfaceDark,
    surfaceVariant = WelliSurfaceVariantDark,
    onSurfaceVariant = WelliOnSurfaceVariantDark,
    error = WelliErrorDark,
    onError = WelliOnErrorDark
)

@Composable
fun WelliTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WelliDarkColorScheme else WelliLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WelliTypography,
        shapes = WelliShapes,
        content = content
    )
}
