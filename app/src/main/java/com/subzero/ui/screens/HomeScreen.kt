package com.subzero.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.subzero.data.MandateEntity
import com.subzero.data.SubZeroDatabase
import com.subzero.engine.*
import com.subzero.ui.components.M3TactileSwitch
import com.subzero.ui.components.UpiAppIcon
import com.subzero.ui.components.UpiAppSyncRow
import com.subzero.ui.components.bouncyClickable
import com.subzero.ui.theme.*
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { SubZeroDatabase.getInstance(context) }
    val mandateDao = database.mandateDao()

    val activeMandates by mandateDao.getActiveMandates().collectAsState(initial = emptyList())
    val activeCount by mandateDao.getActiveMandateCount().collectAsState(initial = 0)
    val totalDrain by mandateDao.getTotalMonthlyDrain().collectAsState(initial = 0.0)
    val blockedTrapsCount by mandateDao.getBlockedTrapsCount().collectAsState(initial = 0)

    val installedUpiApps = remember { InstalledUpiDetector.getInstalledUpiApps(context) }
    var syncingAppPackage by remember { mutableStateOf<String?>(null) }
    val scopedSyncEngine = remember { AppScopedSyncEngine(context, mandateDao) }

    val mandatesPerApp = remember(activeMandates) {
        activeMandates.groupBy { it.sourceAppName }.mapValues { it.value.size }
    }

    var screenWatcherActive by remember { mutableStateOf(true) }
    var smsAuditActive by remember { mutableStateOf(true) }
    var preDebit48hActive by remember { mutableStateOf(true) }
    var hapticAlertsActive by remember { mutableStateOf(true) }
    var selectedMandateForRevoke by remember { mutableStateOf<MandateEntity?>(null) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            DailyAutoPaySyncWorker.scheduleDailySync(context)
        }
    }

    Scaffold(containerColor = M3CanvasBackground) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP BAR: TITLE + WALKTHROUGH ACTION BUTTON
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SubZero",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = M3TextPrimary,
                            fontFamily = GoogleSansFamily,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Daily Synced • On-Device Guardian",
                            fontSize = 12.sp,
                            color = M3TextSecondary,
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = M3SurfaceWhite,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(42.dp)
                            .bouncyClickable {
                                scope.launch {
                                    WalkthroughEngine.start(context, mandateDao, onLaunchWalkthrough)
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Walkthrough",
                                tint = M3PinePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Walkthrough",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3PinePrimary,
                                fontFamily = GoogleSansFamily
                            )
                        }
                    }
                }
            }

            // 2. PERFECTLY CENTERED METRIC TILES
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlignedMetricTile(
                        label = "Active",
                        value = "$activeCount",
                        modifier = Modifier.weight(1f)
                    )
                    AlignedMetricTile(
                        label = "Monthly Drain",
                        value = "₹${(totalDrain ?: 0.0).toInt()}",
                        modifier = Modifier.weight(1.2f)
                    )
                    AlignedMetricTile(
                        label = "Blocked Traps",
                        value = "$blockedTrapsCount",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. EXPOSURE GAUGE
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = M3SurfaceWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = M3PinePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Protection Exposure Limit",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = M3TextPrimary,
                                    fontFamily = GoogleSansFamily
                                )
                                Text(
                                    text = "Monthly auto-debit ceiling: ₹5,000",
                                    fontSize = 12.sp,
                                    color = M3TextSecondary,
                                    fontFamily = GoogleSansFamily
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val progressFraction = ((totalDrain ?: 0.0) / 5000.0).coerceIn(0.0, 1.0).toFloat()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(M3PineContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = if (progressFraction == 0f) 0.02f else progressFraction)
                                    .clip(RoundedCornerShape(50))
                                    .background(M3PinePrimary)
                            )
                        }
                    }
                }
            }

            // 4. CONNECTED UPI APPS CAROUSEL (App-Scoped Sync Row)
            item {
                UpiAppSyncRow(
                    installedApps = installedUpiApps,
                    syncingPackage = syncingAppPackage,
                    mandatesPerApp = mandatesPerApp,
                    onAppSelected = { selectedApp ->
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            scope.launch {
                                syncingAppPackage = selectedApp.packageName
                                try {
                                    scopedSyncEngine.syncAppMandates(selectedApp)
                                } finally {
                                    syncingAppPackage = null
                                }
                            }
                        } else {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    }
                )
            }

            // 5. GUARDIAN SECURITY TOGGLES (Includes 48h Pre-Debit Alarm)
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = M3SurfaceWhite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 18.dp)) {
                        ToggleRowItem(
                            icon = Icons.Rounded.Visibility,
                            title = "Screen Watcher (SLM AI)",
                            subtitle = "Intercepts deceptive 3-day trial terms before payment",
                            checked = screenWatcherActive,
                            onCheckedChange = { screenWatcherActive = it }
                        )
                        HorizontalDivider(color = M3CanvasBackground, thickness = 1.dp)

                        ToggleRowItem(
                            icon = Icons.Rounded.SyncLock,
                            title = "Start-of-Day Daily Sync",
                            subtitle = "Audits all UPI apps every morning at 06:00 AM",
                            checked = smsAuditActive,
                            onCheckedChange = { smsAuditActive = it }
                        )
                        HorizontalDivider(color = M3CanvasBackground, thickness = 1.dp)

                        ToggleRowItem(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "48-Hour Pre-Debit Alarm",
                            subtitle = "Reminds you 48 hours before interbank clearing lock-in",
                            checked = preDebit48hActive,
                            onCheckedChange = { preDebit48hActive = it }
                        )
                        HorizontalDivider(color = M3CanvasBackground, thickness = 1.dp)

                        ToggleRowItem(
                            icon = Icons.Rounded.Vibration,
                            title = "Haptic Alerts",
                            subtitle = "Provides tactile pulse whenever a trap is caught",
                            checked = hapticAlertsActive,
                            onCheckedChange = { hapticAlertsActive = it }
                        )
                    }
                }
            }

            // 6. ACTIVE MANDATES LIST (With Remaining Time & Price Change Badges)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Mandates",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3TextPrimary,
                        fontFamily = GoogleSansFamily
                    )
                    Text(
                        text = "$activeCount Indexed",
                        fontSize = 12.sp,
                        color = M3TextSecondary,
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (activeMandates.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = M3SurfaceWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                tint = M3PinePrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Zero Active Mandates",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3TextPrimary,
                                fontFamily = GoogleSansFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap any connected UPI app above to sync subscriptions.",
                                fontSize = 12.sp,
                                color = M3TextSecondary,
                                fontFamily = GoogleSansFamily
                            )
                        }
                    }
                }
            } else {
                items(activeMandates, key = { it.umn }) { mandate ->
                    DetailedM3MandateCard(
                        mandate = mandate,
                        onStopClick = {
                            selectedMandateForRevoke = mandate
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Render Modal Bottom Sheet when a mandate is selected
    selectedMandateForRevoke?.let { mandate ->
        CancelGuideSheet(
            mandate = mandate,
            onDismiss = {
                selectedMandateForRevoke = null
            }
        )
    }
}

// -----------------------------------------------------------------------
// DETAILED MANDATE CARD (SHOWING DYNAMIC COUNTDOWN & PRICE NOTICES)
// -----------------------------------------------------------------------

@Composable
fun DetailedM3MandateCard(
    mandate: MandateEntity,
    onStopClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = M3SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Merchant Icon + Details
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = M3PineContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = mandate.merchantName.take(1).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3PinePrimary,
                                fontFamily = GoogleSansFamily
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = mandate.merchantName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3TextPrimary,
                            fontFamily = GoogleSansFamily
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Active in: ",
                                fontSize = 11.sp,
                                color = M3TextSecondary,
                                fontFamily = GoogleSansFamily
                            )
                            UpiAppIcon(
                                packageName = mandate.sourceAppPackage,
                                appName = mandate.sourceAppName,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = mandate.sourceAppName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = M3PinePrimary,
                                fontFamily = GoogleSansFamily
                            )
                        }
                    }
                }

                // Amount & Price Change Notice
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${mandate.maxDebitAmount.toInt()}/mo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = M3PinePrimary,
                        fontFamily = GoogleSansFamily
                    )
                    mandate.previousAmount?.let { prev ->
                        if (prev != mandate.maxDebitAmount) {
                            Text(
                                text = "Was ₹${prev.toInt()}",
                                fontSize = 10.sp,
                                color = M3AlertCoral,
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = M3CanvasBackground, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Dynamic Remaining Time Badge + Revoke Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic Countdown Badge
                val remainingText = mandate.getRemainingTimeFormatted()
                val isImminent = remainingText.contains("Debiting") || remainingText.contains("Tomorrow") || remainingText.contains("h")

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isImminent) M3AlertCoralContainer else M3BadgeMint
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (isImminent) M3AlertCoral else M3BadgeMintText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = remainingText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isImminent) M3AlertCoral else M3BadgeMintText,
                            fontFamily = GoogleSansFamily
                        )
                    }
                }

                // Stop AutoPay Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = M3AlertCoralContainer,
                    modifier = Modifier.bouncyClickable { onStopClick() }
                ) {
                    Text(
                        text = "Stop in ${mandate.sourceAppName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3AlertCoral,
                        fontFamily = GoogleSansFamily,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------
// PERFECTLY CENTER-ALIGNED METRIC TILE COMPONENT
// -----------------------------------------------------------------------

@Composable
fun AlignedMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = M3SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = M3TextSecondary,
                fontFamily = GoogleSansFamily,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

// Backwards-compatible alias for existing callers
@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) = AlignedMetricTile(label = label, value = value, modifier = modifier)

@Composable
fun ToggleRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = M3TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = M3TextSecondary,
                fontFamily = GoogleSansFamily,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        M3TactileSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Backwards-compatible alias for existing M3MandateCard callers
@Composable
fun M3MandateCard(
    mandate: MandateEntity,
    onStopClick: () -> Unit
) = DetailedM3MandateCard(mandate = mandate, onStopClick = onStopClick)
