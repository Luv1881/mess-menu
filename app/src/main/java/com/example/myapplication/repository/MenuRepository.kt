package com.example.myapplication.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.model.WeeklyMenu
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.menuDataStore: DataStore<Preferences> by preferencesDataStore(name = "menu_data")

/**
 * Repository for storing and retrieving weekly menu data.
 * Uses DataStore for persistent storage with JSON serialization.
 */
class MenuRepository(private val context: Context) {
    
    private val gson = Gson()
    private val excelParser = ExcelParser(context)
    
    companion object {
        private val WEEKLY_MENU_KEY = stringPreferencesKey("weekly_menu")
        private val SOURCE_URI_KEY = stringPreferencesKey("source_uri")
        
        @Volatile
        private var instance: MenuRepository? = null
        
        fun getInstance(context: Context): MenuRepository {
            return instance ?: synchronized(this) {
                instance ?: MenuRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    
    val weeklyMenuFlow: Flow<WeeklyMenu> = context.menuDataStore.data
        .map { preferences ->
            val json = preferences[WEEKLY_MENU_KEY]
            if (json != null) {
                try {
                    gson.fromJson(json, WeeklyMenu::class.java)
                } catch (e: Exception) {
                    WeeklyMenu.empty()
                }
            } else {
                WeeklyMenu.empty()
            }
        }
    
    suspend fun getWeeklyMenu(): WeeklyMenu {
        return weeklyMenuFlow.first()
    }
    
    /**
     * Import a menu from an Excel file.
     * @param uri URI of the Excel file
     * @return Result containing success or error message
     */
    suspend fun importFromExcel(uri: Uri): Result<WeeklyMenu> {
        return when (val parseResult = excelParser.parseExcel(uri)) {
            is ExcelParser.ParseResult.Success -> {
                saveWeeklyMenu(parseResult.menu)
                saveSourceUri(uri.toString())
                Result.success(parseResult.menu)
            }
            is ExcelParser.ParseResult.Error -> {
                Result.failure(Exception(parseResult.message))
            }
        }
    }
    
    /**
     * Reload menu from the previously imported Excel file.
     * @return Result containing success or error message
     */
    suspend fun reloadFromSource(): Result<WeeklyMenu> {
        val sourceUri = getSourceUri() ?: return Result.failure(Exception("No source file found"))
        return try {
            val uri = Uri.parse(sourceUri)
            importFromExcel(uri)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to reload: ${e.message}"))
        }
    }
    
    private suspend fun saveWeeklyMenu(menu: WeeklyMenu) {
        val json = gson.toJson(menu)
        context.menuDataStore.edit { preferences ->
            preferences[WEEKLY_MENU_KEY] = json
        }
    }
    
    private suspend fun saveSourceUri(uri: String) {
        context.menuDataStore.edit { preferences ->
            preferences[SOURCE_URI_KEY] = uri
        }
    }
    
    private suspend fun getSourceUri(): String? {
        return context.menuDataStore.data.first()[SOURCE_URI_KEY]
    }
    
    suspend fun clearMenu() {
        context.menuDataStore.edit { preferences ->
            preferences.remove(WEEKLY_MENU_KEY)
            preferences.remove(SOURCE_URI_KEY)
        }
    }
}
