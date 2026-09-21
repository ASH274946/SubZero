package com.subzero.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.subzero.ai.LocalModelEngine
import com.subzero.ai.ModelConfig
import com.subzero.engine.AutoPayNavigator
import com.subzero.security.SecurityPolicy
import com.subzero.ui.OverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Privacy-first Accessibility Ingestion Service.
 *
 * Implements:
 * 1. Fail-closed security boundary for protected banking/UPI applications.
 * 2. 500ms coroutine debouncing.
 * 3. Safe, bounded node tree traversal without password inspection.
 * 4. Lightweight Stage 1 Regex Gatekeeper.
 * 5. On-device Stage 2 SLM inference.
 * 6. Non-touchable warning HUD presentation.
 */
class PaywallAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null

    private var lastAnalyzedTextHash: Int = 0
    private var lastWarningTimestamp: Long = 0L

    companion object {
        /**
         * Stage 1 Regex Gatekeeper — Compiled once for ultra-fast matching (<2 ms).
         * Handles word-boundary keywords as well as slash-prefixed frequency tokens (/mo, /yr).
         */
        val STAGE_1_GATE_REGEX: Regex = Regex(
            "(?i)(?:\\b(trial|renews|autopay|mandate|billed annually|per month|e-mandate)\\b|/mo\\b|/yr\\b)"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AutoPayNavigator.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgNameStr = event.packageName?.toString() ?: ""
        val rootNode = try { rootInActiveWindow } catch (_: Exception) { null }
            ?: try { event.source } catch (_: Exception) { null }

        // 1. Check if the user is in an active Assisted AutoPay navigation sequence
        if (AutoPayNavigator.pendingPackage == pkgNameStr && rootNode != null) {
            AutoPayNavigator.service = this
            AutoPayNavigator.onAccessibilityEvent(rootNode, pkgNameStr)
            return
        }

        val packageName = event.packageName
        // Security Boundary: Check for protected banking / UPI applications immediately
        if (!SecurityPolicy.shouldAllowAccessibilityInspection(packageName)) {
            // Cancel any pending debounced analysis
            debounceJob?.cancel()
            // Dismiss warning overlay when user switches into a protected app
            OverlayManager.getInstance(applicationContext).dismissWarning()
            return
        }

        // Only process window state and content changes
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        // Debounce accessibility events (500 ms)
        debounceJob?.cancel()
        debounceJob = serviceScope.launch {
            delay(ModelConfig.ACCESSIBILITY_DEBOUNCE_MS)
            processActiveWindow()
        }
    }

    private suspend fun processActiveWindow() {
        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return

        try {
            // Re-verify package at root level
            val currentPkg = rootNode.packageName
            if (!SecurityPolicy.shouldAllowAccessibilityInspection(currentPkg)) {
                OverlayManager.getInstance(applicationContext).dismissWarning()
                return
            }

            // Extract visible text safely and bound length
            val extractedText = extractVisibleText(rootNode)
            if (extractedText.isBlank()) return

            val textHash = extractedText.hashCode()
            val now = System.currentTimeMillis()

            // Stage 1: Fast Regex Gatekeeper
            val isGatekeeperMatched = STAGE_1_GATE_REGEX.containsMatchIn(extractedText)
            if (!isGatekeeperMatched) {
                // If the screen no longer contains paywall triggers, dismiss stale overlay
                if (textHash != lastAnalyzedTextHash) {
                    lastAnalyzedTextHash = textHash
                    OverlayManager.getInstance(applicationContext).dismissWarning()
                }
                return
            }

            // Avoid re-analyzing identical screens within cooldown
            if (textHash == lastAnalyzedTextHash && (now - lastWarningTimestamp) < ModelConfig.WARNING_COOLDOWN_MS) {
                return
            }
            lastAnalyzedTextHash = textHash

            // Stage 2: Local SLM Risk Analysis
            val riskReport = LocalModelEngine.evaluateRisk(extractedText)

            if (riskReport.isDeceptive && riskReport.riskScore >= ModelConfig.DEFAULT_RISK_THRESHOLD) {
                lastWarningTimestamp = now
                OverlayManager.getInstance(applicationContext).showWarning(riskReport)
            } else {
                OverlayManager.getInstance(applicationContext).dismissWarning()
            }
        } finally {
            recycleNode(rootNode)
        }
    }

    /**
     * Traverses the node hierarchy recursively, extracting visible text while ignoring passwords.
     */
    private fun extractVisibleText(root: AccessibilityNodeInfo): String {
        val stringBuilder = StringBuilder()
        val visitedNodes = HashSet<Int>()

        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || stringBuilder.length >= ModelConfig.MAX_MODEL_INPUT_CHARS) return

            val nodeId = System.identityHashCode(node)
            if (visitedNodes.contains(nodeId)) return
            visitedNodes.add(nodeId)

            // Strictly skip password or private input fields
            if (node.isPassword) return

            if (node.isVisibleToUser) {
                val nodeText = node.text?.toString()?.trim()
                if (!nodeText.isNullOrBlank()) {
                    stringBuilder.append(nodeText).append(" ")
                }

                val contentDesc = node.contentDescription?.toString()?.trim()
                if (!contentDesc.isNullOrBlank() && contentDesc != nodeText) {
                    stringBuilder.append(contentDesc).append(" ")
                }
            }

            for (i in 0 until node.childCount) {
                if (stringBuilder.length >= ModelConfig.MAX_MODEL_INPUT_CHARS) break
                val child = try {
                    node.getChild(i)
                } catch (_: Exception) {
                    null
                }
                if (child != null) {
                    traverse(child)
                    recycleNode(child)
                }
            }
        }

        traverse(root)
        return stringBuilder.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun recycleNode(node: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                @Suppress("DEPRECATION")
                node.recycle()
            } catch (_: Exception) {
            }
        }
    }

    override fun onInterrupt() {
        debounceJob?.cancel()
        OverlayManager.getInstance(applicationContext).dismissWarning()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (AutoPayNavigator.service === this) {
            AutoPayNavigator.service = null
        }
        debounceJob?.cancel()
        OverlayManager.getInstance(applicationContext).dismissWarning()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (AutoPayNavigator.service === this) {
            AutoPayNavigator.service = null
        }
        debounceJob?.cancel()
        serviceScope.cancel()
        OverlayManager.getInstance(applicationContext).dismissWarning()
    }
}
