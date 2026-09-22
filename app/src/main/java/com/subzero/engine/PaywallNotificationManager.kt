package com.subzero.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.subzero.MainActivity

object PaywallNotificationManager {

    private const val CHANNEL_ID = "subzero_paywall_alerts"
    private const val CHANNEL_NAME = "Subscription & Trial Alerts"
    private const val NOTIFICATION_ID = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Enables heads-up banner
            ).apply {
                description = "Alerts when an app disguises a recurring AutoPay subscription as a free trial"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a genuine system notification.
     * Displays a heads-up alert for a few seconds, then smoothly rolls into the notification drawer.
     */
    fun postDarkPatternAlert(
        context: Context,
        appName: String,
        amount: String,
        renewalDetails: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_HIGHLIGHT_TRAP", true)
            putExtra("EXTRA_TRAP_APP", appName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Hidden Recurring Charge in $appName")
            .setContentText("This trial auto-renews at $amount. Tap to review.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("SubZero detected that $appName will automatically deduct $amount via AutoPay. $renewalDetails")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Heads-up behavior
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true) // Dismisses from tray when tapped
            .setContentIntent(pendingIntent)
            .setColor(0xFF1E5144.toInt()) // Material 3 Pine Primary
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun dismissAlert(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}
