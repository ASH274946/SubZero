package com.subzero.security

import android.view.WindowManager

/**
 * Security policy enforcer for fail-closed checks, overlay safety flags, and privacy constraints.
 */
object SecurityPolicy {

    /**
     * Checks if accessibility inspection is permitted for the given package.
     * Fail-closed: returns false if package name is null or blank, or if it matches a protected package.
     */
    fun shouldAllowAccessibilityInspection(packageName: CharSequence?): Boolean {
        if (packageName.isNullOrBlank()) {
            return false // Fail-closed
        }
        val pkgStr = packageName.toString()
        if (ProtectedPackages.isProtected(pkgStr)) {
            return false
        }
        return true
    }

    /**
     * Verifies that the given WindowManager.LayoutParams strictly satisfy the non-touchable
     * and non-focusable overlay requirements to guarantee zero touch interception.
     */
    fun validateOverlayFlags(flags: Int): Boolean {
        val hasNotTouchable = (flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0
        val hasNotFocusable = (flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0
        return hasNotTouchable && hasNotFocusable
    }
}
