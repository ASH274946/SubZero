package com.subzero.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Records an intercepted deceptive subscription or dark pattern paywall caught by SubZero.
 */
@Entity(tableName = "blocked_traps")
data class BlockedTrapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String = "",

    @ColumnInfo(name = "trap_type")
    val trapType: String = "Deceptive AutoPay",

    @ColumnInfo(name = "detected_text")
    val detectedText: String = "",

    @ColumnInfo(name = "risk_score")
    val riskScore: Int = 85,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
