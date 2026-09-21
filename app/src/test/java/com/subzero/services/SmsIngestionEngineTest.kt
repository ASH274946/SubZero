package com.subzero.services

import com.subzero.test.Test
import com.subzero.test.assertEquals
import com.subzero.test.assertFalse
import com.subzero.test.assertNotNull
import com.subzero.test.assertNull
import com.subzero.test.assertTrue

class SmsIngestionEngineTest {

    @Test
    fun testIsRegulatorySender_traiWhitelistedHeaders() {
        assertTrue(SmsIngestionEngine.isRegulatorySender("AD-HDFCBK"))
        assertTrue(SmsIngestionEngine.isRegulatorySender("VM-SBIUPI"))
        assertTrue(SmsIngestionEngine.isRegulatorySender("BZ-PAYTM"))
        assertTrue(SmsIngestionEngine.isRegulatorySender("ICICIB"))
        assertTrue(SmsIngestionEngine.isRegulatorySender("AXISBK"))
        assertTrue(SmsIngestionEngine.isRegulatorySender("KOTAKB"))
        assertTrue(SmsIngestionEngine.isRegulatorySender("VK-NPCI"))
    }

    @Test
    fun testIsRegulatorySender_rejectsPersonalMobileNumbers() {
        assertFalse(SmsIngestionEngine.isRegulatorySender("+919876543210"))
        assertFalse(SmsIngestionEngine.isRegulatorySender("9876543210"))
        assertFalse(SmsIngestionEngine.isRegulatorySender("09876543210"))
        assertFalse(SmsIngestionEngine.isRegulatorySender("+918123456789"))
    }

    @Test
    fun testIsRegulatorySender_rejectsUnknownOrChatHeaders() {
        assertFalse(SmsIngestionEngine.isRegulatorySender("UNKNOWN"))
        assertFalse(SmsIngestionEngine.isRegulatorySender("FRIENDCHAT"))
        assertFalse(SmsIngestionEngine.isRegulatorySender(""))
        assertFalse(SmsIngestionEngine.isRegulatorySender(null))
    }

    @Test
    fun testIsAutoPayRegulatoryMessage_rejectsOtps() {
        val otpBody1 = "Your OTP for mandate setup is 481920. Valid for 10 minutes. Do not share with anyone."
        assertFalse(SmsIngestionEngine.isAutoPayRegulatoryMessage("AD-HDFCBK", otpBody1))

        val otpBody2 = "One Time Password (OTP) for Rs 499 AutoPay approval on DocuScan is 884123."
        assertFalse(SmsIngestionEngine.isAutoPayRegulatoryMessage("VM-SBIUPI", otpBody2))

        val otpBody3 = "Your security code to authenticate recurring debit is 901234."
        assertFalse(SmsIngestionEngine.isAutoPayRegulatoryMessage("ICICIB", otpBody3))
    }

    @Test
    fun testParseRegulatorySms_validMandateExtracted() {
        val validSms = "HDFC Bank: e-Mandate created for DocuScan Pro of Rs 899 monthly. UMN: HDFC98421034871290"
        val mandate = SmsIngestionEngine.parseRegulatorySms("AD-HDFCBK", validSms)

        assertNotNull(mandate)
        assertEquals("DocuScan Pro", mandate!!.merchantName)
        assertEquals(899.0, mandate.maxDebitAmount, 0.001)
        assertEquals("Monthly", mandate.billingFrequency)
        assertEquals("HDFC98421034871290", mandate.umn)
        assertFalse(mandate.isRevoked)
    }

    @Test
    fun testParseRegulatorySms_ignoresNonMandateFinancialMessage() {
        val transferSms = "Rs 1500 credited to account XX4901 on 14-Sep via UPI. Ref 90182410."
        val parsed = SmsIngestionEngine.parseRegulatorySms("AD-HDFCBK", transferSms)
        assertNull(parsed)
    }
}
