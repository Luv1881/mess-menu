package com.example.myapplication.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.myapplication.R
import com.example.myapplication.model.DayMenu
import com.example.myapplication.model.WeeklyMenu
import com.example.myapplication.repository.MenuRepository
import com.example.myapplication.repository.PreferencesManager
import com.example.myapplication.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar

/**
 * AppWidgetProvider for the Mess Menu widget.
 * Displays today's menu and auto-updates at midnight.
 */
class MessMenuWidgetProvider : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "MessMenuWidgetProvider"
        private const val DATA_LOAD_TIMEOUT_MS = 5000L
        
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        
        /**
         * Trigger an update for all widget instances.
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, MessMenuWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, MessMenuWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }
    

    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        
        scope.launch {
            try {
                val menuRepository = MenuRepository.getInstance(context)
                val preferencesManager = PreferencesManager.getInstance(context)
                
                // Use timeout to prevent hanging on DataStore operations
                val weeklyMenu = withTimeoutOrNull(DATA_LOAD_TIMEOUT_MS) {
                    try {
                        menuRepository.weeklyMenuFlow.firstOrNull()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading weekly menu", e)
                        null
                    }
                } ?: WeeklyMenu.empty()
                
                val widgetSettings = withTimeoutOrNull(DATA_LOAD_TIMEOUT_MS) {
                    try {
                        preferencesManager.widgetSettingsFlow.firstOrNull()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading widget settings", e)
                        null
                    }
                } ?: PreferencesManager.WidgetSettings()
                
                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, weeklyMenu, widgetSettings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widgets", e)
                // Fallback: update all widgets with empty views
                for (appWidgetId in appWidgetIds) {
                    try {
                        val views = createEmptyViews(context, "Error updating")
                        setupClickIntent(context, views)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error creating fallback views for widget $appWidgetId", ex)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        updateAllWidgets(context)
    }
    
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult = goAsync()
        scope.launch {
            try {
                val menuRepository = MenuRepository.getInstance(context)
                val preferencesManager = PreferencesManager.getInstance(context)
                val weeklyMenu = withTimeoutOrNull(DATA_LOAD_TIMEOUT_MS) {
                    menuRepository.weeklyMenuFlow.firstOrNull()
                } ?: WeeklyMenu.empty()
                val widgetSettings = withTimeoutOrNull(DATA_LOAD_TIMEOUT_MS) {
                    preferencesManager.widgetSettingsFlow.firstOrNull()
                } ?: PreferencesManager.WidgetSettings()
                
                updateWidget(context, appWidgetManager, appWidgetId, weeklyMenu, widgetSettings)
            } catch (e: Exception) {
                Log.e(TAG, "Error onAppWidgetOptionsChanged", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    override fun onDisabled(context: Context) {
        // Last widget removed
    }
    
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        weeklyMenu: WeeklyMenu,
        widgetSettings: PreferencesManager.WidgetSettings
    ) {
        val todayMenu = weeklyMenu.getTodayMenu()
        
        val views = if (todayMenu != null && weeklyMenu.isLoaded() && !todayMenu.isEmpty()) {
            createMenuViews(context, todayMenu, widgetSettings)
        } else {
            val message = if (weeklyMenu.isLoaded()) "No menu for today" else null
            createEmptyViews(context, message)
        }
        
        // Set click to open main app
        setupClickIntent(context, views)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    private fun createMenuViews(
        context: Context,
        todayMenu: DayMenu,
        widgetSettings: PreferencesManager.WidgetSettings
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_mess_menu)
        
        // Set day name
        views.setTextViewText(R.id.tvWidgetDay, todayMenu.dayOfWeek)
        
        // Set meals
        views.setTextViewText(
            R.id.tvWidgetBreakfast,
            todayMenu.breakfast.ifBlank { "—" }
        )
        views.setTextViewText(
            R.id.tvWidgetLunch,
            todayMenu.lunch.ifBlank { "—" }
        )
        views.setTextViewText(
            R.id.tvWidgetDinner,
            todayMenu.dinner.ifBlank { "—" }
        )
        
        // Apply text size setting
        val textSizeSp = widgetSettings.textSize
        views.setTextViewTextSize(R.id.tvWidgetBreakfast, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        views.setTextViewTextSize(R.id.tvWidgetLunch, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        views.setTextViewTextSize(R.id.tvWidgetDinner, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        
        val iconVisibility = if (widgetSettings.showIcons) View.VISIBLE else View.GONE
        views.setViewVisibility(R.id.ivBreakfast, iconVisibility)
        views.setViewVisibility(R.id.ivLunch, iconVisibility)
        views.setViewVisibility(R.id.ivDinner, iconVisibility)
        
        return views
    }
    
    private fun createEmptyViews(context: Context, message: String? = null): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_mess_menu_empty)
        
        // Get today's day name
        val dayName = getTodayDayName()
        views.setTextViewText(R.id.tvWidgetDay, dayName)
        
        if (message != null) {
            views.setTextViewText(R.id.tvWidgetNoMenu, message)
        }
        views.setViewVisibility(R.id.tvWidgetNoMenu, View.VISIBLE)
        
        // Set click to open main app
        setupClickIntent(context, views)
        
        return views
    }
    
    private fun setupClickIntent(context: Context, views: RemoteViews) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
    }
    
    private fun getTodayDayName(): String {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return when (today) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Today"
        }
    }
    
}
