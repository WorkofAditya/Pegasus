package com.pegasus.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pegasus.utils.LogUtils

/**
 * Notification access is sensitive. This service logs lifecycle events only and does
 * not transmit notification content or message previews to a remote endpoint.
 */
class NotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        LogUtils.i("Notification posted by package: ${sbn.packageName}")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        LogUtils.i("Notification removed from package: ${sbn.packageName}")
    }
}
