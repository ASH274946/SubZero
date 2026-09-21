package com.subzero.ai

import com.subzero.services.PaywallAccessibilityService
import com.subzero.test.Test
import com.subzero.test.assertFalse
import com.subzero.test.assertTrue
import kotlin.system.measureNanoTime

class Stage1RegexTest {

    @Test
    fun testStage1Regex_positiveMatches() {
        val positiveSamples = listOf(
            "START 3-DAY FREE TRIAL",
            "renews at ₹899/month automatically via UPI AutoPay",
            "Your subscription will be billed annually",
            "Monthly plan for ₹199 /mo",
            "Auto-debit mandate registered successfully",
            "e-mandate active on your account",
            "7-day trial then ₹499 per month"
        )

        for (sample in positiveSamples) {
            val matched = PaywallAccessibilityService.STAGE_1_GATE_REGEX.containsMatchIn(sample)
            assertTrue(matched, "Expected positive match for: $sample")
        }
    }

    @Test
    fun testStage1Regex_negativeControls() {
        val negativeSamples = listOf(
            "Welcome back to your chat messenger",
            "Settings and Profile information",
            "Tap to capture a photo using your camera",
            "Order delivered to your doorstep"
        )

        for (sample in negativeSamples) {
            val matched = PaywallAccessibilityService.STAGE_1_GATE_REGEX.containsMatchIn(sample)
            assertFalse(matched, "Expected negative match for: $sample")
        }
    }

    @Test
    fun testStage1Regex_executionSpeedUnder2ms() {
        val sampleText = "DocuScan Pro START 3-DAY FREE TRIAL renews at ₹899/month automatically via UPI AutoPay"

        // Warm up JIT
        repeat(1000) {
            PaywallAccessibilityService.STAGE_1_GATE_REGEX.containsMatchIn(sampleText)
        }

        // Benchmark
        val iterations = 500
        val totalNanos = measureNanoTime {
            repeat(iterations) {
                PaywallAccessibilityService.STAGE_1_GATE_REGEX.containsMatchIn(sampleText)
            }
        }

        val avgMs = (totalNanos / iterations.toDouble()) / 1_000_000.0
        // Target is < 2.0 ms
        assertTrue(avgMs < 2.0, "Expected average matching time < 2.0ms, got $avgMs ms")
    }
}

