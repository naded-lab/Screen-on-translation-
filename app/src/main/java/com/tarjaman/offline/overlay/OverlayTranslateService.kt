package com.tarjaman.offline.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.tarjaman.offline.MainActivity
import com.tarjaman.offline.R
import com.tarjaman.offline.data.ocr.TextRecognitionRepository
import com.tarjaman.offline.data.translate.TranslationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * خدمة أمامية (Foreground Service) تدير:
 * 1) زراً عائماً صغيراً قابلاً للسحب فوق أي تطبيق آخر.
 * 2) عند الضغط عليه: التقاط لقطة واحدة من الشاشة الحالية فقط (وليس بثاً مستمراً لتوفير البطارية).
 * 3) قراءة النص داخل الفقاعات عبر OCR محلي، ثم ترجمته فوراً عبر محرك الترجمة الأوفلاين.
 * 4) رسم صناديق الترجمة فوق أماكن الفقاعات الأصلية بالضبط.
 *
 * لا يتم حفظ أي لقطة شاشة على القرص ولا إرسالها لأي خادم خارجي - كل المعالجة محلية ومؤقتة في الذاكرة.
 */
class OverlayTranslateService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_SOURCE_LANG = "extra_source_lang"
        const val EXTRA_TARGET_LANG = "extra_target_lang"
        const val ACTION_STOP = "com.tarjaman.offline.action.STOP_OVERLAY"
        const val ACTION_STOPPED = "com.tarjaman.offline.action.OVERLAY_STOPPED"
        private const val NOTIFICATION_CHANNEL_ID = "overlay_translate_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var translationJob: Job? = null

    private lateinit var windowManager: WindowManager
    private var floatingButton: View? = null
    private var overlayContainer: FrameLayout? = null

    private var mediaProjection: MediaProjection? = null
    private var captureManager: ScreenCaptureManager? = null

    private val textRecognitionRepository = TextRecognitionRepository()
    private val translationRepository = TranslationRepository()

    private var sourceLang = "en"
    private var targetLang = "ar"

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getParcelableExtraCompat(intent: Intent?, key: String): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(key, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(key)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val resultData = getParcelableExtraCompat(intent, EXTRA_RESULT_DATA)
        sourceLang = intent?.getStringExtra(EXTRA_SOURCE_LANG) ?: "en"
        targetLang = intent?.getStringExtra(EXTRA_TARGET_LANG) ?: "ar"

        startForeground(NOTIFICATION_ID, buildNotification())

        if (resultData != null && resultCode == android.app.Activity.RESULT_OK) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            val metrics = screenMetrics()
            mediaProjection?.let { captureManager = ScreenCaptureManager(it, metrics) }
            showFloatingButton()
        }

        return START_NOT_STICKY
    }

    /** يحصل على أبعاد الشاشة بالطريقة الحديثة (API 30+) مع بديل متوافق للخلف حتى API 26 */
    private fun screenMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = resources.configuration.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayTranslateService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.overlay_stop), stopIntent)
            .setOngoing(true)
            .build()
    }

    // ---------- الزر العائم القابل للسحب ----------

    @Suppress("ClickableViewAccessibility")
    private fun showFloatingButton() {
        if (floatingButton != null) return

        val button = LayoutInflater.from(this).let {
            TextView(this).apply {
                text = "\u0623" // رمز بسيط يدل على الترجمة (حرف عربي داخل دائرة عبر الخلفية)
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setBackgroundResource(android.R.drawable.btn_default)
            }
        }

        val size = (56 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size, size,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 300

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) captureAndTranslate()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(button, params)
        floatingButton = button
    }

    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    // ---------- التقاط الشاشة + OCR + ترجمة + رسم النتيجة ----------

    private fun captureAndTranslate() {
        translationJob?.cancel()
        translationJob = serviceScope.launch {
            clearOverlayBoxes()
            val bitmap = captureManager?.captureSingleFrame() ?: return@launch
            val blocks = textRecognitionRepository.recognize(bitmap)
            bitmap.recycle()

            blocks.forEach { block ->
                if (block.text.isBlank()) return@forEach
                val result = translationRepository.translate(block.text, sourceLang, targetLang)
                result.onSuccess { translated ->
                    if (translated.isNotBlank()) drawTranslationBox(block.boundingBox, translated)
                }
            }
        }
    }

    private fun drawTranslationBox(box: android.graphics.Rect, text: String) {
        ensureOverlayContainer()

        val textView = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(230, 20, 20, 20))
            textSize = 12f
            setPadding(8, 6, 8, 6)
            gravity = Gravity.CENTER
        }

        val params = FrameLayout.LayoutParams(box.width(), box.height())
        params.leftMargin = box.left
        params.topMargin = box.top

        overlayContainer?.addView(textView, params)
    }

    private fun ensureOverlayContainer() {
        if (overlayContainer != null) return
        val container = FrameLayout(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(container, params)
        overlayContainer = container
    }

    private fun clearOverlayBoxes() {
        overlayContainer?.removeAllViews()
    }

    override fun onDestroy() {
        translationJob?.cancel()
        floatingButton?.let { runCatching { windowManager.removeView(it) } }
        overlayContainer?.let { runCatching { windowManager.removeView(it) } }
        captureManager?.release()
        mediaProjection?.stop()
        textRecognitionRepository.close()
        translationRepository.closeAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        sendBroadcast(Intent(ACTION_STOPPED).setPackage(packageName))
        super.onDestroy()
    }
}
