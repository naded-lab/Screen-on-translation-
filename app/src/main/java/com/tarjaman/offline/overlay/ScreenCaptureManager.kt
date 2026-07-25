package com.tarjaman.offline.overlay

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * يلتقط إطاراً واحداً فقط من الشاشة عند الطلب (وليس بثاً مستمراً)، لتقليل استهلاك
 * البطارية والذاكرة إلى أدنى حد ممكن - مناسب جداً للأجهزة الضعيفة.
 * لا تُحفظ أي صورة على القرص ولا تُرسل لأي خادم؛ المعالجة بالكامل في ذاكرة التطبيق المؤقتة.
 */
class ScreenCaptureManager(
    private val mediaProjection: MediaProjection,
    private val metrics: DisplayMetrics
) {
    private val handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    suspend fun captureSingleFrame(): Bitmap? = suspendCancellableCoroutine { continuation ->
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null
        var resumed = false

        imageReader.setOnImageAvailableListener({ reader ->
            if (resumed) return@setOnImageAvailableListener
            val image = reader.acquireLatestImage()
            if (image != null) {
                resumed = true
                val bitmap = imageToBitmap(image.planes[0].buffer, image.planes[0].pixelStride, image.planes[0].rowStride, width, height)
                image.close()
                virtualDisplay?.release()
                imageReader.close()
                continuation.resume(bitmap)
            }
        }, handler)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "TarjamanCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, handler
        )

        continuation.invokeOnCancellation {
            virtualDisplay?.release()
            imageReader.close()
        }
    }

    private fun imageToBitmap(buffer: java.nio.ByteBuffer, pixelStride: Int, rowStride: Int, width: Int, height: Int): Bitmap {
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    fun release() {
        handlerThread.quitSafely()
    }
}
