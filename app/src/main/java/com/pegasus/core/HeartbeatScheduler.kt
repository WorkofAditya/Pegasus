package com.pegasus.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.pegasus.utils.LogUtils

class HeartbeatScheduler(
    private val context: Context,
    private val c2Client: C2Client
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private val INTERVAL_MS = 30000L // 30 seconds

    fun start() {
        if (isRunning) return
        isRunning = true
        LogUtils.i("Heartbeat scheduler started")
        scope.launch {
            while (isRunning) {
                try {
                    c2Client.sendHeartbeat()
                } catch (e: Exception) {
                    LogUtils.e("Heartbeat failed: ${e.message}")
                }
                delay(INTERVAL_MS)
            }
        }
    }

    fun stop() {
        isRunning = false
        LogUtils.i("Heartbeat scheduler stopped")
    }
}