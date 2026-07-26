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

    /** دەگەڕێتەوە null ئەگەر بەرهەمەکە نەدۆزرایەوە یان هەڵەیەک ڕوویدا */
    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,image_front_url,image_url")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (json.optInt("status", 0) != 1) return@withContext null

            val product = json.optJSONObject("product") ?: return@withContext null
            val name = product.optString("product_name", "").trim().ifEmpty { null }
            val imageUrl = product.optString("image_front_url", "").ifEmpty {
                product.optString("image_url", "").ifEmpty { null }
            }
            ProductInfo(name = name, imageUrl = imageUrl)
        } catch (e: Exception) {
            null
        }
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
