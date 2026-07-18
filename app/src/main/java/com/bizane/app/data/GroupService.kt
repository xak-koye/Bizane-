package com.bizane.app.data

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/** وەکو GroupService.swift */
object GroupService {
    private val client get() = OkHttpClientProvider.client
    private val JSON = "application/json".toMediaType()

    data class Member(
        val uid: String, val name: String, val isOwner: Boolean,
        val canEdit: Boolean, val canDelete: Boolean = true
    )

    private fun randomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // بەبێ 0/O و 1/I
        return (1..6).map { chars.random() }.joinToString("")
    }

    /** دواتر لینک‌کردنەوەی هەژماری Google (یان کردنەوەی ئەپ لەسەر مۆبایلێکی نوێ)، ئەم فەنکشنە
     * دەگەڕێت بۆ هەر گروپێک کە ئەم uid ـە پێشتر ئەندامی بووە، تاکو خۆکار بگەڕێتەوە ناوی — بێ پێویستی
     * بە دووبارە نووسینی کۆدی گروپ، چونکە بوونی لە گروپەکە خۆی لە سێرڤەردا هەر ماوە.
     * completion: (groupId, groupName, groupCode, ownerId, error) */
    fun findMyGroup(completion: (String?, String?, String?, String?, String?) -> Unit) {
        AuthManager.validToken { token ->
            val uid = AuthManager.uid
            if (token == null || uid == null) {
                mainThread { completion(null, null, null, null, "پێویستە لۆگین بیت") }; return@validToken
            }
            val queryBody = JSONObject().apply {
                put("structuredQuery", JSONObject().apply {
                    put("from", JSONArray().put(JSONObject().put("collectionId", "groups")))
                    put("where", JSONObject().put("fieldFilter", JSONObject().apply {
                        put("field", JSONObject().put("fieldPath", "memberIds"))
                        put("op", "ARRAY_CONTAINS")
                        put("value", JSONObject().put("stringValue", uid))
                    }))
                    put("limit", 1)
                })
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("${FirebaseConfig.firestoreBase}:runQuery")
                .addHeader("Authorization", "Bearer $token")
                .post(queryBody).build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mainThread { completion(null, null, null, null, null) } // هیچ گروپێک نەدۆزرایەوە، ئەمە هەڵە نییە
                }
                override fun onResponse(call: Call, response: Response) {
                    val bodyStr = response.body?.string()
                    var first: JSONObject? = null
                    try {
                        val arr = JSONArray(bodyStr ?: "[]")
                        for (i in 0 until arr.length()) {
                            val doc = arr.optJSONObject(i)?.optJSONObject("document")
                            if (doc != null) { first = doc; break }
                        }
                    } catch (e: Exception) { /* ignore */ }
                    val docName = first?.optString("name")
                    if (first == null || docName.isNullOrEmpty()) {
                        mainThread { completion(null, null, null, null, null) }; return
                    }
                    val groupId = FSValue.docId(docName)
                    val dict = FSValue.dict(first)
                    val groupName = dict.optString("name", "گروپ")
                    val groupCode = dict.optString("code", "")
                    val ownerId = dict.optString("ownerId", "")
                    mainThread { completion(groupId, groupName, groupCode, ownerId, null) }
                }
            })
        }
    }

    private fun parseJson(response: Response): JSONObject? = try {
        response.body?.string()?.let { JSONObject(it) }
    } catch (e: Exception) { null }

    /** completion: (groupId, code, ownerId, error) */
    fun createGroup(name: String, completion: (String?, String?, String?, String?) -> Unit) {
        AuthManager.validToken { token ->
            val uid = AuthManager.uid
            if (token == null || uid == null) {
                mainThread { completion(null, null, null, "پێویستە لۆگین بیت") }; return@validToken
            }
            val code = randomCode()
            val myName = AppSettings.userName.ifEmpty { "ئەندام" }
            val fields = JSONObject().apply {
                put("name", FSValue.str(name))
                put("code", FSValue.str(code))
                put("ownerId", FSValue.str(uid))
                put("memberIds", FSValue.strArr(listOf(uid)))
                put("memberNames", FSValue.strMap(mapOf(uid to myName)))
            }
            val body = JSONObject().put("fields", fields).toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("${FirebaseConfig.firestoreBase}/groups")
                .addHeader("Authorization", "Bearer $token")
                .post(body).build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mainThread { completion(null, null, null, e.localizedMessage ?: "هەڵەیەک ڕوویدا") }
                }
                override fun onResponse(call: Call, response: Response) {
                    val json = parseJson(response)
                    val docName = json?.optString("name")
                    if (docName.isNullOrEmpty()) {
                        mainThread { completion(null, null, null, "هەڵەیەک ڕوویدا") }; return
                    }
                    mainThread { completion(FSValue.docId(docName), code, uid, null) }
                }
            })
        }
    }

    /** completion: (groupId, groupName, ownerId, error) */
    fun joinGroup(code: String, completion: (String?, String?, String?, String?) -> Unit) {
        AuthManager.validToken { token ->
            val uid = AuthManager.uid
            if (token == null || uid == null) {
                mainThread { completion(null, null, null, "پێویستە لۆگین بیت") }; return@validToken
            }
            val cleanCode = code.uppercase(Locale.ROOT).trim()
            val queryBody = JSONObject().apply {
                put("structuredQuery", JSONObject().apply {
                    put("from", JSONArray().put(JSONObject().put("collectionId", "groups")))
                    put("where", JSONObject().put("fieldFilter", JSONObject().apply {
                        put("field", JSONObject().put("fieldPath", "code"))
                        put("op", "EQUAL")
                        put("value", JSONObject().put("stringValue", cleanCode))
                    }))
                    put("limit", 1)
                })
            }.toString().toRequestBody(JSON)
            val req = Request.Builder()
                .url("${FirebaseConfig.firestoreBase}:runQuery")
                .addHeader("Authorization", "Bearer $token")
                .post(queryBody).build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    mainThread { completion(null, null, null, "هیچ گروپێک بەم کۆدە نەدۆزرایەوە") }
                }
                override fun onResponse(call: Call, response: Response) {
                    val bodyStr = response.body?.string()
                    var first: JSONObject? = null
                    try {
                        val arr = JSONArray(bodyStr ?: "[]")
                        for (i in 0 until arr.length()) {
                            val doc = arr.optJSONObject(i)?.optJSONObject("document")
                            if (doc != null) { first = doc; break }
                        }
                    } catch (e: Exception) { /* ignore */ }
                    val docName = first?.optString("name")
                    if (first == null || docName.isNullOrEmpty()) {
                        mainThread { completion(null, null, null, "هیچ گروپێک بەم کۆدە نەدۆزرایەوە") }; return
                    }
                    val groupId = FSValue.docId(docName)
                    val dict = FSValue.dict(first)
                    val groupName = dict.optString("name", "گروپ")
                    val ownerId = dict.optString("ownerId", "")
                    val memberIds = FSValue.stringList(dict, "memberIds").toMutableList()
                    val memberNames = FSValue.stringMap(dict, "memberNames")
                    val myName = AppSettings.userName.ifEmpty { "ئەندام" }
                    if (!memberIds.contains(uid)) {
                        memberIds.add(uid)
                        memberNames.put(uid, myName)
                        updateMemberIds(docName, memberIds, memberNames, token) {
                            mainThread { completion(groupId, groupName, ownerId, null) }
                        }
                    } else {
                        mainThread { completion(groupId, groupName, ownerId, null) }
                    }
                }
            })
        }
    }

    private fun updateMemberIds(
        docName: String, memberIds: List<String>, memberNames: JSONObject,
        token: String, completion: (Boolean) -> Unit
    ) {
        val url = "https://firestore.googleapis.com/v1/$docName?updateMask.fieldPaths=memberIds&updateMask.fieldPaths=memberNames"
        val nameMap = mutableMapOf<String, String>()
        memberNames.keys().forEach { k -> nameMap[k] = memberNames.optString(k) }
        val fields = JSONObject().apply {
            put("memberIds", FSValue.strArr(memberIds))
            put("memberNames", FSValue.strMap(nameMap))
        }
        val body = JSONObject().put("fields", fields).toString().toRequestBody(JSON)
        val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token")
            .method("PATCH", body).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { completion(false) }
            override fun onResponse(call: Call, response: Response) { completion(true) }
        })
    }

    /** دەگەڕێتەوە: زانیاری هەموو ئەندامانی گروپ. تەنیا بۆ ئەدمین بەکاردێت. */
    fun fetchMembers(groupId: String, completion: (List<Member>) -> Unit) {
        AuthManager.validToken { token ->
            if (token == null) { mainThread { completion(emptyList()) }; return@validToken }
            val req = Request.Builder()
                .url("${FirebaseConfig.firestoreBase}/groups/$groupId")
                .addHeader("Authorization", "Bearer $token").build()
            OkHttpClientProvider.shortTimeout().newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { mainThread { completion(emptyList()) } }
                override fun onResponse(call: Call, response: Response) {
                    val doc = parseJson(response)
                    if (doc == null) { mainThread { completion(emptyList()) }; return }
                    val dict = FSValue.dict(doc)
                    val memberIds = FSValue.stringList(dict, "memberIds")
                    val memberNames = FSValue.stringMap(dict, "memberNames")
                    val ownerId = dict.optString("ownerId", "")
                    val viewOnlyIds = FSValue.stringList(dict, "viewOnlyIds")
                    val noDeleteIds = FSValue.stringList(dict, "noDeleteIds")
                    val result = memberIds.map { uid ->
                        val name = memberNames.optString(uid, "ئەندام")
                        Member(
                            uid, name, uid == ownerId,
                            uid == ownerId || !viewOnlyIds.contains(uid),
                            uid == ownerId || !noDeleteIds.contains(uid)
                        )
                    }
                    mainThread { completion(result) }
                }
            })
        }
    }

    /** ئەدمین ئەندامێک لادەبات لە گروپ */
    fun removeMember(groupId: String, uid: String, completion: (Boolean) -> Unit = {}) {
        fetchGroupRaw(groupId) { dict, docPath, token ->
            if (dict == null || docPath == null || token == null) { completion(false); return@fetchGroupRaw }
            val memberIds = FSValue.stringList(dict, "memberIds").toMutableList()
            val memberNames = FSValue.stringMap(dict, "memberNames")
            val viewOnlyIds = FSValue.stringList(dict, "viewOnlyIds").toMutableList()
            val noDeleteIds = FSValue.stringList(dict, "noDeleteIds").toMutableList()
            memberIds.remove(uid)
            memberNames.remove(uid)
            viewOnlyIds.remove(uid)
            noDeleteIds.remove(uid)
            val nameMap = mutableMapOf<String, String>()
            memberNames.keys().forEach { k -> nameMap[k] = memberNames.optString(k) }
            patchGroup(
                docPath, token,
                mapOf(
                    "memberIds" to FSValue.strArr(memberIds),
                    "memberNames" to FSValue.strMap(nameMap),
                    "viewOnlyIds" to FSValue.strArr(viewOnlyIds),
                    "noDeleteIds" to FSValue.strArr(noDeleteIds)
                ),
                listOf("memberIds", "memberNames", "viewOnlyIds", "noDeleteIds"), completion
            )
        }
    }

    /** ئەندامێکی ئاسایی خۆی لە گروپ دەربدەبات */
    fun leaveGroup(groupId: String, completion: (Boolean) -> Unit = {}) {
        val uid = AuthManager.uid ?: run { completion(false); return }
        removeMember(groupId, uid, completion)
    }

    /** ئەدمین گروپەکە کۆتایی پێدەهێنێت: هەموو ئەندامان لادەبردرێن (لە پۆلینگی داهاتوودا هەموو وەک دەرکراو ئاگادار دەبنەوە) */
    fun endGroup(groupId: String, completion: (Boolean) -> Unit = {}) {
        fetchGroupRaw(groupId) { _, docPath, token ->
            if (docPath == null || token == null) { completion(false); return@fetchGroupRaw }
            patchGroup(
                docPath, token,
                mapOf(
                    "memberIds" to FSValue.strArr(emptyList()),
                    "memberNames" to FSValue.strMap(emptyMap()),
                    "viewOnlyIds" to FSValue.strArr(emptyList()),
                    "noDeleteIds" to FSValue.strArr(emptyList())
                ),
                listOf("memberIds", "memberNames", "viewOnlyIds", "noDeleteIds"), completion
            )
        }
    }

    /** ئەدمین دەسەڵاتی زیادکردنی ئەندامێک دەگۆڕێت (canEdit == false واتە تەنیا بینین) */
    fun setMemberPermission(groupId: String, uid: String, canEdit: Boolean, completion: (Boolean) -> Unit = {}) {
        fetchGroupRaw(groupId) { dict, docPath, token ->
            if (dict == null || docPath == null || token == null) { completion(false); return@fetchGroupRaw }
            val viewOnlyIds = FSValue.stringList(dict, "viewOnlyIds").toMutableList()
            viewOnlyIds.remove(uid)
            if (!canEdit) viewOnlyIds.add(uid)
            patchGroup(docPath, token, mapOf("viewOnlyIds" to FSValue.strArr(viewOnlyIds)), listOf("viewOnlyIds"), completion)
        }
    }

    /** دەزانێت ئایا ئەم ئەندامە (uid) دەسەڵاتی زیادکردنی هەیە لەم گروپەدا */
    fun fetchMyPermission(groupId: String, completion: (Boolean) -> Unit) {
        val uid = AuthManager.uid ?: run { completion(true); return }
        fetchGroupRaw(groupId) { dict, _, _ ->
            if (dict == null) { completion(true); return@fetchGroupRaw }
            val ownerId = dict.optString("ownerId", "")
            val viewOnlyIds = FSValue.stringList(dict, "viewOnlyIds")
            completion(uid == ownerId || !viewOnlyIds.contains(uid))
        }
    }

    /** کردنەوە/داخستنی دەسەڵاتی سڕینەوە بۆ هەموو ئەندامانی گروپ */
    fun setDeleteUnlocked(groupId: String, unlocked: Boolean, completion: (Boolean) -> Unit = {}) {
        fetchGroupRaw(groupId) { _, docPath, token ->
            if (docPath == null || token == null) { completion(false); return@fetchGroupRaw }
            patchGroup(docPath, token, mapOf("deleteUnlocked" to FSValue.bool(unlocked)), listOf("deleteUnlocked"), completion)
        }
    }

    /** دەزانێت ئایا سڕینەوە بۆ هەموو ئەندامان کراوەتەوە یان نا */
    fun fetchDeleteUnlocked(groupId: String, completion: (Boolean) -> Unit) {
        fetchGroupRaw(groupId) { dict, _, _ ->
            completion((dict?.opt("deleteUnlocked") as? Boolean) ?: false)
        }
    }

    /** ئەدمین دەسەڵاتی سڕینەوەی ئایتمی خۆی بۆ ئەندامێک دەگۆڕێت (canDelete == false: تەنیا دەتوانێت زیاد بکات) */
    fun setMemberDeletePermission(groupId: String, uid: String, canDelete: Boolean, completion: (Boolean) -> Unit = {}) {
        fetchGroupRaw(groupId) { dict, docPath, token ->
            if (dict == null || docPath == null || token == null) { completion(false); return@fetchGroupRaw }
            val noDeleteIds = FSValue.stringList(dict, "noDeleteIds").toMutableList()
            noDeleteIds.remove(uid)
            if (!canDelete) noDeleteIds.add(uid)
            patchGroup(docPath, token, mapOf("noDeleteIds" to FSValue.strArr(noDeleteIds)), listOf("noDeleteIds"), completion)
        }
    }

    /** دەزانێت ئایا ئەم ئەندامە دەسەڵاتی سڕینەوەی ئایتمی خۆی هەیە لەم گروپەدا */
    fun fetchMyDeletePermission(groupId: String, completion: (Boolean) -> Unit) {
        val uid = AuthManager.uid ?: run { completion(true); return }
        fetchGroupRaw(groupId) { dict, _, _ ->
            if (dict == null) { completion(true); return@fetchGroupRaw }
            val ownerId = dict.optString("ownerId", "")
            val noDeleteIds = FSValue.stringList(dict, "noDeleteIds")
            completion(uid == ownerId || !noDeleteIds.contains(uid))
        }
    }

    /** یوزەرنەیم/پاسی تایبەت بە کردنەوەی دەسەڵاتی سڕینەوە دادەنێت (تەنیا ئەدمین بانگی دەکات) */
    fun setDeleteCredentials(groupId: String, username: String, password: String, completion: (Boolean) -> Unit = {}) {
        fetchGroupRaw(groupId) { _, docPath, token ->
            if (docPath == null || token == null) { completion(false); return@fetchGroupRaw }
            patchGroup(
                docPath, token,
                mapOf(
                    "deleteUnlockUser" to FSValue.str(username),
                    "deleteUnlockPassHash" to FSValue.str(sha256(password))
                ),
                listOf("deleteUnlockUser", "deleteUnlockPassHash"), completion
            )
        }
    }

    /** یوزەرنەیم و هاشی وشەی نهێنی هەڵگیراو دەگەڕێنێتەوە (بۆ بەراوردکردن لە کاتی کردنەوەی قوفڵ) */
    fun fetchDeleteCredentials(groupId: String, completion: (String?, String?) -> Unit) {
        fetchGroupRaw(groupId) { dict, _, _ ->
            if (dict == null) { completion(null, null); return@fetchGroupRaw }
            val user = if (dict.has("deleteUnlockUser")) dict.optString("deleteUnlockUser") else null
            val hash = if (dict.has("deleteUnlockPassHash")) dict.optString("deleteUnlockPassHash") else null
            completion(user, hash)
        }
    }

    fun sha256(s: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { String.format("%02x", it) }
    }

    /** دەزانێت ئایا ئەم ئەندامە هێشتا لە گروپەکەیە یان لەلایەن ئەدمینەوە دەرکراوە.
     * ئەگەر هەڵەیەکی تۆڕ ڕوویدا، بە ئەمنی وەک "هێشتا ئەندامە" دادەنرێت. */
    fun fetchIsMember(groupId: String, completion: (Boolean) -> Unit) {
        val uid = AuthManager.uid ?: run { completion(true); return }
        fetchGroupRaw(groupId) { dict, _, _ ->
            if (dict == null) { completion(true); return@fetchGroupRaw }
            val memberIds = FSValue.stringList(dict, "memberIds")
            completion(memberIds.contains(uid))
        }
    }

    private fun fetchGroupRaw(groupId: String, completion: (JSONObject?, String?, String?) -> Unit) {
        AuthManager.validToken { token ->
            if (token == null) { mainThread { completion(null, null, null) }; return@validToken }
            val req = Request.Builder()
                .url("${FirebaseConfig.firestoreBase}/groups/$groupId")
                .addHeader("Authorization", "Bearer $token").build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { mainThread { completion(null, null, null) } }
                override fun onResponse(call: Call, response: Response) {
                    val doc = parseJson(response)
                    val docName = doc?.optString("name")
                    if (doc == null || docName.isNullOrEmpty()) {
                        mainThread { completion(null, null, null) }; return
                    }
                    mainThread { completion(FSValue.dict(doc), docName, token) }
                }
            })
        }
    }

    private fun patchGroup(
        docPath: String, token: String, fields: Map<String, JSONObject>, mask: List<String>,
        completion: (Boolean) -> Unit
    ) {
        val maskQuery = mask.joinToString("&") { "updateMask.fieldPaths=$it" }
        val url = "https://firestore.googleapis.com/v1/$docPath?$maskQuery"
        val fieldsObj = JSONObject()
        fields.forEach { (k, v) -> fieldsObj.put(k, v) }
        val body = JSONObject().put("fields", fieldsObj).toString().toRequestBody(JSON)
        val req = Request.Builder().url(url).addHeader("Authorization", "Bearer $token")
            .method("PATCH", body).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { mainThread { completion(false) } }
            override fun onResponse(call: Call, response: Response) { mainThread { completion(true) } }
        })
    }
}
