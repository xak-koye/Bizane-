package com.bizane.app.data

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

// MARK: - Category
enum class FoodCategory(val raw: String, val emoji: String) {
    ALL("هەموو", "🛒"),
    FOOD("خواردن", "🍽"),
    DRINK("نەساتل", "🧃"),
    FRIDGE("بەراد", "❄️");

    companion object {
        /** ئەو سێ جۆرەی دەتوانرێت لە کاتی زیادکردن/دەستکاریکردنی ئایتم هەڵبژێردرێت (بێ "هەموو") */
        val selectable = listOf(FOOD, DRINK, FRIDGE)

        fun fromRaw(raw: String): FoodCategory = values().find { it.raw == raw } ?: FOOD
    }
}

// MARK: - Model
data class FoodItem(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var category: FoodCategory = FoodCategory.FOOD,
    var purchaseDate: Long = System.currentTimeMillis(),
    var expiryDate: Long,
    var imageBase64: String? = null,
    var notes: String = ""
) {
    val daysLeft: Int
        get() {
            val today = startOfDay(System.currentTimeMillis())
            val exp = startOfDay(expiryDate)
            return ((exp - today) / 86_400_000L).toInt()
        }

    val isExpired: Boolean get() = daysLeft < 0

    val statusText: String
        get() {
            val d = daysLeft
            if (d < 0) return "بەسەرچووە!"
            if (d == 0) return "ئەمڕۆ بەسەردەچێت"
            if (d == 1) return "١ رۆژ ماوە"
            if (d < 7) return "$d رۆژ ماوە"
            val w = d / 7
            val r = d % 7
            return if (r == 0) "$w هەفتە ماوە" else "$w هەفتە، $r رۆژ ماوە"
        }

    val progress: Float
        get() {
            val total = (expiryDate - purchaseDate).toFloat()
            if (total <= 0f) return 1f
            val elapsed = (System.currentTimeMillis() - purchaseDate).toFloat()
            return (elapsed / total).coerceIn(0f, 1f)
        }

    /** ARGB Int (android.graphics.Color) — لە Compose‌دا بە Color(item.statusColor) بەکاردێت */
    val statusColor: Int
        get() {
            val d = daysLeft
            return when {
                d < 0 -> Color.parseColor("#FF3B30")   // systemRed
                d <= 3 -> Color.parseColor("#FF9500")  // systemOrange
                d <= 7 -> Color.parseColor("#FFCC00")  // زەرد
                else -> Color.parseColor("#33D976")    // سەوز
            }
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category.raw)
        put("purchaseDate", purchaseDate)
        put("expiryDate", expiryDate)
        put("imageBase64", imageBase64 ?: JSONObject.NULL)
        put("notes", notes)
    }

    companion object {
        fun fromJson(o: JSONObject): FoodItem = FoodItem(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", ""),
            category = FoodCategory.fromRaw(o.optString("category", FoodCategory.FOOD.raw)),
            purchaseDate = o.optLong("purchaseDate", System.currentTimeMillis()),
            expiryDate = o.optLong("expiryDate", System.currentTimeMillis()),
            imageBase64 = if (o.isNull("imageBase64")) null else o.optString("imageBase64", null),
            notes = o.optString("notes", "")
        )
    }
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// MARK: - Trash entry (هەموو داتای ئایتمەکەی تێدایە، بۆ گەڕاندنەوە)
data class TrashItem(
    val item: FoodItem,
    val deletedAt: Long
)

/** نووسینی فایلێکی JSON بە شێوەی atomic (یان بە تەواوی سەرکەوتوو دەبێت یان هیچ) —
 *  تاوەکو کاتێک ئەپ دەستبەجێ دوای دەستکارییەک دادەخرێت، داتا لەناونەچێت. */
private fun writeAtomic(file: File, content: String) {
    val tmp = File(file.parentFile, "${file.name}.tmp")
    tmp.writeText(content)
    tmp.renameTo(file) // rename لەسەر هەمان فایل-سیستەم atomic ـە
}

// MARK: - Local trash storage (کۆگای سڕاوەکان — ئۆفلاین، تەنیا لەسەر ئەم مۆبایلە)
object LocalTrashStorage {
    private const val FILE_NAME = "trash_items_v2.json"
    private val file get() = File(Storage.dir, FILE_NAME)

    var entries: MutableList<TrashItem> = mutableListOf()
        private set

    fun load() {
        if (!file.exists()) return
        entries = try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                TrashItem(FoodItem.fromJson(o.getJSONObject("item")), o.optLong("deletedAt"))
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    private fun save() {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply { put("item", e.item.toJson()); put("deletedAt", e.deletedAt) })
        }
        try { writeAtomic(file, arr.toString()) } catch (e: Exception) { }
    }

    fun add(item: FoodItem) {
        entries.add(0, TrashItem(item, System.currentTimeMillis()))
        if (entries.size > 100) entries = entries.take(100).toMutableList()
        save()
    }

    /** ئایتمەکە لادەبات لە خۆڵدان و دەیگەڕێنێتەوە بۆ ئەوەی بتوانرێت زیاد بکرێتەوە بۆ لیستی سەرەکی */
    fun restore(index: Int): FoodItem? {
        if (index !in entries.indices) return null
        val e = entries.removeAt(index)
        save()
        return e.item
    }

    fun removeForever(index: Int) {
        if (index !in entries.indices) return
        entries.removeAt(index)
        save()
    }

    fun clear() {
        entries = mutableListOf()
        save()
    }
}

/** شوێنی فایلەکان لە کۆگای ناوخۆیی ئامێرەکە — لە BizaneApp.onCreate دەست‌پێدەکات */
object Storage {
    lateinit var dir: File
        private set

    fun init(context: android.content.Context) {
        dir = context.filesDir
    }
}

// MARK: - Storage
// تێبینی: خواردنەکان لە فایلێکی JSON دا پاشەکەوت دەکرێن (نووسینی atomic)، نەک SharedPreferences —
// چونکە کاتێک وێنەی خواردن هەبوو، نووسینی گەورە دەکرێت هەندێک جار بەتەواوی نەگاتە دیسک ئەگەر
// ئەپ دەستبەجێ دوای دەستکارییەک داخرێت، لەبەرئەوە خواردنی سڕاوە/نوێکراوە پاش داخستنەوە دەگەڕایەوە.
object FoodStorage {
    private const val FILE_NAME = "food_items_v3.json"
    private val file get() = File(Storage.dir, FILE_NAME)
    private const val LEGACY_KEY = "food_items_v2" // شوێنی کۆن (SharedPreferences) بۆ گواستنەوەی یەکجارەکی

    var items: MutableList<FoodItem> = mutableListOf()
        private set

    fun load() {
        if (file.exists()) {
            items = try {
                val arr = JSONArray(file.readText())
                (0 until arr.length()).map { FoodItem.fromJson(arr.getJSONObject(it)) }.toMutableList()
            } catch (e: Exception) { mutableListOf() }
            return
        }
        // گواستنەوەی یەکجارەکی: ئەگەر یوزەرێک پێشتر داتای هەبووبێت لە SharedPreferences ـی کۆن
        val legacyRaw = Prefs.sp.getString(LEGACY_KEY, null)
        if (legacyRaw != null) {
            items = try {
                val arr = JSONArray(legacyRaw)
                (0 until arr.length()).map { FoodItem.fromJson(arr.getJSONObject(it)) }.toMutableList()
            } catch (e: Exception) { mutableListOf() }
            persist()
            Prefs.sp.edit().remove(LEGACY_KEY).apply()
        }
    }

    private fun persist() {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        try { writeAtomic(file, arr.toString()) } catch (e: Exception) { }
    }

    fun add(item: FoodItem) {
        items.add(item); persist()
    }

    fun update(item: FoodItem) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) { items[i] = item; persist() }
    }

    fun delete(id: String) {
        items.find { it.id == id }?.let { LocalTrashStorage.add(it) }
        items.removeAll { it.id == id }
        persist()
    }

    fun addSamplesIfNeeded() {
        if (items.isNotEmpty()) return
        val now = System.currentTimeMillis()
        fun d(days: Int) = now + days * 86_400_000L
        listOf(
            FoodItem(name = "شیر", category = FoodCategory.FRIDGE, purchaseDate = d(-5), expiryDate = d(3)),
            FoodItem(name = "ئایس کریم", category = FoodCategory.FRIDGE, purchaseDate = d(-20), expiryDate = d(30)),
            FoodItem(name = "ئاو", category = FoodCategory.DRINK, purchaseDate = d(-1), expiryDate = d(60)),
            FoodItem(name = "شەربەتی سێو", category = FoodCategory.DRINK, purchaseDate = d(-3), expiryDate = d(15)),
            FoodItem(name = "نان", category = FoodCategory.FOOD, purchaseDate = d(-2), expiryDate = d(5)),
            FoodItem(name = "نوشتەی چاکلیت", category = FoodCategory.FOOD, purchaseDate = d(-7), expiryDate = d(20))
        ).forEach { add(it) }
    }
}

// MARK: - Settings (وەکو AppSettings ـی سویفت)
object AppSettings {
    private val sp get() = Prefs.sp

    var isCardView: Boolean
        get() = sp.getBoolean("view_mode", false)
        set(v) = sp.edit().putBoolean("view_mode", v).apply()

    var notifDays: Int
        get() { val v = sp.getInt("notif_days", 0); return if (v == 0) 3 else v }
        set(v) = sp.edit().putInt("notif_days", v).apply()

    var userName: String
        get() = sp.getString("user_name", "") ?: ""
        set(v) = sp.edit().putString("user_name", v).apply()

    /** 0 = نزیکترین بەسەرچوون، 1 = دوورترین بەسەرچوون، 2 = ناو (ئا-یی) */
    var sortMode: Int
        get() = sp.getInt("sort_mode", 0)
        set(v) = sp.edit().putInt("sort_mode", v).apply()
}
