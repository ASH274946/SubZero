package com.subzero.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import com.subzero.data.MandateEntity
import com.subzero.services.PaywallAccessibilityService

object UpiRedirectManager {

    /**
     * Checks if the SubZero PaywallAccessibilityService is active and enabled in Android Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${PaywallAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val component = colonSplitter.next()
            if (component.equals(expectedServiceName, ignoreCase = true) ||
                component.contains("PaywallAccessibilityService", ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Safely opens the target UPI app using a clean launcher intent,
     * triggers accessibility-assisted navigation directly to the AutoPay screen,
     * and ensures zero floating WindowManager overlays.
     */
    fun openUpiAppForRevoke(context: Context, mandate: MandateEntity) {
        // 1. Copy UMN to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AutoPay UMN", mandate.umn)
        clipboard.setPrimaryClip(clip)

        val targetPackage = mandate.sourceAppPackage.trim()
        val packageManager = context.packageManager

        // 2. Check if accessibility service is enabled to assist navigation
        val isServiceEnabled = isAccessibilityServiceEnabled(context)
        if (!isServiceEnabled) {
            Toast.makeText(
                context,
                "Please enable SubZero in Accessibility Settings to automatically open AutoPay.",
                Toast.LENGTH_LONG
            ).show()
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
            return
        }

        Toast.makeText(
            context,
            "Opening AutoPay in ${mandate.sourceAppName}...",
            Toast.LENGTH_SHORT
        ).show()

        // 3. Instruct Accessibility Service to navigate to AutoPay upon app launch
        if (targetPackage.isNotBlank() && targetPackage != "generic") {
            AutoPayNavigator.startNavigation(targetPackage)
        }

        // 4. Launch the target UPI application using its native clean launch intent
        // (Avoids invalid URI schemes that trigger "Invalid link" in Google Pay or crash dialogs in PhonePe)
        val launchIntent = if (targetPackage.isNotBlank() && targetPackage != "generic") {
            packageManager.getLaunchIntentForPackage(targetPackage)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
        } else null

        if (launchIntent != null) {
            try {
                context.startActivity(launchIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open ${mandate.sourceAppName}", Toast.LENGTH_SHORT).show()
                AutoPayNavigator.clear()
            }
        } else {
            Toast.makeText(context, "${mandate.sourceAppName} is not installed.", Toast.LENGTH_SHORT).show()
            AutoPayNavigator.clear()
        }

        // NOTE: CancelGuide.show() is intentionally omitted. ZERO floating overlays will be created.
    }

    /**
     * Backwards-compatible alias for existing callers.
     */
    fun redirectToMandateRevoke(context: Context, mandate: MandateEntity) {
        openUpiAppForRevoke(context, mandate)
    }
}
