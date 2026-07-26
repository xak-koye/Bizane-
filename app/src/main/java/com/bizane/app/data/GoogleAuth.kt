package com.bizane.app.data

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * چوونەژوورەوە بە Google بەکارهێنانی loopback flow (RFC 8252) + PKCE:
 * سێرڤەرێکی بچووکی HTTP لەسەر 127.0.0.1 هەڵدەستێنین، وێبگەڕی مۆبایل دەکەینەوە بۆ
 * پەڕەی چوونەژوورەوەی Google، Google دەگەڕێتەوە بۆ 127.0.0.1:<port> لەگەڵ authorization code،
 * ئیمە کۆدەکە دەگۆڕین بۆ token. هیچ SHA-1 یان Android Studio ـی پێویست نییە.
 */
object GoogleAuth {
    private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"

    val isConfigured: Boolean get() = DriveTokenStore.CLIENT_ID.isNotEmpty()

    fun signIn(activity: Activity, onResult: (success: Boolean, errorMessage: String?) -> Unit) {
        if (!isConfigured) {
            onResult(false, "Google Sign-In ڕێکنەخراوە (CLIENT_ID بەتاڵە)")
            return
        }

        val scope = "${DriveTokenStore.DRIVE_SCOPE} email profile openid"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
                val port = server.localPort
                val redirectUri = "http://127.0.0.1:$port"

                val verifier = randomVerifier()
                val challenge = codeChallenge(verifier)

                val authUrl = AUTH_URL +
                    "?client_id=${Uri.encode(DriveTokenStore.CLIENT_ID)}" +
                    "&redirect_uri=${Uri.encode(redirectUri)}" +
                    "&response_type=code" +
                    "&scope=${Uri.encode(scope)}" +
                    "&access_type=offline" +
                    "&prompt=consent" +
                    "&code_challenge=$challenge" +
                    "&code_challenge_method=S256"

                withContext(Dispatchers.Main) {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                }

                // چاوەڕوانی یەک ڕەوانەکردنی HTTP لە وێبگەڕەکەوە (redirect ـی دوای چوونەژوورەوە)
                server.soTimeout = 120_000 // ٢ خولەک وەستان بۆ چوونەژوورەوە
                val client = server.accept()
                val requestLine = client.getInputStream().bufferedReader().readLine() ?: ""
                // نموونە: "GET /?code=4/0Ab... HTTP/1.1"
                val path = requestLine.split(" ").getOrNull(1) ?: ""
                val uri = Uri.parse("http://127.0.0.1$path")
                val code = uri.getQueryParameter("code")
                val error = uri.getQueryParameter("error")

                val html = if (code != null)
                    "<html><body style='font-family:sans-serif;text-align:center;margin-top:60px'>" +
                        "\u2705 چوونەژوورەوە سەرکەوتوو بوو.<br>دەتوانیت ئەم پەڕەیە داخەیت و بگەڕێیتەوە بۆ ئەپەکە.</body></html>"
                else
                    "<html><body style='font-family:sans-serif;text-align:center;margin-top:60px'>" +
                        "\u274C هەڵەیەک ڕوویدا: $error</body></html>"
                client.getOutputStream().write(
                    ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n" + html)
                        .toByteArray()
                )
                client.getOutputStream().flush()
                client.close()
                server.close()

                if (code == null) {
                    withContext(Dispatchers.Main) { onResult(false, error ?: "Cancelled") }
                    return@launch
                }

                val ok = exchangeCodeForToken(code, redirectUri, verifier)
                withContext(Dispatchers.Main) {
                    if (ok) onResult(true, null) else onResult(false, "Token exchange failed")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "Unknown error") }
            }
        }
    }

    fun signOut() {
        DriveTokenStore.signOut()
    }

    private fun exchangeCodeForToken(code: String, redirectUri: String, verifier: String): Boolean {
        return try {
            val conn = URL(TOKEN_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val params = mutableListOf(
                "code" to code,
                "client_id" to DriveTokenStore.CLIENT_ID,
                "redirect_uri" to redirectUri,
                "grant_type" to "authorization_code",
                "code_verifier" to verifier
            )
            if (DriveTokenStore.CLIENT_SECRET.isNotEmpty()) {
                params.add("client_secret" to DriveTokenStore.CLIENT_SECRET)
            }
            val body = params.joinToString("&") { (k, v) -> "$k=${Uri.encode(v)}" }
            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                if (err != null) android.util.Log.e("GoogleAuth", "Token exchange error: $err")
                return false
            }

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val accessToken = json.optString("access_token", "")
            val refreshToken = if (json.has("refresh_token")) json.optString("refresh_token") else null
            val expiresIn = json.optLong("expires_in", 3600)
            val idToken = json.optString("id_token", "")

            DriveTokenStore.save(accessToken, refreshToken, expiresIn)
            decodeEmailFromIdToken(idToken)?.let { DriveTokenStore.accountEmail = it }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** id_token ـێکی JWT ـە — تەنیا payload ـەکەی decode دەکەین بۆ وەرگرتنی ئیمەیل (بێ پشکنینی واژۆ، تەنیا بۆ پیشاندان) */
    private fun decodeEmailFromIdToken(idToken: String): String? {
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
            val email = JSONObject(payload).optString("email", "")
            email.ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }

    private fun randomVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
