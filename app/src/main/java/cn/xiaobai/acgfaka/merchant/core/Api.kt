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

    fun lockStatus(): Boolean {
        val d = Http.postSync("/admin/api/authentication/lockStatus").optJSONObject("data") ?: JSONObject()
        return d.optBoolean("locked", false)
    }

    fun unlockPassword(password: String): String =
        Http.postSync("/admin/api/authentication/unlockPassword", mapOf("password" to password)).optString("msg", "解锁成功")

    /** type: 0今天 1昨天 2本周 3本月 4全部 */
    fun dashboardData(type: Int): JSONObject =
        Http.getSync("/admin/api/dashboard/data", mapOf("type" to type.toString())).optJSONObject("data") ?: JSONObject()

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
        val params = mutableMapOf("id" to id.toString(), "status" to if (reject) "1" else "0")
        if (reject && message.isNotBlank()) params["message"] = message
        return Http.postSync("/admin/api/cash/decide", params).optString("msg", "已处理")
    }

    fun commodityStatus(ids: List<Int>, online: Boolean): String {
        val params = mutableMapOf("status" to if (online) "1" else "0")
        ids.forEachIndexed { i, id -> params["list[$i]"] = id.toString() }
        return Http.postSync("/admin/api/commodity/status", params).optString("msg", "已更新")
    }

    // ---------- 插件管理 ----------

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

    /** 保存插件配置（任意键值） */
    fun pluginSaveConfig(id: String, fields: Map<String, String>): String {
        val params = mutableMapOf("id" to id)
        params.putAll(fields)
        return Http.postSync("/admin/api/plugin/setConfig", params).optString("msg", "配置已生效")
    }

    /** 插件运行日志 */
    fun pluginLog(handle: String): String =
        Http.postSync("/admin/api/plugin/getPluginLog", mapOf("handle" to handle)).optJSONObject("data")?.optString("log", "") ?: ""

    fun pluginClearLog(handle: String): String =
        Http.postSync("/admin/api/plugin/ClearPluginLog", mapOf("handle" to handle)).optString("msg", "已清空")

    // ---------- 应用商店 ----------

    data class StoreResult(val list: List<JSONObject>, val total: Int)

    fun appStore(page: Int = 1, limit: Int = 15, keyword: String = "", group: Int = 0): StoreResult {
        val params = mutableMapOf(
            "page" to page.toString(), "limit" to limit.toString(), "group" to group.toString()
        )
        if (keyword.isNotBlank()) params["keywords"] = keyword
        val json = Http.postSync("/admin/api/app/plugins", params)
        val data = json.optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return StoreResult(list, data.optInt("total", list.size))
    }

    /** 商店下单购买。type: 0插件 1支付 2主题 */
    fun appPurchase(type: Int, pluginId: Int, payType: Int): String =
        Http.postSync("/admin/api/app/purchase", mapOf(
            "type" to type.toString(), "plugin_id" to pluginId.toString(), "payType" to payType.toString()
        )).optString("msg", "下单成功")

    fun appInstall(pluginKey: String, type: Int, pluginId: Int): String =
        Http.postSync("/admin/api/app/install", mapOf(
            "plugin_key" to pluginKey, "type" to type.toString(), "plugin_id" to pluginId.toString()
        )).optString("msg", "安装完成")

    fun appUpgrade(pluginKey: String, type: Int, pluginId: Int): String =
        Http.postSync("/admin/api/app/upgrade", mapOf(
            "plugin_key" to pluginKey, "type" to type.toString(), "plugin_id" to pluginId.toString()
        )).optString("msg", "更新完成")

    fun appUninstall(pluginKey: String, type: Int): String =
        Http.postSync("/admin/api/app/uninstall", mapOf(
            "plugin_key" to pluginKey, "type" to type.toString()
        )).optString("msg", "卸载完成")

    // ---------- 会员管理 ----------

    fun userStatistics(id: Int): JSONObject =
        Http.getSync("/admin/api/user/statistics", mapOf("id" to id.toString())).optJSONObject("data") ?: JSONObject()

    fun userBalance(id: Int, add: Boolean, amount: Double, log: String, total: Boolean = false): String =
        Http.postSync("/admin/api/user/recharge", mapOf(
            "id" to id.toString(), "action" to if (add) "1" else "0",
            "amount" to amount.toString(), "log" to log, "total" to if (total) "1" else "0"
        )).optString("msg", "操作成功")

    fun userCoin(id: Int, add: Boolean, amount: Double, log: String, total: Boolean = false): String =
        Http.postSync("/admin/api/user/coin", mapOf(
            "id" to id.toString(), "action" to if (add) "1" else "0",
            "amount" to amount.toString(), "log" to log, "total" to if (total) "1" else "0"
        )).optString("msg", "操作成功")

    fun userSave(fields: Map<String, String>): String =
        Http.postSync("/admin/api/user/save", fields).optString("msg", "已保存")

    fun userSetStatus(id: Int, status: Int): String =
        Http.postSync("/admin/api/user/save", mapOf("id" to id.toString(), "status" to status.toString())).optString("msg", "已更新")

    fun userDelete(ids: List<Int>): String {
        val params = mutableMapOf<String, String>()
        ids.forEachIndexed { i, id -> params["list[$i]"] = id.toString() }
        return Http.postSync("/admin/api/user/del", params).optString("msg", "已删除")
    }

    /** 关闭/开启店铺 */
    fun userShopClosed(id: Int, closed: Boolean): String =
        Http.postSync("/admin/api/user/shopClosed", mapOf("id" to id.toString(), "status" to if (closed) "1" else "0")).optString("msg", "已更新")

    /** 会员操作日志 */
    fun userLog(id: Int, p: Int = 1, l: Int = 15): Page {
        val data = Http.getSync("/admin/api/user/log", mapOf("id" to id.toString(), "page" to p.toString(), "limit" to l.toString()))
            .optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // ---------- 支付设置 ----------

    fun payList(p: Int = 1, l: Int = 15): Page = page("/admin/api/pay/data", p, l)

    /** 保存支付接口。字段：name/icon/code/commodity/recharge/handle/sort/equipment */
    fun paySave(fields: Map<String, String>): String =
        Http.postSync("/admin/api/pay/save", fields).optString("msg", "保存成功")

    fun payDel(ids: List<Int>): String {
        val params = mutableMapOf<String, String>()
        ids.forEachIndexed { i, id -> params["list[$i]"] = id.toString() }
        return Http.postSync("/admin/api/pay/del", params).optString("msg", "已删除")
    }

    /** 支付插件列表（用于新增支付接口时选插件） */
    fun payPlugins(): List<JSONObject> {
        val arr = Http.getSync("/admin/api/pay/getPlugins").optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    /** 保存支付插件配置 */
    fun paySetPluginConfig(handle: String, fields: Map<String, String>): String {
        val params = mutableMapOf("id" to handle)
        params.putAll(fields)
        return Http.postSync("/admin/api/pay/setPluginConfig", params).optString("msg", "修改成功")
    }

    fun payPluginLog(handle: String): String =
        Http.postSync("/admin/api/pay/getPluginLog", mapOf("handle" to handle)).optJSONObject("data")?.optString("log", "") ?: ""

    /** 支付接口拨测 */
    fun payTest(id: Int): String =
        Http.postSync("/admin/api/pay/test", mapOf("id" to id.toString())).optString("msg", "测试完成")
}
