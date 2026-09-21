package com.subzero.ui.screens

import androidx.compose.runtime.Composable
import com.subzero.data.MandateEntity

/**
 * Compatibility delegation to CancelGuide.
 */
@Composable
fun AssistedNavigatorOverlay(
    mandate: MandateEntity,
    onNavigateBack: () -> Unit
) {
    CancelGuide(
        mandate = mandate,
        onClose = onNavigateBack
    )
}
