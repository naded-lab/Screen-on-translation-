package com.tarjaman.offline.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.tarjaman.offline.ui.screens.history.HistoryViewModel
import com.tarjaman.offline.ui.screens.settings.SettingsViewModel
import com.tarjaman.offline.ui.screens.translate.TranslateViewModel

/** مصنع بسيط يبني ViewModels يدوياً بدون الحاجة لمكتبة Hilt/Dagger */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(TranslateViewModel::class.java) -> TranslateViewModel(
                repository = container.translationRepository,
                historyRepository = container.historyRepository,
                preferences = container.preferencesManager
            ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                preferences = container.preferencesManager,
                translationRepository = container.translationRepository
            ) as T

            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
                historyRepository = container.historyRepository
            ) as T

            else -> throw IllegalArgumentException("ViewModel غير معروف: ${modelClass.name}")
        }
    }
}
