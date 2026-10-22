package com.pegasus.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pegasus.utils.LogUtils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LogUtils.i("BootReceiver triggered: ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            "android.intent.action.LOCKED_BOOT_COMPLETED" -> {
                val serviceIntent = Intent(context, PegasusService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}