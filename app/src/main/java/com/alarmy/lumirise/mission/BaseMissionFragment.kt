package com.alarmy.lumirise.mission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alarmy.lumirise.databinding.FragmentBaseMissionBinding
import kotlinx.coroutines.launch

abstract class BaseMissionFragment : Fragment(), MissionCallback {
    
    private var _binding: FragmentBaseMissionBinding? = null
    protected val binding get() = _binding!!
    
    protected abstract val missionType: MissionType
    
    protected val missionManager = MissionManager.getInstance()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBaseMissionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBaseUI()
        observeMissionState()
        setupMissionUI()
    }
    
    private fun setupBaseUI() {
        binding.missionTypeText.text = getMissionTypeDisplayName()
        binding.cancelButton.setOnClickListener {
            missionManager.cancelMission()
        }
    }
    
    private fun observeMissionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    missionManager.progress.collect { progress ->
                        binding.progressBar.progress = progress
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                missionManager.hint.collect { hint ->
                    if (hint.isNotEmpty()) {
                        binding.hintText.visibility = View.VISIBLE
                        binding.hintText.text = hint
                    }
                }
            }
        }
    }
    
    protected open fun setupMissionUI() {}
    
    override fun onMissionStarted(type: MissionType) {}
    
    override fun onMissionProgress(progress: Int, message: String) {
        binding.progressBar.progress = progress
        if (message.isNotEmpty()) {
            binding.statusText.text = message
        }
    }
    
    override fun onMissionComplete(result: MissionResult) {}
    
    override fun onMissionCancelled() {
        parentFragmentManager.popBackStack()
    }
    
    override fun onMissionHintRequested(): String {
        return getHintText()
    }
    
    protected open fun getMissionTypeDisplayName(): String {
        return when (missionType) {
            MissionType.NONE -> "No Mission"
            MissionType.MATH -> "Math Challenge"
            MissionType.SHAKE -> "Shake to Dismiss"
            MissionType.PHOTO -> "Photo Match"
        }
    }
    
    protected abstract fun getHintText(): String
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
