package com.bizane.app.data

import org.json.JSONArray

/**
 * ئایتمی خواردنی گروپ کە هێشتا نەگەیشتووەتە سێرڤەر (بەهۆی نەبوونی ئینتەرنێت یان خەتی لاواز).
 * ئایتمەکە دەخرێتە ئێرە تا کاتێک ئینتەرنێت/خەت باش بێت، ئینجا خۆکار دووبارە هەوڵدەدرێت بۆ ناردنی.
 * بەم شێوەیە ئایتمەکە فەوران لە لیستەکەدا دەردەکەوێت و ون نابێت، تەنانەت لە کاتی نەبوونی خەتیشدا.
 */
object PendingSyncStorage {
    private fun key(groupId: String) = "pending_sync_$groupId"

    fun get(groupId: String): MutableList<FoodItem> = try {
        val raw = Prefs.sp.getString(key(groupId), null)
        if (raw == null) mutableListOf() else {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { FoodItem.fromJson(arr.getJSONObject(it)) }.toMutableList()
        }
    } catch (e: Exception) { mutableListOf() }

    private fun persist(groupId: String, items: List<FoodItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        // بە commit() نەک apply(): commit ڕاستەوخۆ و بە هەمان جێگە (synchronous) دەنووسێتە
        // دیسک و دڵنیایی دەدات نووسینەکە تەواو بووە پێش گەڕانەوە. بەم شێوەیە ئەگەر
        // بەکارهێنەر یەکسەر دوای زیادکردنی ئایتم ئەپەکە دابخات (یان ئۆپەراتینگ سیستەم
        // پرۆسەکە بکوژێت)، ئایتمە نوێیەکە لەناو ناچێت چونکە پێش گەڕانەوەی enqueue()
        // بە تەواوی لە دیسکدا پارێزراوە.
        Prefs.sp.edit().putString(key(groupId), arr.toString()).commit()
    }

    /** ئایتمێک زیاد دەکات یان نوێ دەکاتەوە لە ڕیزی چاوەڕوانی ناردن (بەپێی id) */
    fun enqueue(groupId: String, item: FoodItem) {
        val items = get(groupId)
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) items[i] = item else items.add(item)
        persist(groupId, items)
    }

    /** دوای گەیشتنی سەرکەوتووی ئایتمەکە بۆ سێرڤەر، لە ڕیزی چاوەڕوانی دەریدەکات */
    fun remove(groupId: String, itemId: String) {
        val items = get(groupId)
        if (items.removeAll { it.id == itemId }) persist(groupId, items)
    }

    fun hasPending(groupId: String): Boolean = get(groupId).isNotEmpty()
}
