package com.bizane.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * تۆکنی Google Drive (access + refresh) پارێزراو دەکات و خۆکار نوێی دەکاتەوە.
 * سکێلێتۆنە — هەمان لۆجیکی DriveTokenStore.swift، بەڵام هێشتا پەیوەست نەکراوە بە هیچ
 * OAuth client ID ـێکی ڕاستەقینەوە. بۆ چالاککردنی تەواو پێویستە:
 *   1) پڕۆژەیەکی Firebase/Google Cloud (هەمانی iOS) + google-services.json زیاد بکرێت
 *   2) OAuth Client ID ـی جۆری Android (بە SHA-1 fingerprint) دروست بکرێت
 *   3) لە GoogleAuth.kt ـدا CLIENT_ID پڕبکرێتەوە
 */
object DriveTokenStore {
    // OAuth Client ID (جۆری Web application، بەکارهاتووە لەگەڵ redirect http://127.0.0.1:<port>)
    const val CLIENT_ID = "78294912903-ffjf1b5q162c2dibho92ftb4jm7nqabo.apps.googleusercontent.com"
    const val CLIENT_SECRET = "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"
    const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    private const val ACCESS_KEY = "drive_access_token"
    private const val REFRESH_KEY = "drive_refresh_token"
    private const val EXPIRY_KEY = "drive_token_expiry"
    private const val LAST_BACKUP_KEY = "drive_last_backup_date"
    private const val ACCOUNT_EMAIL_KEY = "drive_account_email"

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var expiresAtMillis: Long = 0L

    fun load() {
        val sp = Prefs.sp
        accessToken = sp.getString(ACCESS_KEY, null)
        refreshToken = sp.getString(REFRESH_KEY, null)
        expiresAtMillis = sp.getLong(EXPIRY_KEY, 0L)
    }

    private fun persist() {
        Prefs.sp.edit()
            .putString(ACCESS_KEY, accessToken)
            .putString(REFRESH_KEY, refreshToken)
            .putLong(EXPIRY_KEY, expiresAtMillis)
            .apply()
    }

    val isLinked: Boolean get() = refreshToken != null

    var accountEmail: String?
        get() = Prefs.sp.getString(ACCOUNT_EMAIL_KEY, null)
        set(value) = Prefs.sp.edit().putString(ACCOUNT_EMAIL_KEY, value).apply()

    var lastBackupMillis: Long?
        get() = Prefs.sp.getLong(LAST_BACKUP_KEY, 0L).takeIf { it > 0 }
        set(value) = Prefs.sp.edit().putLong(LAST_BACKUP_KEY, value ?: 0L).apply()

    /** پاش چوونەژوورەوەی سەرەتایی بانگ دەکرێت (لە GoogleAuth ـەوە) */
    fun save(accessToken: String?, refreshToken: String?, expiresInSeconds: Long?) {
        accessToken?.let { this.accessToken = it }
        refreshToken?.let { this.refreshToken = it } // تەنیا یەکەم جار دێت، هەڵیدەگرین
        expiresInSeconds?.let { this.expiresAtMillis = System.currentTimeMillis() + (it - 60) * 1000 }
        persist()
    }

    fun signOut() {
        accessToken = null; refreshToken = null; expiresAtMillis = 0L
        Prefs.sp.edit().remove(ACCESS_KEY).remove(REFRESH_KEY).remove(EXPIRY_KEY).remove(ACCOUNT_EMAIL_KEY).apply()
    }

    /** access_token ـێکی دروست دەگەڕێنێتەوە، ئەگەر پێویست بێت بە refresh_token نوێی دەکاتەوە */
    suspend fun validAccessToken(): String? = withContext(Dispatchers.IO) {
        val token = accessToken
        if (token != null && System.currentTimeMillis() < expiresAtMillis) return@withContext token
        val refresh = refreshToken ?: return@withContext null
        if (CLIENT_ID.isEmpty()) return@withContext null // هێشتا ڕێکنەخراوە

        try {
            val conn = URL("https://oauth2.googleapis.com/token").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val body = "client_id=$CLIENT_ID" +
                (if (CLIENT_SECRET.isNotEmpty()) "&client_secret=$CLIENT_SECRET" else "") +
                "&refresh_token=$refresh&grant_type=refresh_token"
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val newToken = json.optString("access_token", null.toString())
            val expiresIn = json.optLong("expires_in", 3600)
            save(accessToken = newToken, refreshToken = null, expiresInSeconds = expiresIn)
            newToken
        } catch (e: Exception) {
            null
        }
    }
}
