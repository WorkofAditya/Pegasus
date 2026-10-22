package com.pegasus.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.pegasus.utils.LogUtils

class PegasusDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        LogUtils.i("Pegasus Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        LogUtils.i("Pegasus Device Admin disabled")
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
        LogUtils.i("Password changed")
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        LogUtils.i("Password failed")
    }
}
