package cn.xiaobai.acgfaka.admin.core

import org.json.JSONObject

object Json {
    fun str(o: JSONObject, k: String, def: String = ""): String = if (o.has(k) && !o.isNull(k)) o.optString(k, def) else def
    fun int(o: JSONObject, k: String, def: Int = 0): Int = if (o.has(k) && !o.isNull(k)) o.optInt(k, def) else def
    fun dbl(o: JSONObject, k: String, def: Double = 0.0): Double = if (o.has(k) && !o.isNull(k)) o.optDouble(k, def) else def
    fun nullable(o: JSONObject, k: String): String? {
        if (!o.has(k) || o.isNull(k)) return null
        val v = o.optString(k, "")
        return v.ifBlank { null }
    }
}
