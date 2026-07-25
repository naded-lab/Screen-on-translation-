package com.tarjaman.offline.data.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "tarjaman_history")

private const val MAX_HISTORY_ITEMS = 30

/**
 * يحفظ آخر [MAX_HISTORY_ITEMS] ترجمة محلياً فقط (لا مزامنة سحابية، خصوصية كاملة للمستخدم).
 */
class HistoryRepository(context: Context) {

    private val store = context.applicationContext.historyDataStore
    private val KEY_ITEMS = stringSetPreferencesKey("items")

    val historyFlow: Flow<List<HistoryItem>> = store.data.map { prefs ->
        (prefs[KEY_ITEMS] ?: emptySet())
            .mapNotNull { HistoryItem.deserialize(it) }
            .sortedByDescending { it.timestamp }
    }

    suspend fun addEntry(sourceLang: String, targetLang: String, sourceText: String, translatedText: String) {
        if (sourceText.isBlank() || translatedText.isBlank()) return
        val newItem = HistoryItem(
            id = System.currentTimeMillis(),
            sourceLang = sourceLang,
            targetLang = targetLang,
            sourceText = sourceText,
            translatedText = translatedText,
            timestamp = System.currentTimeMillis()
        )
        store.edit { prefs ->
            val current = (prefs[KEY_ITEMS] ?: emptySet())
                .mapNotNull { HistoryItem.deserialize(it) }
                .sortedByDescending { it.timestamp }
                .toMutableList()
            current.add(0, newItem)
            val trimmed = current.take(MAX_HISTORY_ITEMS)
            prefs[KEY_ITEMS] = trimmed.map { it.serialize() }.toSet()
        }
    }

    suspend fun deleteEntry(id: Long) {
        store.edit { prefs ->
            val current = (prefs[KEY_ITEMS] ?: emptySet())
                .mapNotNull { HistoryItem.deserialize(it) }
                .filter { it.id != id }
            prefs[KEY_ITEMS] = current.map { it.serialize() }.toSet()
        }
    }

    suspend fun clearAll() {
        store.edit { it[KEY_ITEMS] = emptySet() }
    }
}
