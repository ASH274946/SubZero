package com.subzero.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.data.MandateEntity
import com.subzero.engine.UpiRedirectManager
import com.subzero.ui.components.UpiAppIcon
import com.subzero.ui.components.bouncyClickable
import com.subzero.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelGuideSheet(
    mandate: MandateEntity,
    onDismiss: () -> Unit,
    onMarkAsRevoked: (String) -> Unit = {}
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = M3SurfaceWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = M3CanvasBackground,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row: Merchant Name & App Origin Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cancel ${mandate.merchantName} Subscription",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = M3TextPrimary,
                        fontFamily = GoogleSansFamily
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Active in ${mandate.sourceAppName} • ₹${mandate.maxDebitAmount.toInt()}/mo",
                        fontSize = 13.sp,
                        color = M3TextSecondary,
                        fontFamily = GoogleSansFamily
                    )
                }

                // Origin App Pill
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = M3PineContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        UpiAppIcon(
                            packageName = mandate.sourceAppPackage,
                            appName = mandate.sourceAppName,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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

            Spacer(modifier = Modifier.height(14.dp))

            // UMN Chip (Tap to Copy)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = M3CanvasBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AutoPay UMN", mandate.umn)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "UMN copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "UNIQUE MANDATE NUMBER (UMN)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3TextSecondary,
                            fontFamily = GoogleSansFamily
                        )
                        Text(
                            text = mandate.umn,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = M3TextPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy UMN",
                        tint = M3PinePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Step-by-Step Revocation Guide
            Text(
                text = "Revocation Steps in ${mandate.sourceAppName}:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = M3TextPrimary,
                fontFamily = GoogleSansFamily
            )

            Spacer(modifier = Modifier.height(10.dp))

            val steps = remember(mandate.sourceAppName, mandate.merchantName) {
                com.subzero.ui.overlays.CancelGuide.getRevocationStepList(
                    mandate.sourceAppName,
                    mandate.merchantName
                )
            }

            for ((index, stepText) in steps.withIndex()) {
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                StepGuideItem(
                    stepNumber = "${index + 1}",
                    text = stepText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. PRIMARY ACTION BUTTON: "Open [AppName] & Cancel Subscription"
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = M3PinePrimary,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bouncyClickable {
                        onDismiss()
                        UpiRedirectManager.openUpiAppForRevoke(context, mandate)
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Launch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open ${mandate.sourceAppName} & Cancel Subscription",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = GoogleSansFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SECONDARY ACTION BUTTON: "Done"
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = M3CanvasBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .bouncyClickable {
                        onDismiss()
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3TextPrimary,
                        fontFamily = GoogleSansFamily
                    )
                }
            }
        }
    }
}

@Composable
fun StepGuideItem(
    stepNumber: String,
    text: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = M3PineContainer,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = M3PinePrimary,
                    fontFamily = GoogleSansFamily
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = M3TextSecondary,
            fontFamily = GoogleSansFamily,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
