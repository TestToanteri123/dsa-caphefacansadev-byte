package com.alarmy.lumirise

import android.app.Application
import com.alarmy.lumirise.util.ThemeManager

class LumiRiseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)
        ThemeManager.applyTheme()
    }
}
