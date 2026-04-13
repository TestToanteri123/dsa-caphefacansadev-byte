package com.alarmy.lumirise.mission

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.alarmy.lumirise.databinding.FragmentShakeMissionBinding

class ShakeMissionFragment : BaseMissionFragment() {

    private var _shakeBinding: FragmentShakeMissionBinding? = null
    private val shakeBinding get() = _shakeBinding!!

    private lateinit var shakeDetector: ShakeDetector
    private var vibrator: Vibrator? = null

    private var currentShakeCount = 0
    private var targetShakeCount = DEFAULT_SHAKE_COUNT

    companion object {
        private const val DEFAULT_SHAKE_COUNT = 30
        private const val VIBRATION_DURATION_MS = 50L
        private const val VIBRATION_AMPLITUDE = 200

        fun newInstance(targetShakes: Int = DEFAULT_SHAKE_COUNT): ShakeMissionFragment {
            return ShakeMissionFragment().apply {
                arguments = android.os.Bundle().apply {
                    putInt(ARG_TARGET_SHAKES, targetShakes)
                }
            }
        }

        private const val ARG_TARGET_SHAKES = "target_shakes"
    }

    override val missionType = MissionType.SHAKE

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetShakeCount = it.getInt(ARG_TARGET_SHAKES, DEFAULT_SHAKE_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        _shakeBinding = FragmentShakeMissionBinding.inflate(inflater, container, false)
        binding.missionContentContainer.addView(shakeBinding.root)
        return binding.root
    }

    override fun setupMissionUI() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        shakeDetector = ShakeDetector(requireContext()) {
            onShakeDetected()
        }

        shakeBinding.shakeProgressText.text = "$currentShakeCount/$targetShakeCount"
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hintText.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.stop()
    }

    override fun getHintText(): String = "Keep shaking!"

    private fun onShakeDetected() {
        currentShakeCount++

        vibrate()

        val progressPercent = (currentShakeCount * 100) / targetShakeCount
        shakeBinding.shakeProgressText.text = "$currentShakeCount/$targetShakeCount"
        binding.progressBar.progress = progressPercent
        MissionManager.getInstance().updateProgress(progressPercent, "Shake: $currentShakeCount/$targetShakeCount")

        if (currentShakeCount >= targetShakeCount) {
            shakeDetector.stop()
            MissionManager.getInstance().completeMission()
        }
    }

    private fun vibrate() {
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VIBRATION_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(VIBRATION_DURATION_MS)
            }
        }
    }

    override fun onDestroyView() {
        shakeDetector.stop()
        (shakeBinding.root.parent as? ViewGroup)?.removeView(shakeBinding.root)
        _shakeBinding = null
        super.onDestroyView()
    }
}
