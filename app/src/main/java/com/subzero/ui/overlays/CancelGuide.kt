package com.subzero.ui.overlays

import android.content.Context
import com.subzero.data.MandateEntity

/**
 * DEPRECATED: Floating WindowManager overlay is completely disabled per UX requirements.
 * Revocation guidance is handled exclusively by the in-app CancelGuideSheet.
 */
object CancelGuide {
    fun show(context: Context, mandate: MandateEntity) {
        // No-op: WindowManager overlay is completely removed to prevent obscuring target apps.
    }

    fun dismiss(context: Context) {
        // No-op
    }

    fun getRevocationStepsForApp(appName: String, merchantName: String): String {
        return when (appName.lowercase()) {
            "phonepe" -> {
                "1. Tap your Profile Photo at top-left.\n" +
                "2. Under 'Payment Management', tap 'AutoPay'.\n" +
                "3. Select '$merchantName' and tap 'Pause' or 'Remove AutoPay'."
            }
            "google pay", "gpay" -> {
                "1. Tap your Profile icon at top-right.\n" +
                "2. Tap 'Autopay' under Payments.\n" +
                "3. Select '$merchantName' and tap 'Cancel Autopay'."
            }
            "paytm" -> {
                "1. Tap Profile icon at top-left.\n" +
                "2. Tap 'UPI & Payment Settings' -> 'Automatic Payments'.\n" +
                "3. Find '$merchantName' and tap 'Cancel'."
            }
            else -> {
                "1. Open your UPI app and tap Profile/Settings.\n" +
                "2. Select 'AutoPay' or 'Mandates'.\n" +
                "3. Find '$merchantName' and tap 'Cancel'."
            }
        }
    }

    fun getRevocationStepList(appName: String, merchantName: String): List<String> {
        return when (appName.lowercase()) {
            "phonepe" -> listOf(
                "Tap your Profile Photo at top-left.",
                "Under 'Payment Management', tap 'AutoPay'.",
                "Select '$merchantName' and tap 'Pause' or 'Remove AutoPay'."
            )
            "google pay", "gpay" -> listOf(
                "Tap your Profile icon at top-right.",
                "Tap 'Autopay' under Payments.",
                "Select '$merchantName' and tap 'Cancel Autopay'."
            )
            "paytm" -> listOf(
                "Tap Profile icon at top-left.",
                "Tap 'UPI & Payment Settings' -> 'Automatic Payments'.",
                "Find '$merchantName' and tap 'Cancel'."
            )
            else -> listOf(
                "Open $appName and tap Profile/Settings.",
                "Select 'AutoPay Settings' or 'Mandates'.",
                "Locate '$merchantName' and tap 'Pause' or 'Cancel'."
            )
        }
    }
}
