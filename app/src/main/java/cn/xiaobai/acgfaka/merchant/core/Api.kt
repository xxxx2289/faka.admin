package cn.xiaobai.acgfaka.admin.core

import org.json.JSONArray
import org.json.JSONObject

// 平台后台接口封装，路径 /admin/api 下，独立管理员会话。
object Api {

    class NeedTotpException(message: String) : Exception(message)
    class LockedException(message: String) : Exception(message)

    fun login(username: String, password: String, captcha: String, code: String = "", remember: Boolean = true): String {
        val params = mutableMapOf(
            "username" to username.trim(), "password" to password,
            "captcha" to captcha.trim(), "remember" to if (remember) "1" else "0"
        )
        if (code.isNotBlank()) params["code"] = code.trim()
        try {
            return Http.postSync("/admin/api/authentication/login", params).optString("msg", "登录成功")
        } catch (e: ApiException) {
            val m = e.message ?: ""
            if (m.contains("谷歌") || m.contains("两步")) throw NeedTotpException(m)
            if (m.contains("锁定")) throw LockedException(m)
            throw e
        }
    }

    fun loginCaptcha(): ByteArray =
        Http.getBytesSync("/user/captcha/image?action=adminLogin&t=${System.currentTimeMillis()}")

    fun ping(): Boolean {
        val d = Http.postSync("/admin/api/authentication/ping").optJSONObject("data") ?: JSONObject()
        return d.optBoolean("locked", false)
    }

    // ---------- 数据概览 ----------
    /** type: 0今天 1昨天 2本周 3本月 4全部 */
    fun dashboardData(type: Int): JSONObject =
        Http.getSync("/admin/api/dashboard/data", mapOf("type" to type.toString())).optJSONObject("data") ?: JSONObject()

    // ---------- 通用分页 ----------
    data class Page(val list: List<JSONObject>, val total: Int)

    private fun page(path: String, p: Int, l: Int, extra: Map<String, String> = emptyMap()): Page {
        val params = mutableMapOf("page" to p.toString(), "limit" to l.toString())
        params.putAll(extra)
        val data = Http.postSync(path, params).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun orders(p: Int = 1, l: Int = 15): Page = page("/admin/api/order/data", p, l)
    fun commodities(p: Int = 1, l: Int = 15): Page = page("/admin/api/commodity/data", p, l)
    fun cards(p: Int = 1, l: Int = 15): Page = page("/admin/api/card/data", p, l)
    fun users(p: Int = 1, l: Int = 15): Page = page("/admin/api/user/data", p, l)
    fun cashList(p: Int = 1, l: Int = 15): Page = page("/admin/api/cash/data", p, l)
    fun logs(p: Int = 1, l: Int = 15): Page = page("/admin/api/log/data", p, l)

    fun cashDecide(id: Int, reject: Boolean, message: String = ""): String {
        val params = mutableMapOf(
            "id" to id.toString(),
            "status" to if (reject) "1" else "0"
        )
        if (reject && message.isNotBlank()) params["message"] = message
        return Http.postSync("/admin/api/cash/decide", params).optString("msg", "已处理")
    }

    fun commodityStatus(ids: List<Int>, online: Boolean): String {
        val params = mutableMapOf("status" to if (online) "1" else "0")
        ids.forEachIndexed { i, id -> params["list[$i]"] = id.toString() }
        return Http.postSync("/admin/api/commodity/status", params).optString("msg", "已更新")
    }

    /**
     * 已安装插件列表。服务端返回 {data:{list:[...]}}，data 是对象不是数组。
     */
    fun plugins(keyword: String = ""): List<JSONObject> {
        val params = mutableMapOf<String, String>()
        if (keyword.isNotBlank()) params["keywords"] = keyword
        val json = if (params.isEmpty()) Http.getSync("/admin/api/plugin/getPlugins")
                   else Http.postSync("/admin/api/plugin/getPlugins", params)
        val data = json.optJSONObject("data") ?: return emptyList()
        val arr = data.optJSONArray("list") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    /** 插件开关 */
    fun pluginSetConfig(id: String, status: Boolean): String =
        Http.postSync("/admin/api/plugin/setConfig", mapOf("id" to id, "STATUS" to if (status) "1" else "0")).optString("msg", "已更新")

    /**
     * 应用商店。官方远端插件市场。
     * 服务端返回 {data:{list:[...], total:N}, user:..., purchase:...}
     * group: 0=插件 1=支付 2=主题；owner: -1=全部
     */
    data class StoreResult(val list: List<JSONObject>, val total: Int)

    fun appStore(page: Int = 1, limit: Int = 15, keyword: String = "", group: Int = 0): StoreResult {
        val params = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString(),
            "group" to group.toString()
        )
        if (keyword.isNotBlank()) params["keywords"] = keyword
        val json = Http.postSync("/admin/api/app/plugins", params)
        val data = json.optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return StoreResult(list, data.optInt("total", list.size))
    }
}
