package com.pegasus.utils

import android.util.Log

object LogUtils {
    private const val TAG = "Pegasus"
    private var enabled = true

    fun init(tag: String) {
        this.TAG = tag
    }

    fun i(message: String) {
        if (enabled) Log.i(TAG, message)
    }

    fun e(message: String) {
        if (enabled) Log.e(TAG, message)
    }

    fun d(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }
}