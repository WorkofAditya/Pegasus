package com.pegasus.modules

import android.content.Context
import android.media.MediaRecorder
import com.pegasus.core.PegasusModule
import org.json.JSONObject
import java.io.File
import java.util.Base64

class AudioModule(private val context: Context) : PegasusModule {
    override val name = "audio"
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action")
        val duration = payload.optInt("duration", 30) // seconds
        val outputFormat = payload.optString("format", "mp4")

        return when (action) {
            "start" -> startRecording(outputFormat)
            "stop" -> stopRecording()
            "record" -> recordForDuration(duration, outputFormat)
            else -> JSONObject().put("error", "Unknown audio action")
        }
    }

    private fun startRecording(format: String): JSONObject {
        if (isRecording) return JSONObject().put("error", "Already recording")

        try {
            val dir = File(context.getExternalFilesDir(null), "recordings")
            dir.mkdirs()
            val file = File(dir, "recording_${System.currentTimeMillis()}.$format")

            recorder = createRecorder()
            recorder?.setOutputFile(file.absolutePath)
            recorder?.prepare()
            recorder?.start()
            isRecording = true
            return JSONObject().put("success", true).put("file_path", file.absolutePath)
        } catch (e: Exception) {
            return JSONObject().put("error", e.message)
        }
    }

    private fun stopRecording(): JSONObject {
        if (!isRecording) return JSONObject().put("error", "Not recording")
        try {
            recorder?.stop()
            recorder?.release()
            isRecording = false
            return JSONObject().put("success", true)
        } catch (e: Exception) {
            return JSONObject().put("error", e.message)
        }
    }

    private fun recordForDuration(duration: Int, format: String): JSONObject {
        startRecording(format)
        Thread.sleep(duration.toLong() * 1000)
        stopRecording()
        // In a real scenario, you'd read the file and return it as base64
        return JSONObject().put("success", true).put("duration", duration)
    }

    private fun createRecorder(): MediaRecorder {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }
}