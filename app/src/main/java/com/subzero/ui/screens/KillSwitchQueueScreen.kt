package com.subzero.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.data.MandateEntity
import com.subzero.services.UniversalRevocationManager
import com.subzero.ui.theme.LiquidGlassTokens
import com.subzero.ui.theme.bounceClick
import com.subzero.ui.theme.liquidGlassCanvasBackground
import com.subzero.ui.theme.liquidGlassSurface
import java.util.Locale

/**
 * Screen 3: Assisted Universal Revocation Queue ("Kill Switch").
 *
 * Coordinates sequential mandate cancellation across PhonePe, Google Pay, and Paytm.
 * Displays real-time progress, deep-link triggers, assisted instructions, and auto-advances
 * as regulatory bank SMS/notification confirmations arrive.
 */
@Composable
fun KillSwitchQueueScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val queueItems by UniversalRevocationManager.queueItems.collectAsState()
    val currentIndex by UniversalRevocationManager.currentIndex.collectAsState()
    val isQueueActive by UniversalRevocationManager.isQueueActive.collectAsState()
    val lastAutoAdvancedUmn by UniversalRevocationManager.lastAutoAdvancedUmn.collectAsState()

    val currentMandate = if (currentIndex in queueItems.indices) queueItems[currentIndex] else null
    val totalCount = queueItems.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassCanvasBackground()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Top Bar
            QueueTopBar(
                onBack = {
                    UniversalRevocationManager.clearQueue()
                    onNavigateBack()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!isQueueActive || currentMandate == null) {
                // Completed State View
                QueueCompletedView(
                    totalRevoked = totalCount,
                    onReturn = {
                        UniversalRevocationManager.clearQueue()
                        onNavigateBack()
                    }
                )
            } else {
                // Active Queue Content
                QueueProgressHeader(
                    currentStep = currentIndex + 1,
                    totalSteps = totalCount,
                    merchantName = currentMandate.merchantName
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Advancement HUD Status Pill
                AutoAdvancingHudBanner(lastAutoAdvancedUmn = lastAutoAdvancedUmn)

                Spacer(modifier = Modifier.height(16.dp))

                // Target Mandate Card
                TargetMandateCard(mandate = currentMandate)

                Spacer(modifier = Modifier.height(20.dp))

                // Step-by-Step Revocation Guide
                RevocationInstructionsCard(mandate = currentMandate)

                Spacer(modifier = Modifier.height(20.dp))

                // Deep-Link UPI App Launchers
                UpiAppLaunchersSection(context = context)

                Spacer(modifier = Modifier.height(24.dp))

                // Primary CoralGradient Action: "Revoke & Next Mandate"
                val isLast = (currentIndex == totalCount - 1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LiquidGlassTokens.CoralGradient)
                        .bounceClick {
                            UniversalRevocationManager.revokeCurrentAndAdvance(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLast) "Mark Revoked & Finish ⚡" else "Mark Revoked & Next Mandate ⚡",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary Action: Skip Mandate
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LiquidGlassTokens.GlassSurfaceDefault)
                        .border(1.dp, LiquidGlassTokens.GlassBorderSubtle, RoundedCornerShape(50))
                        .bounceClick {
                            UniversalRevocationManager.advanceQueue()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Skip for Now",
                        color = LiquidGlassTokens.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun QueueTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(LiquidGlassTokens.GlassSurfaceElevated)
                .border(1.dp, LiquidGlassTokens.GlassBorderSubtle, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = LiquidGlassTokens.TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Universal Kill Switch",
                color = LiquidGlassTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Sequential Assisted Revocation",
                color = LiquidGlassTokens.PastelRose,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0x26FDA4AF))
                .border(1.dp, LiquidGlassTokens.PastelRose.copy(alpha = 0.4f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Assisted Mode",
                color = LiquidGlassTokens.PastelRose,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QueueProgressHeader(
    currentStep: Int,
    totalSteps: Int,
    merchantName: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step $currentStep of $totalSteps: $merchantName",
                color = LiquidGlassTokens.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${((currentStep.toFloat() / totalSteps) * 100).toInt()}% Done",
                color = LiquidGlassTokens.PastelPeriwinkle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..totalSteps) {
                val isCompleted = i < currentStep
                val isCurrent = i == currentStep
                val segmentColor = when {
                    isCompleted -> LiquidGlassTokens.PastelMint
                    isCurrent -> LiquidGlassTokens.PastelRose
                    else -> LiquidGlassTokens.GlassSurfaceElevated
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(segmentColor)
                )
            }
        }
    }
}

@Composable
private fun AutoAdvancingHudBanner(lastAutoAdvancedUmn: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1493C5FD))
            .border(1.dp, LiquidGlassTokens.PastelPeriwinkle.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = LiquidGlassTokens.PastelPeriwinkle,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Auto-Advance Guard Active",
                    color = LiquidGlassTokens.PastelPeriwinkle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Listening for bank SMS/notification confirmations to auto-advance queue.",
                    color = LiquidGlassTokens.TextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TargetMandateCard(mandate: MandateEntity) {
    val initial = mandate.merchantName.firstOrNull()?.uppercase() ?: "S"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = LiquidGlassTokens.GlassSurfaceDeep,
                borderBrush = LiquidGlassTokens.GlassBorderGradient,
                borderWidth = 1.dp
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(LiquidGlassTokens.GlassSurfaceElevated)
                            .border(1.dp, LiquidGlassTokens.PastelRose.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = LiquidGlassTokens.PastelRose,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = mandate.merchantName,
                            color = LiquidGlassTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UMN: ${mandate.maskedUmn}",
                            color = LiquidGlassTokens.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Liability Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.getDefault(), "₹%.0f", mandate.maxDebitAmount),
                        color = LiquidGlassTokens.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "/ ${mandate.billingFrequency.lowercase()}",
                        color = LiquidGlassTokens.PastelRose,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RevocationInstructionsCard(mandate: MandateEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = LiquidGlassTokens.GlassSurfaceDefault,
                borderBrush = LiquidGlassTokens.GlassBorderSubtle,
                borderWidth = 1.dp
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Assisted Instructions",
                color = LiquidGlassTokens.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InstructionRow(
                    stepNumber = "1",
                    text = buildAnnotatedString {
                        append("Open your UPI app (")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.PastelPeriwinkle)) {
                            append("PhonePe, GPay, or Paytm")
                        }
                        append(").")
                    }
                )

                InstructionRow(
                    stepNumber = "2",
                    text = buildAnnotatedString {
                        append("Tap your ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.TextPrimary)) {
                            append("Profile / Settings")
                        }
                        append(" → Select ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.TextPrimary)) {
                            append("AutoPay / Mandates")
                        }
                        append(".")
                    }
                )

                InstructionRow(
                    stepNumber = "3",
                    text = buildAnnotatedString {
                        append("Locate ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.TextPrimary)) {
                            append(mandate.merchantName)
                        }
                        append(" and tap ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.PastelRose)) {
                            append("Cancel / Revoke AutoPay")
                        }
                        append(".")
                    }
                )
            }
        }
    }
}

@Composable
private fun InstructionRow(
    stepNumber: String,
    text: androidx.compose.ui.text.AnnotatedString
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(LiquidGlassTokens.GlassSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = LiquidGlassTokens.PastelPeriwinkle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = LiquidGlassTokens.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UpiAppLaunchersSection(context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Launch UPI Application",
            color = LiquidGlassTokens.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PhonePe Launcher
            AppLauncherPill(
                title = "PhonePe",
                modifier = Modifier.weight(1f),
                onClick = { UniversalRevocationManager.openPhonePe(context) }
            )

            // Google Pay Launcher
            AppLauncherPill(
                title = "Google Pay",
                modifier = Modifier.weight(1f),
                onClick = { UniversalRevocationManager.openGooglePay(context) }
            )

            // Paytm Launcher
            AppLauncherPill(
                title = "Paytm",
                modifier = Modifier.weight(1f),
                onClick = { UniversalRevocationManager.openPaytm(context) }
            )
        }
    }
}

@Composable
private fun AppLauncherPill(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(50))
            .background(LiquidGlassTokens.GlassSurfaceElevated)
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(50))
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                tint = LiquidGlassTokens.PastelPeriwinkle,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                color = LiquidGlassTokens.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QueueCompletedView(
    totalRevoked: Int,
    onReturn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x26A7F3D0))
                .border(2.dp, LiquidGlassTokens.PastelMint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = LiquidGlassTokens.PastelMint,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Queue Cleared!",
            color = LiquidGlassTokens.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "All targeted AutoPay mandates have been sequentially processed. Your recurring monthly liability has been neutralized.",
            color = LiquidGlassTokens.TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(50))
                .background(LiquidGlassTokens.PrimaryActionGradient)
                .bounceClick { onReturn() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Return to Dashboard",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
