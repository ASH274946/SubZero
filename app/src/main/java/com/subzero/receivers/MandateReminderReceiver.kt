package com.subzero.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.subzero.MainActivity
import com.subzero.R
import com.subzero.SubZeroApp
import com.subzero.services.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Predictive 36-Hour Pre-Debit Broadcast Receiver.
 *
 * Implements:
 * 1. Notification dispatch via high-priority 'mandate_reminders' channel.
 * 2. Pre-debit warning displaying merchant, amount, and 36-hour countdown.
 * 3. Direct action button to launch the Assisted Revocation Queue.
 * 4. Automatic rescheduling for subsequent billing cycles.
 */
class MandateReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "mandate_reminders"
        const val CHANNEL_NAME = "Mandate Pre-Debit Alerts"
        const val EXTRA_ROUTE = "EXTRA_ROUTE"
        const val ROUTE_KILL_SWITCH_QUEUE = "kill_switch_queue"
        const val EXTRA_TARGET_UMN = "EXTRA_TARGET_UMN"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val umn = intent.getStringExtra(ReminderScheduler.EXTRA_UMN) ?: return
        val merchant = intent.getStringExtra(ReminderScheduler.EXTRA_MERCHANT) ?: "AutoPay Merchant"
        val amount = intent.getDoubleExtra(ReminderScheduler.EXTRA_AMOUNT, 0.0)

        // Ensure notification channel exists
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Tap Intent -> Launch MainActivity into Kill Switch Queue
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, ROUTE_KILL_SWITCH_QUEUE)
            putExtra(EXTRA_TARGET_UMN, umn)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            umn.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Button: "Assisted Revoke"
        val revokeActionIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, ROUTE_KILL_SWITCH_QUEUE)
            putExtra(EXTRA_TARGET_UMN, umn)
        }
        val revokePendingIntent = PendingIntent.getActivity(
            context,
            umn.hashCode() + 1000,
            revokeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = if (amount > 0.0) "₹${amount.toInt()}" else "AutoPay"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AutoPay Pre-Debit Alert: $formattedAmount in 36 Hours")
            .setContentText("$merchant is scheduled to debit $formattedAmount in 36 hours. Tap to review or revoke.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$merchant will debit $formattedAmount from your UPI AutoPay in 36 hours (prior to the interbank clearing cutoff). Open SubZero to cancel before charges settle.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_delete,
                "Assisted Revoke",
                revokePendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(umn.hashCode(), notification)

        // Reschedule for next recurring cycle if mandate is still active
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = SubZeroApp.instance.mandateRepository.getMandateByUmn(umn)
                if (entity != null && !entity.isRevoked) {
                    ReminderScheduler.scheduleReminder(context, entity)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts 36 hours before recurring AutoPay debits or trial expirations"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
