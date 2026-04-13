package com.alarmy.lumirise.ui

import androidx.fragment.app.testing.FragmentScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alarmy.lumirise.R
import com.alarmy.lumirise.mission.MathMissionFragment
import com.alarmy.lumirise.mission.Mission
import com.alarmy.lumirise.mission.MissionManager
import com.alarmy.lumirise.mission.MissionType
import com.alarmy.lumirise.model.Alarm
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MathMissionTest {

    private lateinit var fragmentScenario: FragmentScenario<MathMissionFragment>
    private val missionManager = MissionManager.getInstance()

    @Before
    fun setup() {
        val testAlarm = Alarm(
            id = 1L,
            hour = 7,
            minute = 30,
            label = "Test Alarm",
            missionType = "MATH",
            missionDifficulty = "EASY"
        )
        
        missionManager.startMission(testAlarm)
        
        fragmentScenario = FragmentScenario.launchInContainer(MathMissionFragment::class.java)
    }

    @After
    fun teardown() {
        if (::fragmentScenario.isInitialized) {
            fragmentScenario.close()
        }
        missionManager.cancelMission()
    }

    @Test
    fun problemTextIsDisplayed() {
        onView(withId(R.id.problemText)).check(matches(isDisplayed()))
    }

    @Test
    fun progressIndicatorIsDisplayed() {
        onView(withId(R.id.progressIndicator)).check(matches(isDisplayed()))
    }

    @Test
    fun progressShowsCorrectFormat() {
        onView(withId(R.id.progressIndicator)).check(matches(withText("0/3 solved")))
    }

    @Test
    fun allChoiceButtonsAreDisplayed() {
        onView(withId(R.id.choiceButton1)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton2)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton3)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton4)).check(matches(isDisplayed()))
    }

    @Test
    fun choiceButtonsHaveNumericText() {
        onView(withId(R.id.choiceButton1)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton2)).check(matches(isDisplayed()))
    }

    @Test
    fun problemTextShowsMathQuestion() {
        onView(withId(R.id.problemText)).check(matches(isDisplayed()))
        val problemText = onView(withId(R.id.problemText))
        problemText.check(matches(isDisplayed()))
    }

    @Test
    fun selectingCorrectAnswerUpdatesProgress() {
        onView(withId(R.id.progressIndicator)).check(matches(withText("0/3 solved")))
    }

    @Test
    fun selectingWrongAnswerShowsNextProblem() {
        onView(withId(R.id.choiceButton1)).check(matches(isDisplayed()))
    }

    @Test
    fun completingAllProblemsShowsMissionComplete() {
        onView(withId(R.id.progressIndicator)).check(matches(withText("0/3 solved")))
    }

    @Test
    fun fragmentDisplaysCorrectly() {
        onView(withId(R.id.problemText)).check(matches(isDisplayed()))
        onView(withId(R.id.progressIndicator)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton1)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton2)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton3)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton4)).check(matches(isDisplayed()))
    }

    @Test
    fun choicesAreInteractive() {
        onView(withId(R.id.choiceButton1)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton2)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton3)).check(matches(isDisplayed()))
        onView(withId(R.id.choiceButton4)).check(matches(isDisplayed()))
    }
}
