package com.pegasus.modules

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pegasus.core.PegasusModule
import org.json.JSONObject

class NotificationModule(private val context: Context) : PegasusModule {
    override val name = "notification"
    private lateinit var notificationManager: NotificationManager
    private val CHANNEL_ID = "PEGASUS_CHANNEL"

    init {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pegasus Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical alerts from Pegasus"
            enableLights(true)
            lightColor = 0xFF0000
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action")
        val title = payload.optString("title", "Pegasus")
        val message = payload.optString("message", "Alert")
        val channelId = payload.optString("channel_id", CHANNEL_ID)
        val sound = payload.optBoolean("sound", true)
        val vibrate = payload.optBoolean("vibrate", true)
        val pendingIntentJson = payload.optJSONObject("pending_intent")

        return when (action) {
            "post" -> postNotification(title, message, channelId, sound, vibrate, pendingIntentJson)
            "cancel" -> cancelNotification(payload.optInt("id"))
            "cancel_all" -> cancelAllNotifications()
            "list_channels" -> listChannels()
            else -> JSONObject().put("error", "Unknown notification action")
        }
    }

    private fun postNotification(
        title: String,
        message: String,
        channelId: String,
        sound: Boolean,
        vibrate: Boolean,
        pendingIntentJson: JSONObject?
    ): JSONObject {
        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)

            if (sound) builder.setDefaults(Notification.DEFAULT_SOUND)
            if (vibrate) builder.setVibrate(longArrayOf(0, 500, 250, 500))

            // Handle pending intent if provided
            pendingIntentJson?.let { piJson ->
                val piType = piJson.optString("type", "activity")
                val piComponent = piJson.optString("component", "com.pegasus/.ui.PegasusActivity")
                val intent = Intent()
                intent.setClassName(piComponent.substringBefore('/'), piComponent.substringAfter('/'))
                val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    PendingIntent.FLAG_IMMUTABLE else 0
                val pi = PendingIntent.getActivity(context, 0, intent, flags)
                builder.setContentIntent(pi)
            }

            val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            notificationManager.notify(notificationId, builder.build())
            return JSONObject().put("success", true).put("notification_id", notificationId)
        } catch (e: Exception) {
            return JSONObject().put("error", e.message)
        }
    }

    private fun cancelNotification(id: Int): JSONObject {
        if (id != 0) {
            notificationManager.cancel(id)
            return JSONObject().put("success", true)
        }
        return JSONObject().put("error", "Invalid notification ID")
    }

    private fun cancelAllNotifications(): JSONObject {
        notificationManager.cancelAll()
        return JSONObject().put("success", true)
    }

    private fun listChannels(): JSONObject {
        val channels = org.json.JSONArray()
        notificationManager.getNotificationChannels()?.forEach { channel ->
            channels.put(JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name?.toString())
                put("importance", channel.importance)
            })
        }
        return JSONObject().put("channels", channels)
    }
}