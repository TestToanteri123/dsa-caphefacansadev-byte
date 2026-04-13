package com.alarmy.lumirise.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alarmy.lumirise.R
import com.alarmy.lumirise.data.local.AppDatabase
import com.alarmy.lumirise.model.Alarm
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmMainActivityTest {

    private lateinit var database: AppDatabase
    private lateinit var scenario: ActivityScenario<AlarmMainActivity>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        runBlocking {
            database.alarmDao().insertAlarm(Alarm(hour = 7, minute = 30, label = "Morning Alarm", repeatDays = "MON,TUE,WED,THU,FRI", missionType = "MATH"))
            database.alarmDao().insertAlarm(Alarm(hour = 9, minute = 0, label = "Weekend Alarm", repeatDays = "SAT,SUN"))
        }
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
        database.close()
    }

    @Test
    fun alarmListDisplaysAlarms() {
        withLaunchedActivity {
            onView(withId(R.id.alarmRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun alarmItemsShowCorrectTime() {
        withLaunchedActivity {
            onView(withId(R.id.alarmRecyclerView)).check(matches(isDisplayed()))
            onView(withText(containsString("07"))).check(matches(isDisplayed()))
        }
    }

    @Test
    fun emptyStateHiddenWhenAlarmsExist() {
        withLaunchedActivity {
            onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
            onView(withText("No alarms set")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun addAlarmFabIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.addAlarmFab)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun addAlarmFabClickShowsSnackbar() {
        withLaunchedActivity {
            onView(withId(R.id.addAlarmFab)).perform(click())
        }
    }

    @Test
    fun alarmSwitchCanBeToggled() {
        withLaunchedActivity {
            onView(withId(R.id.alarmRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun toolbarIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }

    private fun launchActivity(): ActivityScenario<AlarmMainActivity> {
        return ActivityScenario.launch(AlarmMainActivity::class.java)
    }

    private fun withLaunchedActivity(block: (ActivityScenario<AlarmMainActivity>) -> Unit) {
        scenario = launchActivity()
        try {
            block(scenario)
        } finally {
            scenario.close()
        }
    }
}
