package cn.xiaobai.acgfaka.merchant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.merchant.core.Api
import cn.xiaobai.acgfaka.merchant.core.Http
import cn.xiaobai.acgfaka.merchant.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(Session.isLoggedIn(ctx)) }
    var dash by remember { mutableStateOf<Api.Dashboard?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                dash = withContext(Dispatchers.IO) { Api.dashboard() }
                loggedIn = Session.isLoggedIn(ctx)
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { if (loggedIn) load() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Column { Text("商家工作台", fontWeight = FontWeight.Bold) } },
            actions = {
                IconButton(onClick = { nav.navigate("server") }) { Icon(Icons.Default.Settings, "服务器") }
                if (loggedIn) IconButton(onClick = {
                    Http.clearSession(); Session.setLoggedIn(ctx, false); Session.saveUsername(ctx, "")
                    loggedIn = false; dash = null
                }) { Icon(Icons.Default.Logout, "退出") }
            }
        )
        if (!loggedIn) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Storefront, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("请先登录商家账号")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate("login") }) { Text("登录") }
                }
            }
            return@Column
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        dash?.let { d ->
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, null, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(d.username.ifBlank { "商家" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("余额 ¥${fmt(d.balance)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("订单总数", d.orderTotal.toString(), Modifier.weight(1f))
                    StatCard("待发货", d.orderPending.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("商品数", d.commodityTotal.toString(), Modifier.weight(1f))
                    StatCard("卡密库存", d.cardStock.toString(), Modifier.weight(1f))
                }
                Text("快捷入口", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickEntry(Icons.Default.ReceiptLong, "订单", Modifier.weight(1f)) { nav.navigate("orders") }
                    QuickEntry(Icons.Default.Inventory2, "商品", Modifier.weight(1f)) { nav.navigate("commodities") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickEntry(Icons.Default.Key, "卡密", Modifier.weight(1f)) { nav.navigate("cards") }
                    QuickEntry(Icons.Default.Category, "分类", Modifier.weight(1f)) { nav.navigate("categories") }
                }
                Button(onClick = { load() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("刷新数据") }
            }
        }
    }
}

@Composable
private fun QuickEntry(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
