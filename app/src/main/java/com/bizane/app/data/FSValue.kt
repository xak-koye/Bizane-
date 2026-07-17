package com.bizane.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * یارمەتیدەری خوێندنەوە/نووسینی JSON ـی Firestore REST API.
 * وەکو FSValue.swift، بەڵام بەرواران وەک epoch-millis (Long) هەڵدەگیرێن نەک Date object.
 */
object FSValue {
    fun str(s: String): JSONObject = JSONObject().put("stringValue", s)
    fun int(i: Long): JSONObject = JSONObject().put("integerValue", i.toString())
    fun bool(b: Boolean): JSONObject = JSONObject().put("booleanValue", b)
    fun ts(millis: Long): JSONObject = JSONObject().put("timestampValue", Instant.ofEpochMilli(millis).toString())

    fun arr(values: List<JSONObject>): JSONObject {
        val jArr = JSONArray()
        values.forEach { jArr.put(it) }
        return JSONObject().put("arrayValue", JSONObject().put("values", jArr))
    }

    fun strArr(items: List<String>): JSONObject = arr(items.map { str(it) })

    fun mapVal(fields: JSONObject): JSONObject = JSONObject().put("mapValue", JSONObject().put("fields", fields))

    fun strMap(map: Map<String, String>): JSONObject {
        val f = JSONObject()
        map.forEach { (k, v) -> f.put(k, str(v)) }
        return mapVal(f)
    }

    /** یەک fieldـی Firestore دەکاتەوە بۆ Kotlin value (String / Long / Double / Boolean / List<Any?> / JSONObject) */
    fun decode(field: JSONObject): Any? {
        if (field.has("stringValue")) return field.getString("stringValue")
        if (field.has("integerValue")) return field.getString("integerValue").toLongOrNull()
        if (field.has("doubleValue")) return field.getDouble("doubleValue")
        if (field.has("booleanValue")) return field.getBoolean("booleanValue")
        if (field.has("timestampValue")) {
            return try { Instant.parse(field.getString("timestampValue")).toEpochMilli() } catch (e: Exception) { null }
        }
        if (field.has("arrayValue")) {
            val values = field.optJSONObject("arrayValue")?.optJSONArray("values") ?: JSONArray()
            val list = mutableListOf<Any?>()
            for (i in 0 until values.length()) list.add(decode(values.getJSONObject(i)))
            return list
        }
        if (field.has("mapValue")) {
            val innerFields = field.optJSONObject("mapValue")?.optJSONObject("fields") ?: JSONObject()
            val out = JSONObject()
            innerFields.keys().forEach { k -> out.put(k, decode(innerFields.getJSONObject(k))) }
            return out
        }
        return null
    }

    /** دۆکیومێنتێکی خاوی Firestore دەکاتە JSONObject ـێکی سادە (key -> decoded value) */
    fun dict(doc: JSONObject): JSONObject {
        val fieldsObj = doc.optJSONObject("fields") ?: JSONObject()
        val out = JSONObject()
        fieldsObj.keys().forEach { k -> out.put(k, decode(fieldsObj.getJSONObject(k))) }
        return out
    }

    fun docId(name: String): String = name.substringAfterLast("/")

    @Suppress("UNCHECKED_CAST")
    fun stringList(dict: JSONObject, key: String): List<String> =
        (dict.opt(key) as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

    fun stringMap(dict: JSONObject, key: String): JSONObject = dict.optJSONObject(key) ?: JSONObject()
}
