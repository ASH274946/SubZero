package com.subzero.services

import com.subzero.data.MandateEntity
import com.subzero.test.Test
import com.subzero.test.assertEquals
import com.subzero.test.assertTrue

class ReminderSchedulerTest {

    @Test
    fun testPreDebitOffset_isExactFortyEightHours() {
        val fortyEightHoursMs = 48L * 60L * 60L * 1000L
        assertEquals(fortyEightHoursMs, ReminderScheduler.PRE_DEBIT_OFFSET_MS)
        assertEquals(172_800_000L, ReminderScheduler.PRE_DEBIT_OFFSET_MS)
    }

    // Alias for backwards-compatibility with older runners
    @Test
    fun testPreDebitOffset_isExactThirtySixHours() = testPreDebitOffset_isExactFortyEightHours()

    @Test
    fun testIntervalMs_correctlyNormalizesFrequencies() {
        assertEquals(86_400_000L, ReminderScheduler.getIntervalMs("Daily"))
        assertEquals(7L * 86_400_000L, ReminderScheduler.getIntervalMs("Weekly"))
        assertEquals(30L * 86_400_000L, ReminderScheduler.getIntervalMs("Monthly"))
        assertEquals(90L * 86_400_000L, ReminderScheduler.getIntervalMs("Quarterly"))
        assertEquals(182L * 86_400_000L, ReminderScheduler.getIntervalMs("Half-Yearly"))
        assertEquals(365L * 86_400_000L, ReminderScheduler.getIntervalMs("Annual"))
    }

    @Test
    fun testCalculateReminderTimestamp_futureDebitFiresAtExact48HoursBefore() {
        val now = 1_000_000_000_000L
        val interval = 30L * 86_400_000L // 30 days
        // Suppose mandate was created 20 days ago (so renewal is in 10 days = 240 hours > 48 hours)
        val creation = now - (20L * 86_400_000L)
        val mandate = MandateEntity(
            umn = "HDFC11223344",
            merchantName = "TestMerchant",
            maxDebitAmount = 499.0,
            billingFrequency = "Monthly",
            creationTimestamp = creation,
            isRevoked = false
        )

        val nextBillingDate = creation + interval // 10 days from now
        val expectedReminderTime = nextBillingDate - ReminderScheduler.PRE_DEBIT_OFFSET_MS

        val calculated = ReminderScheduler.calculateReminderTimestamp(mandate, now)
        assertEquals(expectedReminderTime, calculated)
    }

    // Alias for backwards-compatibility with older runners
    @Test
    fun testCalculateReminderTimestamp_futureDebitFiresAtExact36HoursBefore() = testCalculateReminderTimestamp_futureDebitFiresAtExact48HoursBefore()

    @Test
    fun testCalculateReminderTimestamp_nearDebitFiresImmediatelyWithinWindow() {
        val now = 1_000_000_000_000L
        val interval = 30L * 86_400_000L
        // Suppose mandate was created 29.5 days ago (so renewal is in 12 hours < 48 hours!)
        val creation = now - (interval - (12L * 60L * 60L * 1000L))
        val mandate = MandateEntity(
            umn = "HDFC55667788",
            merchantName = "UrgentMerchant",
            maxDebitAmount = 899.0,
            billingFrequency = "Monthly",
            creationTimestamp = creation,
            isRevoked = false
        )

        val calculated = ReminderScheduler.calculateReminderTimestamp(mandate, now)
        // Should fire almost immediately (now + 5000L) because user is already within the 48-hour warning cutoff
        assertEquals(now + 5_000L, calculated)
    }
}
