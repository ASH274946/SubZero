package com.subzero.ai

/**
 * Centralized configuration constants for the NLP/SLM pipeline, timeouts, and thresholds.
 */
object ModelConfig {

    /**
     * Target timeout for local SLM inference in milliseconds.
     */
    const val MODEL_TIMEOUT_MS: Long = 400L

    /**
     * Debounce duration for window content / state accessibility events in milliseconds.
     */
    const val ACCESSIBILITY_DEBOUNCE_MS: Long = 500L

    /**
     * Minimum risk score (0-100) required to trigger the warning overlay.
     */
    const val DEFAULT_RISK_THRESHOLD: Int = 70

    /**
     * Maximum character count extracted from the accessibility tree passed to the model.
     */
    const val MAX_MODEL_INPUT_CHARS: Int = 1000

    /**
     * Cooldown period in milliseconds before displaying a warning for identical content.
     */
    const val WARNING_COOLDOWN_MS: Long = 10_000L

    /**
     * Preferred development/demo model path.
     */
    const val EXTERNAL_MODEL_PATH: String = "/data/local/tmp/subzero/models/model.bin"

    /**
     * Application internal files directory model filename.
     */
    const val INTERNAL_MODEL_FILENAME: String = "subzero_slm.bin"

    /**
     * Maximum consecutive model failures before the circuit breaker trips.
     */
    const val MAX_CONSECUTIVE_FAILURES: Int = 3

    /**
     * Reset cooldown for the circuit breaker in milliseconds.
     */
    const val CIRCUIT_BREAKER_RESET_MS: Long = 30_000L
}
