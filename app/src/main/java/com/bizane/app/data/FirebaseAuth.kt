package com.bizane.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ئۆتینتیکەیشنی نەناسراو (anonymous) بەسەر Identity Toolkit REST API، هاوشێوەی
 * AuthManager لە FirebaseREST.swift — بۆ ئەوەی SharedProductDB.kt بتوانێت
 * idToken ـێکی دروست بەکاربهێنێت کاتی نووسین بۆ Firestore (خوێندنەوە ڕێگەپێدراوە
 * بۆ هەمووان، بەڵام نووسین تەنیا بۆ ئۆتینتیکەیتکراوەکان، تەنانەت بە شێوەی anonymous).
 *
 * تەنیا بەشی سەرەکی (sign up + refresh) لێرە پەیڕەوکراوە؛ بەستنەوەی هەژماری
 * Google/Apple (وەک لە iOS) پێویست نییە بۆ داتابەیسی هاوبەشی بارکۆد.
 */
object FirebaseAuthTokenStore {
    private const val UID_KEY = "fb_uid"
    private const val ID_TOKEN_KEY = "fb_idtoken"
    private const val REFRESH_TOKEN_KEY = "fb_refresh"
    private const val EXPIRY_KEY = "fb_expiry"

    private var uid: String? = null
    private var idToken: String? = null
    private var refreshToken: String? = null
    private var expiresAtMillis: Long = 0L
    private var loaded = false

    private fun ensureLoaded() {
        if (loaded) return
        val sp = Prefs.sp
        uid = sp.getString(UID_KEY, null)
        idToken = sp.getString(ID_TOKEN_KEY, null)
        refreshToken = sp.getString(REFRESH_TOKEN_KEY, null)
        expiresAtMillis = sp.getLong(EXPIRY_KEY, 0L)
        loaded = true
    }

    private fun persist() {
        Prefs.sp.edit()
            .putString(UID_KEY, uid)
            .putString(ID_TOKEN_KEY, idToken)
            .putString(REFRESH_TOKEN_KEY, refreshToken)
            .putLong(EXPIRY_KEY, expiresAtMillis)
            .apply()
    }

    /** idToken ـێکی دروست دەگەڕێنێتەوە؛ ئەگەر پێویست بێت login/refresh دەکات. لە هەڵەدا null. */
    suspend fun validToken(): String? = withContext(Dispatchers.IO) {
        ensureLoaded()
        val token = idToken
        if (token != null && System.currentTimeMillis() < expiresAtMillis) return@withContext token
        val refresh = refreshToken
        if (refresh != null) refreshSession(refresh) else signUpAnonymously()
    }

    private fun signUpAnonymously(): String? {
        return try {
            val conn = URL(
                "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FirebaseConfig.apiKey}"
            ).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write("""{"returnSecureToken":true}""".toByteArray()) }
            if (conn.responseCode !in 200..299) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            applyAuthResponse(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun refreshSession(refresh: String): String? {
        return try {
            val conn = URL(
                "https://securetoken.googleapis.com/v1/token?key=${FirebaseConfig.apiKey}"
            ).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val body = "grant_type=refresh_token&refresh_token=$refresh"
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            uid = json.optString("user_id", uid)
            idToken = json.optString("id_token", null.toString()).takeIf { it != "null" }
            refreshToken = json.optString("refresh_token", refreshToken)
            val expiresIn = json.optString("expires_in", "3600").toDoubleOrNull() ?: 3600.0
            expiresAtMillis = System.currentTimeMillis() + ((expiresIn - 60) * 1000).toLong()
            persist()
            idToken
        } catch (e: Exception) {
            // هەوڵی داهاتوو (بۆ نموونە کاتێک ئینتەرنێت گەڕایەوە) دەتوانێت سەرکەوتوو بێت.
            null
        }
    }

    private fun applyAuthResponse(json: JSONObject): String? {
        val token = json.optString("idToken", null.toString()).takeIf { it != "null" } ?: return null
        val newUid = json.optString("localId", null.toString()).takeIf { it != "null" }
        val refresh = json.optString("refreshToken", null.toString()).takeIf { it != "null" }
        val expiresIn = json.optString("expiresIn", "3600").toDoubleOrNull() ?: 3600.0
        uid = newUid; idToken = token; refreshToken = refresh
        expiresAtMillis = System.currentTimeMillis() + ((expiresIn - 60) * 1000).toLong()
        persist()
        return token
    }
}
