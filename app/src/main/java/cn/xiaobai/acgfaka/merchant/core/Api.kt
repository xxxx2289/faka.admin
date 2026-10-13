package cn.xiaobai.acgfaka.merchant.core

import org.json.JSONArray
import org.json.JSONObject

// 商家后台接口封装。商家即前台登录用户，走 /user/api 路径。
object Api {

    // ---------- 认证 ----------
    class NeedTotpException(message: String) : Exception(message)

    /** 登录：验证码 action 必须是 login */
    fun login(username: String, password: String, captcha: String, code: String = "", remember: Boolean = true): String {
        val params = mutableMapOf(
            "username" to username.trim(), "password" to password,
            "captcha" to captcha.trim(), "remember" to if (remember) "1" else "0"
        )
        if (code.isNotBlank()) params["code"] = code.trim()
        try {
            return Http.postSync("/user/api/authentication/login", params).optString("msg", "登录成功")
        } catch (e: ApiException) {
            val m = e.message ?: ""
            if (m.contains("谷歌") || m.contains("两步")) throw NeedTotpException(m)
            throw e
        }
    }

    /** 登录图形验证码：action=login */
    fun loginCaptcha(): ByteArray =
        Http.getBytesSync("/user/captcha/image?action=login&t=${System.currentTimeMillis()}")

    data class SiteInfo(val shopName: String, val notice: String?)
    fun siteInfo(): SiteInfo {
        val d = Http.getSync("/user/api/site/info").optJSONObject("data") ?: JSONObject()
        return SiteInfo(Json.str(d, "shop_name", "异次元商家"), Json.nullable(d, "notice"))
    }

    // ---------- 工作台概览 ----------
    data class Dashboard(
        val orderTotal: Int, val orderToday: Int, val orderPending: Int,
        val commodityTotal: Int, val cardStock: Int, val balance: Double, val username: String
    )

    fun dashboard(): Dashboard {
        var orderTotal = 0; var orderPending = 0
        var commodityTotal = 0; var cardStock = 0
        var balance = 0.0; var username = ""

        runCatching {
            val d = Http.postSync("/user/api/commodityOrder/data", mapOf("page" to "1", "limit" to "1"))
            val data = d.optJSONObject("data") ?: JSONObject()
            orderTotal = data.optInt("total", 0)
        }
        runCatching {
            val d = Http.postSync("/user/api/commodityOrder/data", mapOf("page" to "1", "limit" to "1", "delivery_status" to "0"))
            val data = d.optJSONObject("data") ?: JSONObject()
            orderPending = data.optInt("total", 0)
        }
        runCatching {
            val d = Http.postSync("/user/api/commodity/data", mapOf("page" to "1", "limit" to "1"))
            val data = d.optJSONObject("data") ?: JSONObject()
            commodityTotal = data.optInt("total", 0)
        }
        runCatching {
            val d = Http.postSync("/user/api/card/data", mapOf("page" to "1", "limit" to "1"))
            val data = d.optJSONObject("data") ?: JSONObject()
            cardStock = data.optInt("total", 0)
        }
        runCatching {
            val p = Http.getSync("/user/api/security/personal").optJSONObject("data") ?: JSONObject()
            balance = Json.dbl(p, "balance")
            username = Json.str(p, "username")
        }
        return Dashboard(orderTotal, 0, orderPending, commodityTotal, cardStock, balance, username)
    }

    // ---------- 商品管理 ----------
    data class Page(val list: List<JSONObject>, val total: Int)

    fun commodities(page: Int = 1, limit: Int = 15, keyword: String = ""): Page {
        val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
        if (keyword.isNotBlank()) params["name"] = keyword
        val data = Http.postSync("/user/api/commodity/data", params).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun setCommodityStatus(id: Int, status: Int): String {
        return Http.postSync("/user/api/commodity/save", mapOf("id" to id.toString(), "status" to status.toString())).optString("msg", "已更新")
    }

    fun deleteCommodity(id: Int): String {
        return Http.postSync("/user/api/commodity/del", mapOf("id" to id.toString())).optString("msg", "已删除")
    }

    // ---------- 订单管理 ----------
    fun orders(page: Int = 1, limit: Int = 15, keyword: String = "", deliveryStatus: Int? = null): Page {
        val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
        if (keyword.isNotBlank()) params["trade_no"] = keyword
        if (deliveryStatus != null) params["delivery_status"] = deliveryStatus.toString()
        val data = Http.postSync("/user/api/commodityOrder/data", params).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun deliver(orderId: Int): String {
        return Http.postSync("/user/api/commodityOrder/delivery", mapOf("id" to orderId.toString())).optString("msg", "发货成功")
    }

    // ---------- 卡密管理 ----------
    fun cards(page: Int = 1, limit: Int = 15, keyword: String = "", commodityId: Int? = null): Page {
        val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
        if (keyword.isNotBlank()) params["draft"] = keyword
        if (commodityId != null) params["commodity_id"] = commodityId.toString()
        val data = Http.postSync("/user/api/card/data", params).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    fun addCards(commodityId: Int, draft: String, premium: Double = 0.0, race: String = ""): String {
        val params = mutableMapOf(
            "commodity_id" to commodityId.toString(),
            "draft" to draft,
            "premium" to premium.toString()
        )
        if (race.isNotBlank()) params["race"] = race
        return Http.postSync("/user/api/card/save", params).optString("msg", "添加成功")
    }

    fun deleteCard(id: Int): String = Http.postSync("/user/api/card/del", mapOf("id" to id.toString())).optString("msg", "已删除")

    // ---------- 分类 ----------
    fun categories(page: Int = 1, limit: Int = 50): Page {
        val data = Http.postSync("/user/api/category/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }

    // ---------- 账单 ----------
    fun bills(page: Int = 1, limit: Int = 15): Page {
        val data = Http.postSync("/user/api/bill/data", mapOf("page" to page.toString(), "limit" to limit.toString())).optJSONObject("data") ?: JSONObject()
        val arr = data.optJSONArray("list") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        return Page(list, data.optInt("total", list.size))
    }
}
