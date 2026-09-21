package com.subzero.engine

import android.content.Context
import android.provider.Telephony
import com.subzero.data.MandateDao
import com.subzero.data.MandateEntity
import com.subzero.services.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans, extracts, and reconciles AutoPay mandates scoped specifically to a selected UPI application.
 */
class AppScopedSyncEngine(
    private val context: Context,
    private val mandateDao: MandateDao
) {

    suspend fun syncAppMandates(app: InstalledUpiApp): Int = withContext(Dispatchers.IO) {
        val existingMandatesMap = mandateDao.getAllActiveMandatesSync().associateBy { it.umn }.toMutableMap()
        var updatedCount = 0

        // Prune any stale demo dummy records (like DocuScan Pro) so user only sees real subscriptions
        existingMandatesMap.values.filter {
            it.merchantName.contains("DocuScan", ignoreCase = true) || it.umn.startsWith("HDFC092384")
        }.forEach { dummy ->
            mandateDao.deleteMandate(dummy.umn)
            existingMandatesMap.remove(dummy.umn)
        }

        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                null,
                null,
                Telephony.Sms.Inbox.DATE + " ASC" // Chronological order: creates first, then updates/revocations
            )
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

            while (it.moveToNext()) {
                val sender = it.getString(addressIdx) ?: ""
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)

                val parsed = AutoPayParser.parse(sender, body, date) ?: continue

                // Check if this parsed mandate should be attributed to the selected app:
                // 1. Matches this app package explicitly
                // 2. Or is a bank UPI mandate and the selected app is PhonePe / primary UPI handler
                val isMatchingApp = parsed.matchedAppPackage == app.packageName ||
                        (app.packageName == "com.phonepe.app" && (parsed.matchedAppPackage == "com.phonepe.app" || parsed.matchedAppPackage == "generic"))

                if (!isMatchingApp) continue

                // Check for Revocations
                if (parsed.isRevocation) {
                    val toRevoke = existingMandatesMap.values.firstOrNull { existing ->
                        existing.umn == parsed.umn ||
                                existing.merchantName.equals(parsed.merchantName, ignoreCase = true) ||
                                (parsed.amount > 0.0 && existing.maxDebitAmount == parsed.amount)
                    }
                    if (toRevoke != null) {
                        mandateDao.markAsRevoked(toRevoke.umn)
                        existingMandatesMap.remove(toRevoke.umn)
                        updatedCount++
                    }
                    continue
                }

                val existing = existingMandatesMap[parsed.umn] ?: existingMandatesMap.values.firstOrNull {
                    it.merchantName.equals(parsed.merchantName, ignoreCase = true)
                }

                if (existing != null) {
                    // Delta reconciliation: price change detection & billing date updates
                    val amountChanged = existing.maxDebitAmount != parsed.amount && parsed.amount > 0.0
                    val updated = existing.copy(
                        previousAmount = if (amountChanged) existing.maxDebitAmount else existing.previousAmount,
                        maxDebitAmount = if (amountChanged) parsed.amount else existing.maxDebitAmount,
                        nextBillingTimestamp = parsed.nextBillingTimestamp ?: existing.nextBillingTimestamp,
                        lastSyncedTimestamp = System.currentTimeMillis(),
                        sourceAppPackage = app.packageName,
                        sourceAppName = app.appName,
                        isRevoked = false
                    )
                    mandateDao.insertOrUpdate(updated)
                    existingMandatesMap[updated.umn] = updated
                    updatedCount++

                    ReminderScheduler.schedule48HourPreDebitReminder(
                        context,
                        updated.umn,
                        updated.merchantName,
                        updated.maxDebitAmount,
                        updated.nextBillingTimestamp
                    )
                } else {
                    // New AutoPay Mandate
                    val nextBilling = parsed.nextBillingTimestamp ?: (date + (30L * 24L * 60L * 60L * 1000L))
                    val newMandate = MandateEntity(
                        umn = parsed.umn,
                        merchantName = parsed.merchantName,
                        maxDebitAmount = parsed.amount,
                        billingFrequency = "Monthly",
                        creationTimestamp = date,
                        nextBillingTimestamp = nextBilling,
                        lastSyncedTimestamp = System.currentTimeMillis(),
                        sourceAppPackage = app.packageName,
                        sourceAppName = app.appName,
                        isRevoked = false
                    )
                    mandateDao.insertOrUpdate(newMandate)
                    existingMandatesMap[newMandate.umn] = newMandate
                    updatedCount++

                    ReminderScheduler.schedule48HourPreDebitReminder(
                        context,
                        newMandate.umn,
                        newMandate.merchantName,
                        newMandate.maxDebitAmount,
                        newMandate.nextBillingTimestamp
                    )
                }
            }
        }

        updatedCount
    }
}
