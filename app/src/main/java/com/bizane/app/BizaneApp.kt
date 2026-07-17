package com.bizane.app

import android.app.Application
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.Prefs

class BizaneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        FoodStorage.load()
    }
}
