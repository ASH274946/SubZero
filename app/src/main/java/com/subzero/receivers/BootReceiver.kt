package com.subzero.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.subzero.data.SubZeroDatabase
import com.subzero.engine.DailyAutoPaySyncWorker
import com.subzero.services.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules daily AutoPay synchronization and active 48-hour pre-debit alarms upon device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // 1. Reinstate daily 06:00 AM background audit
        DailyAutoPaySyncWorker.scheduleDailySync(context)

        // 2. Reinstate exact 48-hour pre-debit alarms for all active mandates
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = SubZeroDatabase.getInstance(context)
                val activeMandates = db.mandateDao().getAllActiveMandatesSync()

                activeMandates.forEach { mandate ->
                    ReminderScheduler.schedule48HourPreDebitReminder(
                        context,
                        mandate.umn,
                        mandate.merchantName,
                        mandate.maxDebitAmount,
                        mandate.nextBillingTimestamp
                    )
                }
            } catch (_: Exception) {
                // Fail-safe catch to ensure receiver finishes gracefully
            } finally {
                pendingResult.finish()
            }
        }
    }
}
