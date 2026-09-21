package com.subzero.ui.screens

import androidx.compose.runtime.Composable

/**
 * Compatibility delegation to DemoPaywallScreen.
 */
@Composable
fun MockPaywallScreen(onNavigateBack: () -> Unit) {
    DemoPaywallScreen(onNavigateBack = onNavigateBack)
}
