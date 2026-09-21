package com.subzero.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MandateDao {

    @Query("SELECT COUNT(*) FROM active_mandates WHERE is_revoked = 0")
    fun getActiveMandateCount(): Flow<Int>

    @Query("SELECT SUM(max_debit_amount) FROM active_mandates WHERE is_revoked = 0")
    fun getTotalMonthlyDrain(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM blocked_traps")
    fun getBlockedTrapsCount(): Flow<Int>

    @Query("SELECT * FROM active_mandates WHERE is_revoked = 0 ORDER BY next_billing_timestamp ASC")
    fun getActiveMandates(): Flow<List<MandateEntity>>

    @Query("SELECT * FROM active_mandates WHERE is_revoked = 0")
    suspend fun getAllActiveMandatesSync(): List<MandateEntity>

    @Query("SELECT * FROM active_mandates WHERE is_revoked = 0 ORDER BY creation_timestamp DESC")
    suspend fun getActiveMandatesList(): List<MandateEntity>

    @Query("SELECT * FROM active_mandates ORDER BY creation_timestamp DESC")
    fun getAllMandates(): Flow<List<MandateEntity>>

    @Query("SELECT * FROM active_mandates WHERE umn = :umn LIMIT 1")
    suspend fun getMandateByUmn(umn: String): MandateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mandate: MandateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMandate(mandate: MandateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(mandates: List<MandateEntity>)

    @Query("UPDATE active_mandates SET is_revoked = 1 WHERE umn = :umn")
    suspend fun markAsRevoked(umn: String)

    @Query("DELETE FROM active_mandates WHERE umn = :umn")
    suspend fun deleteMandate(umn: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordBlockedTrap(trap: BlockedTrapEntity)
}
