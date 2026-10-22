package com.pegasus.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

object DeviceUtils {

    fun getDeviceModel(): String = Build.MODEL
    fun getManufacturer(): String = Build.MANUFACTURER
    fun getAndroidVersion(): String = Build.VERSION.RELEASE
    fun getSdkInt(): Int = Build.VERSION.SDK_INT

    fun getTotalStorage(context: Context): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockSizeLong * stat.blockCountLong
    }

    fun getAvailableStorage(context: Context): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    fun isExternalStorageAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun getExternalStoragePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }
}