package com.subzero.engine

import android.content.Context
import android.content.pm.PackageManager

data class InstalledUpiApp(
    val packageName: String,
    val appName: String,
    val vpaHandles: List<String>,
    val isInstalled: Boolean = true,
    val brandColorHex: Long = 0xFF1E5144
)

object InstalledUpiDetector {

    val KNOWN_UPI_APPS = listOf(
        InstalledUpiApp(
            packageName = "com.phonepe.app",
            appName = "PhonePe",
            vpaHandles = listOf("phonepe", "@ybl", "@axl", "@ibl"),
            brandColorHex = 0xFF5F259F
        ),
        InstalledUpiApp(
            packageName = "com.google.android.apps.nbu.paisa.user",
            appName = "Google Pay",
            vpaHandles = listOf("google pay", "gpay", "@okhdfc", "@okaxis", "@oksbi", "@okicici"),
            brandColorHex = 0xFF1A73E8
        ),
        InstalledUpiApp(
            packageName = "net.one97.paytm",
            appName = "Paytm",
            vpaHandles = listOf("paytm", "@paytm"),
            brandColorHex = 0xFF002E6E
        ),
        InstalledUpiApp(
            packageName = "in.org.npci.upiapp",
            appName = "BHIM UPI",
            vpaHandles = listOf("bhim", "@upi"),
            brandColorHex = 0xFF007A3D
        ),
        InstalledUpiApp(
            packageName = "com.dreamplug.androidapp",
            appName = "CRED",
            vpaHandles = listOf("cred", "@axisb", "@yesbank"),
            brandColorHex = 0xFF1A1A1A
        )
    )

    fun getInstalledUpiApps(context: Context): List<InstalledUpiApp> {
        val pm = context.packageManager
        val evaluated = KNOWN_UPI_APPS.map { app ->
            val installed = try {
                pm.getPackageInfo(app.packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
            app.copy(isInstalled = installed)
        }

        val actuallyInstalled = evaluated.filter { it.isInstalled }
        // On emulators or devices without native UPI apps installed,
        // return the complete set of apps to allow interactive testing & UI rendering
        return if (actuallyInstalled.isNotEmpty()) {
            actuallyInstalled
        } else {
            evaluated
        }
    }
}
