package com.subzero.engine

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

data class ParsedMandateResult(
    val merchantName: String,
    val amount: Double,
    val umn: String,
    val isRevocation: Boolean,
    val nextBillingTimestamp: Long?,
    val matchedAppPackage: String,
    val matchedAppName: String
)

object AutoPayParser {

    private val bankingSenderPattern = Pattern.compile(
        "(?i).*(CENTBK|HDFC|SBI|ICICI|AXIS|KOTAK|NPCI|PHONEPE|PAYTM|YESB|PNB|BOB|IDFC|CANBNK|UNIONB|INDBNK|IOB|BOISMS|FEDBNK|RBL|INDUS|BANDHAN|CITI|HSBC|SCB|AUFIN|EQUITAS|UCO|MAHB).*"
    )

    private val revocationPattern = Pattern.compile(
        "(?i)(?:mandate|autopay|e-mandate).*?(?:revoked|cancelled|deleted|stopped|paused)|(?:revoked|cancelled|deleted|stopped|paused).*?(?:mandate|autopay)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 1: towards <Merchant> for ...
    private val merchantTowardsPattern = Pattern.compile(
        "(?i)towards\\s+([A-Za-z0-9\\s&.,_-]+?)(?:\\s+for|\\s+with|\\s+having|\\s+of|\\s+upto|\\s+rs|\\s+inr|\\s+₹|\\.)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 2: (mandate|autopay)... (created|registered|authorized|active|setup|approved) for <Merchant> ...
    private val merchantForPattern = Pattern.compile(
        "(?i)(?:mandate|autopay|e-mandate|recurring).*?(?:created|registered|authorized|active|setup|approved|for)\\s+(?:for|towards)?\\s*([A-Za-z0-9\\s&.,_-]+?)(?:\\s+with|\\s+having|\\s+for|\\s+of|\\s+upto|\\s+rs|\\s+inr|\\s+₹|\\.)",
        Pattern.CASE_INSENSITIVE
    )

    // Pattern 3: <Merchant> has sent you an AutoPay request
    private val merchantRequestPattern = Pattern.compile(
        "(?i)^([A-Za-z0-9\\s&.,_-]+?)\\s+has\\s+sent\\s+you\\s+an\\s+autopay\\s+request",
        Pattern.CASE_INSENSITIVE
    )

    // Amount: Rs139.00 or Rs. 199.00 or ₹499
    private val amountPattern = Pattern.compile(
        "(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Date in upcoming debit alert: e.g. "set 05-Apr-26"
    private val datePattern = Pattern.compile(
        "(?i)(?:set|date|on|by)\\s+([0-9]{1,2}-[A-Za-z]{3}-[0-9]{2,4})",
        Pattern.CASE_INSENSITIVE
    )

    fun isEligibleSms(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        val isBanking = bankingSenderPattern.matcher(sender).matches() || s.endsWith("-s") || s.endsWith("-t")
        val isMandateTopic = b.contains("mandate") || b.contains("autopay") || b.contains("e-mandate")
        return isMandateTopic || (isBanking && (b.contains("recurring") || b.contains("standing instruction")))
    }

    fun parse(sender: String, body: String, date: Long): ParsedMandateResult? {
        if (!isEligibleSms(sender, body)) return null

        val isRevocation = revocationPattern.matcher(body).find()

        // Merchant extraction
        var merchantRaw: String? = null
        val towardsMatcher = merchantTowardsPattern.matcher(body)
        if (towardsMatcher.find()) {
            merchantRaw = towardsMatcher.group(1)
        } else {
            val forMatcher = merchantForPattern.matcher(body)
            if (forMatcher.find()) {
                merchantRaw = forMatcher.group(1)
            } else {
                val reqMatcher = merchantRequestPattern.matcher(body)
                if (reqMatcher.find()) {
                    merchantRaw = reqMatcher.group(1)
                }
            }
        }

        // Amount extraction
        val amtMatcher = amountPattern.matcher(body)
        val amount = if (amtMatcher.find()) {
            amtMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        if (merchantRaw.isNullOrBlank()) {
            if (isRevocation && amount > 0.0) {
                // If merchant is blank in revocation (e.g. "revoked by for Rs15000"), return special revocation marker
                return ParsedMandateResult(
                    merchantName = "Unknown",
                    amount = amount,
                    umn = "REVOKE-AMT-${amount.toInt()}",
                    isRevocation = true,
                    nextBillingTimestamp = null,
                    matchedAppPackage = "com.phonepe.app",
                    matchedAppName = "PhonePe"
                )
            }
            return null
        }

        val cleanMerchant = cleanMerchantName(merchantRaw)

        // Generate deterministic UMN per merchant so that creation and upcoming alerts for the same service reconcile cleanly
        val normalizedMerchantKey = cleanMerchant.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val umn = "UMN-$normalizedMerchantKey"

        // Next billing date extraction
        val dateMatcher = datePattern.matcher(body)
        var nextBilling: Long? = null
        if (dateMatcher.find()) {
            val dateStr = dateMatcher.group(1)
            nextBilling = parseDateString(dateStr)
        }

        // App attribution
        val (appPkg, appName) = resolveUpiAppSource(body, sender)

        return ParsedMandateResult(
            merchantName = cleanMerchant,
            amount = amount,
            umn = umn,
            isRevocation = isRevocation,
            nextBillingTimestamp = nextBilling,
            matchedAppPackage = appPkg,
            matchedAppName = appName
        )
    }

    fun cleanMerchantName(raw: String): String {
        val base = raw.split(Regex("(?i)\\s+(via|using|on|towards|for|ref|upto|rs|inr|with)\\s+"))[0]
            .replace(Regex("[^A-Za-z0-9\\s&]"), "")
            .trim()
            .take(24)
            .ifBlank { "Service Provider" }

        // Format into Title Case
        return base.lowercase().split(Regex("\\s+")).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
        }
    }

    fun parseDateString(raw: String?): Long? {
        if (raw == null) return null
        return try {
            val parts = raw.split("-")
            val format = if (parts.lastOrNull()?.length == 4) {
                SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            } else {
                SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH)
            }
            val parsed = format.parse(raw)?.time ?: return null
            val now = System.currentTimeMillis()
            if (parsed > now) {
                parsed
            } else {
                // If past date, advance in 30-day recurring intervals until in future
                val diff = now - parsed
                val cycles = (diff / (30L * 24L * 60L * 60L * 1000L)) + 1
                parsed + (cycles * 30L * 24L * 60L * 60L * 1000L)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun resolveUpiAppSource(body: String, sender: String): Pair<String, String> {
        val lower = "$body $sender".lowercase()
        return when {
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
            lower.contains("cred") -> {
                "com.dreamplug.androidapp" to "CRED"
            }
            else -> {
                // Default to PhonePe as primary installed UPI ecosystem
                "com.phonepe.app" to "PhonePe"
            }
        }
    }
}
