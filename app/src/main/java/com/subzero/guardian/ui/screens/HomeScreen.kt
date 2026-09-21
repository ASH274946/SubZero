package com.subzero.guardian.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.subzero.data.MandateEntity

@Composable
fun HomeScreen(
    onLaunchWalkthrough: () -> Unit = {},
    onNavigateToDemoPaywall: () -> Unit = onLaunchWalkthrough,
    onNavigateToMockPaywall: () -> Unit = onLaunchWalkthrough,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAssistedRevoke: ((MandateEntity) -> Unit)? = null,
    onNavigateToKillSwitchQueue: () -> Unit = {},
    onTriggerSimulatedWarning: (() -> Unit)? = null
) {
    com.subzero.ui.screens.HomeScreen(
        onLaunchWalkthrough = onLaunchWalkthrough,
        onNavigateToDemoPaywall = onNavigateToDemoPaywall,
        onNavigateToMockPaywall = onNavigateToMockPaywall,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAssistedRevoke = onNavigateToAssistedRevoke,
        onNavigateToKillSwitchQueue = onNavigateToKillSwitchQueue,
        onTriggerSimulatedWarning = onTriggerSimulatedWarning
    )
}

@Composable
fun DetailedM3MandateCard(
    mandate: MandateEntity,
    onStopClick: () -> Unit
) {
    com.subzero.ui.screens.DetailedM3MandateCard(
        mandate = mandate,
        onStopClick = onStopClick
    )
}

@Composable
fun AlignedMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    com.subzero.ui.screens.AlignedMetricTile(
        label = label,
        value = value,
        modifier = modifier
    )
}
