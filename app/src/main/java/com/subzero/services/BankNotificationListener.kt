package com.subzero.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.subzero.SubZeroApp
import com.subzero.data.MandateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Local UPI AutoPay / e-Mandate Notification Listener.
 *
 * Implements:
 * 1. Offline extraction of mandate registrations from bank and UPI notifications.
 * 2. Strict input validation and normalization.
 * 3. Local Room persistence without saving raw sensitive notification messages.
 */
class BankNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        // Compiled regex patterns for mandate parsing
        val MANDATE_KEYWORD_REGEX = Regex(
            "(?i)\\b(mandate|autopay|e-mandate|standing instruction|auto-debit|recurring)\\b"
        )

        val MERCHANT_REGEX = Regex(
            "(?i)mandate.*?(?:created|registered|scheduled|approved).*?for\\s+([A-Za-z0-9\\s&.-]+?)(?=(?:\\s+(?:of|amount|umn|via|on|ref)|[.,;]|$))"
        )

        val AMOUNT_REGEX = Regex(
            "(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"
        )

        val UMN_REGEX = Regex(
            "(?i)umn[:\\s]+([A-Za-z0-9]{12,35})"
        )

        val FREQUENCY_REGEX = Regex(
            "(?i)\\b(monthly|quarterly|half-yearly|annual|yearly|weekly|daily)\\b"
        )

        val CANCELLATION_KEYWORD_REGEX = Regex(
            "(?i)\\b(mandate|autopay|e-mandate).*?\\b(cancelled|revoked|deleted|deactivated|stopped|terminated|paused)\\b|\\b(cancelled|revoked|deleted|deactivated|stopped|terminated|paused)\\b.*?\\b(mandate|autopay|e-mandate)\\b"
        )

        /**
         * Helper parser method exposed for both notification listening and manual debug injection.
         */
        fun parseMandateFromText(text: String, timestamp: Long = System.currentTimeMillis()): MandateEntity? {
            if (!MANDATE_KEYWORD_REGEX.containsMatchIn(text)) {
                return null
            }

            // Extract Amount
            val amountMatch = AMOUNT_REGEX.find(text) ?: return null
            val rawAmount = amountMatch.groupValues[1].replace(",", "")
            val maxDebitAmount = rawAmount.toDoubleOrNull() ?: return null
            if (maxDebitAmount <= 0.0) return null

            // Extract UMN
            val umnMatch = UMN_REGEX.find(text)
            val umn = umnMatch?.groupValues?.get(1)?.trim() ?: run {
                // If UMN is not in standard format, generate a deterministic synthetic hash ID based on merchant + amount + time
                val fallbackId = (text.hashCode().toString() + timestamp.toString()).takeLast(16)
                "SZSYNTH$fallbackId"
            }
            if (umn.length < 8) return null

            // Extract Merchant
            val merchantMatch = MERCHANT_REGEX.find(text)
            var merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: ""
            if (merchant.isBlank()) {
                // Try secondary extraction pattern (e.g. "at <Merchant>")
                val secondaryMerchant = Regex("(?i)\\b(?:to|at|for)\\s+([A-Za-z0-9\\s&.-]{3,30})").find(text)
                merchant = secondaryMerchant?.groupValues?.get(1)?.trim() ?: "Unknown Merchant"
            }
            // Normalize merchant name
            merchant = merchant.replace(Regex("\\s+"), " ")
                .replace(Regex("(?i)\\b(ltd|pvt|inc|corp|services|india)\\b"), "")
                .trim()
                .ifBlank { "AutoPay Merchant" }

            // Extract Frequency
            val freqMatch = FREQUENCY_REGEX.find(text)
            val billingFrequency = freqMatch?.value?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Monthly"

            // Resolve UPI app / Bank Source
            val lower = text.lowercase()
            val (sourcePackage, sourceName) = when {
                lower.contains("phonepe") || lower.contains("@ybl") || lower.contains("@axl") || lower.contains("@ibl") -> {
                    "com.phonepe.app" to "PhonePe"
                }
                lower.contains("google pay") || lower.contains("gpay") || lower.contains("@okhdfc") || lower.contains("@okaxis") || lower.contains("@oksbi") || lower.contains("@okicici") -> {
                    "com.google.android.apps.nbu.paisa.user" to "Google Pay"
                }
                lower.contains("paytm") || lower.contains("@paytm") -> {
                    "net.one97.paytm" to "Paytm"
                }
                lower.contains("bhim") || lower.contains("@upi") -> {
                    "in.org.npci.upiapp" to "BHIM UPI"
                }
                else -> {
                    "generic" to "Bank Direct"
                }
            }

            return MandateEntity(
                umn = umn,
                merchantName = merchant,
                maxDebitAmount = maxDebitAmount,
                billingFrequency = billingFrequency,
                creationTimestamp = timestamp,
                nextBillingTimestamp = timestamp + (30L * 24L * 60L * 60L * 1000L),
                sourceAppPackage = sourcePackage,
                sourceAppName = sourceName,
                isRevoked = false
            )
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combinedContent = "$title $text $bigText".trim()
        if (combinedContent.isBlank()) return

        // 1. Check for cancellation/revocation alerts first
        if (CANCELLATION_KEYWORD_REGEX.containsMatchIn(combinedContent)) {
            val umnMatch = UMN_REGEX.find(combinedContent)
            val umn = umnMatch?.groupValues?.get(1)?.trim()
            val merchantMatch = MERCHANT_REGEX.find(combinedContent)
            val merchant = merchantMatch?.groupValues?.get(1)?.trim()

            serviceScope.launch {
                try {
                    UniversalRevocationManager.onRegulatoryCancellationDetected(
                        applicationContext,
                        umn,
                        merchant
                    )
                } catch (_: Exception) {
                }
            }
            return
        }

        // 2. Check for mandate creation alerts
        val mandate = parseMandateFromText(combinedContent, sbn.postTime) ?: return

        serviceScope.launch {
            try {
                SubZeroApp.instance.mandateRepository.insertMandate(mandate)
                // Schedule 36-hour pre-debit alarm
                ReminderScheduler.scheduleReminder(applicationContext, mandate)
            } catch (_: Exception) {
                // Fail-safe error handling
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
