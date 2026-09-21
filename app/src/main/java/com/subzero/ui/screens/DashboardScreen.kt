package com.subzero.ui.screens

import androidx.compose.runtime.Composable
import com.subzero.data.MandateEntity

/**
 * Compatibility delegation to HomeScreen with Liquid Glass aesthetics.
 */
@Composable
fun DashboardScreen(
    onNavigateToMockPaywall: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAssistedRevoke: (MandateEntity) -> Unit
) {
    HomeScreen(
        onNavigateToMockPaywall = onNavigateToMockPaywall,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAssistedRevoke = onNavigateToAssistedRevoke
    )
}
