package com.example.myapplication.model

import java.io.Serializable

data class DayMenu(
    val dayOfWeek: String,
    val breakfast: String,
    val lunch: String,
    val dinner: String
) : Serializable {
    
    companion object {
        val DAYS_OF_WEEK = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )
        
        fun empty(dayOfWeek: String) = DayMenu(
            dayOfWeek = dayOfWeek,
            breakfast = "",
            lunch = "",
            dinner = ""
        )
    }
    
    fun isEmpty(): Boolean = breakfast.isBlank() && lunch.isBlank() && dinner.isBlank()
}
