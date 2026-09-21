package com.subzero.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.ai.RiskReport
import com.subzero.ui.components.bouncyClickable
import com.subzero.ui.theme.*

typealias WarningOverlay = com.subzero.ui.overlays.WarningOverlay

enum class AlertLanguage {
    ENGLISH, TELUGU, HINDI
}

@Composable
fun WarningOverlayContent(
    riskReport: RiskReport = RiskReport(
        headline = "This trial is not free",
        termsDetail = "This app will automatically deduct ₹899 every month starting in 3 days.",
        riskScore = 94,
        isDeceptive = true
    ),
    onDontPayClicked: () -> Unit = {},
    onDismissClicked: () -> Unit = {}
) {
    var selectedLanguage by remember { mutableStateOf(AlertLanguage.ENGLISH) }

    val headlineText = when (selectedLanguage) {
        AlertLanguage.ENGLISH -> riskReport.headline.ifBlank { "This trial is not free" }
        AlertLanguage.TELUGU -> "ఈ ఉచిత ట్రయల్ పూర్తిగా ఉచితం కాదు"
        AlertLanguage.HINDI -> "यह ट्रायल मुफ़्त नहीं है"
    }

    val bodyText = when (selectedLanguage) {
        AlertLanguage.ENGLISH -> riskReport.termsDetail.ifBlank {
            "This app will automatically deduct ₹899 every month starting in 3 days."
        }
        AlertLanguage.TELUGU -> "ఈ యాప్ 3 రోజుల తర్వాత ప్రతి నెలా మీ ఖాతా నుండి ₹899 కట్ చేస్తుంది."
        AlertLanguage.HINDI -> "यह ऐप 3 दिनों के बाद हर महीने आपके बैंक से ₹899 काट लेगा।"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x33000000))
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = M3SurfaceWhite,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 52.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header row: Warning Badge + Risk Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = M3AlertCoralContainer,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚠ Warning",
                            color = M3AlertCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFamily,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "RISK ${riskReport.riskScore}%",
                        color = M3AlertCoral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = GoogleSansFamily,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedContent(
                    targetState = headlineText,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "headlineAnim"
                ) { text ->
                    Text(
                        text = text,
                        color = M3TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFamily
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedContent(
                    targetState = bodyText,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bodyAnim"
                ) { text ->
                    Text(
                        text = text,
                        color = M3TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = GoogleSansFamily,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Multilingual Language Chips
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = M3CanvasBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                    ) {
                        AlertChipM3(
                            label = "English",
                            isSelected = selectedLanguage == AlertLanguage.ENGLISH,
                            onClick = { selectedLanguage = AlertLanguage.ENGLISH },
                            modifier = Modifier.weight(1f)
                        )
                        AlertChipM3(
                            label = "తెలుగు",
                            isSelected = selectedLanguage == AlertLanguage.TELUGU,
                            onClick = { selectedLanguage = AlertLanguage.TELUGU },
                            modifier = Modifier.weight(1f)
                        )
                        AlertChipM3(
                            label = "हिंदी",
                            isSelected = selectedLanguage == AlertLanguage.HINDI,
                            onClick = { selectedLanguage = AlertLanguage.HINDI },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Card Actions: "Don't Pay" & "Dismiss"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = M3AlertCoral,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bouncyClickable { onDontPayClicked() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Don't Pay",
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
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .bouncyClickable { onDismissClicked() }
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

@Composable
private fun AlertChipM3(
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFamily
            )
        }
    }
}
