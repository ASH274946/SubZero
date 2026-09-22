package com.subzero.services

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.subzero.data.BlockedTrapEntity
import com.subzero.data.SubZeroDatabase
import com.subzero.engine.AutoPayNavigator
import com.subzero.engine.PaywallNotificationManager
import kotlinx.coroutines.*
import java.util.regex.Pattern

class PaywallAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private val packageCooldownMap = mutableMapOf<String, Long>()

    // APPS THAT MUST NEVER BE SCANNED (Exclusion Whitelist / Immunity List)
    private val packageExclusionList = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.google.android.youtube",
        "com.instagram.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.facebook.katana",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.android.systemui",
        "com.google.android.apps.nexuslauncher",
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        "com.dreamplug.androidapp"
    )

    // Strict multi-signal patterns
    private val trialSignal = Pattern.compile("(?i)\\b(free trial|start trial|try for free|trial period|3-day|7-day)\\b")
    private val currencySignal = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+)")
    private val cadenceSignal = Pattern.compile("(?i)\\b(/month|/yr|per month|per year|billed annually|auto-renews?|recurring)\\b")
    private val paymentExclusionSignal = Pattern.compile("(?i)\\b(scan qr|enter upi pin|paying to|send money|recharge)\\b")

    companion object {
        val STAGE_1_GATE_REGEX: Regex = Regex(
            "(?i)(?:\\b(trial|renews|autopay|mandate|billed annually|per month|e-mandate)\\b|/mo\\b|/yr\\b)"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AutoPayNavigator.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Route AutoPay navigation if active
        if (AutoPayNavigator.pendingPackage == packageName) {
            val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return
            AutoPayNavigator.service = this
            AutoPayNavigator.onAccessibilityEvent(root, packageName)
            return
        }

        // 2. Immediate Exclusion Check
        if (shouldSkipPackage(packageName)) return

        // 3. Debounce inspection (600ms) to ensure screen is statically rendered
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            delay(600)
            inspectActiveWindow(packageName)
        }
    }

    private fun shouldSkipPackage(packageName: String): Boolean {
        if (packageName == applicationContext.packageName) return true
        if (packageExclusionList.contains(packageName)) return true
        if (packageName.contains("launcher") || packageName.contains("browser") || packageName.contains("camera")) return true

        // Cooldown check: 10 minutes between alerts for the same app
        val lastAlertTime = packageCooldownMap[packageName] ?: 0L
        if (SystemClock.elapsedRealtime() - lastAlertTime < 600_000L) {
            return true
        }

        return false
    }

    private suspend fun inspectActiveWindow(packageName: String) = withContext(Dispatchers.Default) {
        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return@withContext

        val extractedTextBuilder = StringBuilder()
        var hasClickableCTA = false

        extractTextAndEvaluate(rootNode, extractedTextBuilder) { isClickable ->
            if (isClickable) hasClickableCTA = true
        }

        val fullText = extractedTextBuilder.toString()
        if (fullText.isBlank()) return@withContext

        // Safeguard: Never trigger on regular UPI checkouts or QR payments
        if (paymentExclusionSignal.matcher(fullText).find()) return@withContext

        // MULTI-SIGNAL VALIDATION: All 4 signals must pass simultaneously
        val hasTrial = trialSignal.matcher(fullText).find()
        val currencyMatcher = currencySignal.matcher(fullText)
        val hasCurrency = currencyMatcher.find()
        val hasCadence = cadenceSignal.matcher(fullText).find()

        if (hasTrial && hasCurrency && hasCadence && hasClickableCTA) {
            val detectedAmount = currencyMatcher.group(0) ?: "₹899"
            val appLabel = getAppNameFromPackage(packageName)

            // Trigger genuine Android system notification
            withContext(Dispatchers.Main) {
                PaywallNotificationManager.postDarkPatternAlert(
                    context = applicationContext,
                    appName = appLabel,
                    amount = "$detectedAmount/mo",
                    renewalDetails = "Charges apply automatically after the trial period expires."
                )
            }

            // Register in Cooldown and Database
            packageCooldownMap[packageName] = SystemClock.elapsedRealtime()

            val database = SubZeroDatabase.getInstance(applicationContext)
            database.mandateDao().recordBlockedTrap(
                BlockedTrapEntity(
                    targetAppPackage = packageName,
                    detectedHeadline = "Trial Trap ($detectedAmount/mo)"
                )
            )
        }
    }

    private fun extractTextAndEvaluate(
        node: AccessibilityNodeInfo,
        builder: StringBuilder,
        onButtonFound: (Boolean) -> Unit
    ) {
        if (!node.isVisibleToUser) return
        if (node.isPassword) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if (!text.isNullOrBlank()) builder.append(text).append(" ")
        if (!desc.isNullOrBlank()) builder.append(desc).append(" ")

        if (node.isClickable && (!text.isNullOrBlank() || !desc.isNullOrBlank())) {
            val content = "${text ?: ""} ${desc ?: ""}".lowercase()
            if (content.contains("start") || content.contains("continue") || content.contains("try") || content.contains("unlock")) {
                onButtonFound(true)
            }
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            extractTextAndEvaluate(child, builder, onButtonFound)
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            "Subscription App"
        }
    }

    override fun onInterrupt() {
        scanJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AutoPayNavigator.service === this) {
            AutoPayNavigator.service = null
        }
        scanJob?.cancel()
        serviceScope.cancel()
    }
}
