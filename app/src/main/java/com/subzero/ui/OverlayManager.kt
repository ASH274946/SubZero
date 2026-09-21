package com.subzero.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.subzero.R
import com.subzero.ai.RiskReport
import com.subzero.security.SecurityPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the non-touchable, non-focusable security warning HUD overlay.
 *
 * Implements:
 * 1. Strict FLAG_NOT_TOUCHABLE and FLAG_NOT_FOCUSABLE WindowManager flags.
 * 2. High-visibility #FF2A54 neon HUD presentation.
 * 3. Multi-language warning text support (English, Telugu, Hindi).
 * 4. Safe presentation and removal with lifecycle checks.
 */
class OverlayManager private constructor(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var isOverlayAttached: Boolean = false

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

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
     * Shows or updates the non-touchable security warning overlay HUD on the main thread.
     */
    fun showWarning(riskReport: RiskReport) {
        mainHandler.post {
            if (!Settings.canDrawOverlays(context)) {
                return@post
            }

            if (overlayView == null) {
                overlayView = createOverlayView()
            }

            val view = overlayView ?: return@post
            updateOverlayContent(view, riskReport)

            if (!isOverlayAttached) {
                val params = createOverlayLayoutParams()
                // Verify Security Policy Flags
                if (SecurityPolicy.validateOverlayFlags(params.flags)) {
                    try {
                        windowManager.addView(view, params)
                        isOverlayAttached = true
                    } catch (_: Exception) {
                        isOverlayAttached = false
                    }
                }
            }
        }
    }

    /**
     * Dismisses and removes the warning HUD from WindowManager on the main thread.
     */
    fun dismissWarning() {
        mainHandler.post {
            if (isOverlayAttached && overlayView != null) {
                try {
                    windowManager.removeViewImmediate(overlayView)
                } catch (_: Exception) {
                } finally {
                    isOverlayAttached = false
                    overlayView = null
                }
            }
        }
    }

    private fun createOverlayLayoutParams(): WindowManager.LayoutParams {
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // NON-NEGOTIABLE OVERLAY FLAGS: Strictly non-touchable and non-focusable
        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80 // Top margin below status bar
        }
    }

    @SuppressLint("InflateParams")
    private fun createOverlayView(): View {
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(R.layout.overlay_warning_hud, null)
    }

    private fun updateOverlayContent(view: View, riskReport: RiskReport) {
        val titleView = view.findViewById<TextView>(R.id.overlay_title)
        val headlineView = view.findViewById<TextView>(R.id.overlay_headline)
        val detailView = view.findViewById<TextView>(R.id.overlay_detail)
        val riskScoreBadge = view.findViewById<TextView>(R.id.overlay_risk_badge)

        val titleRes = when (currentLanguage) {
            WarningLanguage.ENGLISH -> R.string.warning_title_en
            WarningLanguage.TELUGU -> R.string.warning_title_te
            WarningLanguage.HINDI -> R.string.warning_title_hi
        }

        titleView?.text = context.getString(titleRes)
        headlineView?.text = riskReport.headline
        detailView?.text = riskReport.termsDetail
        riskScoreBadge?.text = context.getString(R.string.risk_score_label, riskReport.riskScore)

        // Visual risk color coding
        val riskColor = when {
            riskReport.riskScore >= 75 -> ContextCompat.getColor(context, R.color.risk_high)
            riskReport.riskScore >= 50 -> ContextCompat.getColor(context, R.color.risk_medium)
            else -> ContextCompat.getColor(context, R.color.risk_low)
        }
        riskScoreBadge?.setTextColor(riskColor)
    }
}
