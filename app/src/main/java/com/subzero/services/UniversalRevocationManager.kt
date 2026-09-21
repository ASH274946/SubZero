package com.subzero.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.subzero.SubZeroApp
import com.subzero.data.MandateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Universal Revocation Queue Manager ("Kill Switch").
 *
 * Implements:
 * 1. Queue lifecycle coordination across unrevoked mandates.
 * 2. Deep-link orchestration targeting PhonePe, Google Pay, and Paytm.
 * 3. Reactive auto-advancement when regulatory cancellation alerts/SMS arrive.
 * 4. Cancellation confirmation & Room database synchronization.
 */
object UniversalRevocationManager {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _queueItems = MutableStateFlow<List<MandateEntity>>(emptyList())
    val queueItems: StateFlow<List<MandateEntity>> = _queueItems.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isQueueActive = MutableStateFlow(false)
    val isQueueActive: StateFlow<Boolean> = _isQueueActive.asStateFlow()

    private val _lastAutoAdvancedUmn = MutableStateFlow<String?>(null)
    val lastAutoAdvancedUmn: StateFlow<String?> = _lastAutoAdvancedUmn.asStateFlow()

    // Package identifiers for popular Indian UPI apps
    const val PKG_PHONEPE = "com.phonepe.app"
    const val PKG_GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PKG_PAYTM = "net.one97.paytm"

    /**
     * Initializes and starts the sequential revocation queue with the provided mandates.
     */
    fun startQueue(mandates: List<MandateEntity>, targetUmn: String? = null) {
        val activeOnly = mandates.filter { !it.isRevoked }
        _queueItems.value = activeOnly
        val startIndex = if (targetUmn != null) {
            val idx = activeOnly.indexOfFirst { it.umn.equals(targetUmn, ignoreCase = true) }
            if (idx >= 0) idx else 0
        } else {
            0
        }
        _currentIndex.value = startIndex
        _isQueueActive.value = activeOnly.isNotEmpty()
        _lastAutoAdvancedUmn.value = null
    }

    /**
     * Advances to the next mandate in the queue.
     */
    fun advanceQueue() {
        val items = _queueItems.value
        val next = _currentIndex.value + 1
        if (next < items.size) {
            _currentIndex.value = next
        } else {
            _isQueueActive.value = false
        }
    }

    /**
     * Marks the current mandate as revoked and advances to the next mandate.
     */
    fun revokeCurrentAndAdvance(context: Context) {
        val items = _queueItems.value
        val idx = _currentIndex.value
        if (idx in items.indices) {
            val mandate = items[idx]
            CoroutineScope(Dispatchers.IO).launch {
                SubZeroApp.instance.mandateRepository.markAsRevoked(mandate.umn)
                ReminderScheduler.cancelReminder(context, mandate.umn)
            }
            advanceQueue()
        }
    }

    /**
     * Invoked when a regulatory SMS or notification reports a mandate cancellation.
     * Automatically updates the Room database and advances the queue.
     */
    fun onRegulatoryCancellationDetected(context: Context, umn: String?, merchant: String?) {
        val items = _queueItems.value
        if (items.isEmpty()) return

        val matched = items.firstOrNull { mandate ->
            (!umn.isNullOrBlank() && mandate.umn.equals(umn, ignoreCase = true)) ||
                    (!merchant.isNullOrBlank() && mandate.merchantName.contains(merchant, ignoreCase = true))
        } ?: return

        CoroutineScope(Dispatchers.IO).launch {
            SubZeroApp.instance.mandateRepository.markAsRevoked(matched.umn)
            ReminderScheduler.cancelReminder(context, matched.umn)
        }

        scope.launch {
            _lastAutoAdvancedUmn.value = matched.umn
            val current = getCurrentMandate()
            if (current?.umn == matched.umn) {
                advanceQueue()
            }
        }
    }

    /**
     * Returns the currently active mandate in the revocation queue, or null if complete.
     */
    fun getCurrentMandate(): MandateEntity? {
        val items = _queueItems.value
        val idx = _currentIndex.value
        return if (idx in items.indices) items[idx] else null
    }

    /**
     * Deep-links directly to PhonePe AutoPay / UPI settings or main app.
     */
    fun openPhonePe(context: Context): Boolean {
        return launchPackageOrUpi(context, PKG_PHONEPE)
    }

    /**
     * Deep-links directly to Google Pay AutoPay / UPI settings or main app.
     */
    fun openGooglePay(context: Context): Boolean {
        return launchPackageOrUpi(context, PKG_GPAY)
    }

    /**
     * Deep-links directly to Paytm AutoPay / UPI settings or main app.
     */
    fun openPaytm(context: Context): Boolean {
        return launchPackageOrUpi(context, PKG_PAYTM)
    }

    /**
     * Launches the installed UPI application or falls back to generic UPI intent chooser.
     */
    private fun launchPackageOrUpi(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }

        // Fallback: Generic UPI Chooser
        val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("upi://pay")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(Intent.createChooser(fallbackIntent, "Open UPI Application"))
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ends or clears the current revocation queue.
     */
    fun clearQueue() {
        _queueItems.value = emptyList()
        _currentIndex.value = 0
        _isQueueActive.value = false
        _lastAutoAdvancedUmn.value = null
    }
}
