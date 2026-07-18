package com.bizane.app.data

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** وەکو FoodSyncService.swift — poll-based sync، بێ پێویستی بە Firestore realtime SDK */
object FoodSyncService {
    private val client get() = OkHttpClientProvider.client
    private val JSON = "application/json".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pollRunnable: Runnable? = null

    private fun itemsUrl(groupId: String, itemId: String? = null): String {
        var url = "${FirebaseConfig.firestoreBase}/groups/$groupId/items"
        if (itemId != null) url += "/$itemId"
        return url
    }

    private fun trashUrl(groupId: String, trashId: String? = null): String {
        var url = "${FirebaseConfig.firestoreBase}/groups/$groupId/trash"
        if (trashId != null) url += "/$trashId"
        return url
    }

    // MARK: - Local cache (بۆ کاتێک هێڵی ئینتەرنێت کاردەکات یان هەڵە ڕوودەدات)
    private fun cacheKey(groupId: String) = "group_items_cache_$groupId"

    private fun cachedItems(groupId: String): List<FoodItem> = try {
        val raw = Prefs.sp.getString(cacheKey(groupId), null)
        if (raw == null) emptyList() else {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { FoodItem.fromJson(arr.getJSONObject(it)) }
        }
    } catch (e: Exception) { emptyList() }

    private fun cacheItems(groupId: String, items: List<FoodItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        Prefs.sp.edit().putString(cacheKey(groupId), arr.toString()).apply()
    }

    /** سنافشوتی خێرا و هەمیشەیی (بێ چاوەڕوانی تۆڕ): کۆگای دواین-بینراو + ئایتمە چاوەڕوانەکان.
     * بۆ پیشاندانی ئایتمەکان دەستبەجێ لە کردنەوەی ئەپەکەدا، پێش وەرگرتنی وەڵامی سێرڤەر،
     * تاوەکو بەکارهێنەر هەرگیز لیستی بەتاڵ نەبینێت کاتێک بابەتی ڕاستەقینەی هەیە. */
    fun localSnapshot(groupId: String): List<FoodItem> = mergeWithPending(groupId, cachedItems(groupId))

    // MARK: - Pending items (ئایتمی چاوەڕوانی ناردن، بۆ نەون‌بوونیان لە کاتی نەبوونی خەت)
    private fun mergeWithPending(groupId: String, items: List<FoodItem>): List<FoodItem> {
        val pending = PendingSyncStorage.get(groupId)
        if (pending.isEmpty()) return items
        val existingIds = items.map { it.id }.toSet()
        return items + pending.filter { it.id !in existingIds }
    }

    fun fetchItems(groupId: String, completion: (List<FoodItem>) -> Unit) {
        AuthManager.validToken { token ->
            if (token == null) {
                mainThread { completion(mergeWithPending(groupId, cachedItems(groupId))) }; return@validToken
            }
            fetchItemsPage(groupId, token, null, emptyList()) { allItems ->
                if (allItems == null) {
                    mainThread { completion(mergeWithPending(groupId, cachedItems(groupId))) }
                } else {
                    cacheItems(groupId, allItems)
                    mainThread { completion(mergeWithPending(groupId, allItems)) }
                }
            }
        }
    }

    /** Firestore ـی listDocuments لاپەڕەیی دەکات (pagination)؛ ئەگەر nextPageToken بگەڕێنرێتەوە
     * دەبێت بەردەوام بین لە داواکردنی لاپەڕەکانی تر، نەوەک تەنیا لاپەڕەی یەکەم وەربگیرێت و ئایتمەکانی تر ون بن. */
    private fun fetchItemsPage(
        groupId: String, token: String, pageToken: String?, accumulated: List<FoodItem>,
        completion: (List<FoodItem>?) -> Unit
    ) {
        var url = itemsUrl(groupId) + "?pageSize=300"
        if (pageToken != null) {
            url += "&pageToken=" + java.net.URLEncoder.encode(pageToken, "UTF-8")
        }
        val req = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $token").build()
        OkHttpClientProvider.shortTimeout().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { completion(null) }
            override fun onResponse(call: Call, response: Response) {
                val json = try {
                    response.body?.string()?.let { JSONObject(it) }
                } catch (e: Exception) { null }
                if (json == null) { completion(null); return }
                val docs = json.optJSONArray("documents") ?: JSONArray()
                val pageItems = mutableListOf<FoodItem>()
                for (i in 0 until docs.length()) {
                    val doc = docs.getJSONObject(i)
                    val dict = FSValue.dict(doc)
                    if (!dict.has("name") || !dict.has("expiryDate")) continue
                    val name = dict.optString("name", "")
                    val expiry = (dict.opt("expiryDate") as? Long) ?: continue
                    val catRaw = dict.optString("category", FoodCategory.FOOD.raw)
                    val imgB64 = if (dict.has("imageData")) dict.optString("imageData") else null
                    val item = FoodItem(
                        name = name,
                        category = FoodCategory.fromRaw(catRaw),
                        purchaseDate = (dict.opt("purchaseDate") as? Long) ?: System.currentTimeMillis(),
                        expiryDate = expiry,
                        imageBase64 = imgB64,
                        notes = dict.optString("notes", ""),
                        ownerId = if (dict.has("ownerId")) dict.optString("ownerId") else null,
                        ownerName = if (dict.has("ownerName")) dict.optString("ownerName") else null
                    )
                    val docName = doc.optString("name")
                    if (docName.isNotEmpty()) item.firestoreId = FSValue.docId(docName)
                    pageItems.add(item)
                }
                val combined = accumulated + pageItems
                val nextToken = json.optString("nextPageToken", "")
                if (nextToken.isNotEmpty()) {
                    fetchItemsPage(groupId, token, nextToken, combined, completion)
                } else {
                    completion(combined)
                }
            }
        })
    }

    fun save(groupId: String, item: FoodItem, completion: (Boolean) -> Unit = {}) {
        AuthManager.validToken { token ->
            if (token == null) {
                PendingSyncStorage.enqueue(groupId, item)
                mainThread { completion(false) }
                return@validToken
            }
            val fields = JSONObject().apply {
                put("name", FSValue.str(item.name))
                put("category", FSValue.str(item.category.raw))
                put("purchaseDate", FSValue.ts(item.purchaseDate))
                put("expiryDate", FSValue.ts(item.expiryDate))
                put("notes", FSValue.str(item.notes))
            }
            item.imageBase64?.let { fields.put("imageData", FSValue.str(it)) }
            item.ownerId?.let { fields.put("ownerId", FSValue.str(it)) }
            item.ownerName?.let { fields.put("ownerName", FSValue.str(it)) }
            val body = JSONObject().put("fields", fields).toString().toRequestBody(JSON)
            val fid = item.firestoreId
            val builder = Request.Builder()
                .url(if (fid != null) itemsUrl(groupId, fid) else itemsUrl(groupId))
                .addHeader("Authorization", "Bearer $token")
            val req = if (fid != null) builder.method("PATCH", body).build() else builder.post(body).build()
            OkHttpClientProvider.shortTimeout(8).newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    PendingSyncStorage.enqueue(groupId, item)
                    mainThread { completion(false) }
                }
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        PendingSyncStorage.remove(groupId, item.id)
                        mainThread { completion(true) }
                    } else {
                        PendingSyncStorage.enqueue(groupId, item)
                        mainThread { completion(false) }
                    }
                }
            })
        }
    }

    /** هەوڵدانەوەی خۆکاری ناردنی هەموو ئایتمە چاوەڕوانەکان (کاتێک خەت باش بێت) */
    fun syncPendingItems(groupId: String, onDone: () -> Unit = {}) {
        val pending = PendingSyncStorage.get(groupId)
        if (pending.isEmpty()) { onDone(); return }
        var remaining = pending.size
        pending.forEach { item ->
            save(groupId, item) {
                remaining--
                if (remaining <= 0) onDone()
            }
        }
    }

    /** ئایتمێک دەسڕێتەوە، بەڵام پێش سڕینەوە تۆمارێکی کورتی لە «تەنەکەی خۆڵ»ی گروپ زیاد دەکات */
    fun delete(groupId: String, item: FoodItem, deletedByName: String, completion: (Boolean) -> Unit = {}) {
        val firestoreId = item.firestoreId ?: run { completion(false); return }
        logToTrash(groupId, item, deletedByName)
        AuthManager.validToken { token ->
            if (token == null) { mainThread { completion(false) }; return@validToken }
            val req = Request.Builder().url(itemsUrl(groupId, firestoreId))
                .addHeader("Authorization", "Bearer $token").delete().build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { mainThread { completion(false) } }
                override fun onResponse(call: Call, response: Response) { mainThread { completion(true) } }
            })
        }
    }

    private fun logToTrash(groupId: String, item: FoodItem, deletedByName: String) {
        AuthManager.validToken { token ->
            if (token == null) return@validToken
            val fields = JSONObject().apply {
                put("name", FSValue.str(item.name))
                put("deletedByName", FSValue.str(deletedByName))
                put("deletedAt", FSValue.ts(System.currentTimeMillis()))
            }
            item.ownerName?.let { fields.put("ownerName", FSValue.str(it)) }
            val body = JSONObject().put("fields", fields).toString().toRequestBody(JSON)
            val req = Request.Builder().url(trashUrl(groupId))
                .addHeader("Authorization", "Bearer $token").post(body).build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {}
            })
        }
    }

    /** دەرهێنانی لیستی «تەنەکەی خۆڵ»ی گروپ (تەنیا ئەدمین بانگی دەکات) */
    fun fetchTrash(groupId: String, completion: (List<TrashEntry>) -> Unit) {
        AuthManager.validToken { token ->
            if (token == null) { mainThread { completion(emptyList()) }; return@validToken }
            val req = Request.Builder().url(trashUrl(groupId))
                .addHeader("Authorization", "Bearer $token").build()
            OkHttpClientProvider.shortTimeout().newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { mainThread { completion(emptyList()) } }
                override fun onResponse(call: Call, response: Response) {
                    val json = try { response.body?.string()?.let { JSONObject(it) } } catch (e: Exception) { null }
                    if (json == null) { mainThread { completion(emptyList()) }; return }
                    val docs = json.optJSONArray("documents") ?: JSONArray()
                    val entries = mutableListOf<TrashEntry>()
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val dict = FSValue.dict(doc)
                        if (!dict.has("name") || !dict.has("deletedByName") || !dict.has("deletedAt")) continue
                        val fid = FSValue.docId(doc.optString("name"))
                        entries.add(
                            TrashEntry(
                                name = dict.optString("name"),
                                deletedByName = dict.optString("deletedByName"),
                                ownerName = if (dict.has("ownerName")) dict.optString("ownerName") else null,
                                deletedAt = (dict.opt("deletedAt") as? Long) ?: 0L,
                                firestoreId = fid
                            )
                        )
                    }
                    mainThread { completion(entries.sortedByDescending { it.deletedAt }) }
                }
            })
        }
    }

    /** بەتاڵکردنەوەی هەموو تۆمارەکانی «تەنەکەی خۆڵ»ی گروپ (سڕینەوەی ڕاستەقینەیی لە سێرڤەریش) */
    fun clearTrash(groupId: String, completion: (Boolean) -> Unit = {}) {
        fetchTrash(groupId) { entries ->
            val ids = entries.mapNotNull { it.firestoreId }
            if (ids.isEmpty()) { mainThread { completion(true) }; return@fetchTrash }
            AuthManager.validToken { token ->
                if (token == null) { mainThread { completion(false) }; return@validToken }
                var remaining = ids.size
                var allOk = true
                ids.forEach { id ->
                    val req = Request.Builder().url(trashUrl(groupId, id))
                        .addHeader("Authorization", "Bearer $token").delete().build()
                    client.newCall(req).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            allOk = false; remaining--
                            if (remaining == 0) mainThread { completion(allOk) }
                        }
                        override fun onResponse(call: Call, response: Response) {
                            remaining--
                            if (remaining == 0) mainThread { completion(allOk) }
                        }
                    })
                }
            }
        }
    }

    fun startPolling(
        groupId: String, intervalMs: Long = 4000,
        onUpdate: (List<FoodItem>) -> Unit, onKicked: () -> Unit = {}
    ) {
        stopPolling()
        fun tick() {
            fetchItems(groupId, onUpdate)
            syncPendingItems(groupId) { fetchItems(groupId, onUpdate) }
            refreshPermission(groupId)
            refreshDeletePermission(groupId)
            refreshDeleteLock(groupId)
            checkMembership(groupId, onKicked)
        }
        val runnable = object : Runnable {
            override fun run() {
                tick()
                mainHandler.postDelayed(this, intervalMs)
            }
        }
        pollRunnable = runnable
        tick()
        mainHandler.postDelayed(runnable, intervalMs)
    }

    private fun refreshPermission(groupId: String) {
        GroupService.fetchMyPermission(groupId) { canEdit -> AppSettings.canEditGroup = canEdit }
    }

    private fun refreshDeletePermission(groupId: String) {
        GroupService.fetchMyDeletePermission(groupId) { canDelete -> AppSettings.canDeleteOwnItems = canDelete }
    }

    private fun refreshDeleteLock(groupId: String) {
        GroupService.fetchDeleteUnlocked(groupId) { unlocked -> AppSettings.deleteUnlocked = unlocked }
    }

    private fun checkMembership(groupId: String, onKicked: () -> Unit) {
        GroupService.fetchIsMember(groupId) { isMember -> if (!isMember) onKicked() }
    }

    fun stopPolling() {
        pollRunnable?.let { mainHandler.removeCallbacks(it) }
        pollRunnable = null
    }
}
