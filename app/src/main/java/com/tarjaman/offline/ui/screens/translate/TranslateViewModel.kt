package com.tarjaman.offline.ui.screens.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarjaman.offline.data.history.HistoryRepository
import com.tarjaman.offline.data.prefs.PreferencesManager
import com.tarjaman.offline.data.translate.ModelState
import com.tarjaman.offline.data.translate.TranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TranslateUiState(
    val sourceLang: String = "ar",
    val targetLang: String = "en",
    val inputText: String = "",
    val outputText: String = "",
    val isTranslating: Boolean = false,
    val sourceModelState: ModelState = ModelState.NotDownloaded,
    val targetModelState: ModelState = ModelState.NotDownloaded
)

class TranslateViewModel(
    private val repository: TranslationRepository,
    private val historyRepository: HistoryRepository,
    private val preferences: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TranslateUiState())
    val uiState: StateFlow<TranslateUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            val savedSource = preferences.lastSourceLangFlow.first()
            val savedTarget = preferences.lastTargetLangFlow.first()
            _uiState.value = _uiState.value.copy(sourceLang = savedSource, targetLang = savedTarget)
            refreshModelStatus()
        }
    }

    private fun refreshModelStatus() {
        viewModelScope.launch {
            val state = _uiState.value
            val sourceReady = repository.isModelDownloaded(state.sourceLang)
            val targetReady = repository.isModelDownloaded(state.targetLang)
            _uiState.value = _uiState.value.copy(
                sourceModelState = if (sourceReady) ModelState.Ready else ModelState.NotDownloaded,
                targetModelState = if (targetReady) ModelState.Ready else ModelState.NotDownloaded
            )
        }
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(350) // تأخير بسيط لتفادي ترجمة كل حرف على حدة (يوفر معالجة وبطارية)
            performTranslation()
        }
    }

    fun swapLanguages() {
        val state = _uiState.value
        _uiState.value = state.copy(
            sourceLang = state.targetLang,
            targetLang = state.sourceLang,
            inputText = state.outputText,
            outputText = state.inputText,
            sourceModelState = state.targetModelState,
            targetModelState = state.sourceModelState
        )
        viewModelScope.launch {
            preferences.setLastLanguagePair(_uiState.value.sourceLang, _uiState.value.targetLang)
        }
    }

    fun clearInput() {
        _uiState.value = _uiState.value.copy(inputText = "", outputText = "")
    }

    fun downloadModel(langCode: String, wifiOnly: Boolean) {
        viewModelScope.launch {
            repository.downloadModel(langCode, wifiOnly).collect { state ->
                val current = _uiState.value
                _uiState.value = if (langCode == current.sourceLang) {
                    current.copy(sourceModelState = state)
                } else {
                    current.copy(targetModelState = state)
                }
                if (state is ModelState.Ready) performTranslation()
            }
        }
    }

    private fun performTranslation() {
        val state = _uiState.value
        if (state.inputText.isBlank()) {
            _uiState.value = state.copy(outputText = "")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTranslating = true)
            val result = repository.translate(state.inputText, state.sourceLang, state.targetLang)
            result.onSuccess { translated ->
                _uiState.value = _uiState.value.copy(outputText = translated, isTranslating = false)
                historyRepository.addEntry(state.sourceLang, state.targetLang, state.inputText, translated)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isTranslating = false)
                refreshModelStatus() // على الأرجح النموذج غير مثبَّت، نحدّث الحالة ليعرف المستخدم
            }
        }
    }
}
