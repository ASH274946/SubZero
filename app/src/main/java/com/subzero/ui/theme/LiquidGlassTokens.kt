package com.subzero.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * SubZero Unified Design System Tokens.
 * Configured strictly to the Material 3 Pine/Sage tactile aesthetic:
 * Pale Sage canvas (#EAF2ED), Solid White cards (#FFFFFF), Deep Pine Green (#1E5144).
 */
object LiquidGlassTokens {

    // Canvas Background (Pale Sage)
    val BgGradientStart = M3CanvasBackground
    val BgGradientEnd = M3CanvasBackground

    // Ambient Lighting Glow Orbs (Subtle/Clear in M3 tactile design)
    val AmbientOrbIndigo = Color.Transparent
    val AmbientOrbTeal = Color.Transparent

    // Surface Tokens (Pure Solid Surfaces)
    val GlassSurfaceDefault = M3SurfaceWhite
    val GlassSurfaceElevated = M3SurfaceWhite
    val GlassSurfacePressed = M3SurfaceVariant
    val GlassSurfaceHeavy = M3SurfaceWhite
    val GlassSurfaceDeep = M3SurfaceWhite

    // Tactile Borders
    val GlassBorderGradient = Brush.verticalGradient(
        colors = listOf(
            M3SurfaceVariant,
            M3CanvasBackground
        )
    )

    val GlassBorderSubtle = Brush.verticalGradient(
        colors = listOf(
            M3SurfaceVariant,
            M3SurfaceVariant
        )
    )

    // Accents & Badges
    val PastelLavender = M3PinePrimary
    val PastelPeriwinkle = M3PinePrimary
    val PastelRose = M3AlertCoral
    val PastelAmber = Color(0xFFD97706)
    val PastelMint = M3BadgeMintText

    // Actions & Buttons
    val PrimaryActionGradient = Brush.horizontalGradient(
        colors = listOf(M3PinePrimary, M3PinePrimary)
    )

    val WarningActionGradient = Brush.horizontalGradient(
        colors = listOf(M3AlertCoral, M3AlertCoral)
    )

    val CoralGradient = WarningActionGradient

    val HeroCardGradient = Brush.verticalGradient(
        colors = listOf(M3SurfaceWhite, M3SurfaceWhite)
    )

    val HeroBorderGradient = Brush.verticalGradient(
        colors = listOf(M3SurfaceVariant, M3SurfaceVariant)
    )

    val DemoCtaGradient = Brush.horizontalGradient(
        colors = listOf(M3PinePrimary, M3PinePrimary)
    )

    // Vignette Warning Glow
    val VignetteWarningGlow = Color(0x33D9534F)

    // Typography
    val TextPrimary = M3TextPrimary
    val TextSecondary = M3TextSecondary
    val TextTertiary = M3TextTertiary
    val TextHiddenDisclaimer = Color(0xFF94A3B8)
}
