package com.subzero.security

import android.view.WindowManager
import com.subzero.test.Test
import com.subzero.test.assertFalse
import com.subzero.test.assertTrue

class SecurityPolicyTest {

    @Test
    fun testProtectedPackages_blocksPhonePe() {
        val phonePe = "com.phonepe.app"
        val isAllowed = SecurityPolicy.shouldAllowAccessibilityInspection(phonePe)
        assertFalse(isAllowed)
    }

    @Test
    fun testProtectedPackages_blocksGooglePay() {
        val gpay = "com.google.android.apps.nbu.paisa.user"
        val isAllowed = SecurityPolicy.shouldAllowAccessibilityInspection(gpay)
        assertFalse(isAllowed)
    }

    @Test
    fun testProtectedPackages_blocksPaytm() {
        val paytm = "net.one97.paytm"
        val isAllowed = SecurityPolicy.shouldAllowAccessibilityInspection(paytm)
        assertFalse(isAllowed)
    }

    @Test
    fun testProtectedPackages_blocksBhimAndBanking() {
        val bhim = "in.org.npci.upiapp"
        val sbi = "com.sbi.upi"
        val icici = "com.icicibank.pockets"

        assertFalse(SecurityPolicy.shouldAllowAccessibilityInspection(bhim))
        assertFalse(SecurityPolicy.shouldAllowAccessibilityInspection(sbi))
        assertFalse(SecurityPolicy.shouldAllowAccessibilityInspection(icici))
    }

    @Test
    fun testProtectedPackages_failClosedOnNullOrBlank() {
        assertFalse(SecurityPolicy.shouldAllowAccessibilityInspection(null))
        assertFalse(SecurityPolicy.shouldAllowAccessibilityInspection(""))
        assertFalse(SecurityPolicy.shouldAllowAccessibilityInspection("   "))
    }

    @Test
    fun testProtectedPackages_allowsGenericThirdPartyApp() {
        val docuScan = "com.docuscan.pro.app"
        assertTrue(SecurityPolicy.shouldAllowAccessibilityInspection(docuScan))
    }

    @Test
    fun testOverlayFlags_requiresNotTouchableAndNotFocusable() {
        val validFlags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        val invalidFlagsTouchable = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val invalidFlagsFocusable = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

        assertTrue(SecurityPolicy.validateOverlayFlags(validFlags))
        assertFalse(SecurityPolicy.validateOverlayFlags(invalidFlagsTouchable))
        assertFalse(SecurityPolicy.validateOverlayFlags(invalidFlagsFocusable))
    }
}

