package com.bizane.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** گەڕان بۆ زانیاری بەرهەم بەپێی بارکۆد (Open Food Facts، خۆڕایی، بێ کلیلی API) — هاوشێوەی BarcodeScanner.swift */
object OpenFoodFactsLookup {

    data class ProductInfo(val name: String?, val imageUrl: String?)

    /** دەگەڕێتەوە null ئەگەر بەرهەمەکە نەدۆزرایەوە یان هەڵەیەک ڕوویدا.
     *  هەوڵی بارکۆدی خۆی دەدات، پاشان (ئەگەر نەدۆزرایەوە) شێوازی جیاوازی
     *  UPC-A/EAN-13 (سفری پێشەوە) کە زۆرجار وا لێدێت لەلایەن سکانەری Google‌ەوە. */
    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        fetchProduct(barcode)?.let { return@withContext it }
        for (alt in alternateBarcodes(barcode)) {
            fetchProduct(alt)?.let { return@withContext it }
        }
        null
    }

    /** Open Food Facts داوای دەکات هەموو ئەپەکان User-Agent ـێکی ڕوون بنێرن، بێ ئەوە
     *  هەندێک جار داواکارییەکان ڕەتدەکرێنەوە یان سنووردار دەکرێن. */
    private fun fetchProduct(barcode: String): ProductInfo? {
        return try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,image_front_url,image_url")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Bizane-Android/1.0 (https://github.com/) - Food expiry tracker")

            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optInt("status", 0) != 1) return null

            val product = json.optJSONObject("product") ?: return null
            val name = product.optString("product_name", "").trim().ifEmpty { null }
            val imageUrl = product.optString("image_front_url", "").ifEmpty {
                product.optString("image_url", "").ifEmpty { null }
            }
            if (name == null && imageUrl == null) null else ProductInfo(name = name, imageUrl = imageUrl)
        } catch (e: Exception) {
            null
        }
    }

    private fun alternateBarcodes(barcode: String): List<String> {
        val digitsOnly = barcode.trim()
        if (!digitsOnly.all { it.isDigit() }) return emptyList()
        val alts = mutableListOf<String>()
        if (digitsOnly.length == 12) alts.add("0$digitsOnly")           // UPC-A -> EAN-13
        if (digitsOnly.length == 13 && digitsOnly.startsWith("0")) alts.add(digitsOnly.substring(1)) // EAN-13 -> UPC-A
        return alts
    }

    suspend fun downloadImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(imageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }
}
