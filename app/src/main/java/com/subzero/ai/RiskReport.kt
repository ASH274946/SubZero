package com.subzero.ai

/**
 * Structured risk evaluation produced by the NLP/SLM pipeline.
 *
 * @property isDeceptive True if the analyzed text exhibits deceptive subscription or paywall terms.
 * @property headline High-level headline summarizing the detected pattern.
 * @property termsDetail Concise explanation of the hidden or recurring terms.
 * @property riskScore Internal risk score bounded between 0 and 100.
 */
data class RiskReport(
    val isDeceptive: Boolean,
    val headline: String,
    val termsDetail: String,
    val riskScore: Int
) {
    init {
        require(riskScore in 0..100) { "Risk score must be between 0 and 100, got $riskScore" }
    }

    companion object {
        val SAFE = RiskReport(
            isDeceptive = false,
            headline = "No deceptive terms detected",
            termsDetail = "The visible screen text does not appear to conceal recurring charges or deceptive trials.",
            riskScore = 0
        )

        val TIMEOUT_FALLBACK = RiskReport(
            isDeceptive = true,
            headline = "Potential subscription terms detected (Analysis timed out)",
            termsDetail = "Keywords for recurring billing/auto-renewal were found, but local SLM evaluation timed out.",
            riskScore = 75
        )
    }
}
