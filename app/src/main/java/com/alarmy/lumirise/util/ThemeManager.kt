package com.alarmy.lumirise.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * ThemeManager - Manages light/dark theme preferences for LumiRise app.
 * 
 * Default is dark mode to match the app's original design.
 */
object ThemeManager {

    private const val PREFS_NAME = "lumirise_theme_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"
    
    // Default to dark mode
    private const val DEFAULT_DARK_MODE = true

    private lateinit var prefs: SharedPreferences

    /**
     * Initialize ThemeManager. Should be called once from Application class.
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if dark mode is enabled.
     * @return true if dark mode is enabled, false otherwise
     */
    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, DEFAULT_DARK_MODE)
    }

    /**
     * Set dark mode preference.
     * @param enabled true to enable dark mode, false for light mode
     */
    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        applyTheme()
    }

    /**
     * Toggle dark mode.
     * @return the new dark mode state after toggle
     */
    fun toggleDarkMode(): Boolean {
        val newState = !isDarkMode()
        setDarkMode(newState)
        return newState
    }

    /**
     * Apply the theme to the current process.
     * Uses AppCompatDelegate for global theme application.
     */
    fun applyTheme() {
        val mode = if (isDarkMode()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Apply theme to a specific activity.
     * This should be called before setContentView() in onCreate().
     * 
     * @param activity The activity to apply theme to
     */
    fun applyTheme(activity: Activity) {
        if (isDarkMode()) {
            activity.setTheme(com.alarmy.lumirise.R.style.Theme_LumiRise)
        } else {
            activity.setTheme(com.alarmy.lumirise.R.style.Theme_LumiRise)
        }
    }

    /**
     * Apply alarm-specific theme to a specific activity.
     * 
     * @param activity The activity to apply alarm theme to
     */
    fun applyAlarmTheme(activity: Activity) {
        if (isDarkMode()) {
            activity.setTheme(com.alarmy.lumirise.R.style.Theme_LumiRise_Alarm)
        } else {
            activity.setTheme(com.alarmy.lumirise.R.style.Theme_LumiRise_Alarm)
        }
    }

    /**
     * Get the current night mode constant for AppCompatDelegate.
     * @return MODE_NIGHT_YES or MODE_NIGHT_NO
     */
    fun getNightMode(): Int {
        return if (isDarkMode()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    /**
     * Recreate all activities to apply theme change.
     * Should be called after toggling dark mode.
     * 
     * @param activity The activity to recreate (usually the main activity)
     */
    fun recreateForThemeChange(activity: Activity) {
        applyTheme()
        activity.recreate()
    }
}
