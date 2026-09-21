package com.subzero.engine

import android.content.Context
import com.subzero.data.BlockedTrapEntity
import com.subzero.data.MandateDao
import com.subzero.data.MandateEntity
import com.subzero.data.SubZeroDatabase
import com.subzero.services.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WalkthroughEngine {

    fun getDummyMandates(now: Long = System.currentTimeMillis()): List<MandateEntity> {
        return listOf(
            MandateEntity(
                umn = "HDFC092384019283",
                merchantName = "DocuScan Pro",
                maxDebitAmount = 899.0,
                previousAmount = 499.0, // Demonstrates detected price hike!
                billingFrequency = "Monthly",
                creationTimestamp = now - (28L * 24L * 60L * 60L * 1000L),
                nextBillingTimestamp = now + (1L * 24L * 60L * 60L * 1000L), // Renews tomorrow!
                lastSyncedTimestamp = now,
                sourceAppPackage = "com.phonepe.app",
                sourceAppName = "PhonePe",
                isRevoked = false
            ),
            MandateEntity(
                umn = "SBI772391029384",
                merchantName = "StreamVibe HD",
                maxDebitAmount = 1199.0,
                previousAmount = null,
                billingFrequency = "Monthly",
                creationTimestamp = now - (15L * 24L * 60L * 60L * 1000L),
                nextBillingTimestamp = now + (15L * 24L * 60L * 60L * 1000L),
                lastSyncedTimestamp = now,
                sourceAppPackage = "com.google.android.apps.nbu.paisa.user",
                sourceAppName = "Google Pay",
                isRevoked = false
            ),
            MandateEntity(
                umn = "ICIC551209384721",
                merchantName = "CloudVault Storage",
                maxDebitAmount = 399.0,
                previousAmount = null,
                billingFrequency = "Monthly",
                creationTimestamp = now - (5L * 24L * 60L * 60L * 1000L),
                nextBillingTimestamp = now + (25L * 24L * 60L * 60L * 1000L),
                lastSyncedTimestamp = now,
                sourceAppPackage = "net.one97.paytm",
                sourceAppName = "Paytm",
                isRevoked = false
            )
        )
    }

    suspend fun start(
        context: Context,
        mandateDao: MandateDao,
        onNavigateToDemoPaywall: () -> Unit
    ) = withContext(Dispatchers.IO) {
        // 1. Seed realistic historical mandates so the dashboard is immediately functional
        val dummyMandates = getDummyMandates()
        mandateDao.insertOrUpdateAll(dummyMandates)

        // 2. Schedule reminders
        dummyMandates.forEach { mandate ->
            ReminderScheduler.schedule48HourPreDebitReminder(
                context,
                mandate.umn,
                mandate.merchantName,
                mandate.maxDebitAmount,
                mandate.nextBillingTimestamp
            )
        }

        // 3. Seed sample blocked traps if needed
        val trapCount = mandateDao.getBlockedTrapsCount()
        mandateDao.recordBlockedTrap(
            BlockedTrapEntity(
                packageName = "com.docuscan.fake",
                trapType = "Deceptive 3-Day Trial AutoPay",
                detectedText = "Start 3-Day Free Trial - Renews at ₹899/month",
                riskScore = 92
            )
        )
        mandateDao.recordBlockedTrap(
            BlockedTrapEntity(
                packageName = "com.vibe.stream.fake",
                trapType = "Concealed Annual Recurring Charge",
                detectedText = "Try 7 Days Free - Billed ₹1199 annually",
                riskScore = 88
            )
        )

        // 4. Launch the demo paywall on the UI thread
        withContext(Dispatchers.Main) {
            onNavigateToDemoPaywall()
        }
    }

    suspend fun seedInitialMandatesIfEmpty(context: Context) = withContext(Dispatchers.IO) {
        val database = SubZeroDatabase.getInstance(context)
        val dao = database.mandateDao()
        val currentMandates = dao.getAllActiveMandatesSync()
        if (currentMandates.isEmpty()) {
            val mandates = getDummyMandates()
            dao.insertOrUpdateAll(mandates)
            mandates.forEach {
                ReminderScheduler.schedule48HourPreDebitReminder(
                    context,
                    it.umn,
                    it.merchantName,
                    it.maxDebitAmount,
                    it.nextBillingTimestamp
                )
            }
            dao.recordBlockedTrap(
                BlockedTrapEntity(
                    packageName = "com.docuscan.fake",
                    trapType = "Deceptive 3-Day Trial AutoPay",
                    detectedText = "Start 3-Day Free Trial - Renews at ₹899/month",
                    riskScore = 92
                )
            )
        }
    }
}
