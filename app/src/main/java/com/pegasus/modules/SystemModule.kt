package com.pegasus.modules

import android.content.Context
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.pegasus.core.PegasusModule
import org.json.JSONObject

class SystemModule(private val context: Context) : PegasusModule {
    override val name = "system"

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action", "info")
        return when (action) {
            "info" -> getSystemInfo()
            "battery" -> getBatteryStatus()
            "storage" -> getStorageInfo()
            "processes" -> getRunningProcesses()
            else -> JSONObject().put("error", "Unknown system action")
        }
    }

    private fun getSystemInfo(): JSONObject {
        return JSONObject().apply {
            put("device", android.os.Build.DEVICE)
            put("manufacturer", android.os.Build.MANUFACTURER)
            put("model", android.os.Build.MODEL)
            put("brand", android.os.Build.BRAND)
            put("android_version", android.os.Build.VERSION.RELEASE)
            put("sdk_int", android.os.Build.VERSION.SDK_INT)
            put("build_id", android.os.Build.ID)
            put("cpu_abi", android.os.Build.SUPPORTED_ABIS[0])
            put("total_ram", getTotalRam())
            put("available_ram", getAvailableRam())
        }
    }

    private fun getBatteryStatus(): JSONObject {
        val battery = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        return JSONObject().apply {
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val temp = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

            put("level", (level.toDouble() / scale * 100).toInt())
            put("plugged", plugged)
            put("status", status)
            put("temperature", temp)
        }
    }

    private fun getStorageInfo(): JSONObject {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val total = stat.blockSizeLong * stat.blockCountLong
        val free = stat.blockSizeLong * stat.availableBlocksLong
        return JSONObject().apply {
            put("total_bytes", total)
            put("free_bytes", free)
            put("used_bytes", total - free)
        }
    }

    private fun getRunningProcesses(): JSONObject {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val processes = activityManager.runningAppProcesses
        val procArray = org.json.JSONArray()
        processes?.forEach { proc ->
            procArray.put(JSONObject().apply {
                put("pid", proc.pid)
                put("process_name", proc.processName)
                put("importance", proc.importance)
            })
        }
        return JSONObject().put("processes", procArray)
    }

    private fun getTotalRam(): Long {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        am.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    private fun getAvailableRam(): Long {
        val memInfo = android.app.ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        am.getMemoryInfo(memInfo)
        return memInfo.availMem
    }
}