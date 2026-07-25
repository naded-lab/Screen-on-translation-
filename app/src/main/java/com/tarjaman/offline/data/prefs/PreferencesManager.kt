package com.tarjaman.offline.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tarjaman_prefs")

/** أوضاع المظهر المتاحة */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * مدير تفضيلات المستخدم، مبني على Jetpack DataStore (بديل خفيف وحديث لـ SharedPreferences
 * يعمل بشكل غير متزامن وآمن من تلف البيانات).
 */
class PreferencesManager(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_KEY = stringPreferencesKey("font_key")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SOURCE_LANG = stringPreferencesKey("last_source_lang")
        val LAST_TARGET_LANG = stringPreferencesKey("last_target_lang")
    }

    /** العربية هي اللغة الافتراضية للتطبيق كما طُلب صراحة */
    val appLanguageFlow: Flow<String> =
        store.data.map { it[Keys.APP_LANGUAGE] ?: "ar" }

    suspend fun setAppLanguage(languageTag: String) {
        store.edit { it[Keys.APP_LANGUAGE] = languageTag }
    }

    val themeModeFlow: Flow<ThemeMode> =
        store.data.map { prefs ->
            when (prefs[Keys.THEME_MODE]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /** المفتاح الافتراضي لخط "Cairo" لأنه من أوضح الخطوط العربية الحديثة للقراءة */
    val fontKeyFlow: Flow<String> =
        store.data.map { it[Keys.FONT_KEY] ?: "cairo" }

    suspend fun setFontKey(key: String) {
        store.edit { it[Keys.FONT_KEY] = key }
    }

    /** معامل تكبير الخط: 0.85 إلى 1.4 */
    val fontScaleFlow: Flow<Float> =
        store.data.map { it[Keys.FONT_SCALE] ?: 1.0f }

    suspend fun setFontScale(scale: Float) {
        store.edit { it[Keys.FONT_SCALE] = scale }
    }

    val wifiOnlyDownloadFlow: Flow<Boolean> =
        store.data.map { it[Keys.WIFI_ONLY_DOWNLOAD] ?: true }

    suspend fun setWifiOnlyDownload(enabled: Boolean) {
        store.edit { it[Keys.WIFI_ONLY_DOWNLOAD] = enabled }
    }

    val lastSourceLangFlow: Flow<String> =
        store.data.map { it[Keys.LAST_SOURCE_LANG] ?: "ar" }

    val lastTargetLangFlow: Flow<String> =
        store.data.map { it[Keys.LAST_TARGET_LANG] ?: "en" }

    suspend fun setLastLanguagePair(source: String, target: String) {
        store.edit {
            it[Keys.LAST_SOURCE_LANG] = source
            it[Keys.LAST_TARGET_LANG] = target
        }
    }
}
