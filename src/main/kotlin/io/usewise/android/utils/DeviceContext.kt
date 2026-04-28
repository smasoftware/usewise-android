package io.usewise.android.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.DisplayMetrics
import java.io.File

data class DeviceContext(
    val deviceOs: String,
    val deviceModel: String,
    val appVersion: String,
    val isVpn: Boolean,
    val isRooted: Boolean,
    val screenWidth: Int,
    val screenHeight: Int,
) {
    companion object {
        fun capture(context: Context): DeviceContext {
            val os = "Android ${Build.VERSION.RELEASE}"
            val model = "${Build.MANUFACTURER} ${Build.MODEL}"

            val version = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
            } catch (_: Exception) { "0.0.0" }

            val vpn = try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val net = cm.activeNetwork
                val caps = net?.let { cm.getNetworkCapabilities(it) }
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } catch (_: Exception) { false }

            val rooted = arrayOf("/su", "/system/xbin/su", "/system/bin/su", "/data/local/bin/su", "/data/local/xbin/su")
                .any { File(it).exists() }

            val dm = context.resources.displayMetrics
            return DeviceContext(
                deviceOs = os,
                deviceModel = model,
                appVersion = version,
                isVpn = vpn,
                isRooted = rooted,
                screenWidth = (dm.widthPixels / dm.density).toInt(),
                screenHeight = (dm.heightPixels / dm.density).toInt(),
            )
        }
    }
}
