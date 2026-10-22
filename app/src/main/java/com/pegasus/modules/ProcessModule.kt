package com.pegasus.modules

import android.app.ActivityManager
import android.content.Context
import com.pegasus.core.PegasusModule
import org.json.JSONObject

class ProcessModule(private val context: Context) : PegasusModule {
    override val name = "process"

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action")
        val pid = payload.optInt("pid")
        val processName = payload.optString("process_name")

        return when (action) {
            "list" -> listProcesses()
            "kill" -> killProcess(pid, processName)
            else -> JSONObject().put("error", "Unknown process action")
        }
    }

    private fun listProcesses(): JSONObject {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = org.json.JSONArray()
        am.runningAppProcesses?.forEach { proc ->
            processes.put(JSONObject().apply {
                put("pid", proc.pid)
                put("name", proc.processName)
                put("importance", proc.importance)
            })
        }
        return JSONObject().put("processes", processes)
    }

    private fun killProcess(pid: Int, processName: String): JSONObject {
        return try {
            if (pid > 0) {
                android.os.Process.killProcess(pid)
            } else if (processName.isNotEmpty()) {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.runningAppProcesses?.firstOrNull { it.processName == processName }?.let {
                    android.os.Process.killProcess(it.pid)
                }
            }
            JSONObject().put("success", true)
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }
}