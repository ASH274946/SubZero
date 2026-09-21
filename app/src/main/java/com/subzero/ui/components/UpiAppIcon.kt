package com.subzero.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subzero.R
import com.subzero.ui.theme.GoogleSansFamily
import com.subzero.ui.theme.M3PineContainer
import com.subzero.ui.theme.M3PinePrimary

/**
 * Renders the authentic native application icon of installed UPI apps via PackageManager.
 * If the app is not installed on the current device (or running in an emulator),
 * gracefully falls back to high-resolution brand vector logos.
 */
@Composable
fun UpiAppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier.size(36.dp)
) {
    val context = LocalContext.current

    // 1. Try to load native app icon from PackageManager
    val nativeIconBitmap: ImageBitmap? = remember(packageName) {
        if (packageName.isBlank() || packageName == "generic") {
            null
        } else {
            try {
                val pm = context.packageManager
                val drawable = pm.getApplicationIcon(packageName)
                val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    // 2. High-resolution vector fallback resource if app is not installed or pm fails
    val fallbackResId = remember(packageName, appName) {
        when {
            packageName.contains("phonepe", ignoreCase = true) || appName.contains("PhonePe", ignoreCase = true) -> R.drawable.ic_upi_phonepe
            packageName.contains("paisa", ignoreCase = true) || packageName.contains("google", ignoreCase = true) || appName.contains("Google", ignoreCase = true) || appName.contains("GPay", ignoreCase = true) -> R.drawable.ic_upi_gpay
            packageName.contains("paytm", ignoreCase = true) || appName.contains("Paytm", ignoreCase = true) -> R.drawable.ic_upi_paytm
            packageName.contains("npci", ignoreCase = true) || appName.contains("BHIM", ignoreCase = true) -> R.drawable.ic_upi_bhim
            packageName.contains("dreamplug", ignoreCase = true) || appName.contains("CRED", ignoreCase = true) -> R.drawable.ic_upi_cred
            else -> null
        }
    }

    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
    ) {
        if (nativeIconBitmap != null) {
            Image(
                bitmap = nativeIconBitmap,
                contentDescription = "$appName icon",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (fallbackResId != null) {
            Image(
                painter = painterResource(id = fallbackResId),
                contentDescription = "$appName icon",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Absolute fallback only if completely unknown app
            Surface(
                shape = CircleShape,
                color = M3PineContainer,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = appName.take(1).uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = M3PinePrimary,
                        fontFamily = GoogleSansFamily
                    )
                }
            }
        }
    }
}
