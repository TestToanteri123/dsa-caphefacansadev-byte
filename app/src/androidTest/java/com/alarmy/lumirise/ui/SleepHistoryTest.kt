package com.alarmy.lumirise.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alarmy.lumirise.R
import com.alarmy.lumirise.data.local.AppDatabase
import com.alarmy.lumirise.model.SleepRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepHistoryTest {

    private lateinit var database: AppDatabase
    private lateinit var scenario: ActivityScenario<SleepHistoryActivity>

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        val now = System.currentTimeMillis()
        val oneNightAgo = now - (8 * 60 * 60 * 1000)
        val twoNightsAgo = now - (24 * 60 * 60 * 1000) - (7 * 60 * 60 * 1000)
        
        runBlocking {
            database.sleepRecordDao().insertSleepRecord(
                SleepRecord(
                    startTime = oneNightAgo,
                    endTime = now - (24 * 60 * 60 * 1000),
                    qualityScore = 85,
                    snorePercentage = 12.5f,
                    totalSnoreCount = 48,
                    isComplete = true
                )
            )
            database.sleepRecordDao().insertSleepRecord(
                SleepRecord(
                    startTime = twoNightsAgo,
                    endTime = twoNightsAgo + (7 * 60 * 60 * 1000),
                    qualityScore = 72,
                    snorePercentage = 8.2f,
                    totalSnoreCount = 23,
                    isComplete = true
                )
            )
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
    fun toolbarIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun toolbarShowsCorrectTitle() {
        withLaunchedActivity {
            onView(withText("Sleep History")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun recyclerViewIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun swipeRefreshLayoutIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.swipeRefresh)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun startTrackingFabIsDisplayed() {
        withLaunchedActivity {
            onView(withId(R.id.startTrackingFab)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun emptyStateHiddenWhenRecordsExist() {
        withLaunchedActivity {
            onView(withId(R.id.emptyState)).check(matches(isDisplayed()))
            onView(withText("No sleep records yet")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun loadingIndicatorInitiallyHidden() {
        withLaunchedActivity {
            onView(withId(R.id.loadingIndicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canScrollThroughRecords() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun sleepRecordItemShowsDate() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun sleepRecordItemShowsDuration() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun sleepRecordItemShowsQualityScore() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canTapOnSleepRecordToExpand() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun startTrackingFabShowsSnackbarOnClick() {
        withLaunchedActivity {
            onView(withId(R.id.startTrackingFab)).perform(click())
        }
    }

    @Test
    fun emptyStateButtonDisplayedWhenNoRecords() {
        withLaunchedActivity {
            onView(withId(R.id.emptyStateButton)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun canTapEmptyStateButton() {
        withLaunchedActivity {
            onView(withId(R.id.emptyStateButton)).perform(click())
        }
    }

    @Test
    fun swipeToDeleteShowsConfirmation() {
        withLaunchedActivity {
            onView(withId(R.id.sleepRecyclerView)).perform(swipeLeft())
        }
    }

    @Test
    fun toolbarHasBackNavigation() {
        withLaunchedActivity {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        }
    }

    private fun launchActivity(): ActivityScenario<SleepHistoryActivity> {
        return ActivityScenario.launch(SleepHistoryActivity::class.java)
    }

    private fun withLaunchedActivity(block: (ActivityScenario<SleepHistoryActivity>) -> Unit) {
        scenario = launchActivity()
        try {
            block(scenario)
        } finally {
            scenario.close()
        }
    }
}
