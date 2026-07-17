package com.bizane.app.data

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
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
    var notes: String = "",
    var firestoreId: String? = null,
    var ownerId: String? = null,
    var ownerName: String? = null
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
        put("firestoreId", firestoreId ?: JSONObject.NULL)
        put("ownerId", ownerId ?: JSONObject.NULL)
        put("ownerName", ownerName ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): FoodItem = FoodItem(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", ""),
            category = FoodCategory.fromRaw(o.optString("category", FoodCategory.FOOD.raw)),
            purchaseDate = o.optLong("purchaseDate", System.currentTimeMillis()),
            expiryDate = o.optLong("expiryDate", System.currentTimeMillis()),
            imageBase64 = if (o.isNull("imageBase64")) null else o.optString("imageBase64", null),
            notes = o.optString("notes", ""),
            firestoreId = if (o.isNull("firestoreId")) null else o.optString("firestoreId", null),
            ownerId = if (o.isNull("ownerId")) null else o.optString("ownerId", null),
            ownerName = if (o.isNull("ownerName")) null else o.optString("ownerName", null)
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

// MARK: - Trash entry (تۆماری «تەنەکەی خۆڵ»ی گروپ)
data class TrashEntry(
    val name: String,
    val deletedByName: String,
    val ownerName: String? = null,
    val deletedAt: Long,
    val firestoreId: String? = null
)

// MARK: - Local personal storage (کۆگای کەسی، بێ گروپ)
object FoodStorage {
    private const val KEY = "food_items_v2"
    private val sp get() = Prefs.sp

    var items: MutableList<FoodItem> = mutableListOf()
        private set

    fun load() {
        val raw = sp.getString(KEY, null) ?: return
        items = try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { FoodItem.fromJson(arr.getJSONObject(it)) }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun persist() {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        sp.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(item: FoodItem) {
        items.add(item); persist()
    }

    fun update(item: FoodItem) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) { items[i] = item; persist() }
    }

    fun delete(id: String) {
        items.removeAll { it.id == id }; persist()
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

    // Group
    var groupId: String
        get() = sp.getString("group_id", "") ?: ""
        set(v) = sp.edit().putString("group_id", v).apply()
    var groupName: String
        get() = sp.getString("group_name", "") ?: ""
        set(v) = sp.edit().putString("group_name", v).apply()
    var groupCode: String
        get() = sp.getString("group_code", "") ?: ""
        set(v) = sp.edit().putString("group_code", v).apply()
    var groupOwnerId: String
        get() = sp.getString("group_owner_id", "") ?: ""
        set(v) = sp.edit().putString("group_owner_id", v).apply()

    val isGroupAdmin: Boolean
        get() = groupId.isNotEmpty() && groupOwnerId.isNotEmpty() && groupOwnerId == AuthManager.uid

    fun setGroup(id: String, name: String, code: String, ownerId: String) {
        groupId = id; groupName = name; groupCode = code; groupOwnerId = ownerId
    }

    fun clearGroup() {
        groupId = ""; groupName = ""; groupCode = ""; groupOwnerId = ""; deleteUnlocked = false
    }

    /** کاتێک true بێت، هەموو ئەندامان (تەنانەت ئەدمینیش) دەتوانن هەر ئایتمێک بسڕنەوە */
    var deleteUnlocked: Boolean
        get() = sp.getBoolean("group_delete_unlocked", false)
        set(v) = sp.edit().putBoolean("group_delete_unlocked", v).apply()

    /** ئایا ئەم ئەندامە ڕێگەی زیادکردن/دەستکاریکردنی ئایتمی هەیە (ئەدمین هەمیشە بەڵێیە) */
    var canEditGroup: Boolean
        get() {
            if (isGroupAdmin) return true
            if (!sp.contains("group_can_edit")) return true
            return sp.getBoolean("group_can_edit", true)
        }
        set(v) = sp.edit().putBoolean("group_can_edit", v).apply()

    /** ئایا ئەم ئەندامە ڕێگەی سڕینەوەی ئایتمی خۆی هەیە (ئەدمین هەمیشە بەڵێیە) */
    var canDeleteOwnItems: Boolean
        get() {
            if (isGroupAdmin) return true
            if (!sp.contains("group_can_delete")) return true
            return sp.getBoolean("group_can_delete", true)
        }
        set(v) = sp.edit().putBoolean("group_can_delete", v).apply()
}
