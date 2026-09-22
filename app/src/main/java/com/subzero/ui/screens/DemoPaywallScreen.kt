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

import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subzero.data.BlockedTrapEntity
import com.subzero.data.SubZeroDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class DemoAlertLanguage {
    ENGLISH, TELUGU, HINDI
}

@Composable
fun DemoPaywallScreen(
    onDismiss: () -> Unit = {},
    onNavigateBack: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWarningPopup by remember { mutableStateOf(false) }

    val handleDismiss = {
        onNavigateBack()
    }

    if (showWarningPopup) {
        DemoWarningPopup(
            onDismiss = { showWarningPopup = false },
            onBlockAndExit = {
                scope.launch(Dispatchers.IO) {
                    try {
                        SubZeroDatabase.getInstance(context).mandateDao().recordBlockedTrap(
                            BlockedTrapEntity(
                                packageName = "com.docuscan.fake",
                                trapType = "Deceptive 3-Day Trial AutoPay",
                                detectedText = "Start 3-Day Free Trial - Renews at ₹899/month",
                                riskScore = 92
                            )
                        )
                    } catch (_: Exception) {}
                }
                showWarningPopup = false
                handleDismiss()
            }
        )
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
                        showWarningPopup = true
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

@Composable
fun DemoWarningPopup(
    onDismiss: () -> Unit,
    onBlockAndExit: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(DemoAlertLanguage.ENGLISH) }

    val headlineText = when (selectedLanguage) {
        DemoAlertLanguage.ENGLISH -> "This Free Trial is Not Free"
        DemoAlertLanguage.TELUGU -> "ఈ ఉచిత ట్రయల్ పూర్తిగా ఉచితం కాదు"
        DemoAlertLanguage.HINDI -> "यह ट्रायल मुफ़्त नहीं है"
    }

    val bodyText = when (selectedLanguage) {
        DemoAlertLanguage.ENGLISH -> "DocuScan Pro will automatically deduct ₹899 every month starting in 3 days via UPI AutoPay."
        DemoAlertLanguage.TELUGU -> "ఈ యాప్ 3 రోజుల తర్వాత ప్రతి నెలా మీ బ్యాంక్ నుండి ₹899 ఆటో-డెబిట్ చేస్తుంది."
        DemoAlertLanguage.HINDI -> "यह ऐप 3 दिनों के बाद हर महीने आपके बैंक खाते से ₹899 ऑटोपे के ज़रिए काट लेगा।"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = M3SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE6E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    // Header: Warning Badge + Risk Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = M3AlertCoralContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = M3AlertCoral,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Dark Pattern Detected",
                                    color = M3AlertCoral,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSansFamily
                                )
                            }
                        }

                        Text(
                            text = "RISK 92%",
                            color = M3AlertCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = GoogleSansFamily,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = headlineText,
                        color = M3TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFamily
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bodyText,
                        color = M3TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = GoogleSansFamily,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Language Selector Chips
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = M3CanvasBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                        ) {
                            DemoLanguageChip(
                                label = "English",
                                isSelected = selectedLanguage == DemoAlertLanguage.ENGLISH,
                                onClick = { selectedLanguage = DemoAlertLanguage.ENGLISH },
                                modifier = Modifier.weight(1f)
                            )
                            DemoLanguageChip(
                                label = "తెలుగు",
                                isSelected = selectedLanguage == DemoAlertLanguage.TELUGU,
                                onClick = { selectedLanguage = DemoAlertLanguage.TELUGU },
                                modifier = Modifier.weight(1f)
                            )
                            DemoLanguageChip(
                                label = "हिंदी",
                                isSelected = selectedLanguage == DemoAlertLanguage.HINDI,
                                onClick = { selectedLanguage = DemoAlertLanguage.HINDI },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Breakdown Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = M3CanvasBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Target App", fontSize = 12.sp, color = M3TextSecondary, fontFamily = GoogleSansFamily)
                                Text("DocuScan Pro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = M3TextPrimary, fontFamily = GoogleSansFamily)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hidden AutoPay", fontSize = 12.sp, color = M3TextSecondary, fontFamily = GoogleSansFamily)
                                Text("₹899.00 / month", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = M3AlertCoral, fontFamily = GoogleSansFamily)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Auto-Deduction", fontSize = 12.sp, color = M3TextSecondary, fontFamily = GoogleSansFamily)
                                Text("After 72 Hours", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = M3TextPrimary, fontFamily = GoogleSansFamily)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = M3AlertCoral,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .bouncyClickable { onBlockAndExit() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Block AutoPay",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSansFamily
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = M3CanvasBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDCE6E0)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .bouncyClickable { onDismiss() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Dismiss",
                                    color = M3TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSansFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoLanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = if (isSelected) M3SurfaceWhite else Color.Transparent,
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (isSelected) M3PinePrimary else M3TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = GoogleSansFamily
            )
        }
    }
}

