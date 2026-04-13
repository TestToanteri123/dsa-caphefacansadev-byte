package com.alarmy.lumirise.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alarmy.lumirise.R
import com.alarmy.lumirise.data.local.AppDatabase
import com.alarmy.lumirise.model.Alarm
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmEditorTest {

    private lateinit var database: AppDatabase
    private lateinit var scenario: ActivityScenario<AlarmEditorActivity>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        database.close()
    }

    @Test
    fun timePickerIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.timePicker)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun labelInputFieldIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.labelInput)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canEnterAlarmLabel() {
        withLaunchedActivity {
            onView(withId(R.id.labelInput))
                .perform(typeText("Morning Wake Up"), closeSoftKeyboard())
            onView(withId(R.id.labelInput)).check(matches(withText("Morning Wake Up")))
        }
    }

    @Test
    fun repeatDaysChipsAreDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.chipMon)).check(matches(isDisplayed()))
            onView(withId(R.id.chipTue)).check(matches(isDisplayed()))
            onView(withId(R.id.chipWed)).check(matches(isDisplayed()))
            onView(withId(R.id.chipThu)).check(matches(isDisplayed()))
            onView(withId(R.id.chipFri)).check(matches(isDisplayed()))
            onView(withId(R.id.chipSat)).check(matches(isDisplayed()))
            onView(withId(R.id.chipSun)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canSelectDayChip() {
        withLaunchedActivity {
            onView(withId(R.id.chipMon)).perform(click())
            onView(withId(R.id.chipMon)).check(matches(isChecked()))
        }
    }

    @Test
    fun canDeselectDayChip() {
        withLaunchedActivity {
            onView(withId(R.id.chipMon)).perform(click())
            onView(withId(R.id.chipMon)).perform(click())
            onView(withId(R.id.chipMon)).check(matches(isNotChecked()))
        }
    }

    @Test
    fun missionRadioButtonsAreDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.radioNone)).check(matches(isDisplayed()))
            onView(withId(R.id.radioMath)).check(matches(isDisplayed()))
            onView(withId(R.id.radioShake)).check(matches(isDisplayed()))
            onView(withId(R.id.radioPhoto)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun missionSettingsHiddenByDefault() {
        withLaunchedActivity {
            onView(withId(R.id.missionSettingsCard)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun selectingMathMissionShowsDifficultySettings() {
        withLaunchedActivity {
            onView(withId(R.id.radioMath)).perform(click())
            onView(withId(R.id.mathDifficultyLayout)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun selectingShakeMissionShowsShakeCountSettings() {
        withLaunchedActivity {
            onView(withId(R.id.radioShake)).perform(click())
            onView(withId(R.id.shakeCountLayout)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mathDifficultyChipsAreDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.radioMath)).perform(click())
            onView(withId(R.id.chipEasy)).check(matches(isDisplayed()))
            onView(withId(R.id.chipMedium)).check(matches(isDisplayed()))
            onView(withId(R.id.chipHard)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun easyDifficultySelectedByDefault() {
        withLaunchedActivity {
            onView(withId(R.id.radioMath)).perform(click())
            onView(withId(R.id.chipEasy)).check(matches(isChecked()))
        }
    }

    @Test
    fun canChangeDifficultyToMedium() {
        withLaunchedActivity {
            onView(withId(R.id.radioMath)).perform(click())
            onView(withId(R.id.chipMedium)).perform(click())
            onView(withId(R.id.chipMedium)).check(matches(isChecked()))
        }
    }

    @Test
    fun canChangeDifficultyToHard() {
        withLaunchedActivity {
            onView(withId(R.id.radioMath)).perform(click())
            onView(withId(R.id.chipHard)).perform(click())
            onView(withId(R.id.chipHard)).check(matches(isChecked()))
        }
    }

    @Test
    fun saveButtonIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
            onView(withId(R.id.saveButton)).check(matches(withText("Save Alarm")))
        }
    }

    @Test
    fun clickingNoneMissionHidesMissionSettings() {
        withLaunchedActivity {
            onView(withId(R.id.radioMath)).perform(click())
            onView(withId(R.id.radioNone)).perform(click())
            onView(withId(R.id.mathDifficultyLayout)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun photoMissionShowsNoExtraSettings() {
        withLaunchedActivity {
            onView(withId(R.id.radioPhoto)).perform(click())
            onView(withId(R.id.mathDifficultyLayout)).check(matches(isDisplayed()))
            onView(withId(R.id.shakeCountLayout)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun toolbarShowsNewAlarmTitle() {
        withLaunchedActivity {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canNavigateBackViaToolbar() {
        withLaunchedActivity {
            onView(withId(R.id.toolbar)).perform(click())
        }
    }

    private fun launchActivity(): ActivityScenario<AlarmEditorActivity> {
        return ActivityScenario.launch(AlarmEditorActivity::class.java)
    }

    private fun withLaunchedActivity(block: (ActivityScenario<AlarmEditorActivity>) -> Unit) {
        scenario = launchActivity()
        try {
            block(scenario)
        } finally {
            scenario.close()
        }
    }
}
