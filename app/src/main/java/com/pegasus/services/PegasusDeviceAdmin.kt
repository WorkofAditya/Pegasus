package com.pegasus.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.pegasus.utils.LogUtils

class PegasusDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        LogUtils.i("Pegasus Device Admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        LogUtils.i("Pegasus Device Admin disabled")
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        LogUtils.i("Password changed")
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        LogUtils.i("Password failed")
    }

    override fun onLockNow(context: Context, intent: Intent) {
        LogUtils.i("Lock now triggered")
    }

    override fun onSecureLockNow(context: Context, intent: Intent) {
        LogUtils.i("Secure lock now triggered")
    }

    override fun onKeyguardLocked(context: Context) {
        LogUtils.i("Keyguard locked")
    }

    override fun onKeyguardUnlocked(context: Context) {
        LogUtils.i("Keyguard unlocked")
    }
}