package com.subzero.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Local on-device SLM inference engine.
 *
 * Implements:
 * 1. Safe offline model loading (app storage or /data/local/tmp/subzero/models/model.bin).
 * 2. Strict 400ms timeout with coroutine cancellation.
 * 3. Circuit breaker for backoff upon repeated failures.
 * 4. Deterministic parsing of structured classification outputs.
 * 5. Deterministic heuristic fallback when model is uninitialized or unavailable.
 */
object LocalModelEngine {

    private val mutex = Mutex()
    private var llmInference: LlmInference? = null
    private var isInitialized: Boolean = false
    private var consecutiveFailures: Int = 0
    private var lastFailureTimestamp: Long = 0L

    // Compiled regex patterns for fallback analysis
    private val FREE_TRIAL_PATTERN = Regex("(?i)\\b(free trial|3-day trial|7-day trial|14-day trial|30-day trial|try free|start trial)\\b")
    private val RECURRING_BILLING_PATTERN = Regex("(?i)\\b(renews? at|renews? automatically|per month|/mo|billed annually|/yr|recurring|auto-renewal|autopay|e-mandate)\\b")
    private val HIDDEN_COST_PATTERN = Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+)")

    /**
     * Initializes the local model engine with thread safety.
     */
    suspend fun initialize(context: Context): Boolean = mutex.withLock {
        if (isInitialized && llmInference != null) return true

        return@withLock withContext(Dispatchers.IO) {
            try {
                val modelFile = resolveModelFile(context)
                if (modelFile != null && modelFile.exists() && modelFile.canRead()) {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(256)
                        .setTemperature(0.1f) // Deterministic low temperature
                        .setTopK(10)
                        .build()

                    llmInference = LlmInference.createFromOptions(context, options)
                    isInitialized = true
                    consecutiveFailures = 0
                    true
                } else {
                    // Model file not present on device - engine remains ready via deterministic fallback
                    isInitialized = false
                    false
                }
            } catch (e: Throwable) {
                isInitialized = false
                llmInference = null
                false
            }
        }
    }

    /**
     * Evaluates whether the given screen text contains deceptive paywall/subscription patterns.
     * Enforces the 400ms timeout target and circuit breaker.
     */
    suspend fun evaluateRisk(text: String): RiskReport = withContext(Dispatchers.Default) {
        val sanitized = text.take(ModelConfig.MAX_MODEL_INPUT_CHARS).trim()
        if (sanitized.isBlank()) {
            return@withContext RiskReport.SAFE
        }

        // Check Circuit Breaker
        val now = System.currentTimeMillis()
        val isCircuitOpen = consecutiveFailures >= ModelConfig.MAX_CONSECUTIVE_FAILURES &&
                (now - lastFailureTimestamp) < ModelConfig.CIRCUIT_BREAKER_RESET_MS

        if (isCircuitOpen || llmInference == null) {
            return@withContext performDeterministicHeuristicEvaluation(sanitized)
        }

        val prompt = buildClassificationPrompt(sanitized)

        try {
            val inferenceResult: String? = withTimeoutOrNull(ModelConfig.MODEL_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    mutex.withLock {
                        // MediaPipe LlmInference returns String
                        val instance = llmInference
                        if (instance != null) {
                            try {
                                val method = instance.javaClass.getMethod("generateResponse", String::class.java)
                                method.invoke(instance, prompt) as? String
                            } catch (_: Exception) {
                                null
                            }
                        } else null
                    }
                }
            }

            if (!inferenceResult.isNullOrBlank()) {
                consecutiveFailures = 0
                parseModelResponse(inferenceResult, sanitized)
            } else {
                // Timeout or missing result occurred
                recordFailure()
                performDeterministicHeuristicEvaluation(sanitized, isTimeout = true)
            }
        } catch (e: Throwable) {
            recordFailure()
            performDeterministicHeuristicEvaluation(sanitized)
        }
    }

    /**
     * Resolves the model file location prioritizing app storage then external debug path.
     */
    fun resolveModelFile(context: Context): File? {
        val internalFile = File(context.filesDir, ModelConfig.INTERNAL_MODEL_FILENAME)
        if (internalFile.exists() && internalFile.length() > 0) {
            return internalFile
        }

        val externalFile = File(ModelConfig.EXTERNAL_MODEL_PATH)
        if (externalFile.exists() && externalFile.canRead() && externalFile.length() > 0) {
            return externalFile
        }

        return null
    }

    /**
     * Closes and cleans up model resources.
     */
    fun close() {
        try {
            val instance = llmInference
            if (instance is java.io.Closeable) {
                instance.close()
            } else if (instance != null) {
                try {
                    instance.javaClass.getMethod("close").invoke(instance)
                } catch (_: Exception) {
                }
            }
        } catch (_: Throwable) {
        } finally {
            llmInference = null
            isInitialized = false
        }
    }

    fun isModelReady(): Boolean = isInitialized && llmInference != null

    private fun recordFailure() {
        consecutiveFailures++
        lastFailureTimestamp = System.currentTimeMillis()
    }

    private fun buildClassificationPrompt(screenText: String): String {
        return """
            You are a deterministic security classifier analyzing mobile app paywalls for deceptive subscription dark patterns.
            Analyze the following text extracted from a mobile screen:
            ---
            $screenText
            ---
            Output ONLY in this exact format:
            DECEPTIVE=<true|false>
            RISK_SCORE=<integer between 0 and 100>
            HEADLINE=<short description of the dark pattern or safe status>
            DETAIL=<brief factual explanation of recurring billing or trial trap>
        """.trimIndent()
    }

    /**
     * Parses the raw model output safely and deterministically.
     */
    fun parseModelResponse(rawOutput: String, originalText: String): RiskReport {
        try {
            var hasStructuredOutput = false
            var isDeceptive: Boolean? = null
            var riskScore: Int? = null
            var headline: String? = null
            var detail: String? = null

            rawOutput.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("DECEPTIVE=", ignoreCase = true) -> {
                        val value = trimmed.substringAfter("=").trim().lowercase()
                        isDeceptive = value == "true" || value == "1" || value == "yes"
                        hasStructuredOutput = true
                    }
                    trimmed.startsWith("RISK_SCORE=", ignoreCase = true) -> {
                        val value = trimmed.substringAfter("=").trim().filter { it.isDigit() }
                        riskScore = value.toIntOrNull()
                        hasStructuredOutput = true
                    }
                    trimmed.startsWith("HEADLINE=", ignoreCase = true) -> {
                        headline = trimmed.substringAfter("=").trim().take(120)
                        hasStructuredOutput = true
                    }
                    trimmed.startsWith("DETAIL=", ignoreCase = true) -> {
                        detail = trimmed.substringAfter("=").trim().take(250)
                        hasStructuredOutput = true
                    }
                }
            }

            if (!hasStructuredOutput || isDeceptive == null) {
                // If model returned arbitrary unstructured text, use deterministic heuristic fallback
                return performDeterministicHeuristicEvaluation(originalText)
            }

            val finalDeceptive = isDeceptive ?: false
            val validatedScore = (riskScore ?: if (finalDeceptive) 75 else 10).coerceIn(0, 100)
            val finalHeadline = headline?.ifBlank { null }
                ?: if (finalDeceptive) "Potential deceptive subscription terms" else "Standard terms"
            val finalDetail = detail?.ifBlank { null }
                ?: if (finalDeceptive) "Screen indicates automatic recurring billing or disguised trial terms." else "No dark patterns detected."

            return RiskReport(
                isDeceptive = finalDeceptive,
                headline = finalHeadline,
                termsDetail = finalDetail,
                riskScore = validatedScore
            )
        } catch (_: Exception) {
            return performDeterministicHeuristicEvaluation(originalText)
        }
    }

    /**
     * Deterministic rule-based evaluation when local SLM is uninitialized or timed out.
     */
    fun performDeterministicHeuristicEvaluation(text: String, isTimeout: Boolean = false): RiskReport {
        val hasFreeTrial = FREE_TRIAL_PATTERN.containsMatchIn(text)
        val hasRecurring = RECURRING_BILLING_PATTERN.containsMatchIn(text)
        val hasCost = HIDDEN_COST_PATTERN.containsMatchIn(text)

        val costMatch = HIDDEN_COST_PATTERN.find(text)?.value ?: ""

        return when {
            hasFreeTrial && hasRecurring -> {
                RiskReport(
                    isDeceptive = true,
                    headline = if (isTimeout) "Subscription Auto-Renewal Detected" else "Free Trial Automatically Renews",
                    termsDetail = if (costMatch.isNotBlank()) {
                        "Disguised trial converts into recurring billing of $costMatch via UPI AutoPay."
                    } else {
                        "Disguised trial indicates recurring recurring charges after trial period."
                    },
                    riskScore = 88
                )
            }
            hasRecurring && hasCost -> {
                RiskReport(
                    isDeceptive = true,
                    headline = "Recurring AutoPay Mandate Detected",
                    termsDetail = "Application presents recurring billing terms of $costMatch.",
                    riskScore = 78
                )
            }
            hasRecurring -> {
                RiskReport(
                    isDeceptive = true,
                    headline = "Automatic Renewal Terms Found",
                    termsDetail = "Screen contains terms indicating continuous subscription billing.",
                    riskScore = 72
                )
            }
            else -> {
                RiskReport.SAFE
            }
        }
    }
}
