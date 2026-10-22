package com.pegasus.core

import android.content.Context
import org.json.JSONObject

class CommandDispatcher(private val context: Context) {

    private val modules = mutableMapOf<String, (JSONObject) -> JSONObject>()

    fun registerModule(module: PegasusModule) {
        // Modules register themselves by name
        modules[module.name] = { payload -> module.execute(payload) }
    }

    fun execute(commandType: String, payload: JSONObject): JSONObject {
        val handler = modules[commandType]
        return if (handler != null) {
            handler(payload)
        } else {
            JSONObject().put("error", "Unknown command type: $commandType")
        }
    }
}

interface PegasusModule {
    val name: String
    fun execute(payload: JSONObject): JSONObject
}