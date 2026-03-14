package com.example.myapplication.repository

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_settings")

/**
 * Manager for widget appearance preferences and app settings.
 */
class PreferencesManager(private val context: Context) {
    
    companion object {
        private val TEXT_SIZE_KEY = floatPreferencesKey("text_size")
        private val WIDGET_THEME_KEY = stringPreferencesKey("widget_theme")
        private val SHOW_ICONS_KEY = booleanPreferencesKey("show_icons")
        
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        
        private val BREAKFAST_TIME_KEY = stringPreferencesKey("breakfast_time")
        private val LUNCH_TIME_KEY = stringPreferencesKey("lunch_time")
        private val DINNER_TIME_KEY = stringPreferencesKey("dinner_time")
        
        const val DEFAULT_TEXT_SIZE = 14f
        const val DEFAULT_THEME = "default"
        const val DEFAULT_SHOW_ICONS = true
        const val DEFAULT_BREAKFAST_TIME = "07:30"
        const val DEFAULT_LUNCH_TIME = "12:30"
        const val DEFAULT_DINNER_TIME = "19:30"
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    data class WidgetSettings(
        val textSize: Float = DEFAULT_TEXT_SIZE,
        val theme: String = DEFAULT_THEME,
        val showIcons: Boolean = DEFAULT_SHOW_ICONS
    )
    
    data class NotificationSettings(
        val enabled: Boolean = false,
        val breakfastTime: String = DEFAULT_BREAKFAST_TIME,
        val lunchTime: String = DEFAULT_LUNCH_TIME,
        val dinnerTime: String = DEFAULT_DINNER_TIME
    )
    
    // Widget Settings Flow
    val widgetSettingsFlow: Flow<WidgetSettings> = context.settingsDataStore.data
        .map { preferences ->
            WidgetSettings(
                textSize = preferences[TEXT_SIZE_KEY] ?: DEFAULT_TEXT_SIZE,
                theme = preferences[WIDGET_THEME_KEY] ?: DEFAULT_THEME,
                showIcons = preferences[SHOW_ICONS_KEY] ?: DEFAULT_SHOW_ICONS
            )
        }
    
    // Dark mode Flow
    val darkModeFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    
    // Notification Settings Flow
    val notificationSettingsFlow: Flow<NotificationSettings> = context.settingsDataStore.data
        .map { preferences ->
            NotificationSettings(
                enabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: false,
                breakfastTime = preferences[BREAKFAST_TIME_KEY] ?: DEFAULT_BREAKFAST_TIME,
                lunchTime = preferences[LUNCH_TIME_KEY] ?: DEFAULT_LUNCH_TIME,
                dinnerTime = preferences[DINNER_TIME_KEY] ?: DEFAULT_DINNER_TIME
            )
        }
    
    suspend fun getWidgetSettings(): WidgetSettings = widgetSettingsFlow.first()
    
    suspend fun setTextSize(size: Float) {
        context.settingsDataStore.edit { preferences ->
            preferences[TEXT_SIZE_KEY] = size.coerceIn(10f, 24f)
        }
    }
    
    suspend fun setWidgetTheme(theme: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[WIDGET_THEME_KEY] = theme
        }
    }
    
    suspend fun setShowIcons(show: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SHOW_ICONS_KEY] = show
        }
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setMealTimes(breakfast: String, lunch: String, dinner: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[BREAKFAST_TIME_KEY] = breakfast
            preferences[LUNCH_TIME_KEY] = lunch
            preferences[DINNER_TIME_KEY] = dinner
        }
    }
}
