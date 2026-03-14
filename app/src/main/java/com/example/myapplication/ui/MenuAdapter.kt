package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemDayMenuBinding
import com.example.myapplication.model.DayMenu
import java.util.Calendar

/**
 * RecyclerView adapter for displaying the weekly menu.
 */
class MenuAdapter : ListAdapter<DayMenu, MenuAdapter.DayMenuViewHolder>(DayMenuDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayMenuViewHolder {
        val binding = ItemDayMenuBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayMenuViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DayMenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DayMenuViewHolder(
        private val binding: ItemDayMenuBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(dayMenu: DayMenu) {
            binding.tvDayName.text = dayMenu.dayOfWeek
            binding.tvBreakfast.text = dayMenu.breakfast.ifBlank { "—" }
            binding.tvLunch.text = dayMenu.lunch.ifBlank { "—" }
            binding.tvDinner.text = dayMenu.dinner.ifBlank { "—" }
            
            val isToday = isCurrentDay(dayMenu.dayOfWeek)
            binding.chipToday.isVisible = isToday
        }
        
        private fun isCurrentDay(dayName: String): Boolean {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val dayIndex = when (today) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> -1
            }
            return DayMenu.DAYS_OF_WEEK.getOrNull(dayIndex)?.equals(dayName, ignoreCase = true) == true
        }
    }
    
    private class DayMenuDiffCallback : DiffUtil.ItemCallback<DayMenu>() {
        override fun areItemsTheSame(oldItem: DayMenu, newItem: DayMenu): Boolean {
            return oldItem.dayOfWeek == newItem.dayOfWeek
        }
        
        override fun areContentsTheSame(oldItem: DayMenu, newItem: DayMenu): Boolean {
            return oldItem == newItem
        }
    }
}
