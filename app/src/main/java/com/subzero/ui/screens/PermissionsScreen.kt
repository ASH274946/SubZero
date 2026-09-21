package com.subzero.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.subzero.services.PaywallAccessibilityService
import com.subzero.ui.OverlayManager
import com.subzero.ui.theme.LiquidGlassTokens
import com.subzero.ui.theme.bounceClick
import com.subzero.ui.theme.liquidGlassCanvasBackground
import com.subzero.ui.theme.liquidGlassSurface

@Composable
fun PermissionsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val overlayManager = remember { OverlayManager.getInstance(context) }

    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isAccessibilityGranted by remember { mutableStateOf(checkAccessibilityServiceEnabled(context)) }
    var isNotificationGranted by remember { mutableStateOf(checkNotificationListenerEnabled(context)) }
    var selectedLanguage by remember { mutableStateOf(overlayManager.currentLanguage) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayGranted = Settings.canDrawOverlays(context)
                isAccessibilityGranted = checkAccessibilityServiceEnabled(context)
                isNotificationGranted = checkNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassCanvasBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LiquidGlassTokens.GlassSurfaceDefault)
                        .border(1.dp, LiquidGlassTokens.GlassBorderSubtle, RoundedCornerShape(50))
                        .bounceClick { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LiquidGlassTokens.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Permissions & Privacy",
                    color = LiquidGlassTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Zero-Cloud Privacy Guarantee Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = LiquidGlassTokens.GlassSurfaceDefault,
                        borderBrush = LiquidGlassTokens.GlassBorderGradient
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x1FA7F3D0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LiquidGlassTokens.PastelMint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "100% On-Device Privacy",
                            color = LiquidGlassTokens.PastelMint,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SubZero operates with ZERO internet permissions in its manifest. No cloud servers, no analytics, no external data leakage.",
                            color = LiquidGlassTokens.TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SYSTEM PERMISSIONS",
                color = LiquidGlassTokens.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Permission 1: Screen Watcher (Accessibility)
            PermissionItem(
                title = "Paywall Accessibility Service",
                description = "Inspects visible subscription terms on-device. Strictly excluded from banking apps.",
                isGranted = isAccessibilityGranted,
                icon = Icons.Rounded.AccessibilityNew,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // Permission 2: Bank Notification Listener
            PermissionItem(
                title = "AutoPay Notification Reader",
                description = "Captures official bank SMS and UPI e-mandate registration notifications locally.",
                isGranted = isNotificationGranted,
                icon = Icons.Rounded.NotificationsActive,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // Permission 3: Overlay HUD
            PermissionItem(
                title = "Protective Warning HUD",
                description = "Draws the non-touchable frosted warning border over deceptive apps.",
                isGranted = isOverlayGranted,
                icon = Icons.Rounded.Layers,
                onGrantClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Overlay HUD Language
            Text(
                text = "DEFAULT WARNING LANGUAGE",
                color = LiquidGlassTokens.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassSurface(
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = LiquidGlassTokens.GlassSurfaceDefault,
                        borderBrush = LiquidGlassTokens.GlassBorderGradient
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = LiquidGlassTokens.PastelLavender,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warning Overlay Language",
                            color = LiquidGlassTokens.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageChip(
                            label = "English",
                            isSelected = selectedLanguage == OverlayManager.WarningLanguage.ENGLISH,
                            onClick = {
                                selectedLanguage = OverlayManager.WarningLanguage.ENGLISH
                                overlayManager.currentLanguage = OverlayManager.WarningLanguage.ENGLISH
                            },
                            modifier = Modifier.weight(1f)
                        )

                        LanguageChip(
                            label = "తెలుగు",
                            isSelected = selectedLanguage == OverlayManager.WarningLanguage.TELUGU,
                            onClick = {
                                selectedLanguage = OverlayManager.WarningLanguage.TELUGU
                                overlayManager.currentLanguage = OverlayManager.WarningLanguage.TELUGU
                            },
                            modifier = Modifier.weight(1f)
                        )

                        LanguageChip(
                            label = "हिन्दी",
                            isSelected = selectedLanguage == OverlayManager.WarningLanguage.HINDI,
                            onClick = {
                                selectedLanguage = OverlayManager.WarningLanguage.HINDI
                                overlayManager.currentLanguage = OverlayManager.WarningLanguage.HINDI
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .liquidGlassSurface(
                shape = RoundedCornerShape(20.dp),
                backgroundColor = LiquidGlassTokens.GlassSurfaceDefault,
                borderBrush = LiquidGlassTokens.GlassBorderGradient
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LiquidGlassTokens.GlassSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) LiquidGlassTokens.PastelMint else LiquidGlassTokens.PastelPeriwinkle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        color = LiquidGlassTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isGranted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LiquidGlassTokens.PastelMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active",
                            color = LiquidGlassTokens.PastelMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(LiquidGlassTokens.GlassSurfaceElevated)
                            .border(1.dp, LiquidGlassTokens.PastelPeriwinkle.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .bounceClick { onGrantClick() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Turn On",
                            color = LiquidGlassTokens.PastelPeriwinkle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                color = LiquidGlassTokens.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) Color(0x33C4B5FD) else LiquidGlassTokens.GlassSurfaceElevated
            )
            .border(
                1.dp,
                if (isSelected) LiquidGlassTokens.PastelLavender else Color(0x14FFFFFF),
                RoundedCornerShape(50)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) LiquidGlassTokens.PastelLavender else LiquidGlassTokens.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun checkAccessibilityServiceEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    if (flat != null && flat.contains(context.packageName)) return true
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK or AccessibilityServiceInfo.FEEDBACK_GENERIC)
    val expectedName = PaywallAccessibilityService::class.java.name
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName &&
                it.resolveInfo.serviceInfo.name == expectedName
    }
}

private fun checkNotificationListenerEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}
