package com.subzero.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView

/**
 * 120Hz Optimized Bouncy Click:
 * Reads animation state entirely inside the `graphicsLayer` lambda to skip the Composition phase.
 */
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDownFactor: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDownFactor else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "BouncyScale"
    )

    return this
        // Defer state read to the drawing phase (0 recompositions during animation)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val upOrCancel = waitForUpOrCancellation()
                isPressed = false
                if (upOrCancel != null) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onClick()
                }
            }
        }
}
