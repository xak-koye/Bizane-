package com.bizane.app.data

import android.content.Context
import android.content.SharedPreferences

/** یەکجار لە BizaneApp.onCreate دەست‌پێدەکات، هەموو singleton ـەکانی تر بەکاری دەهێنن. */
object Prefs {
    private const val NAME = "bizane_prefs"
    lateinit var sp: SharedPreferences
        private set

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }
}
