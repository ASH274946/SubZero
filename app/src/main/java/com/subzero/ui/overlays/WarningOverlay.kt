package com.subzero.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.subzero.R

object WarningOverlay {
    private var overlayView: View? = null

    fun show(context: Context) {
        if (overlayView != null) dismiss(context)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.layout_warning_overlay, null)

        val tvBody = overlayView?.findViewById<TextView>(R.id.tvOverlayBody)
        val chipEn = overlayView?.findViewById<TextView>(R.id.chipEnglish)
        val chipTe = overlayView?.findViewById<TextView>(R.id.chipTelugu)
        val chipHi = overlayView?.findViewById<TextView>(R.id.chipHindi)

        val btnDontPay = overlayView?.findViewById<Button>(R.id.btnOverlayDontPay)
        val btnDismiss = overlayView?.findViewById<Button>(R.id.btnOverlayDismiss)

        chipEn?.setOnClickListener {
            resetChips(chipEn, chipTe, chipHi)
            tvBody?.text = "This app will automatically deduct ₹899 every month starting in 3 days."
        }

        chipTe?.setOnClickListener {
            resetChips(chipTe, chipEn, chipHi)
            tvBody?.text = "ఈ యాప్ 3 రోజుల తర్వాత ప్రతి నెలా మీ ఖాతా నుండి ₹899 కట్ చేస్తుంది."
        }

        chipHi?.setOnClickListener {
            resetChips(chipHi, chipEn, chipTe)
            tvBody?.text = "यह ऐप 3 दिनों के बाद हर महीने आपके बैंक से ₹899 काट लेगा।"
        }

        btnDontPay?.setOnClickListener {
            dismiss(context)
        }

        btnDismiss?.setOnClickListener {
            dismiss(context)
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (_: Exception) {}
    }

    private fun resetChips(active: TextView?, vararg inactives: TextView?) {
        active?.setBackgroundResource(R.drawable.bg_m3_chip_active)
        active?.setTextColor(0xFF1E5144.toInt())
        inactives.forEach {
            it?.background = null
            it?.setTextColor(0xFF5A6F66.toInt())
        }
    }

    fun dismiss(context: Context) {
        overlayView?.let {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }
}
