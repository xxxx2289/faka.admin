package cn.xiaobai.acgfaka.admin.core

import android.content.Context
import cn.xiaobai.acgfaka.admin.BuildConfig

object Session {
    private const val PREF = "acg_faka_admin"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun server(ctx: Context): String {
        val saved = sp(ctx).getString("server", "") ?: ""
        return saved.ifBlank { BuildConfig.DEFAULT_SERVER }
    }

    fun saveServer(ctx: Context, url: String) = sp(ctx).edit().putString("server", url).apply()

    fun isLoggedIn(ctx: Context): Boolean = sp(ctx).getBoolean("logged_in", false)
    fun setLoggedIn(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("logged_in", v).apply()

    fun username(ctx: Context): String = sp(ctx).getString("username", "") ?: ""
    fun saveUsername(ctx: Context, u: String) = sp(ctx).edit().putString("username", u).apply()
}
