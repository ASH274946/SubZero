package com.subzero.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an audited UPI AutoPay or e-mandate registration record stored locally.
 */
@Entity(tableName = "active_mandates")
data class MandateEntity(
    @PrimaryKey
    @ColumnInfo(name = "umn")
    val umn: String,

    @ColumnInfo(name = "merchant_name")
    val merchantName: String,

    @ColumnInfo(name = "max_debit_amount")
    val maxDebitAmount: Double,

    @ColumnInfo(name = "previous_amount")
    val previousAmount: Double? = null, // Set when a price change is detected

    @ColumnInfo(name = "billing_frequency")
    val billingFrequency: String = "Monthly",

    @ColumnInfo(name = "creation_timestamp")
    val creationTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "next_billing_timestamp")
    val nextBillingTimestamp: Long = creationTimestamp + (30L * 24L * 60L * 60L * 1000L),

    @ColumnInfo(name = "last_synced_timestamp")
    val lastSyncedTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "source_app_package")
    val sourceAppPackage: String = "generic",

    @ColumnInfo(name = "source_app_name")
    val sourceAppName: String = "Bank Direct",

    @ColumnInfo(name = "is_revoked")
    val isRevoked: Boolean = false
) {
    constructor(
        umn: String,
        merchantName: String,
        maxDebitAmount: Double,
        billingFrequency: String,
        creationTimestamp: Long,
        isRevoked: Boolean = false
    ) : this(
        umn = umn,
        merchantName = merchantName,
        maxDebitAmount = maxDebitAmount,
        previousAmount = null,
        billingFrequency = billingFrequency,
        creationTimestamp = creationTimestamp,
        nextBillingTimestamp = creationTimestamp + (30L * 24L * 60L * 60L * 1000L),
        lastSyncedTimestamp = System.currentTimeMillis(),
        sourceAppPackage = "generic",
        sourceAppName = "Bank Direct",
        isRevoked = isRevoked
    )

    init {
        require(umn.isNotBlank()) { "UMN cannot be blank" }
        require(maxDebitAmount >= 0.0) { "Maximum debit amount cannot be negative" }
        require(merchantName.isNotBlank()) { "Merchant name cannot be blank" }
    }

    /**
     * Computes human-readable remaining time until next debit.
     */
    fun getRemainingTimeFormatted(): String {
        val diffMillis = nextBillingTimestamp - System.currentTimeMillis()
        if (diffMillis <= 0) return "Debiting Today"

        val days = diffMillis / (24 * 60 * 60 * 1000)
        val hours = (diffMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)

        return when {
            days == 0L && hours <= 12L -> "Debits in ${hours}h"
            days == 0L -> "Debits Tomorrow"
            days == 1L -> "Renews in 24h"
            else -> "Renews in ${days} days"
        }
    }

    /**
     * Calculates the estimated monthly drain based on explicit frequency normalization.
     */
    fun calculateMonthlyDrain(): Double {
        if (isRevoked) return 0.0
        return when (billingFrequency.trim().lowercase()) {
            "monthly", "month", "/mo", "per month" -> maxDebitAmount
            "quarterly", "quarter", "per quarter" -> maxDebitAmount / 3.0
            "half-yearly", "half yearly", "semi-annual", "semi-annually" -> maxDebitAmount / 6.0
            "annual", "annually", "yearly", "year", "/yr", "per year" -> maxDebitAmount / 12.0
            "weekly", "week", "per week" -> maxDebitAmount * 4.33
            "daily", "day", "per day" -> maxDebitAmount * 30.0
            else -> 0.0 // Do not silently treat unknown frequency as monthly
        }
    }

    /**
     * Returns a partially masked UMN representation (e.g. XXXX••••7821) to protect privacy.
     */
    val maskedUmn: String
        get() {
            val clean = umn.trim()
            return if (clean.length > 4) {
                "XXXX••••" + clean.takeLast(4)
            } else {
                "XXXX••••" + clean
            }
        }
}
