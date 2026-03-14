package com.example.myapplication.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.MessMenuApplication
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.ui.fragments.MenuFragment
import com.example.myapplication.ui.fragments.SettingsFragment
import com.example.myapplication.widget.MessMenuWidgetProvider
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Main activity containing the TabLayout with Menu and Settings fragments.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private val menuRepository by lazy {
        (application as MessMenuApplication).menuRepository
    }
    
    private val preferencesManager by lazy {
        (application as MessMenuApplication).preferencesManager
    }
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importExcelFile(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            val isDarkMode = preferencesManager.darkModeFlow.first()
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupViewPager()
        setupFab()
        observeMenuState()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupViewPager() {
        binding.viewPager.adapter = ViewPagerAdapter(this)
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.menu_title)
                1 -> getString(R.string.settings_title)
                else -> ""
            }
        }.attach()
    }
    
    private fun setupFab() {
        binding.fabImport.setOnClickListener {
            openFilePicker()
        }
    }
    
    /**
     * Observe menu state to control FAB visibility.
     * FAB is shown only when no menu is loaded.
     */
    private fun observeMenuState() {
        lifecycleScope.launch {
            menuRepository.weeklyMenuFlow.collectLatest { menu ->
                val shouldShowFab = !menu.isLoaded()
                if (shouldShowFab && !binding.fabImport.isVisible) {
                    binding.fabImport.show()
                } else if (!shouldShowFab && binding.fabImport.isVisible) {
                    binding.fabImport.hide()
                }
            }
        }
    }
    
    fun openFilePicker() {
        filePickerLauncher.launch(arrayOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-excel", // .xls
            "application/octet-stream" // fallback
        ))
    }
    
    private fun importExcelFile(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
        }
        
        lifecycleScope.launch {
            try {
                val result = menuRepository.importFromExcel(uri)
                result.fold(
                    onSuccess = {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.success_import,
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        MessMenuWidgetProvider.updateAllWidgets(this@MainActivity)
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            this@MainActivity,
                            error.message ?: getString(R.string.error_unknown),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.error_unknown) + ": ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MenuFragment()
                1 -> SettingsFragment()
                else -> throw IllegalStateException("Invalid position: $position")
            }
        }
    }
}
