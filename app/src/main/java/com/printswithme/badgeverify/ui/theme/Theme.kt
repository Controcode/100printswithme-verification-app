package com.printswithme.badgeverify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnBrandPrimary,
    primaryContainer = BrandTertiary,
    onPrimaryContainer = BrandPrimary,
    secondary = BrandSecondary,
    onSecondary = OnBrandPrimary,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = OnSurfaceSecondary,
    error = Error,
    onError = OnError,
    outline = Border,
    outlineVariant = Divider,
)

@Composable
fun BadgeVerifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = BadgeVerifyTypography,
        content = content
    )
}
