package com.subzero.guardian.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.subzero.ui.components.bouncyClickable

@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDownFactor: Float = 0.96f,
    onClick: () -> Unit
): Modifier = this.bouncyClickable(enabled, scaleDownFactor, onClick)

@Composable
fun M3TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    com.subzero.ui.components.M3TactileSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
    )
}

@Composable
fun UpiAppSyncRow(
    installedApps: List<com.subzero.engine.InstalledUpiApp>,
    syncingPackage: String?,
    mandatesPerApp: Map<String, Int>,
    onAppSelected: (com.subzero.engine.InstalledUpiApp) -> Unit,
    modifier: Modifier = Modifier
) {
    com.subzero.ui.components.UpiAppSyncRow(
        installedApps = installedApps,
        syncingPackage = syncingPackage,
        mandatesPerApp = mandatesPerApp,
        onAppSelected = onAppSelected,
        modifier = modifier
    )
}

@Composable
fun UpiAppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    com.subzero.ui.components.UpiAppIcon(
        packageName = packageName,
        appName = appName,
        modifier = modifier
    )
}
