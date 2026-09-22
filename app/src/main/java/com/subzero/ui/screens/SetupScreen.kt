package com.subzero.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.subzero.R
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.subzero.ui.theme.LiquidGlassTokens
import com.subzero.ui.theme.bounceClick
import com.subzero.ui.theme.liquidGlassCanvasBackground
import com.subzero.ui.theme.liquidGlassSurface

/**
 * Screen 1: Setup Screen (SetupScreen)
 * Shown on first launch to request required permissions with frosted Liquid Glass aesthetics.
 */
@Composable
fun SetupScreen(
    onSetupCompleted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAccessibilityGranted by remember { mutableStateOf(checkAccessibilityEnabled(context)) }
    var isNotificationGranted by remember { mutableStateOf(checkNotificationEnabled(context)) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Re-check permissions automatically on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = checkAccessibilityEnabled(context)
                isNotificationGranted = checkNotificationEnabled(context)
                isOverlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allGranted = isAccessibilityGranted && isNotificationGranted && isOverlayGranted
    val buttonAlpha by animateFloatAsState(
        targetValue = if (allGranted) 1f else 0.45f,
        label = "buttonAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassCanvasBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.subzero_logo),
                contentDescription = "SubZero Logo",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0x0FFFFFFF)) // rgba(255, 255, 255, 0.06)
                    .border(
                        1.dp,
                        LiquidGlassTokens.GlassBorderSubtle,
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SubZero • On-Device Defense",
                        color = LiquidGlassTokens.PastelPeriwinkle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Headline
            Text(
                text = "Set Up SubZero",
                color = LiquidGlassTokens.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Runs 100% on your device. Zero cloud, zero internet, zero data collection.",
                color = LiquidGlassTokens.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Stack of 3 Glass Permission Cards (14.dp spacing)
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Card 1: Screen Watcher
                SetupPermissionCard(
                    title = "Screen Watcher",
                    subtext = "Catches sneaky trial terms before you pay.",
                    icon = Icons.Rounded.AccessibilityNew,
                    isGranted = isAccessibilityGranted,
                    onToggle = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                // Card 2: Bank Alert Reader
                SetupPermissionCard(
                    title = "Bank Alert Reader",
                    subtext = "Reads official bank SMS when a subscription starts.",
                    icon = Icons.Rounded.NotificationsActive,
                    isGranted = isNotificationGranted,
                    onToggle = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                // Card 3: Warning Shield
                SetupPermissionCard(
                    title = "Warning Shield",
                    subtext = "Draws a protective frosted border when an app tries to trick you.",
                    icon = Icons.Rounded.Layers,
                    isGranted = isOverlayGranted,
                    onToggle = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bottom Action Dock (56.dp, RoundedCornerShape(50), PrimaryActionGradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(buttonAlpha)
                    .bounceClick(enabled = allGranted) {
                        if (allGranted) {
                            onSetupCompleted()
                        }
                    }
                    .height(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LiquidGlassTokens.PrimaryActionGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Start Protection",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SetupPermissionCard(
    title: String,
    subtext: String,
    icon: ImageVector,
    isGranted: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = LiquidGlassTokens.GlassSurfaceDefault,
                borderBrush = LiquidGlassTokens.GlassBorderGradient,
                borderWidth = 1.dp
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LiquidGlassTokens.GlassSurfaceElevated)
                        .border(1.dp, LiquidGlassTokens.GlassBorderSubtle, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) LiquidGlassTokens.PastelMint else LiquidGlassTokens.PastelPeriwinkle,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = LiquidGlassTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtext,
                        color = LiquidGlassTokens.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Pill Button (38.dp height, RoundedCornerShape(50))
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x1FA7F3D0)) // rgba(167, 243, 208, 0.12)
                        .border(1.dp, LiquidGlassTokens.PastelMint.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = LiquidGlassTokens.PastelMint,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active",
                            color = LiquidGlassTokens.PastelMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LiquidGlassTokens.GlassSurfaceElevated)
                        .border(1.dp, LiquidGlassTokens.PastelPeriwinkle.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .clickable { onToggle() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Turn On",
                        color = LiquidGlassTokens.PastelPeriwinkle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun checkAccessibilityEnabled(context: Context): Boolean {
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

private fun checkNotificationEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}
