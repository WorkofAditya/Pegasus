package com.pegasus.utils

import android.util.Log

object LogUtils {
    private var tag = "Pegasus"
    private var enabled = true

    fun init(tag: String) {
        this.tag = tag
    }

    fun i(message: String) {
        if (enabled) Log.i(tag, message)
    }

    fun e(message: String) {
        if (enabled) Log.e(tag, message)
    }

    fun d(message: String) {
        if (enabled) Log.d(tag, message)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }
}
