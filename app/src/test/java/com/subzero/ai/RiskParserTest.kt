package com.subzero.ai

import com.subzero.test.Test
import com.subzero.test.assertEquals
import com.subzero.test.assertFalse
import com.subzero.test.assertTrue

class RiskParserTest {

    @Test
    fun testParseModelResponse_validStructuredOutput() {
        val modelOutput = """
            DECEPTIVE=true
            RISK_SCORE=82
            HEADLINE=Free trial automatically renews
            DETAIL=The visible terms indicate automatic recurring billing after the trial.
        """.trimIndent()

        val report = LocalModelEngine.parseModelResponse(modelOutput, "sample original text")

        assertTrue(report.isDeceptive)
        assertEquals(82, report.riskScore)
        assertEquals("Free trial automatically renews", report.headline)
        assertEquals("The visible terms indicate automatic recurring billing after the trial.", report.termsDetail)
    }

    @Test
    fun testParseModelResponse_safeOutput() {
        val modelOutput = """
            DECEPTIVE=false
            RISK_SCORE=10
            HEADLINE=One-time purchase
            DETAIL=Clear non-recurring purchase terms.
        """.trimIndent()

        val report = LocalModelEngine.parseModelResponse(modelOutput, "sample")

        assertFalse(report.isDeceptive)
        assertEquals(10, report.riskScore)
    }

    @Test
    fun testParseModelResponse_outOfBoundsScoreClamping() {
        val modelOutput = """
            DECEPTIVE=true
            RISK_SCORE=150
            HEADLINE=Exaggerated risk
            DETAIL=Testing boundary clamp
        """.trimIndent()

        val report = LocalModelEngine.parseModelResponse(modelOutput, "sample")

        assertEquals(100, report.riskScore)
    }

    @Test
    fun testParseModelResponse_malformedOutputFallback() {
        val malformedOutput = "I think this might be a scam or trial that renews at 899 per month"
        val originalText = "DocuScan START 3-DAY FREE TRIAL renews at ₹899/month via UPI AutoPay"

        val report = LocalModelEngine.parseModelResponse(malformedOutput, originalText)

        assertTrue(report.isDeceptive)
        assertTrue(report.riskScore >= 70)
    }

    @Test
    fun testPerformDeterministicHeuristicEvaluation_trialAndAutoPay() {
        val text = "START 3-DAY FREE TRIAL renews at ₹899/month automatically via UPI AutoPay"
        val report = LocalModelEngine.performDeterministicHeuristicEvaluation(text)

        assertTrue(report.isDeceptive)
        assertEquals(88, report.riskScore)
        assertTrue(report.termsDetail.contains("₹899"))
    }
}

