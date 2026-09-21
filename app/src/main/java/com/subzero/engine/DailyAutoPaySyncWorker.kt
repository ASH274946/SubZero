package com.subzero.engine

import android.content.Context
import android.provider.Telephony
import androidx.work.*
import com.subzero.data.MandateDao
import com.subzero.data.MandateEntity
import com.subzero.data.SubZeroDatabase
import com.subzero.services.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyAutoPaySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val database = SubZeroDatabase.getInstance(context)
        val mandateDao = database.mandateDao()

        val existingMandatesMap = mandateDao.getAllActiveMandatesSync().associateBy { it.umn }.toMutableMap()
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
        } ?: return@withContext Result.success()

        cursor.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

            while (it.moveToNext()) {
                val sender = it.getString(addressIdx) ?: ""
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)

                val parsed = AutoPayParser.parse(sender, body, date) ?: continue

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
                    }
                    continue
                }

                val existing = existingMandatesMap[parsed.umn] ?: existingMandatesMap.values.firstOrNull {
                    it.merchantName.equals(parsed.merchantName, ignoreCase = true)
                }

                if (existing != null) {
                    // Price Change Detection & Billing Updates
                    val amountChanged = existing.maxDebitAmount != parsed.amount && parsed.amount > 0.0
                    val updated = existing.copy(
                        previousAmount = if (amountChanged) existing.maxDebitAmount else existing.previousAmount,
                        maxDebitAmount = if (amountChanged) parsed.amount else existing.maxDebitAmount,
                        nextBillingTimestamp = parsed.nextBillingTimestamp ?: existing.nextBillingTimestamp,
                        lastSyncedTimestamp = System.currentTimeMillis()
                    )
                    mandateDao.insertOrUpdate(updated)
                    existingMandatesMap[updated.umn] = updated

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

        Result.success()
    }

    companion object {
        private const val WORK_NAME = "SubZeroDailyAutoPaySync"

        fun scheduleDailySync(context: Context) {
            val currentDate = Calendar.getInstance()
            val targetDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 6)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(currentDate)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val initialDelayMillis = targetDate.timeInMillis - currentDate.timeInMillis

            val dailySyncRequest = PeriodicWorkRequestBuilder<DailyAutoPaySyncWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailySyncRequest
            )
        }
    }
}
