package com.subzero.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.engine.InstalledUpiApp
import com.subzero.ui.theme.*

/**
 * Material 3 Horizontal tactile carousel showing connected UPI applications with live sync state indicators
 * and active mandate counters.
 */
@Composable
fun UpiAppSyncRow(
    installedApps: List<InstalledUpiApp>,
    syncingPackage: String?,
    mandatesPerApp: Map<String, Int>,
    onAppSelected: (InstalledUpiApp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connected UPI Apps",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily
            )
            Text(
                text = "Tap to sync",
                fontSize = 11.sp,
                color = M3TextSecondary,
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(installedApps, key = { it.packageName }) { app ->
                val isSyncing = syncingPackage == app.packageName
                val activeCount = mandatesPerApp[app.appName] ?: 0

                UpiAppCard(
                    app = app,
                    isSyncing = isSyncing,
                    activeCount = activeCount,
                    onClick = { onAppSelected(app) }
                )
            }
        }
    }
}

@Composable
fun UpiAppCard(
    app: InstalledUpiApp,
    isSyncing: Boolean,
    activeCount: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = M3SurfaceWhite,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(138.dp)
            .bouncyClickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // App Avatar / Logo Initial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UpiAppIcon(
                    packageName = app.packageName,
                    appName = app.appName,
                    modifier = Modifier.size(36.dp)
                )

                if (isSyncing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                        color = M3PinePrimary
                    )
                } else if (activeCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = M3BadgeMint
                    ) {
                        Text(
                            text = "$activeCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3BadgeMintText,
                            fontFamily = GoogleSansFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // App Name
            Text(
                text = app.appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sync Status Label
            if (isSyncing) {
                Text(
                    text = "Syncing...",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = M3PinePrimary,
                    fontFamily = GoogleSansFamily
                )
            } else if (activeCount > 0) {
                Text(
                    text = "$activeCount AutoPay",
                    fontSize = 10.sp,
                    color = M3TextSecondary,
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Tap to audit",
                    fontSize = 10.sp,
                    color = M3TextTertiary,
                    fontFamily = GoogleSansFamily
                )
            }
        }
    }
}
