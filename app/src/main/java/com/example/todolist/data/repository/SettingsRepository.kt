package com.example.todolist.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val FONT_SCALE_INDEX = intPreferencesKey("font_scale_index")
    private val ACCENT_SCHEME = intPreferencesKey("accent_scheme")
    private val MORNING_NOTIF = booleanPreferencesKey("morning_notif")
    private val EVENING_NOTIF = booleanPreferencesKey("evening_notif")

    // 0=small(0.85f), 1=medium(1.0f), 2=large(1.15f)
    val fontScaleIndex: Flow<Int> = context.settingsDataStore.data.map { it[FONT_SCALE_INDEX] ?: 1 }

    // 0=standard, 1=pastel, 2=monochrome
    val accentScheme: Flow<Int> = context.settingsDataStore.data.map { it[ACCENT_SCHEME] ?: 0 }

    val morningNotifEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[MORNING_NOTIF] ?: true }
    val eveningNotifEnabled: Flow<Boolean> = context.settingsDataStore.data.map { it[EVENING_NOTIF] ?: true }

    suspend fun setFontScaleIndex(index: Int) {
        context.settingsDataStore.edit { it[FONT_SCALE_INDEX] = index.coerceIn(0, 2) }
    }

    suspend fun setAccentScheme(scheme: Int) {
        context.settingsDataStore.edit { it[ACCENT_SCHEME] = scheme.coerceIn(0, 2) }
    }

    suspend fun setMorningNotif(enabled: Boolean) {
        context.settingsDataStore.edit { it[MORNING_NOTIF] = enabled }
    }

    suspend fun setEveningNotif(enabled: Boolean) {
        context.settingsDataStore.edit { it[EVENING_NOTIF] = enabled }
    }
}
