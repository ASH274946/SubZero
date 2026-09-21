package com.subzero.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.SubZeroApp
import com.subzero.data.MandateEntity
import com.subzero.ui.theme.LiquidGlassTokens
import com.subzero.ui.theme.bounceClick
import com.subzero.ui.theme.liquidGlassCanvasBackground
import kotlinx.coroutines.launch

/**
 * Component 4: Assisted Cancel Guide (CancelGuide)
 * Floating liquid glass checklist for step-by-step AutoPay revocation.
 */
@Composable
fun CancelGuide(
    mandate: MandateEntity = MandateEntity(
        umn = "HDFC98421034871290",
        merchantName = "DocuScan",
        maxDebitAmount = 899.0,
        billingFrequency = "Monthly",
        creationTimestamp = System.currentTimeMillis() - 86400000L * 20,
        isRevoked = false
    ),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val repository = SubZeroApp.instance.mandateRepository
    val coroutineScope = rememberCoroutineScope()

    val checkedSteps = remember {
        mutableStateListOf(false, false, false)
    }

    var isRevokedLocally by remember { mutableStateOf(mandate.isRevoked) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquidGlassCanvasBackground()
            .padding(horizontal = 16.dp)
    ) {
        // Floating Card Anchored at Top ~10-15% of the screen
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(LiquidGlassTokens.GlassSurfaceDeep) // rgba(15, 23, 42, 0.94)
                    .border(
                        1.5.dp,
                        LiquidGlassTokens.PastelPeriwinkle.copy(alpha = 0.35f),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row with Close Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cancel ${mandate.merchantName} Subscription",
                                color = LiquidGlassTokens.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Step-by-step AutoPay revocation",
                                color = LiquidGlassTokens.PastelPeriwinkle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(50))
                                .background(LiquidGlassTokens.GlassSurfaceElevated)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = LiquidGlassTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3-Step Numbered Checklist (Spacing: 10.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Step 1
                        ChecklistStep(
                            stepNumber = 1,
                            text = buildAnnotatedString {
                                append("Tap your ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.TextPrimary)) {
                                    append("Profile picture")
                                }
                                append(" in the top-left corner.")
                            },
                            isChecked = checkedSteps[0],
                            onToggle = { checkedSteps[0] = !checkedSteps[0] }
                        )

                        // Step 2
                        ChecklistStep(
                            stepNumber = 2,
                            text = buildAnnotatedString {
                                append("Select ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.TextPrimary)) {
                                    append("AutoPay / Subscriptions Settings")
                                }
                                append(".")
                            },
                            isChecked = checkedSteps[1],
                            onToggle = { checkedSteps[1] = !checkedSteps[1] }
                        )

                        // Step 3
                        val maskedSuffix = if (mandate.umn.length >= 4) mandate.umn.takeLast(4) else "9842"
                        ChecklistStep(
                            stepNumber = 3,
                            text = buildAnnotatedString {
                                append("Find ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.TextPrimary)) {
                                    append(mandate.merchantName)
                                }
                                append(" (UMN: ...$maskedSuffix) and tap ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = LiquidGlassTokens.PastelRose)) {
                                    append("Cancel AutoPay")
                                }
                                append(".")
                            },
                            isChecked = checkedSteps[2],
                            onToggle = { checkedSteps[2] = !checkedSteps[2] }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PRIMARY DYNAMIC BUTTON: "Open [AppName] & Cancel Subscription"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(com.subzero.ui.theme.M3PinePrimary)
                            .bounceClick {
                                com.subzero.engine.UpiRedirectManager.openUpiAppForRevoke(context, mandate)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Launch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open ${mandate.sourceAppName} & Cancel Subscription",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mark as Revoked Toggle or Done
                    if (!isRevokedLocally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x1FA7F3D0))
                                .border(1.dp, LiquidGlassTokens.PastelMint.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .bounceClick {
                                    coroutineScope.launch {
                                        repository.markAsRevoked(mandate.umn)
                                        isRevokedLocally = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = LiquidGlassTokens.PastelMint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mark as Revoked in SubZero",
                                    color = LiquidGlassTokens.PastelMint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "✓ Mandate marked as revoked in SubZero database",
                            color = LiquidGlassTokens.PastelMint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Link: "Done"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClose() }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            color = LiquidGlassTokens.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistStep(
    stepNumber: Int,
    text: androidx.compose.ui.text.AnnotatedString,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isChecked) Color(0x14A7F3D0) else LiquidGlassTokens.GlassSurfaceDefault)
            .border(
                width = 1.dp,
                brush = if (isChecked) androidx.compose.ui.graphics.SolidColor(LiquidGlassTokens.PastelMint.copy(alpha = 0.35f)) else LiquidGlassTokens.GlassBorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onToggle() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isChecked) LiquidGlassTokens.PastelMint else LiquidGlassTokens.GlassSurfaceElevated
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        color = LiquidGlassTokens.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                color = LiquidGlassTokens.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
