package com.tarjaman.offline.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarjaman.offline.data.prefs.PreferencesManager
import com.tarjaman.offline.data.prefs.ThemeMode
import com.tarjaman.offline.data.translate.ModelState
import com.tarjaman.offline.data.translate.TranslationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appLanguage: String = "ar",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontKey: String = "system_default",
    val fontScale: Float = 1.0f,
    val wifiOnlyDownload: Boolean = true,
    val arabicModelState: ModelState = ModelState.NotDownloaded,
    val englishModelState: ModelState = ModelState.NotDownloaded
)

class SettingsViewModel(
    private val preferences: PreferencesManager,
    private val translationRepository: TranslationRepository
) : ViewModel() {

    private val _modelsState = MutableStateFlow(Pair<ModelState, ModelState>(ModelState.NotDownloaded, ModelState.NotDownloaded))

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.appLanguageFlow,
        preferences.themeModeFlow,
        preferences.fontKeyFlow,
        preferences.fontScaleFlow,
        preferences.wifiOnlyDownloadFlow,
        _modelsState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val lang = values[0] as String
        val theme = values[1] as ThemeMode
        val font = values[2] as String
        val scale = values[3] as Float
        val wifiOnly = values[4] as Boolean
        val models = values[5] as Pair<ModelState, ModelState>
        SettingsUiState(lang, theme, font, scale, wifiOnly, models.first, models.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        refreshModelsStatus()
    }

    private fun refreshModelsStatus() {
        viewModelScope.launch {
            val ar = translationRepository.isModelDownloaded("ar")
            val en = translationRepository.isModelDownloaded("en")
            _modelsState.value = Pair(
                if (ar) ModelState.Ready else ModelState.NotDownloaded,
                if (en) ModelState.Ready else ModelState.NotDownloaded
            )
        }
    }

    fun setAppLanguage(languageTag: String) {
        viewModelScope.launch { preferences.setAppLanguage(languageTag) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setFontKey(key: String) {
        viewModelScope.launch { preferences.setFontKey(key) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { preferences.setFontScale(scale) }
    }

    fun setWifiOnlyDownload(enabled: Boolean) {
        viewModelScope.launch { preferences.setWifiOnlyDownload(enabled) }
    }

    fun downloadModel(langCode: String) {
        viewModelScope.launch {
            translationRepository.downloadModel(langCode, uiState.value.wifiOnlyDownload).collect { state ->
                val current = _modelsState.value
                _modelsState.value = if (langCode == "ar") current.copy(first = state) else current.copy(second = state)
            }
        }
    }

    fun deleteModel(langCode: String) {
        viewModelScope.launch {
            translationRepository.deleteModel(langCode).onSuccess { refreshModelsStatus() }
        }
    }
}

private fun Pair<ModelState, ModelState>.copy(first: ModelState = this.first, second: ModelState = this.second) =
    Pair(first, second)
