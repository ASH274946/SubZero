package com.subzero.engine

import android.content.Context
import android.provider.Telephony
import com.subzero.data.MandateDao
import com.subzero.data.MandateEntity
import com.subzero.services.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class SyncProgress(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val mandatesFound: Int = 0,
    val isCompleted: Boolean = false
)

class AutoPaySyncEngine(
    private val context: Context,
    private val mandateDao: MandateDao
) {
    private val _syncState = MutableStateFlow(SyncProgress())
    val syncState: StateFlow<SyncProgress> = _syncState.asStateFlow()

    suspend fun scanAndSync(): Int = withContext(Dispatchers.IO) {
        _syncState.value = SyncProgress(isScanning = true)

        val existingMandatesMap = mandateDao.getAllActiveMandatesSync().associateBy { it.umn }.toMutableMap()
        var updatedCount = 0
        var totalScanned = 0

        val cursor = try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.ADDRESS, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE),
                null,
                null,
                Telephony.Sms.Inbox.DATE + " ASC"
            )
        } catch (_: Exception) {
            null
        }

        cursor?.use {
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

            while (it.moveToNext()) {
                totalScanned++
                val sender = it.getString(addressIndex) ?: ""
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)

                val parsed = AutoPayParser.parse(sender, body, date) ?: continue

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
                    val amountChanged = existing.maxDebitAmount != parsed.amount && parsed.amount > 0.0
                    val updated = existing.copy(
                        previousAmount = if (amountChanged) existing.maxDebitAmount else existing.previousAmount,
                        maxDebitAmount = if (amountChanged) parsed.amount else existing.maxDebitAmount,
                        nextBillingTimestamp = parsed.nextBillingTimestamp ?: existing.nextBillingTimestamp,
                        lastSyncedTimestamp = System.currentTimeMillis()
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
                    val nextBilling = parsed.nextBillingTimestamp ?: (date + (30L * 24L * 60L * 60L * 1000L))
                    val newMandate = MandateEntity(
                        umn = parsed.umn,
                        merchantName = parsed.merchantName,
                        maxDebitAmount = parsed.amount,
                        billingFrequency = "Monthly",
                        creationTimestamp = date,
                        nextBillingTimestamp = nextBilling,
                        lastSyncedTimestamp = System.currentTimeMillis(),
                        sourceAppPackage = parsed.matchedAppPackage,
                        sourceAppName = parsed.matchedAppName,
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

                if (totalScanned % 50 == 0) {
                    _syncState.value = _syncState.value.copy(
                        scannedCount = totalScanned,
                        mandatesFound = existingMandatesMap.size
                    )
                }
            }
        }

        _syncState.value = SyncProgress(
            isScanning = false,
            scannedCount = totalScanned,
            mandatesFound = existingMandatesMap.size,
            isCompleted = true
        )

        updatedCount
    }
}
