package com.alarmy.lumirise.mission

import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.getSystemService
import com.alarmy.lumirise.R
import com.google.android.material.button.MaterialButton

class MathMissionFragment : BaseMissionFragment() {

    override val missionType: MissionType = MissionType.MATH

    private var mathContentView: View? = null
    private var problemTextView: TextView? = null
    private var progressTextView: TextView? = null
    
    private lateinit var problemGenerator: MathProblemGenerator
    private var currentProblem: MathProblemGenerator.MathProblem? = null
    private var correctAnswers = 0
    private var requiredAnswers = 3
    private var totalAttempts = 0
    private var vibrator: Vibrator? = null
    
    private var choiceButtons: List<MaterialButton> = emptyList()

    override fun setupMissionUI() {
        vibrator = requireContext().getSystemService()
        setupMathMissionView()
        initMission()
    }
    
    private fun setupMathMissionView() {
        val contentContainer = binding.missionContentContainer
        
        val inflater = LayoutInflater.from(requireContext())
        mathContentView = inflater.inflate(R.layout.fragment_math_mission, contentContainer, false)
        
        contentContainer.addView(mathContentView)
        
        problemTextView = mathContentView?.findViewById(R.id.problemText)
        progressTextView = mathContentView?.findViewById(R.id.progressIndicator)
        
        choiceButtons = listOf(
            mathContentView?.findViewById(R.id.choiceButton1)!!,
            mathContentView?.findViewById(R.id.choiceButton2)!!,
            mathContentView?.findViewById(R.id.choiceButton3)!!,
            mathContentView?.findViewById(R.id.choiceButton4)!!
        )
        
        setupChoiceButtons()
    }

    private fun initMission() {
        val difficulty = MissionDifficulty.fromString(
            MissionManager.getInstance().getCurrentMission()?.alarm?.missionDifficulty ?: "EASY"
        )
        problemGenerator = MathProblemGenerator(difficulty)
        
        requiredAnswers = when (difficulty) {
            MissionDifficulty.EASY -> 3
            MissionDifficulty.MEDIUM -> 5
            MissionDifficulty.HARD -> 7
        }
        
        showNextProblem()
        updateProgress()
    }

    private fun setupChoiceButtons() {
        choiceButtons.forEach { button ->
            button.setOnClickListener {
                onAnswerSelected(button)
            }
        }
    }

    private fun onAnswerSelected(selectedButton: MaterialButton) {
        totalAttempts++
        val selectedAnswer = selectedButton.text.toString().toIntOrNull() ?: return
        val correctAnswer = currentProblem?.correctAnswer ?: return

        if (selectedAnswer == correctAnswer) {
            correctAnswers++
            vibrateSuccess()
            updateProgress()

            if (correctAnswers >= requiredAnswers) {
                onMissionComplete()
            } else {
                showNextProblem()
            }
        } else {
            vibrateError()
            showNextProblem()
        }
    }

    private fun showNextProblem() {
        currentProblem = problemGenerator.generateProblem()
        problemTextView?.text = currentProblem?.question

        currentProblem?.choices?.forEachIndexed { index, choice ->
            if (index < choiceButtons.size) {
                choiceButtons[index].text = choice.toString()
            }
        }
    }

    private fun updateProgress() {
        val percentage = (correctAnswers * 100) / requiredAnswers
        missionManager.updateProgress(percentage, "Solve $correctAnswers/$requiredAnswers")
        progressTextView?.text = "$correctAnswers/$requiredAnswers solved"
    }

    private fun vibrateSuccess() {
        vibrator?.let { v ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(100)
            }
        }
    }

    private fun vibrateError() {
        vibrator?.let { v ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(longArrayOf(0, 50, 50, 50), -1)
            }
        }
    }

    private fun onMissionComplete() {
        missionManager.completeMission()
    }

    override fun getHintText(): String {
        return when ((correctAnswers * 100) / requiredAnswers) {
            in 0..33 -> "Tip: Break down the problem into smaller steps"
            in 34..66 -> "Tip: Double-check your arithmetic"
            else -> "Tip: You're almost there!"
        }
    }

    override fun onDestroyView() {
        mathContentView = null
        problemTextView = null
        progressTextView = null
        choiceButtons = emptyList()
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = MathMissionFragment()
    }
}
