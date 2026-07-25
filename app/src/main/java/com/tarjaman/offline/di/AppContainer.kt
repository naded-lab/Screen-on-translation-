package com.tarjaman.offline.di

import android.content.Context
import com.tarjaman.offline.data.history.HistoryRepository
import com.tarjaman.offline.data.prefs.PreferencesManager
import com.tarjaman.offline.data.translate.TranslationRepository

/**
 * حاوية اعتمادات يدوية بسيطة (Manual Dependency Injection).
 * تم تفضيلها على Hilt/Dagger عمداً لأن حجم المشروع لا يبرر تعقيد وحجم build إضافي
 * من معالجات annotation processing، وهذا يخدم مباشرة هدف "تقليل حجم APK وتسريع الإقلاع".
 */
class AppContainer(context: Context) {
    val preferencesManager: PreferencesManager by lazy { PreferencesManager(context) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(context) }
    val translationRepository: TranslationRepository by lazy { TranslationRepository() }
}
