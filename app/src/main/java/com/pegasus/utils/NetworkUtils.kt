package com.pegasus.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import java.net.NetworkInterface
import java.security.MessageDigest

object NetworkUtils {

    fun getDeviceId(context: Context): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val model = android.os.Build.MODEL
        return hash("$androidId:$model")
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getBatteryLevel(context: Context): Int {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    fun isBatteryPlugged(context: Context): Boolean {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }

    @Suppress("DEPRECATION")
    fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetworkInfo
        return when {
            network == null || !network.isConnected -> "DISCONNECTED"
            network.type == ConnectivityManager.TYPE_WIFI -> "WIFI"
            network.type == ConnectivityManager.TYPE_MOBILE -> "MOBILE"
            else -> "UNKNOWN"
        }
    }

    @Suppress("DEPRECATION")
    fun getWifiSSID(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val ssid = wm.connectionInfo?.ssid
            if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                ssid.replace("\"", "")
            } else "NO_WIFI"
        } catch (e: SecurityException) {
            "PERMISSION_DENIED"
        }
    }

    fun getMacAddress(context: Context): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (intf.name.equals("wlan0", ignoreCase = true)) {
                    val hardware = intf.hardwareAddress
                    if (hardware != null) {
                        return hardware.joinToString(":") { "%02X".format(it) }
                    }
                }
            }
            "UNKNOWN"
        } catch (e: Exception) {
            "ERROR"
        }
    }
}
