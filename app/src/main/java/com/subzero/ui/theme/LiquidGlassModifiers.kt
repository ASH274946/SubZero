package com.subzero.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Modifier extensions for Liquid Glass (Refined Frosted Glassmorphism).
 */

/**
 * Renders the canvas background with Deep Twilight Slate to Midnight Blue-Grey gradient
 * and two ambient light refraction orbs.
 */
fun Modifier.liquidGlassCanvasBackground(): Modifier = this.drawBehind {
    // Canvas Base Gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                LiquidGlassTokens.BgGradientStart,
                LiquidGlassTokens.BgGradientEnd
            )
        )
    )

    // Top-left Ambient Glow Orb (Indigo)
    drawCircle(
        color = LiquidGlassTokens.AmbientOrbIndigo,
        radius = size.width * 0.75f,
        center = Offset(x = size.width * 0.15f, y = size.height * 0.10f)
    )

    // Bottom-right Ambient Glow Orb (Teal)
    drawCircle(
        color = LiquidGlassTokens.AmbientOrbTeal,
        radius = size.width * 0.70f,
        center = Offset(x = size.width * 0.85f, y = size.height * 0.75f)
    )
}

/**
 * Applies frosted liquid glass surface styling with specular gradient border and rounded geometry.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = LiquidGlassTokens.GlassSurfaceDefault,
    borderBrush: Brush = LiquidGlassTokens.GlassBorderGradient,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor, shape)
    .border(borderWidth, borderBrush, shape)

/**
 * Micro-interaction scale bounce on touch press (0.98f scale with gentle spring).
 */
@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.98f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "bounceScale"
    )

    if (!enabled) return this

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                val upOrCancel = waitForUpOrCancellation()
                isPressed = false
                if (upOrCancel != null) {
                    onClick()
                }
            }
        }
}
