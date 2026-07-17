package com.bizane.app.data

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

private val mainHandler = Handler(Looper.getMainLooper())

/** callback ـەکانی OkHttp لەسەر thread ـی پاشبنەمان دێن، ئەمە دەیانباتەوە بۆ main thread (وەک DispatchQueue.main.async لە سویفت) */
fun mainThread(action: () -> Unit) {
    mainHandler.post(action)
}

object OkHttpClientProvider {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /** client ـێکی تایبەت بە timeout ـی کورتتر (بۆ نموونە fetchMembers/fetchItems، وەک سویفت) */
    fun shortTimeout(seconds: Long = 15): OkHttpClient =
        client.newBuilder().callTimeout(seconds, TimeUnit.SECONDS).build()
}
