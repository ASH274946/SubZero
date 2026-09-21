package com.subzero.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.subzero.data.MandateEntity
import com.subzero.receivers.MandateReminderReceiver

/**
 * Predictive 48-Hour Pre-Debit Alarm Scheduler.
 *
 * Implements:
 * 1. Calculation of exact renewal dates and 48-hour pre-debit reminder offsets (172,800,000 ms).
 * 2. Accurate RTC_WAKEUP exact alarms via AlarmManager (canScheduleExactAlarms compliant).
 * 3. Graceful fallback for non-exact alarm states and immediate alert when already within the 48-hour window.
 * 4. Distinct request codes mapped to mandate UMN hashes for clean cancellation and rescheduling.
 */
object ReminderScheduler {

    const val ACTION_MANDATE_REMINDER = "com.subzero.ACTION_MANDATE_REMINDER"
    const val EXTRA_UMN = "com.subzero.extra.UMN"
    const val EXTRA_MERCHANT = "com.subzero.extra.MERCHANT"
    const val EXTRA_AMOUNT = "com.subzero.extra.AMOUNT"
    const val EXTRA_FREQUENCY = "com.subzero.extra.FREQUENCY"

    // 48 hours prior to debit (strictly prior to interbank clearing lock-in)
    const val PRE_DEBIT_OFFSET_MS = 48L * 60L * 60L * 1000L // 172,800,000 ms

    /**
     * Determines the recurrence interval in milliseconds based on billing frequency.
     */
    fun getIntervalMs(frequency: String): Long {
        return when (frequency.trim().lowercase()) {
            "daily", "day", "per day" -> 86_400_000L // 1 day
            "weekly", "week", "per week" -> 7L * 86_400_000L // 7 days
            "quarterly", "quarter", "per quarter" -> 90L * 86_400_000L // 90 days
            "half-yearly", "half yearly", "semi-annual", "semi-annually" -> 182L * 86_400_000L // 182 days
            "annual", "annually", "yearly", "year", "/yr", "per year" -> 365L * 86_400_000L // 365 days
            else -> 30L * 86_400_000L // Default: Monthly (30 days)
        }
    }

    /**
     * Calculates the next exact timestamp when the 48-hour pre-debit alert should fire.
     */
    fun calculateReminderTimestamp(mandate: MandateEntity, now: Long = System.currentTimeMillis()): Long {
        val interval = getIntervalMs(mandate.billingFrequency)
        val baseTime = if (mandate.creationTimestamp > 0L) mandate.creationTimestamp else now

        val nextBillingDate: Long = if (baseTime > now) {
            baseTime
        } else {
            val elapsed = now - baseTime
            val cycles = (elapsed / interval) + 1
            baseTime + (cycles * interval)
        }

        val targetReminderTime = nextBillingDate - PRE_DEBIT_OFFSET_MS

        return when {
            // If the reminder time is in the future, return it
            targetReminderTime > now -> targetReminderTime
            // If the debit is in the future (< 48 hours away), fire almost immediately (e.g. in 5 seconds)
            nextBillingDate > now -> now + 5_000L
            // If somehow both passed, advance to the next cycle's reminder
            else -> (nextBillingDate + interval) - PRE_DEBIT_OFFSET_MS
        }
    }

    /**
     * Schedules an exact system alarm for a mandate 48 hours before debit.
     */
    fun scheduleReminder(context: Context, mandate: MandateEntity) {
        if (mandate.isRevoked) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val targetTime = calculateReminderTimestamp(mandate)

        val intent = Intent(context, MandateReminderReceiver::class.java).apply {
            action = ACTION_MANDATE_REMINDER
            putExtra(EXTRA_UMN, mandate.umn)
            putExtra(EXTRA_MERCHANT, mandate.merchantName)
            putExtra(EXTRA_AMOUNT, mandate.maxDebitAmount)
            putExtra(EXTRA_FREQUENCY, mandate.billingFrequency)
        }

        val requestCode = mandate.umn.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTime,
                pendingIntent
            )
        }
    }

    /**
     * Schedules an exact 48-hour pre-debit alarm directly given a billing timestamp.
     */
    fun schedule48HourPreDebitReminder(
        context: Context,
        umn: String,
        merchantName: String,
        amount: Double,
        billingTimestamp: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Exact 48-Hour Pre-Debit Window (48 * 60 * 60 * 1000L)
        val reminderTime = billingTimestamp - (48L * 60L * 60L * 1000L)
        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent(context, MandateReminderReceiver::class.java).apply {
            action = ACTION_MANDATE_REMINDER
            putExtra(EXTRA_UMN, umn)
            putExtra(EXTRA_MERCHANT, merchantName)
            putExtra(EXTRA_AMOUNT, amount)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            umn.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancels any scheduled reminder alarm for the specified mandate UMN.
     */
    fun cancelReminder(context: Context, umn: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MandateReminderReceiver::class.java).apply {
            action = ACTION_MANDATE_REMINDER
        }

        val requestCode = umn.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
