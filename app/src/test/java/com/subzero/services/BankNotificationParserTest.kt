package com.subzero.services

import com.subzero.test.Test
import com.subzero.test.assertEquals
import com.subzero.test.assertFalse
import com.subzero.test.assertNotNull
import com.subzero.test.assertNull
import com.subzero.test.assertTrue

class BankNotificationParserTest {

    @Test
    fun testParseMandateFromText_validHdfcNotification() {
        val notificationText = "HDFC Bank: e-Mandate created for Netflix Entertainment of Rs 649 monthly. UMN: HDFC1234567890123456"
        val mandate = BankNotificationListener.parseMandateFromText(notificationText)

        assertNotNull(mandate)
        assertTrue(mandate!!.merchantName.contains("Netflix"))
        assertEquals(649.0, mandate.maxDebitAmount, 0.001)
        assertEquals("Monthly", mandate.billingFrequency)
        assertEquals("HDFC1234567890123456", mandate.umn)
        assertFalse(mandate.isRevoked)
    }

    @Test
    fun testParseMandateFromText_validUpiAutoPayNotification() {
        val notificationText = "SBI UPI AutoPay: Mandate registered for Spotify India amount ₹119 monthly. UMN: SBIN98765432101234"
        val mandate = BankNotificationListener.parseMandateFromText(notificationText)

        assertNotNull(mandate)
        assertTrue(mandate!!.merchantName.contains("Spotify"))
        assertEquals(119.0, mandate.maxDebitAmount, 0.001)
        assertEquals("Monthly", mandate.billingFrequency)
        assertEquals("SBIN98765432101234", mandate.umn)
    }

    @Test
    fun testParseMandateFromText_quarterlyMandate() {
        val notificationText = "ICICI Bank: Auto-debit mandate registered for Gym Membership of INR 4,500 quarterly. UMN: ICIC99887766554433"
        val mandate = BankNotificationListener.parseMandateFromText(notificationText)

        assertNotNull(mandate)
        assertEquals(4500.0, mandate!!.maxDebitAmount, 0.001)
        assertEquals("Quarterly", mandate.billingFrequency)
        assertEquals(1500.0, mandate.calculateMonthlyDrain(), 0.001)
    }

    @Test
    fun testParseMandateFromText_annualMandate() {
        val notificationText = "Mandate created for Amazon Prime of Rs. 1499 annual. UMN: AMZN11223344556677"
        val mandate = BankNotificationListener.parseMandateFromText(notificationText)

        assertNotNull(mandate)
        assertEquals(1499.0, mandate!!.maxDebitAmount, 0.001)
        assertEquals("Annual", mandate.billingFrequency)
        assertEquals(124.91, mandate.calculateMonthlyDrain(), 1.0)
    }

    @Test
    fun testParseMandateFromText_nonMandateNotificationIgnored() {
        val normalSms = "Dear customer, your account was credited with Rs 5000 on 12-Sep-2026. Ref: 987654321"
        val mandate = BankNotificationListener.parseMandateFromText(normalSms)

        assertNull(mandate)
    }
}

