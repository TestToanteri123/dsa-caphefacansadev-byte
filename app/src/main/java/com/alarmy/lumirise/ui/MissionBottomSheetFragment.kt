package com.alarmy.lumirise.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alarmy.lumirise.R
import com.alarmy.lumirise.databinding.FragmentMissionBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MissionBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentMissionBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private var onMissionSelected: ((String) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMissionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.missionNoneCard.setOnClickListener {
            onMissionSelected?.invoke("NONE")
            dismiss()
        }
        
        binding.missionMathCard.setOnClickListener {
            onMissionSelected?.invoke("MATH")
            dismiss()
        }
        
        binding.missionShakeCard.setOnClickListener {
            onMissionSelected?.invoke("SHAKE")
            dismiss()
        }
        
        binding.missionPhotoCard.setOnClickListener {
            onMissionSelected?.invoke("PHOTO")
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    fun setOnMissionSelectedListener(listener: (String) -> Unit) {
        onMissionSelected = listener
    }
    
    companion object {
        fun newInstance(): MissionBottomSheetFragment {
            return MissionBottomSheetFragment()
        }
    }
}
