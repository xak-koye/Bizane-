package com.bizane.app

import android.app.Application
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.LocalTrashStorage
import com.bizane.app.data.Prefs
import com.bizane.app.data.Storage

class BizaneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Storage.init(this)
        FoodStorage.load()
        LocalTrashStorage.load()
    }
}
