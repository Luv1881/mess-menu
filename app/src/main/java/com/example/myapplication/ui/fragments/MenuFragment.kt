package com.example.myapplication.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.MessMenuApplication
import com.example.myapplication.databinding.FragmentMenuBinding
import com.example.myapplication.model.WeeklyMenu
import com.example.myapplication.ui.MainActivity
import com.example.myapplication.ui.MenuAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment displaying the weekly menu in a RecyclerView.
 */
class MenuFragment : Fragment() {
    
    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var menuAdapter: MenuAdapter
    
    private val menuRepository by lazy {
        (requireActivity().application as MessMenuApplication).menuRepository
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyState()
        observeMenu()
    }
    
    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter()
        binding.recyclerView.apply {
            adapter = menuAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    menuRepository.reloadFromSource()
                } catch (e: Exception) {
                    // Reload might fail if no source, that's ok
                } finally {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }
    
    private fun setupEmptyState() {
        binding.btnImportEmpty.setOnClickListener {
            (activity as? MainActivity)?.openFilePicker()
        }
    }
    
    private fun observeMenu() {
        viewLifecycleOwner.lifecycleScope.launch {
            menuRepository.weeklyMenuFlow.collectLatest { menu ->
                updateUI(menu)
            }
        }
    }
    
    private fun updateUI(menu: WeeklyMenu) {
        val hasData = menu.isLoaded()
        
        binding.recyclerView.isVisible = hasData
        binding.emptyState.isVisible = !hasData
        
        if (hasData) {
            menuAdapter.submitList(menu.days)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
