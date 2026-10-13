package cn.xiaobai.acgfaka.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.admin.core.Http
import cn.xiaobai.acgfaka.admin.core.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavController) {
    val ctx = LocalContext.current
    val loggedIn = Session.isLoggedIn(ctx)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopAppBar(title = { Text("更多功能") })
        Entry(Icons.Default.Key, "卡密管理") { nav.navigate("cards") }
        Entry(Icons.Default.Inventory2, "商品管理") { nav.navigate("commodities") }
        Entry(Icons.Default.AccountBalanceWallet, "提现审核") { nav.navigate("cash") }
        Entry(Icons.Default.Payment, "支付设置") { nav.navigate("pay") }
        Entry(Icons.Default.Article, "操作日志") { nav.navigate("logs") }
        Entry(Icons.Default.Extension, "插件管理") { nav.navigate("plugins") }
        Entry(Icons.Default.Storefront, "应用商店") { nav.navigate("store") }
        Entry(Icons.Default.Settings, "服务器设置") { nav.navigate("server") }
        if (loggedIn) {
            Entry(Icons.Default.Logout, "退出登录") {
                Http.clearSession()
                Session.setLoggedIn(ctx, false)
                Session.saveUsername(ctx, "")
                nav.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
            }
        } else {
            Entry(Icons.Default.Login, "管理员登录") { nav.navigate("login") }
        }
        Spacer(Modifier.height(24.dp))
        Text("异次元后台 v1.0.0", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Entry(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(Modifier.padding(start = 56.dp))
}
