package com.bizane.app.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/** Anonymous Auth بەسەر Identity Toolkit REST API — وەکو AuthManager.swift */
object AuthManager {
    private val client get() = OkHttpClientProvider.client
    private val sp get() = Prefs.sp

    var uid: String? = null
        private set
    private var idToken: String? = null
    private var refreshToken: String? = null
    private var expiresAt: Long = 0L
    private var loaded = false

    init { ensureLoaded() }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        uid = sp.getString("fb_uid", null)
        idToken = sp.getString("fb_idtoken", null)
        refreshToken = sp.getString("fb_refresh", null)
        expiresAt = sp.getLong("fb_expiry", 0L)
    }

    private fun persist() {
        sp.edit()
            .putString("fb_uid", uid)
            .putString("fb_idtoken", idToken)
            .putString("fb_refresh", refreshToken)
            .putLong("fb_expiry", expiresAt)
            .apply()
    }

    /** token ـێکی دروست دەگەڕێنێتەوە، ئەگەر پێویست بێت login/refresh دەکات */
    fun validToken(completion: (String?) -> Unit) {
        ensureLoaded()
        val token = idToken
        if (token != null && System.currentTimeMillis() < expiresAt) {
            completion(token); return
        }
        val refresh = refreshToken
        if (refresh != null) refreshSession(refresh, completion) else signUpAnonymously(completion)
    }

    private fun signUpAnonymously(completion: (String?) -> Unit) {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FirebaseConfig.apiKey}"
        val body = JSONObject().put("returnSecureToken", true).toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainThread { completion(null) } }
            override fun onResponse(call: Call, response: Response) {
                val json = parseJson(response)
                val token = json?.optString("idToken")
                val newUid = json?.optString("localId")
                val refresh = json?.optString("refreshToken")
                val exp = json?.optString("expiresIn")?.toDoubleOrNull()
                if (token.isNullOrEmpty() || newUid.isNullOrEmpty() || refresh.isNullOrEmpty() || exp == null) {
                    mainThread { completion(null) }; return
                }
                uid = newUid; idToken = token; refreshToken = refresh
                expiresAt = System.currentTimeMillis() + ((exp - 60) * 1000).toLong()
                persist()
                mainThread { completion(token) }
            }
        })
    }

    private fun refreshSession(refresh: String, completion: (String?) -> Unit) {
        val url = "https://securetoken.googleapis.com/v1/token?key=${FirebaseConfig.apiKey}"
        val formBody = "grant_type=refresh_token&refresh_token=$refresh"
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())
        val req = Request.Builder().url(url).post(formBody).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { signUpAnonymously(completion) }
            override fun onResponse(call: Call, response: Response) {
                val json = parseJson(response)
                val token = json?.optString("id_token")
                val newUid = json?.optString("user_id")
                val newRefresh = json?.optString("refresh_token")
                val exp = json?.optString("expires_in")?.toDoubleOrNull()
                if (token.isNullOrEmpty() || newUid.isNullOrEmpty() || newRefresh.isNullOrEmpty() || exp == null) {
                    signUpAnonymously(completion) // ئەگەر refresh شکستی هێنا، login ـی نوێ بکە
                    return
                }
                uid = newUid; idToken = token; refreshToken = newRefresh
                expiresAt = System.currentTimeMillis() + ((exp - 60) * 1000).toLong()
                persist()
                mainThread { completion(token) }
            }
        })
    }

    private fun parseJson(response: Response): JSONObject? = try {
        response.body?.string()?.let { JSONObject(it) }
    } catch (e: Exception) {
        null
    }
}
