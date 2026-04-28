package io.usewise.android.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.net.NetworkInterface

data class DeviceInfo(
    val deviceOs: String,
    val deviceModel: String,
    val appVersion: String,
    val isVpn: Boolean,
    val isRooted: Boolean,
    val screenWidth: Int,
    val screenHeight: Int,
)

object DeviceContext {
    fun capture(context: Context): DeviceInfo {
        val appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)

        return DeviceInfo(
            deviceOs = "Android ${Build.VERSION.RELEASE}",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            appVersion = appVersion,
            isVpn = detectVpn(),
            isRooted = detectRoot(),
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
        )
    }

    private fun detectVpn(): Boolean {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.any {
                it.isUp && (it.name.startsWith("tun") || it.name.startsWith("pptp") || it.name.startsWith("ppp"))
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun detectRoot(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
        )
        return paths.any { File(it).exists() }
    }
}
