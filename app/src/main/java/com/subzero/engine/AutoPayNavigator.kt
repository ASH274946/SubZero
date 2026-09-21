package com.subzero.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Coordinates automated direct navigation to the AutoPay / Mandates screen
 * within third-party UPI applications (PhonePe, Google Pay) using PaywallAccessibilityService.
 */
object AutoPayNavigator {

    var pendingPackage: String? = null
        private set

    @Volatile
    var service: AccessibilityService? = null

    private var navigationStep = 0
    private var lastActionTime = 0L
    private var startTime = 0L
    private var swipeAttempts = 0

    private const val TIMEOUT_MS = 25_000L // 25s timeout
    private const val ACTION_COOLDOWN_MS = 750L // 750ms between actions

    fun startNavigation(packageName: String) {
        pendingPackage = packageName
        navigationStep = 1
        startTime = System.currentTimeMillis()
        lastActionTime = 0L
        swipeAttempts = 0
    }

    fun clear() {
        pendingPackage = null
        navigationStep = 0
        swipeAttempts = 0
    }

    /**
     * Inspects the active window of the target UPI app and performs automated clicks/gestures
     * to navigate directly to the AutoPay settings screen.
     */
    fun onAccessibilityEvent(rootNode: AccessibilityNodeInfo, currentPackage: String) {
        if (pendingPackage == null || pendingPackage != currentPackage) return

        val now = System.currentTimeMillis()
        if (now - startTime > TIMEOUT_MS) {
            clear()
            return
        }

        if (now - lastActionTime < ACTION_COOLDOWN_MS) return

        when (currentPackage) {
            "com.phonepe.app" -> handlePhonePeNavigation(rootNode, now)
            "com.google.android.apps.nbu.paisa.user" -> handleGooglePayNavigation(rootNode, now)
        }
    }

    private fun handlePhonePeNavigation(rootNode: AccessibilityNodeInfo, now: Long) {
        // Condition 1: Are we ALREADY on the AutoPay Screen?
        // The AutoPay screen in PhonePe has tabs "Ongoing" and "Archived", and displays active mandates.
        val hasOngoingTab = findNodeByText(rootNode, "Ongoing") != null
        val hasArchivedTab = findNodeByText(rootNode, "Archived") != null
        val hasAutoPayActiveText = searchRecursively(rootNode, listOf("AutoPay active", "AutoPay paused")) != null

        if ((hasOngoingTab && hasArchivedTab) || hasAutoPayActiveText) {
            clear() // Successfully reached destination!
            return
        }

        // Condition 2: Is the "AutoPay" menu row visible on screen?
        // (In Profile / Payment Settings screen, there is a row with text "AutoPay" and subtitle "Manage your IPOs...")
        val autoPayNode = findAutoPaySettingsNode(rootNode)
        if (autoPayNode != null) {
            clickOrTapNode(autoPayNode)
            lastActionTime = now
            navigationStep = 3
            return
        }

        // Condition 3: Are we in the Profile Screen?
        // ProfileActivity has headers like "Payment Settings", "Payment Methods", "Suggested for you", etc.
        val isProfileScreen = isPhonePeProfileScreen(rootNode)
        if (isProfileScreen) {
            // AutoPay is below the fold! We need to scroll down (swipe up).
            if (swipeAttempts < 4) {
                swipeAttempts++
                swipeUp()
                lastActionTime = now
            }
            return
        }

        // Condition 4: We are on PhonePe's Home Screen (or another bottom tab)
        // Check if Home tab is not currently selected
        val homeTabNode = findNodeById(rootNode, "bottom_nav_Home_tag")
            ?: findNodeByText(rootNode, "Home")
        if (homeTabNode != null && !homeTabNode.isSelected) {
            clickOrTapNode(homeTabNode)
            lastActionTime = now
            return
        }

        // Click top-left Profile icon
        val profileNode = findNodeByTextOrDesc(rootNode, listOf("Profile", "My Profile", "User profile"))
            ?: findNodeById(rootNode, "com.phonepe.app:id/iv_profile")
            ?: findNodeById(rootNode, "com.phonepe.app:id/action_profile")

        if (profileNode != null) {
            clickOrTapNode(profileNode)
            lastActionTime = now
            navigationStep = 2
        } else {
            // Fallback tap on Profile avatar standard screen coordinates (approx x=86, y=202)
            tapAt(86f, 202f)
            lastActionTime = now
            navigationStep = 2
        }
    }

    private fun findAutoPaySettingsNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val list = rootNode.findAccessibilityNodeInfosByText("AutoPay")
        if (!list.isNullOrEmpty()) {
            for (node in list) {
                if (node.isVisibleToUser) {
                    return node
                }
            }
        }
        return searchRecursively(rootNode, listOf("Manage your IPOs", "AutoPay Settings"))
    }

    private fun isPhonePeProfileScreen(rootNode: AccessibilityNodeInfo): Boolean {
        val indicators = listOf(
            "Payment Settings",
            "Payment Methods",
            "Bank Accounts",
            "Suggested for you",
            "Receiving money on PhonePe",
            "Help and support",
            "About PhonePe"
        )
        return findNodeByTextOrDesc(rootNode, indicators) != null
    }

    private fun handleGooglePayNavigation(rootNode: AccessibilityNodeInfo, now: Long) {
        // Check if already on Autopay screen
        val hasActiveTab = findNodeByText(rootNode, "Active") != null || findNodeByText(rootNode, "Live") != null
        val hasAutopayTitle = findNodeByText(rootNode, "Autopay") != null
        if (hasActiveTab && hasAutopayTitle) {
            clear()
            return
        }

        // Check if Autopay row is visible in settings/profile
        val autoPayNode = findNodeByTextOrDesc(rootNode, listOf("Autopay", "AutoPay", "Manage autopay"))
        if (autoPayNode != null) {
            clickOrTapNode(autoPayNode)
            lastActionTime = now
            navigationStep = 3
            return
        }

        // Check if on Profile screen
        val isProfileScreen = findNodeByTextOrDesc(rootNode, listOf("Google Account", "Manage Google Account", "Settings", "Help & feedback")) != null
        if (isProfileScreen) {
            if (swipeAttempts < 3) {
                swipeAttempts++
                swipeUp()
                lastActionTime = now
            }
            return
        }

        // On Home screen: tap Profile avatar at top-right
        val profileNode = findNodeByTextOrDesc(rootNode, listOf("Profile", "Account", "Google Account"))
            ?: findNodeById(rootNode, "com.google.android.apps.nbu.paisa.user:id/avatar")
            ?: findNodeById(rootNode, "com.google.android.apps.nbu.paisa.user:id/og_apd_internal_image_view")

        if (profileNode != null) {
            clickOrTapNode(profileNode)
            lastActionTime = now
            navigationStep = 2
        }
    }

    private fun clickOrTapNode(node: AccessibilityNodeInfo) {
        val clickableNode = getClickableAncestor(node) ?: node
        clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Dispatch physical touch coordinate tap for Compose nodes
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            tapAt(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
    }

    fun tapAt(x: Float, y: Float) {
        val svc = service ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0, 50)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            svc.dispatchGesture(gesture, null, null)
        }
    }

    fun swipeUp() {
        val svc = service ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metrics = svc.resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()
            val startX = width / 2f
            val startY = height * 0.70f
            val endY = height * 0.25f

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }
            val stroke = GestureDescription.StrokeDescription(path, 0, 280)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            svc.dispatchGesture(gesture, null, null)
        }
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val list = root.findAccessibilityNodeInfosByText(text)
        return list?.firstOrNull { it.isVisibleToUser && (it.text?.toString().equals(text, ignoreCase = true) || it.contentDescription?.toString().equals(text, ignoreCase = true)) }
    }

    private fun findNodeByTextOrDesc(root: AccessibilityNodeInfo, targets: List<String>): AccessibilityNodeInfo? {
        for (target in targets) {
            val list = root.findAccessibilityNodeInfosByText(target)
            if (!list.isNullOrEmpty()) {
                for (node in list) {
                    if (node.isVisibleToUser) return node
                }
            }
        }
        return searchRecursively(root, targets)
    }

    private fun searchRecursively(node: AccessibilityNodeInfo, targets: List<String>): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        for (target in targets) {
            if (desc.contains(target, ignoreCase = true) || text.contains(target, ignoreCase = true)) {
                if (node.isVisibleToUser) return node
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchRecursively(child, targets)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeById(root: AccessibilityNodeInfo, viewId: String): AccessibilityNodeInfo? {
        val list = root.findAccessibilityNodeInfosByViewId(viewId)
        return list?.firstOrNull { it.isVisibleToUser }
    }

    private fun getClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }
}
