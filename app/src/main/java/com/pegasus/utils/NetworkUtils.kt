package com.pegasus.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.UUID

object NetworkUtils {

    fun getDeviceId(context: Context): String {
        // Generate a stable device ID based on Android ID and hardware
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val serial = android.os.Build.SERIAL
        val model = android.os.Build.MODEL
        val raw = "$androidId:$serial:$model"
        return hash(raw)
    }

    private fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getBatteryLevel(context: Context): Int {
        val battery = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level.toDouble() / scale * 100).toInt()
    }

    fun isBatteryPlugged(context: Context): Boolean {
        val battery = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged != BatteryManager.BATTERY_PLUGGED_UNPLUGGED
    }

    fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetworkInfo
        return when {
            network == null -> "DISCONNECTED"
            network.type == ConnectivityManager.TYPE_WIFI -> "WIFI"
            network.type == ConnectivityManager.TYPE_MOBILE -> "MOBILE"
            else -> "UNKNOWN"
        }
    }

    fun getWifiSSID(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            val info = wm.connectionInfo
            if (info != null && info.ssid != null && info.ssid != "<unknown ssid>") {
                info.ssid.replace("\"", "")
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
                        return hardware.joinToString(":") { String.format("%02X", it) }
                    }
                }
            }
            "UNKNOWN"
        } catch (e: Exception) {
            "ERROR"
        }
    }
}