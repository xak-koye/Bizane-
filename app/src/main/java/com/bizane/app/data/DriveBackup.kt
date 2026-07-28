package com.bizane.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * باکئەپ/گەڕاندنەوەی خواردنەکان بۆ/لە Google Drive (appDataFolder — بەشێکی شاراوە،
 * تەنیا ئەم ئەپە دەیبینێت). سکێلێتۆنە: لۆجیکەکە تەواوە بەڵام پشت بە
 * DriveTokenStore.validAccessToken() دەبەستێت کە هێشتا کارا نییە (پێویستی بە CLIENT_ID).
 * هاوشێوەی DriveBackup.swift.
 */
object DriveBackup {
    private const val FILE_NAME = "bizane_backup.json"
    private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"

    sealed class Result {
        data object Success : Result()
        data class Failure(val message: String) : Result()
    }

    /** خواردنەکانی ئێستا وەک JSON بارکردن بۆ Drive (appDataFolder)، نوێکردنەوە ئەگەر پێشتر هەبوو */
    suspend fun backup(): Result = withContext(Dispatchers.IO) {
        val token = DriveTokenStore.validAccessToken()
            ?: return@withContext Result.Failure("Not signed in")

        try {
            val payload = JSONObject().apply {
                put("items", JSONArray(FoodStorage.items.map { it.toJson() }))
                put("exportedAt", System.currentTimeMillis())
            }.toString()

            val existingId = findBackupFileId(token)

            if (existingId != null) {
                // نوێکردنەوەی فایلی هەبوو (PATCH + media)
                val conn = URL("$UPLOAD_URL/$existingId?uploadType=media").openConnection() as HttpURLConnection
                conn.requestMethod = "PATCH"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(payload.toByteArray()) }
                if (conn.responseCode !in 200..299) return@withContext Result.Failure("HTTP ${conn.responseCode}")
            } else {
                // فایلی نوێ لەگەڵ metadata (multipart) — دەخرێتە appDataFolder
                val boundary = "bizane_boundary"
                val metadata = JSONObject().apply {
                    put("name", FILE_NAME)
                    put("parents", JSONArray(listOf("appDataFolder")))
                }
                val body = buildString {
                    append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata.toString())
                    append("\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n")
                    append(payload)
                    append("\r\n--$boundary--")
                }
                val conn = URL("$UPLOAD_URL?uploadType=multipart").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.outputStream.use { it.write(body.toByteArray()) }
                if (conn.responseCode !in 200..299) return@withContext Result.Failure("HTTP ${conn.responseCode}")
            }

            DriveTokenStore.lastBackupMillis = System.currentTimeMillis()
            Result.Success
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Unknown error")
        }
    }

    /** هەموو خواردنەکانی ئێستا بە خواردنەکانی باکئەپەکە دەگۆڕێتەوە */
    suspend fun restore(): Result = withContext(Dispatchers.IO) {
        val token = DriveTokenStore.validAccessToken()
            ?: return@withContext Result.Failure("Not signed in")

        try {
            val fileId = findBackupFileId(token) ?: return@withContext Result.Failure("No backup found")
            val conn = URL("$FILES_URL/$fileId?alt=media").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (conn.responseCode !in 200..299) return@withContext Result.Failure("HTTP ${conn.responseCode}")

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val itemsArray = json.optJSONArray("items") ?: JSONArray()
            val restored = (0 until itemsArray.length()).map { FoodItem.fromJson(itemsArray.getJSONObject(it)) }

            FoodStorage.replaceAll(restored)
            Result.Success
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Unknown error")
        }
    }

    private fun findBackupFileId(token: String): String? {
        val query = java.net.URLEncoder.encode("name='$FILE_NAME' and 'appDataFolder' in parents", "UTF-8")
        val conn = URL("$FILES_URL?spaces=appDataFolder&q=$query&fields=files(id,name)")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        if (conn.responseCode !in 200..299) return null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val files = JSONObject(body).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).optString("id").ifEmpty { null }
    }
}

/**
 * چاودێری باکئەپی خۆکارانە — کاتێک ئەپ دەبێتە چالاک، پشکنین دەکات ئایا بەپێی
 * هەڵبژاردەی یوزەر (هەر کردنەوەیەک/ڕۆژانە/هەفتانە) کاتی باکئەپی نوێ هاتووە.
 * بێدەنگانە کاردەکات (بێ هیچ Toast یان لۆدینگ)، تەنیا کاتێک بەکارهێنەر بە Google
 * پارێزراوە. هاوشێوەی AutoBackupManager ـی iOS، بەڵام لێرە بە suspend fun ـە چونکە
 * Drive backup لە Android ـدا کۆرۆتینە.
 */
object AutoBackupManager {
    private const val LAST_AUTO_RUN_KEY = "auto_backup_last_run"

    private var lastAutoRunMillis: Long
        get() = Prefs.sp.getLong(LAST_AUTO_RUN_KEY, 0L)
        set(value) = Prefs.sp.edit().putLong(LAST_AUTO_RUN_KEY, value).apply()

    /** دەبێت لە کاتی چالاکبوونی ئەپ (foreground، بۆ نموونە MainActivity.onCreate) بانگ بکرێت */
    suspend fun runIfNeeded() {
        if (!DriveTokenStore.isLinked) return

        val mode = AppSettings.autoBackupMode
        if (mode == AutoBackupMode.MANUAL) return

        val now = System.currentTimeMillis()
        when (mode) {
            AutoBackupMode.ON_OPEN -> Unit // هەموو جارێک کە ئەپ دەکرێتەوە
            AutoBackupMode.DAILY -> {
                val last = lastAutoRunMillis
                if (last > 0 && now - last < 24L * 3600 * 1000) return
            }
            AutoBackupMode.WEEKLY -> {
                val last = lastAutoRunMillis
                if (last > 0 && now - last < 7L * 24 * 3600 * 1000) return
            }
            AutoBackupMode.MANUAL -> return
        }

        lastAutoRunMillis = now
        DriveBackup.backup() // بێدەنگ — سەرکەوتن/شکستی کاریگەری نییە لەسەر ئەزموونی بەکارهێنەر
    }
}
