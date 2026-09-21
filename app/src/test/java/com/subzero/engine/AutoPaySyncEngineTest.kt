package com.subzero.engine

import com.subzero.data.MandateEntity
import com.subzero.test.Test
import com.subzero.test.assertEquals
import com.subzero.test.assertFalse
import com.subzero.test.assertNotNull
import com.subzero.test.assertTrue
import java.util.regex.Pattern

class AutoPaySyncEngineTest {

    private val bankingSenderPattern = Pattern.compile(
        "(?i).*(HDFC|SBI|ICICI|AXIS|KOTAK|NPCI|PHONEPE|PAYTM|YESB|PNB|BOB|IDFC).*"
    )

    private val mandateCreationPattern = Pattern.compile(
        "(?i)(?:mandate|autopay|e-mandate|recurring).*?(?:created|registered|authorized|active|setup|approved).*?for\\s+([A-Za-z0-9\\s.,_-]+?)(?:\\s+with|\\s+having|\\s+for|\\s+of|\\s+upto|\\s+rs|\\s+inr|\\s+₹|\\.)",
        Pattern.CASE_INSENSITIVE
    )

    private val amountPattern = Pattern.compile(
        "(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val umnPattern = Pattern.compile(
        "(?i)(?:umn|mandate\\s*id|ref(?:erence)?\\s*(?:no|num)?|reg\\s*no)[:\\s]+([A-Za-z0-9]{10,35})",
        Pattern.CASE_INSENSITIVE
    )

    private val revocationPattern = Pattern.compile(
        "(?i)(?:(?:mandate|autopay).*?(?:revoked|cancelled|deleted|stopped|paused|modified))|(?:(?:revoked|cancelled|deleted|stopped|paused|modified).*?(?:mandate|autopay))",
        Pattern.CASE_INSENSITIVE
    )

    @Test
    fun testBankingSenderPattern_matchesRegulatoryHeaders() {
        assertTrue(bankingSenderPattern.matcher("AD-HDFCBK").matches())
        assertTrue(bankingSenderPattern.matcher("VM-SBIUPI").matches())
        assertTrue(bankingSenderPattern.matcher("BZ-PHONEPE").matches())
        assertTrue(bankingSenderPattern.matcher("PAYTM").matches())
        assertTrue(bankingSenderPattern.matcher("AXISBK").matches())
        assertTrue(bankingSenderPattern.matcher("NPCI-UPI").matches())

        assertFalse(bankingSenderPattern.matcher("UBER").matches())
        assertFalse(bankingSenderPattern.matcher("SWIGGY").matches())
        assertFalse(bankingSenderPattern.matcher("+919876543210").matches())
    }

    @Test
    fun testMandateExtraction_creationAndAmounts() {
        val body = "HDFC Bank: AutoPay registered for DocuScan Pro of Rs. 899 monthly. UMN: HDFC98421034871290"
        val createMatcher = mandateCreationPattern.matcher(body)
        assertTrue(createMatcher.find())
        assertEquals("DocuScan Pro", createMatcher.group(1)?.trim())

        val amtMatcher = amountPattern.matcher(body)
        assertTrue(amtMatcher.find())
        assertEquals(899.0, amtMatcher.group(1)?.toDoubleOrNull() ?: 0.0, 0.001)

        val umnMatcher = umnPattern.matcher(body)
        assertTrue(umnMatcher.find())
        assertEquals("HDFC98421034871290", umnMatcher.group(1))
    }

    @Test
    fun testAutoPayParser_realCentralBankAndPhonePeMessages() {
        // 1. Spotify creation
        val spotifyCreate = "Your UPI-Mandate is successfully created towards SPOTIFY for Rs139.00. Funds are blocked from A/C No. XXXX4379 - Central Bank of India"
        val parsed1 = AutoPayParser.parse("AD-CENTBK-S", spotifyCreate, System.currentTimeMillis())
        assertNotNull(parsed1)
        assertEquals("Spotify", parsed1?.merchantName)
        assertEquals(139.0, parsed1?.amount ?: 0.0, 0.001)
        assertFalse(parsed1?.isRevocation ?: true)

        // 2. Spotify upcoming debit / price update (Rs 199)
        val spotifyUpcoming = "Ur upcoming mandate is set 05-Apr-26 and ur A/c will be DR with Rs. 199.00 towards SPOTIFY for Autopay Ref- 609378964304. If mandate is paused execution will not happen - Central Bank of India"
        val parsed2 = AutoPayParser.parse("AX-CENTBK-S", spotifyUpcoming, System.currentTimeMillis())
        assertNotNull(parsed2)
        assertEquals("Spotify", parsed2?.merchantName)
        assertEquals(199.0, parsed2?.amount ?: 0.0, 0.001)
        assertNotNull(parsed2?.nextBillingTimestamp)

        // 3. Seekho mandate
        val seekhoUpcoming = "Ur upcoming mandate is set 06-Nov-25 and ur A/c will be DR with Rs. 199.00 towards Seekho for Create Mandate Ref- 911858743095. If mandate is paused execution will not happen - Central Bank of India"
        val parsed3 = AutoPayParser.parse("AD-CENTBK-S", seekhoUpcoming, System.currentTimeMillis())
        assertNotNull(parsed3)
        assertEquals("Seekho", parsed3?.merchantName)
        assertEquals(199.0, parsed3?.amount ?: 0.0, 0.001)

        // 4. Google Cloud creation & revocation
        val gcloudCreate = "Your UPI-Mandate is successfully created towards Google Cloud for Rs15000.00. Funds are blocked from A/C No. XXXX4379 - Central Bank of India"
        val parsed4 = AutoPayParser.parse("AD-CENTBK-S", gcloudCreate, System.currentTimeMillis())
        assertNotNull(parsed4)
        assertEquals("Google Cloud", parsed4?.merchantName)
        assertEquals(15000.0, parsed4?.amount ?: 0.0, 0.001)

        val gcloudRevoke = "Your UPI-Mandate is successfully Revoked by  for Rs15000.00 - Central Bank of India"
        val parsed5 = AutoPayParser.parse("AD-CENTBK-S", gcloudRevoke, System.currentTimeMillis())
        assertNotNull(parsed5)
        assertTrue(parsed5?.isRevocation ?: false)
    }


    @Test
    fun testRevocationPattern_detectsRevokedMandates() {
        val revokeBody = "Your AutoPay mandate for DocuScan Pro with UMN: HDFC98421034871290 has been revoked successfully."
        val revokeMatcher = revocationPattern.matcher(revokeBody)
        assertTrue(revokeMatcher.find())
    }

    @Test
    fun testResolveUpiAppSource_identifiesEcosystems() {
        fun resolveSource(body: String, sender: String): Pair<String, String> {
            val lower = "$body $sender".lowercase()
            return when {
                lower.contains("phonepe") || lower.contains("@ybl") || lower.contains("@axl") || lower.contains("@ibl") -> {
                    "com.phonepe.app" to "PhonePe"
                }
                lower.contains("google pay") || lower.contains("gpay") || lower.contains("@okhdfc") || lower.contains("@okaxis") || lower.contains("@oksbi") || lower.contains("@okicici") -> {
                    "com.google.android.apps.nbu.paisa.user" to "Google Pay"
                }
                lower.contains("paytm") || lower.contains("@paytm") -> {
                    "net.one97.paytm" to "Paytm"
                }
                lower.contains("bhim") || lower.contains("@upi") -> {
                    "in.org.npci.upiapp" to "BHIM UPI"
                }
                else -> {
                    "generic" to "Bank Direct"
                }
            }
        }

        val phonePeResult = resolveSource("AutoPay setup via merchant@ybl", "VM-HDFCBK")
        assertEquals("com.phonepe.app", phonePeResult.first)
        assertEquals("PhonePe", phonePeResult.second)

        val gpayResult = resolveSource("Recurring mandate on Google Pay for Spotify", "SBIUPI")
        assertEquals("com.google.android.apps.nbu.paisa.user", gpayResult.first)
        assertEquals("Google Pay", gpayResult.second)

        val paytmResult = resolveSource("Mandate created on Paytm", "BZ-PAYTM")
        assertEquals("net.one97.paytm", paytmResult.first)
        assertEquals("Paytm", paytmResult.second)

        val bankResult = resolveSource("Auto-debit setup by Bank", "AD-HDFCBK")
        assertEquals("generic", bankResult.first)
        assertEquals("Bank Direct", bankResult.second)
    }

    @Test
    fun testMandateEntity_fullDataIntegrity() {
        val mandate = MandateEntity(
            umn = "NPCI998877665544332211",
            merchantName = "Netflix Entertainment",
            maxDebitAmount = 649.0,
            billingFrequency = "Monthly",
            creationTimestamp = 1000L,
            nextBillingTimestamp = 1000L + 2592000000L,
            sourceAppPackage = "com.phonepe.app",
            sourceAppName = "PhonePe",
            isRevoked = false
        )

        assertEquals("NPCI998877665544332211", mandate.umn)
        assertEquals("Netflix Entertainment", mandate.merchantName)
        assertEquals(649.0, mandate.maxDebitAmount, 0.001)
        assertEquals("com.phonepe.app", mandate.sourceAppPackage)
        assertEquals("PhonePe", mandate.sourceAppName)
        assertEquals(649.0, mandate.calculateMonthlyDrain(), 0.001)
        assertEquals("XXXX••••2211", mandate.maskedUmn)
    }
}
