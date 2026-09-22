package com.subzero.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.engine.PaywallNotificationManager
import com.subzero.ui.components.bouncyClickable
import com.subzero.ui.theme.*

@Composable
fun DemoPaywallScreen(
    onDismiss: () -> Unit = {},
    onNavigateBack: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val handleDismiss = {
        onNavigateBack()
    }

    Scaffold(
        containerColor = M3CanvasBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = M3PineContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = M3PinePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DocuScan • v4.2",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3TextPrimary,
                        fontFamily = GoogleSansFamily
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = M3SurfaceWhite,
                    onClick = handleDismiss,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = M3TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline & Subtitle
            Text(
                text = "Unlock Pro Document Tools",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Unlimited PDF scanning, OCR text extraction, and export.",
                fontSize = 13.sp,
                color = M3TextSecondary,
                fontFamily = GoogleSansFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Solid White Feature Cards
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = M3SurfaceWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    FeatureItem(
                        icon = Icons.Rounded.Bolt,
                        title = "Fast OCR",
                        desc = "Extract text from images and documents in under a second."
                    )
                    HorizontalDivider(color = M3CanvasBackground, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    FeatureItem(
                        icon = Icons.Rounded.VisibilityOff,
                        title = "Watermark Removal",
                        desc = "Clean, professional document exports with zero branding."
                    )
                    HorizontalDivider(color = M3CanvasBackground, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                    FeatureItem(
                        icon = Icons.Rounded.CloudUpload,
                        title = "Cloud Backup",
                        desc = "Secure synchronization across all your mobile devices."
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // The Deceptive Call-to-Action
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = M3PinePrimary,
                border = androidx.compose.foundation.BorderStroke(1.dp, M3PinePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bouncyClickable {
                        PaywallNotificationManager.postDarkPatternAlert(
                            context = context,
                            appName = "DocuScan Pro",
                            amount = "₹899/mo",
                            renewalDetails = "Charges apply automatically after the 3-day trial period expires."
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "START 3-DAY FREE TRIAL",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontFamily = GoogleSansFamily,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // The Microscopic Dark Pattern Text
            Text(
                text = "Renews at ₹899/month automatically via UPI AutoPay after 72 hours. Cancel anytime in payment settings.",
                fontSize = 8.sp,
                color = Color(0xFF94A3B8), // Intentionally faint low contrast
                textAlign = TextAlign.Center,
                fontFamily = GoogleSansFamily,
                lineHeight = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun FeatureItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = M3PineContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = M3PinePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = M3TextSecondary,
                fontFamily = GoogleSansFamily,
                lineHeight = 16.sp
            )
        }
    }
}
