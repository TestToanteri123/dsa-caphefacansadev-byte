package com.alarmy.lumirise.ui

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alarmy.lumirise.R
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun showsSingleCarViewAndBalance() {
        withLaunchedMain {
            onView(withId(R.id.singleCarContainer)).check(matches(isDisplayed()))
            onView(withId(R.id.singleCarName)).check(matches(isDisplayed()))
            onView(withId(R.id.singleCarPrice)).check(matches(isDisplayed()))
            onView(withId(R.id.balanceText)).check(matches(withText(containsString("$"))))
        }
    }

    @Test
    fun nextAndPreviousButtonsUpdatePageIndicator() {
        withLaunchedMain {
            onView(withId(R.id.nextButton)).perform(click())
            onView(withId(R.id.pageIndicator)).check(matches(withText(containsString("2 /"))))

            onView(withId(R.id.previousButton)).perform(click())
            onView(withId(R.id.pageIndicator)).check(matches(withText(containsString("1 /"))))
        }
    }

    @Test
    fun searchFiltersCarsByName() {
        withLaunchedMain {
            onView(withId(R.id.searchEditText)).perform(typeText("Tesla"), closeSoftKeyboard(), pressImeActionButton())
            onView(withId(R.id.singleCarName)).check(matches(withText(containsString("Tesla"))))
        }
    }

    @Test
    fun rentButtonNavigatesToDetailScreen() {
        withLaunchedMain { scenario ->
            onView(withId(R.id.singleCarRentButton)).perform(click())
            onView(withId(com.alarmy.lumirise.R.id.rentalDaysSlider)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun darkModeToggleAndFavoriteAreDisplayed() {
        withLaunchedMain {
            onView(withId(R.id.darkModeSwitch)).check(matches(isDisplayed()))
            onView(withId(R.id.darkModeSwitch)).perform(click())
            onView(withId(R.id.singleCarFavorite)).check(matches(isDisplayed()))
        }
    }

    private fun launchMain(): ActivityScenario<MainActivity> {
        seedAuthSession()
        return ActivityScenario.launch(MainActivity::class.java)
    }

    private fun withLaunchedMain(block: (ActivityScenario<MainActivity>) -> Unit) {
        val scenario = launchMain()
        try {
            block(scenario)
        } finally {
            scenario.close()
        }
    }

    private fun seedAuthSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", "espresso_token")
            .putString("user_id", "espresso_user")
            .putString("login_type", "email")
            .apply()
    }
}
