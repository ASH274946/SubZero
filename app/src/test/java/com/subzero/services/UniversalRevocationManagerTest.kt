package com.subzero.services

import com.subzero.data.MandateEntity
import com.subzero.test.Test
import com.subzero.test.assertEquals
import com.subzero.test.assertFalse
import com.subzero.test.assertNotNull
import com.subzero.test.assertNull
import com.subzero.test.assertTrue

class UniversalRevocationManagerTest {

    @Test
    fun testStartQueue_initializesActiveMandatesOnly() {
        val mandate1 = MandateEntity("UMN1", "Merchant1", 100.0, "Monthly", 1000L, false)
        val mandate2 = MandateEntity("UMN2", "Merchant2", 200.0, "Monthly", 1000L, true) // Already revoked
        val mandate3 = MandateEntity("UMN3", "Merchant3", 300.0, "Monthly", 1000L, false)

        UniversalRevocationManager.startQueue(listOf(mandate1, mandate2, mandate3))

        val items = UniversalRevocationManager.queueItems.value
        assertEquals(2, items.size)
        assertEquals("UMN1", items[0].umn)
        assertEquals("UMN3", items[1].umn)
        assertEquals(0, UniversalRevocationManager.currentIndex.value)
        assertTrue(UniversalRevocationManager.isQueueActive.value)
    }

    @Test
    fun testAdvanceQueue_transitionsToNextStepAndCompletes() {
        val mandate1 = MandateEntity("UMN1", "Merchant1", 100.0, "Monthly", 1000L, false)
        val mandate2 = MandateEntity("UMN2", "Merchant2", 200.0, "Monthly", 1000L, false)

        UniversalRevocationManager.startQueue(listOf(mandate1, mandate2))

        assertEquals(0, UniversalRevocationManager.currentIndex.value)
        assertEquals("UMN1", UniversalRevocationManager.getCurrentMandate()?.umn)

        UniversalRevocationManager.advanceQueue()
        assertEquals(1, UniversalRevocationManager.currentIndex.value)
        assertEquals("UMN2", UniversalRevocationManager.getCurrentMandate()?.umn)
        assertTrue(UniversalRevocationManager.isQueueActive.value)

        // Advance beyond last item
        UniversalRevocationManager.advanceQueue()
        assertFalse(UniversalRevocationManager.isQueueActive.value)
    }

    @Test
    fun testClearQueue_resetsStateCleanly() {
        val mandate = MandateEntity("UMN1", "Merchant1", 100.0, "Monthly", 1000L, false)
        UniversalRevocationManager.startQueue(listOf(mandate))
        UniversalRevocationManager.clearQueue()

        assertEquals(0, UniversalRevocationManager.queueItems.value.size)
        assertFalse(UniversalRevocationManager.isQueueActive.value)
        assertNull(UniversalRevocationManager.getCurrentMandate())
    }
}
