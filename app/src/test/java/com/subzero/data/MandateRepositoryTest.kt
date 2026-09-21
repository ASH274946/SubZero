package com.subzero.data

import com.subzero.test.Test
import com.subzero.test.assertEquals

class MandateRepositoryTest {

    @Test
    fun testMandateEntity_monthlyDrainCalculations() {
        val monthlyMandate = MandateEntity(
            umn = "UMN123456789012",
            merchantName = "Spotify",
            maxDebitAmount = 119.0,
            billingFrequency = "Monthly",
            creationTimestamp = 1000L
        )
        assertEquals(119.0, monthlyMandate.calculateMonthlyDrain(), 0.001)

        val quarterlyMandate = MandateEntity(
            umn = "UMN123456789013",
            merchantName = "Gym Membership",
            maxDebitAmount = 3000.0,
            billingFrequency = "Quarterly",
            creationTimestamp = 1000L
        )
        assertEquals(1000.0, quarterlyMandate.calculateMonthlyDrain(), 0.001)

        val annualMandate = MandateEntity(
            umn = "UMN123456789014",
            merchantName = "Amazon Prime",
            maxDebitAmount = 1499.0,
            billingFrequency = "Annual",
            creationTimestamp = 1000L
        )
        assertEquals(124.91, annualMandate.calculateMonthlyDrain(), 0.1)

        val halfYearlyMandate = MandateEntity(
            umn = "UMN123456789015",
            merchantName = "Course Pass",
            maxDebitAmount = 600.0,
            billingFrequency = "Half-Yearly",
            creationTimestamp = 1000L
        )
        assertEquals(100.0, halfYearlyMandate.calculateMonthlyDrain(), 0.001)

        val unknownMandate = MandateEntity(
            umn = "UMN123456789016",
            merchantName = "Custom Plan",
            maxDebitAmount = 500.0,
            billingFrequency = "AdHoc",
            creationTimestamp = 1000L
        )
        // Unknown frequency does not silently assume monthly
        assertEquals(0.0, unknownMandate.calculateMonthlyDrain(), 0.001)
    }

    @Test
    fun testMandateEntity_revokedMandateZeroDrain() {
        val revoked = MandateEntity(
            umn = "UMN123456789012",
            merchantName = "Netflix",
            maxDebitAmount = 649.0,
            billingFrequency = "Monthly",
            creationTimestamp = 1000L,
            isRevoked = true
        )
        assertEquals(0.0, revoked.calculateMonthlyDrain(), 0.001)
    }

    @Test
    fun testMandateEntity_maskedUmn() {
        val mandate = MandateEntity(
            umn = "HDFC1234567890123456",
            merchantName = "Netflix",
            maxDebitAmount = 649.0,
            billingFrequency = "Monthly",
            creationTimestamp = 1000L
        )
        assertEquals("XXXX••••3456", mandate.maskedUmn)
    }
}

