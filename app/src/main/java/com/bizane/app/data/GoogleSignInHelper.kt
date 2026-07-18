package com.bizane.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * چوونەژوورەوە بە Google لە ڕێگەی Authorization Code + PKCE، بە بەکارهێنانی Chrome Custom Tabs.
 * هەمان شێوازی GoogleSignInHelper.swift ـی ئایفۆن — بێ پێویستی بە هیچ Google Sign-In SDK ـێک.
 */
object GoogleSignInHelper {
    private var codeVerifier: String? = null
    private var pendingCompletion: ((String?, String?) -> Unit)? = null

    private fun randomString(length: Int): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
        val random = SecureRandom()
        val sb = StringBuilder()
        repeat(length) { sb.append(chars[random.nextInt(chars.length)]) }
        return sb.toString()
    }

    private fun base64Url(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun redirectUri() = "${FirebaseConfig.googleRedirectScheme}:/oauth2redirect"

    /** completion: (idToken, errorMessage) — دەبانگدرێتەوە دوای گەڕانەوەی OAuthRedirectActivity */
    fun start(context: Context, completion: (String?, String?) -> Unit) {
        if (FirebaseConfig.googleClientID.startsWith("YOUR_")) {
            completion(null, "Google Client ID پێشتر لە FirebaseConfig.kt دانەنراوە."); return
        }
        pendingCompletion = completion
        val verifier = randomString(64)
        codeVerifier = verifier
        val challenge = base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8)))

        val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", FirebaseConfig.googleClientID)
            .appendQueryParameter("redirect_uri", redirectUri())
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "openid email profile")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("prompt", "select_account")
            .build()

        val tabsIntent = CustomTabsIntent.Builder().build()
        tabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tabsIntent.launchUrl(context, authUrl)
    }

    /** دەبانگدرێت لە OAuthRedirectActivity کاتێک Google بگەڕێتەوە بۆ ئەپەکە بە کۆدی OAuth */
    fun handleRedirect(uri: Uri?) {
        val completion = pendingCompletion
        pendingCompletion = null
        val verifier = codeVerifier
        codeVerifier = null
        val code = uri?.getQueryParameter("code")
        if (code == null || verifier == null) {
            val err = uri?.getQueryParameter("error")
            mainThread { completion?.invoke(null, err ?: "هیچ وەڵامێک نەگەڕایەوە.") }
            return
        }
        exchangeCode(code, verifier, completion)
    }

    /** کاتێک بەکارهێنەر لە Chrome Custom Tab پاشگەز دەبێتەوە بەبێ تەواوکردنی چوونەژوورەوە */
    fun cancelPending() {
        val completion = pendingCompletion
        pendingCompletion = null
        codeVerifier = null
        if (completion != null) mainThread { completion(null, null) }
    }

    private fun exchangeCode(code: String, verifier: String, completion: ((String?, String?) -> Unit)?) {
        val formBody = FormBody.Builder()
            .add("client_id", FirebaseConfig.googleClientID)
            .add("code", code)
            .add("code_verifier", verifier)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", redirectUri())
            .build()
        val req = Request.Builder().url("https://oauth2.googleapis.com/token").post(formBody).build()
        OkHttpClientProvider.client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainThread { completion?.invoke(null, e.localizedMessage ?: "network") }
            }
            override fun onResponse(call: Call, response: Response) {
                val json = try {
                    response.body?.string()?.let { JSONObject(it) }
                } catch (e: Exception) { null }
                val err = json?.optString("error_description")?.takeIf { it.isNotEmpty() }
                    ?: json?.optString("error")?.takeIf { it.isNotEmpty() }
                if (err != null) { mainThread { completion?.invoke(null, err) }; return }
                val idToken = json?.optString("id_token")
                if (idToken.isNullOrEmpty()) {
                    mainThread { completion?.invoke(null, "id_token نەگەڕایەوە") }; return
                }
                mainThread { completion?.invoke(idToken, null) }
            }
        })
    }
}
