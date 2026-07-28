package com.bizane.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * داتابەیسێکی هاوبەش (Firestore) کە هەموو بەکارهێنەرانی ئەپەکە (Android و iOS پێکەوە)
 * بەکاریدەهێنن، بۆ ئەوەی کاتێک بەکارهێنەرێک بارکۆدێک بۆ یەکەم جار بەدەستی زیاد دەکات
 * (ناو + وێنە)، ئەو زانیارییە پاشەکەوت بکرێت — تاوەکو هەر بەکارهێنەرێکی تر (لەسەر هەر
 * مۆبایل و پلاتفۆرمێک بێت، Android یان iOS) کاتێک هەمان بارکۆد سکان دەکات، دەستبەجێ ناو
 * و وێنەکەی بۆ پڕ بێتەوە، بێ پێویستی بە دووبارە نووسینەوەی هەمان زانیاری.
 *
 * جیاوازە لە OpenFoodFactsLookup: ئەوە داتابەیسێکی گشتی جیهانییە (خۆڕایی، هی هەموو ئەپەکان)،
 * ئەمە تایبەتە بە خودی ئەپەکەی "bizane"، بۆ بەرهەمی ناوخۆیی/کوردستانی کە زۆرجار لە
 * Open Food Facts نادۆزرێنەوە. لە کاتی سکانکردندا، سەرەتا Open Food Facts دەپشکنرێت،
 * دواتر ئەگەر نەدۆزرایەوە ئەم داتابەیسە (هەمان ڕیزبەندی وەک BarcodeScannerScreen/AddItemScreen ـی iOS).
 *
 * پێویستی بە هەمان Firestore Security Rules ـی SharedProductDB.swift هەیە.
 */
object SharedProductDB {

    data class ProductInfo(val name: String?, val image: Bitmap?)

    private const val COLLECTION = "products"

    private fun docUrl(barcode: String): URL? = try {
        val encoded = URLEncoder.encode(barcode, "UTF-8")
        URL("${FirebaseConfig.firestoreBase}/$COLLECTION/$encoded")
    } catch (e: Exception) {
        null
    }

    /** بەدوای ناو/وێنەی بەرهەمێک دەگەڕێت بەپێی بارکۆد لە داتابەیسی هاوبەشدا.
     *  ئەگەر نەدۆزرایەوە یان هیچ ئینتەرنێتێک نەبوو، بێدەنگانە null دەگەڕێنێتەوە. */
    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        try {
            val url = docUrl(barcode) ?: return@withContext null
            val token = FirebaseAuthTokenStore.validToken()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            if (token != null) conn.setRequestProperty("Authorization", "Bearer $token")

            if (conn.responseCode != 200) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val fields = json.optJSONObject("fields") ?: return@withContext null

            val name = fields.optJSONObject("name")?.optString("stringValue")?.takeIf { it.isNotEmpty() }
            val imageBase64 = fields.optJSONObject("image")?.optString("stringValue")?.takeIf { it.isNotEmpty() }
            val image = imageBase64?.let {
                try {
                    val bytes = Base64.decode(it, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }

            if (name == null && image == null) null else ProductInfo(name = name, image = image)
        } catch (e: Exception) {
            null
        }
    }

    /** ناو و وێنەی بەرهەمێک بۆ بارکۆدێک زیاد/نوێ دەکاتەوە لە داتابەیسی هاوبەشدا (upsert)،
     *  تاوەکو بەکارهێنەرانی تر کە دواتر هەمان بارکۆد سکان دەکەن سوودی لێ ببینن.
     *  بێدەنگانە کاردەکات لە پاشبنەمادا — هیچ هەڵەیەک بۆ بەکارهێنەر نیشان نادات، چونکە
     *  شکستی ئەم کارە نابێتە هۆی کێشە بۆ ئایتمەکەی خۆی (کە پێشتر لۆکاڵی پاشەکەوتکراوە).
     *
     *  تێبینی: بە قەستە scope ـی خۆی هەیە (نەک ئەوەی پەڕەکە)، چونکە دوای پاشەکەوتکردن
     *  پەڕەکە دادەخرێت و لەوانەیە rememberCoroutineScope ـی پەڕەکە پێش تەواوبوونی
     *  داواکارییەکە هەڵبوەشێتەوە. */
    fun submit(barcode: String, name: String, image: Bitmap?) {
        CoroutineScope(Dispatchers.IO).launch {
            submitBlocking(barcode, name, image)
        }
    }

    private suspend fun submitBlocking(barcode: String, name: String, image: Bitmap?) = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            if (barcode.isEmpty() || cleanName.isEmpty()) return@withContext
            val url = docUrl(barcode) ?: return@withContext
            val token = FirebaseAuthTokenStore.validToken() ?: return@withContext // بێ ئۆتینتیکەیشن، Rules ڕێگای نووسین نادات

            val fields = JSONObject()
            fields.put("name", JSONObject().put("stringValue", cleanName))
            fields.put("updatedAt", JSONObject().put("timestampValue", isoNow()))
            resizedThumbnail(image, maxDimension = 300)?.let { thumb ->
                val baos = ByteArrayOutputStream()
                thumb.compress(Bitmap.CompressFormat.JPEG, 50, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                fields.put("image", JSONObject().put("stringValue", b64))
            }
            val body = JSONObject().put("fields", fields)

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PATCH"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            conn.responseCode // بێدەنگانە: سەرکەوتن یان شکستی ئەم نووسینە کاریگەری نییە لەسەر ئەزموونی بەکارهێنەری ئێستا
        } catch (e: Exception) {
            // بێدەنگانە شکست دەهێنێت — تەنیا هەوڵێکی یارمەتیدەرە بۆ بەکارهێنەرانی داهاتوو
        }
    }

    private fun resizedThumbnail(image: Bitmap?, maxDimension: Int): Bitmap? {
        if (image == null) return null
        val maxSide = maxOf(image.width, image.height)
        if (maxSide <= maxDimension) return image
        val scale = maxDimension.toFloat() / maxSide
        val newWidth = (image.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (image.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(image, newWidth, newHeight, true)
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
