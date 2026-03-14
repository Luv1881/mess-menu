package com.example.myapplication.model

import java.io.Serializable

/**
 * Data class representing a full weekly menu containing 7 days of meals.
 */
data class WeeklyMenu(
    val days: List<DayMenu>,
    val sourcePath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable {
    
    companion object {
        fun empty() = WeeklyMenu(
            days = DayMenu.DAYS_OF_WEEK.map { DayMenu.empty(it) },
            sourcePath = null,
            lastUpdated = 0
        )
    }
    
    /**
     * Get today's menu based on the current day of the week.
     * @return DayMenu for today, or null if not found
     */
    fun getTodayMenu(): DayMenu? {
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, ..., Saturday=7
        // Our list: Monday=0, Tuesday=1, ..., Sunday=6
        val index = when (today) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> return null
        }
        return days.getOrNull(index)
    }
    
    /**
     * Get menu for a specific day of the week.
     * @param dayName The name of the day (e.g., "Monday")
     * @return DayMenu for the specified day, or null if not found
     */
    fun getMenuForDay(dayName: String): DayMenu? {
        return days.find { it.dayOfWeek.equals(dayName, ignoreCase = true) }
    }
    
    fun isEmpty(): Boolean = days.all { it.isEmpty() }
    
    fun isLoaded(): Boolean = sourcePath != null && !isEmpty()
}
