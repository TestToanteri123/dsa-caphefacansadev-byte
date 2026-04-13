package com.alarmy.lumirise.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.ActivitySleepHistoryBinding
import com.alarmy.lumirise.model.SleepRecord
import com.alarmy.lumirise.service.SleepTrackingService
import com.alarmy.lumirise.ui.adapter.SleepHistoryAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SleepHistoryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySleepHistoryBinding
    private val viewModel: SleepHistoryViewModel by viewModels()
    private lateinit var sleepAdapter: SleepHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySleepHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupFilterChips()
        setupSwipeRefresh()
        setupFab()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        sleepAdapter = SleepHistoryAdapter(
            onRecordClick = { record -> toggleRecordExpansion(record) },
            onRecordLongClick = { record -> showDeleteConfirmation(record) }
        )
        
        binding.sleepRecyclerView.adapter = sleepAdapter
        
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val record = sleepAdapter.currentList[position]
                showDeleteConfirmation(record)
                sleepAdapter.notifyItemChanged(position)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.sleepRecyclerView)
    }
    
    private fun setupFilterChips() {
        // Filter chips not available in current layout - filter is set via date range selection
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }
    
    private fun setupFab() {
        binding.startTrackingFab.setOnClickListener {
            startSleepTracking()
        }
        
        binding.emptyStateButton.setOnClickListener {
            startSleepTracking()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredSleepRecords.collect { records ->
                        sleepAdapter.submitList(records)
                        updateEmptyState(records.isEmpty())
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
                
                launch {
                    viewModel.isRefreshing.collect { isRefreshing ->
                        binding.swipeRefresh.isRefreshing = isRefreshing
                    }
                }
                
                launch {
                    viewModel.expandedRecordId.collect { expandedId ->
                        sleepAdapter.setExpandedRecordId(expandedId)
                    }
                }
                
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }
                
                launch {
                    viewModel.averageSleepDuration.collect { duration ->
                        binding.avgSleepDuration.text = duration
                    }
                }
                
                launch {
                    viewModel.averageQuality.collect { quality ->
                        binding.avgQuality.text = "$quality%"
                    }
                }
                
                launch {
                    viewModel.nightsTracked.collect { nights ->
                        // Nights tracked stat - update UI if view exists
                        // binding.nightsTracked.text = nights.toString()
                    }
                }
                
                launch {
                    viewModel.dateFilter.collect { filter ->
                        // Filter chips not available - filter is set via date range selection
                        // when (filter) {
                        //     DateFilter.WEEK -> binding.chipWeek.isChecked = true
                        //     DateFilter.MONTH -> binding.chipMonth.isChecked = true
                        //     DateFilter.ALL -> binding.chipAll.isChecked = true
                        // }
                    }
                }
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.sleepRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun toggleRecordExpansion(record: SleepRecord) {
        viewModel.toggleExpanded(record.id)
    }
    
    private fun showDeleteConfirmation(record: SleepRecord) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_sleep_record)
            .setMessage(R.string.delete_sleep_record_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteSleepRecord(record)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun startSleepTracking() {
        SleepTrackingService.start(this, -1L)
        Snackbar.make(binding.root, R.string.sleep_tracking_started, Snackbar.LENGTH_SHORT).show()
        finish()
    }
    
    private fun handleUiState(state: SleepHistoryUiState) {
        when (state) {
            is SleepHistoryUiState.Success -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                viewModel.resetUiState()
            }
            is SleepHistoryUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.resetUiState()
            }
            else -> {}
        }
    }
    
    companion object {
        fun newIntent(context: android.content.Context): Intent {
            return Intent(context, SleepHistoryActivity::class.java)
        }
    }
}
