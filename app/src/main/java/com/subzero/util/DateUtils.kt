package com.subzero.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatTimestamp(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    /**
     * Estimates remaining days until next renewal based on creation timestamp and billing frequency.
     */
    fun estimateNextRenewalDays(creationTimestamp: Long, frequency: String): String {
        val now = System.currentTimeMillis()
        val periodDays = when (frequency.trim().lowercase()) {
            "monthly", "month", "/mo", "per month" -> 30L
            "quarterly", "quarter", "per quarter" -> 90L
            "half-yearly", "half yearly", "semi-annual" -> 180L
            "annual", "annually", "yearly", "year", "/yr" -> 365L
            "weekly", "week", "per week" -> 7L
            "daily", "day", "per day" -> 1L
            else -> 30L
        }

        val elapsedMs = (now - creationTimestamp).coerceAtLeast(0L)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMs)
        val cycleRemainingDays = periodDays - (elapsedDays % periodDays)

        return if (cycleRemainingDays <= 1L) {
            "Tomorrow"
        } else {
            "in $cycleRemainingDays days"
        }
    }
}
