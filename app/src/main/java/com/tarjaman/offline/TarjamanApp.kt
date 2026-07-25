package com.tarjaman.offline

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.tarjaman.offline.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * كلاس التطبيق الرئيسي.
 * مسؤول عن التهيئة المبكرة: تطبيق لغة الواجهة المحفوظة قبل أي رسم للشاشة،
 * لتفادي أي وميض (flicker) بلغة خاطئة عند بدء التشغيل، وعن إنشاء حاوية الاعتمادات.
 */
class TarjamanApp : Application() {

    // نطاق Coroutine على مستوى التطبيق لعمليات التهيئة الخفيفة فقط
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applySavedLocaleSync()
    }

    /**
     * نطبّق اللغة المحفوظة (أو العربية كافتراضي) عبر AppCompatDelegate
     * وهو المسار الموصى به رسمياً لدعم "لغة لكل تطبيق" بشكل متوافق للخلف حتى Android 8.
     */
    private fun applySavedLocaleSync() {
        appScope.launch {
            val savedLang = container.preferencesManager.appLanguageFlow.first()
            val locales = LocaleListCompat.forLanguageTags(savedLang)
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    override fun onTerminate() {
        container.translationRepository.closeAll()
        super.onTerminate()
    }
}
