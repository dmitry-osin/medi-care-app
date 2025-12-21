package pro.osin.tools.medicare.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val THEME_KEY = stringPreferencesKey("theme")
        private val BATTERY_OPTIMIZATION_DIALOG_SHOWN_KEY = booleanPreferencesKey("battery_optimization_dialog_shown")
        
        const val LANGUAGE_RU = "ru"
        const val LANGUAGE_EN = "en"
        
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: LANGUAGE_RU
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: THEME_SYSTEM
    }

    val batteryOptimizationDialogShown: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BATTERY_OPTIMIZATION_DIALOG_SHOWN_KEY] ?: false
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setBatteryOptimizationDialogShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BATTERY_OPTIMIZATION_DIALOG_SHOWN_KEY] = shown
        }
    }
}

