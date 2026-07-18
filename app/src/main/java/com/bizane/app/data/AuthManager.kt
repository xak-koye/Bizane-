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

    // MARK: - Google linking (Identity Toolkit REST, بێ پێویستی بە هیچ SDK ـێک)

    /** دەربڕی provider ی بەستراوە بەم هەژمارە ("google.com" / "") */
    val linkedProvider: String get() = sp.getString("fb_linked_provider", "") ?: ""
    val linkedEmail: String get() = sp.getString("fb_linked_email", "") ?: ""

    /** جیاکردنەوەی هەژماری Google لەم مۆبایلە - دووبارە دەبێتەوە بە anonymous.
     * تەنها یادکردنەوەی بەستنەوەی Google لادەبات - هەمان uid و بوونی لە گروپەکان دەمێنێتەوە.
     * (بۆ سڕینەوەی تەواوی هەژمار و دەستپێکردنەوە لە سەرەتا، سڕینەوەی ئەپ لە مۆبایل بکە) */
    fun signOutGoogle() {
        sp.edit().remove("fb_linked_provider").remove("fb_linked_email").apply()
    }

    /** دەبەستێتەوە credential ـی Google بە هەژمارە anonymous ـەکەی ئێستاوە، یان ئەگەر credential ـەکە
     * پێشتر بەسترابوو بە هەژمارێکی تر، بەخۆکار دەگۆڕدرێت بۆ هەمان uid ـی کۆن
     * (ئەمە پارێزگاریکردنی داتایە کاتی گۆڕینی مۆبایل/دامەزراندنەوەی ئەپ). */
    fun linkOrSignIn(providerId: String, idToken: String, completion: (Boolean, String?) -> Unit) {
        validToken { currentToken ->
            val postBody = "id_token=$idToken&providerId=$providerId"
            callSignInWithIdp(postBody, currentToken) { ok, email, err ->
                when {
                    ok -> {
                        sp.edit().putString("fb_linked_provider", providerId)
                            .putString("fb_linked_email", email ?: "").apply()
                        completion(true, null)
                    }
                    err == "CREDENTIAL_ALREADY_IN_USE" || err == "FEDERATED_USER_ID_ALREADY_LINKED" || err == "EMAIL_EXISTS" -> {
                        // ئەم هەژمارە پێشتر بەسترابووە بە uid ـێکی تر - بچۆرەوە بۆ ئەو uid ـە
                        // (ئەمە کاتێکە کە یوزەر لە مۆبایلێکی نوێوە هەوڵ دەدات)
                        callSignInWithIdp(postBody, null) { ok2, email2, err2 ->
                            if (ok2) {
                                sp.edit().putString("fb_linked_provider", providerId)
                                    .putString("fb_linked_email", email2 ?: "").apply()
                            }
                            completion(ok2, if (ok2) null else err2)
                        }
                    }
                    else -> completion(false, err)
                }
            }
        }
    }

    private fun callSignInWithIdp(
        postBody: String, existingIdToken: String?,
        completion: (Boolean, String?, String?) -> Unit
    ) {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=${FirebaseConfig.apiKey}"
        val bodyJson = JSONObject().apply {
            put("postBody", postBody)
            put("requestUri", "https://${FirebaseConfig.projectId}.firebaseapp.com")
            put("returnIdpCredential", true)
            put("returnSecureToken", true)
            if (existingIdToken != null) put("idToken", existingIdToken)
        }
        val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainThread { completion(false, null, "network") }
            }
            override fun onResponse(call: Call, response: Response) {
                val json = parseJson(response)
                val errObj = json?.optJSONObject("error")
                if (errObj != null) {
                    mainThread { completion(false, null, errObj.optString("message")) }; return
                }
                val errorMessage = json?.optString("errorMessage")
                if (!errorMessage.isNullOrEmpty()) {
                    mainThread { completion(false, null, errorMessage) }; return
                }
                if (json?.optBoolean("needConfirmation", false) == true) {
                    mainThread { completion(false, null, "CREDENTIAL_ALREADY_IN_USE") }; return
                }
                val token = json?.optString("idToken")
                val newUid = json?.optString("localId")
                val refresh = json?.optString("refreshToken")
                val exp = json?.optString("expiresIn")?.toDoubleOrNull()
                if (token.isNullOrEmpty() || newUid.isNullOrEmpty() || refresh.isNullOrEmpty() || exp == null) {
                    mainThread { completion(false, null, "parse") }; return
                }
                uid = newUid; idToken = token; refreshToken = refresh
                expiresAt = System.currentTimeMillis() + ((exp - 60) * 1000).toLong()
                persist()
                val email = json.optString("email").takeIf { it.isNotEmpty() }
                mainThread { completion(true, email, null) }
            }
        })
    }
}
