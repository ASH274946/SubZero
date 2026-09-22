package com.subzero.ui

import android.annotation.SuppressLint
import android.content.Context
import com.subzero.ai.RiskReport
import com.subzero.engine.PaywallNotificationManager

/**
 * Manages language preferences and routes alerts through native system notifications.
 * Eliminates legacy WindowManager floating overlays.
 */
class OverlayManager private constructor(private val context: Context) {

    enum class WarningLanguage {
        ENGLISH, TELUGU, HINDI
    }

    var currentLanguage: WarningLanguage = WarningLanguage.ENGLISH

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: OverlayManager? = null

        fun getInstance(context: Context): OverlayManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OverlayManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Posts a genuine system notification via PaywallNotificationManager.
     */
    fun showWarning(riskReport: RiskReport) {
        PaywallNotificationManager.postDarkPatternAlert(
            context = context,
            appName = "Subscription App",
            amount = "Recurring AutoPay",
            renewalDetails = riskReport.termsDetail
        )
    }

    /**
     * Dismisses the system notification alert.
     */
    fun dismissWarning() {
        PaywallNotificationManager.dismissAlert(context)
    }
}
