package com.subzero.security

/**
 * Centralized registry of protected Banking, UPI, and Financial packages.
 * SubZero MUST NOT inspect the accessibility hierarchy or extract text from these applications.
 */
object ProtectedPackages {

    val packages: Set<String> = setOf(
        // Core UPI Applications
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        
        // Banking Applications
        "com.sbi.upi",
        "com.csam.icici.bank.imobile",
        "com.icicibank.pockets",
        "com.hdfcbank.payzapp",
        "com.snapwork.hdfc",
        "com.axis.mobile",
        "com.kotak.kotakbank",
        "com.yono.sbi",
        
        // SubZero own package (Fail-closed self-inspection prevention)
        "com.subzero"
    )

    /**
     * Checks if a package name belongs to the protected set.
     */
    fun isProtected(packageName: String): Boolean {
        val normalized = packageName.trim().lowercase()
        return packages.any { it.equals(normalized, ignoreCase = true) }
    }
}
