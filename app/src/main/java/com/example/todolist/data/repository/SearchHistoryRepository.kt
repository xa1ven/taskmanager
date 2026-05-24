package com.example.todolist.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history")

@Singleton
class SearchHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val HISTORY_KEY = stringPreferencesKey("history")
    private val SEPARATOR = "|||"
    private val MAX_ITEMS = 10

    val history: Flow<List<String>> = context.searchDataStore.data.map { prefs ->
        val raw = prefs[HISTORY_KEY] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    suspend fun addQuery(query: String) {
        if (query.isBlank()) return
        context.searchDataStore.edit { prefs ->
            val raw = prefs[HISTORY_KEY] ?: ""
            val current = if (raw.isBlank()) mutableListOf()
            else raw.split(SEPARATOR).filter { it.isNotBlank() }.toMutableList()
            current.remove(query)
            current.add(0, query)
            if (current.size > MAX_ITEMS) current.removeAt(current.lastIndex)
            prefs[HISTORY_KEY] = current.joinToString(SEPARATOR)
        }
    }

    suspend fun clearHistory() {
        context.searchDataStore.edit { prefs ->
            prefs[HISTORY_KEY] = ""
        }
    }
}
