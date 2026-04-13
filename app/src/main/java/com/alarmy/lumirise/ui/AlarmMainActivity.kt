package com.alarmy.lumirise.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.ActivityAlarmMainBinding
import com.alarmy.lumirise.model.Alarm
import com.alarmy.lumirise.ui.adapter.AlarmAdapter
import com.alarmy.lumirise.util.ThemeManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AlarmMainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAlarmMainBinding
    private val viewModel: AlarmViewModel by viewModels()
    private lateinit var alarmAdapter: AlarmAdapter
    
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == SettingsActivity.RESULT_THEME_CHANGED) {
            recreate()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        binding = ActivityAlarmMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupBottomNavigation()
        observeAlarms()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onAlarmClick = { alarm -> openAlarmEditor(alarm) },
            onAlarmToggle = { alarm, enabled -> viewModel.toggleAlarmEnabled(alarm.id, enabled) },
            onAlarmDelete = { alarm -> deleteAlarm(alarm) }
        )
        
        binding.alarmRecyclerView.adapter = alarmAdapter
        
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val alarm = alarmAdapter.currentList[position]
                deleteAlarm(alarm)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.alarmRecyclerView)
    }
    
    private fun setupFab() {
        binding.addAlarmFab.setOnClickListener {
            openAlarmEditor(null)
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    settingsLauncher.launch(intent)
                    false
                }
                R.id.nav_records -> {
                    val intent = Intent(this, SleepHistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_alarms, R.id.nav_missions -> true
                else -> false
            }
        }
    }
    
    private fun observeAlarms() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.alarms.collect { alarms ->
                    alarmAdapter.submitList(alarms)
                    updateEmptyState(alarms.isEmpty())
                }
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.alarmRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun openAlarmEditor(alarm: Alarm?) {
        Snackbar.make(binding.root, "Alarm editor coming soon", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun deleteAlarm(alarm: Alarm) {
        viewModel.deleteAlarm(alarm)
        Snackbar.make(binding.root, "Alarm deleted", Snackbar.LENGTH_SHORT).show()
    }
}
