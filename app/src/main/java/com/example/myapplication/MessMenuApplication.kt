package com.example.myapplication

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.repository.MenuRepository
import com.example.myapplication.repository.PreferencesManager
import com.example.myapplication.worker.WidgetUpdateWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Application class for initializing repositories and scheduling background work.
 */
class MessMenuApplication : Application(), Configuration.Provider {
    
    lateinit var menuRepository: MenuRepository
        private set
    
    lateinit var preferencesManager: PreferencesManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        menuRepository = MenuRepository.getInstance(this)
        preferencesManager = PreferencesManager.getInstance(this)
        
        scheduleWidgetUpdates()
    }
    
    private fun scheduleWidgetUpdates() {
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WIDGET_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return midnight.timeInMillis - now.timeInMillis
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    companion object {
        const val WIDGET_UPDATE_WORK_NAME = "mess_menu_widget_update"
    }
}
