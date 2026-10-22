package com.pegasus.modules

import android.content.Context
import com.pegasus.core.PegasusModule
import org.json.JSONObject

/**
 * Screen capture is intentionally unavailable in this build. Android screen capture
 * requires a user-approved MediaProjection session and a valid foreground service.
 */
class ScreenModule(private val context: Context) : PegasusModule {
    override val name = "screen"

    override fun execute(payload: JSONObject): JSONObject {
        return when (payload.optString("action", "capture")) {
            "stop" -> JSONObject().put("success", true).put("message", "No screen-capture session is active")
            "capture" -> JSONObject().put("error", "Screen capture is unavailable until a user-approved MediaProjection flow is implemented")
            else -> JSONObject().put("error", "Unknown screen action")
        }
    }
}
