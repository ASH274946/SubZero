package com.subzero.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val M3PineSageColorScheme = lightColorScheme(
    primary = M3PinePrimary,
    onPrimary = M3SurfaceWhite,
    primaryContainer = M3PineContainer,
    onPrimaryContainer = M3PineDark,
    secondary = M3TextSecondary,
    onSecondary = M3SurfaceWhite,
    tertiary = M3BadgeMint,
    onTertiary = M3BadgeMintText,
    background = M3CanvasBackground,
    onBackground = M3TextPrimary,
    surface = M3SurfaceWhite,
    onSurface = M3TextPrimary,
    surfaceVariant = M3SurfaceVariant,
    onSurfaceVariant = M3TextSecondary,
    outline = M3SwitchTrackInactive,
    error = M3AlertCoral,
    onError = M3SurfaceWhite,
    errorContainer = M3AlertCoralContainer,
    onErrorContainer = M3AlertCoral
)

@Composable
fun SubZeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = M3PineSageColorScheme,
        typography = SubZeroTypography,
        content = content
    )
}
