package cn.xiaobai.acgfaka.merchant.ui

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
import cn.xiaobai.acgfaka.merchant.core.Http
import cn.xiaobai.acgfaka.merchant.core.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavController) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopAppBar(title = { Text("更多功能") })
        Entry(Icons.Default.Key, "卡密管理") { nav.navigate("cards") }
        Entry(Icons.Default.Category, "分类管理") { nav.navigate("categories") }
        Entry(Icons.Default.AccountBalanceWallet, "账单明细") { nav.navigate("bills") }
        Entry(Icons.Default.Settings, "服务器设置") { nav.navigate("server") }
        Entry(Icons.Default.Logout, "退出登录") {
            Http.clearSession()
            Session.setLoggedIn(ctx, false)
            Session.saveUsername(ctx, "")
            nav.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
        }
        Spacer(Modifier.height(24.dp))
        Text("异次元商家端 v1.0.0", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Entry(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(Modifier.padding(start = 56.dp))
}
