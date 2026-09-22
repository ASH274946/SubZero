package com.subzero

import android.app.Application
import com.subzero.ai.LocalModelEngine
import com.subzero.data.MandateRepository
import com.subzero.data.SubZeroDatabase
import com.subzero.engine.DailyAutoPaySyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * SubZero Application entry point.
 * Ensures zero internet usage and offline-first initialization.
 */
open class SubZeroApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: SubZeroDatabase by lazy {
        SubZeroDatabase.getInstance(this)
    }

    val mandateRepository: MandateRepository by lazy {
        MandateRepository(database.mandateDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Register the high-priority system notification channel
        com.subzero.engine.PaywallNotificationManager.createNotificationChannel(this)

        // Register persistent daily morning sync (06:00 AM)
        DailyAutoPaySyncWorker.scheduleDailySync(this)

        // Asynchronously initialize local model engine if model exists
        applicationScope.launch {
            LocalModelEngine.initialize(applicationContext)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        LocalModelEngine.close()
    }

    companion object {
        lateinit var instance: SubZeroApp
            private set
    }
}
