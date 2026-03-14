package com.example.myapplication.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MessMenuApplication
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.example.myapplication.ui.MainActivity
import com.example.myapplication.widget.MessMenuWidgetProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val preferencesManager by lazy {
        (requireActivity().application as MessMenuApplication).preferencesManager
    }
    
    private val menuRepository by lazy {
        (requireActivity().application as MessMenuApplication).menuRepository
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Widget settings
            val widgetSettings = preferencesManager.widgetSettingsFlow.first()
            binding.sliderTextSize.value = widgetSettings.textSize
            binding.switchShowIcons.isChecked = widgetSettings.showIcons
            
            // Dark mode
            val isDarkMode = preferencesManager.darkModeFlow.first()
            binding.switchDarkMode.isChecked = isDarkMode
            
            // Notification settings
            val notificationSettings = preferencesManager.notificationSettingsFlow.first()
            binding.switchNotifications.isChecked = notificationSettings.enabled
            binding.notificationTimesContainer.isVisible = notificationSettings.enabled
            binding.btnBreakfastTime.text = notificationSettings.breakfastTime
            binding.btnLunchTime.text = notificationSettings.lunchTime
            binding.btnDinnerTime.text = notificationSettings.dinnerTime
        }
    }
    
    private fun setupListeners() {
        // Text size slider
        binding.sliderTextSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewLifecycleOwner.lifecycleScope.launch {
                    preferencesManager.setTextSize(value)
                    MessMenuWidgetProvider.updateAllWidgets(requireContext())
                }
            }
        }
        
        // Show icons switch
        binding.switchShowIcons.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                preferencesManager.setShowIcons(isChecked)
                MessMenuWidgetProvider.updateAllWidgets(requireContext())
            }
        }
        
        // Dark mode switch
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                preferencesManager.setDarkMode(isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
        
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            binding.notificationTimesContainer.isVisible = isChecked
            viewLifecycleOwner.lifecycleScope.launch {
                preferencesManager.setNotificationsEnabled(isChecked)
            }
        }
        
        // Time pickers
        binding.btnBreakfastTime.setOnClickListener {
            showTimePicker("breakfast", binding.btnBreakfastTime.text.toString())
        }
        
        binding.btnLunchTime.setOnClickListener {
            showTimePicker("lunch", binding.btnLunchTime.text.toString())
        }
        
        binding.btnDinnerTime.setOnClickListener {
            showTimePicker("dinner", binding.btnDinnerTime.text.toString())
        }
        
        // Import file button
        binding.btnImportFile.setOnClickListener {
            (activity as? MainActivity)?.openFilePicker()
        }
        
        // Reload file button
        binding.btnReloadFile.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = menuRepository.reloadFromSource()
                result.fold(
                    onSuccess = {
                        Toast.makeText(
                            requireContext(),
                            R.string.success_reload,
                            Toast.LENGTH_SHORT
                        ).show()
                        MessMenuWidgetProvider.updateAllWidgets(requireContext())
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            error.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
        
        // Clear menu button
        binding.btnClearMenu.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_menu)
                .setMessage("Are you sure you want to clear all menu data?")
                .setPositiveButton(R.string.ok) { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        menuRepository.clearMenu()
                        MessMenuWidgetProvider.updateAllWidgets(requireContext())
                        Toast.makeText(
                            requireContext(),
                            "Menu cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
    
    private fun showTimePicker(meal: String, currentTime: String) {
        val parts = currentTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .setTitleText("Select $meal time")
            .build()
        
        picker.addOnPositiveButtonClickListener {
            val newTime = String.format("%02d:%02d", picker.hour, picker.minute)
            when (meal) {
                "breakfast" -> binding.btnBreakfastTime.text = newTime
                "lunch" -> binding.btnLunchTime.text = newTime
                "dinner" -> binding.btnDinnerTime.text = newTime
            }
            
            viewLifecycleOwner.lifecycleScope.launch {
                preferencesManager.setMealTimes(
                    binding.btnBreakfastTime.text.toString(),
                    binding.btnLunchTime.text.toString(),
                    binding.btnDinnerTime.text.toString()
                )
            }
        }
        
        picker.show(parentFragmentManager, "time_picker")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
