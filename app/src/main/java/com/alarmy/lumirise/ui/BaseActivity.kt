package com.alarmy.lumirise.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alarmy.lumirise.util.ThemeManager

/**
 * BaseActivity - Base class for all activities that need theme support.
 * Handles theme application before content view is set.
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)
    }

    /**
     * Apply the current theme based on user's preference.
     * Override this method in subclasses if they need a different theme.
     */
    protected open fun applyTheme() {
        ThemeManager.applyTheme(this)
    }

    /**
     * Apply alarm-specific theme.
     * Call this in subclasses that need alarm styling.
     */
    protected fun applyAlarmTheme() {
        ThemeManager.applyAlarmTheme(this)
    }

    /**
     * Recreate the activity after theme change.
     */
    protected fun recreateForThemeChange() {
        ThemeManager.recreateForThemeChange(this)
    }
}
