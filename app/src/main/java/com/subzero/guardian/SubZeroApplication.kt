package com.subzero.guardian

import com.subzero.SubZeroApp
import com.subzero.guardian.engine.DailyAutoPaySyncWorker
import com.subzero.guardian.engine.PaywallNotificationManager

class SubZeroApplication : SubZeroApp() {
    override fun onCreate() {
        super.onCreate()
        // Register the high-priority system notification channel
        PaywallNotificationManager.createNotificationChannel(this)
        // Schedule daily morning background sync
        DailyAutoPaySyncWorker.scheduleDailySync(this)
    }
}
