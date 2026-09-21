package com.subzero.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository providing a clean interface for mandate operations and local aggregations.
 */
class MandateRepository(private val mandateDao: MandateDao) {

    val allMandates: Flow<List<MandateEntity>> = mandateDao.getAllMandates()

    val activeMandates: Flow<List<MandateEntity>> = mandateDao.getActiveMandates()

    val totalMonthlyDrain: Flow<Double> = mandateDao.getTotalMonthlyDrain().map { it ?: 0.0 }

    val activeMandateCount: Flow<Int> = mandateDao.getActiveMandateCount()

    suspend fun insertOrUpdateAll(mandates: List<MandateEntity>) = withContext(Dispatchers.IO) {
        mandateDao.insertOrUpdateAll(mandates)
    }

    suspend fun insertMandate(mandate: MandateEntity) = withContext(Dispatchers.IO) {
        mandateDao.insertMandate(mandate)
    }

    suspend fun markAsRevoked(umn: String) = withContext(Dispatchers.IO) {
        mandateDao.markAsRevoked(umn)
    }

    suspend fun deleteMandate(umn: String) = withContext(Dispatchers.IO) {
        mandateDao.deleteMandate(umn)
    }

    suspend fun getMandateByUmn(umn: String): MandateEntity? = withContext(Dispatchers.IO) {
        mandateDao.getMandateByUmn(umn)
    }
}
