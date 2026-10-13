package cn.xiaobai.acgfaka.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.admin.core.Api
import cn.xiaobai.acgfaka.admin.core.Http
import cn.xiaobai.acgfaka.admin.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private fun money(o: JSONObject, key: String): String = Json.str(o, key, "0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load(t: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                data = withContext(Dispatchers.IO) { Api.dashboardData(t) }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(0) }

    val tabs = listOf("今天" to 0, "昨天" to 1, "本周" to 2, "本月" to 3, "全部" to 4)
    Scaffold(
        topBar = { TopAppBar(title = { Text("经营概览") }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            ScrollableTabRow(selectedTabIndex = type) {
                tabs.forEach { (label, v) ->
                    Tab(selected = type == v, onClick = { type = v; load(v) }, text = { Text(label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            if (loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val d = data ?: return@Column

            val turnover = money(d, "turnover")
            val orderNum = Json.str(d, "order_num", "0")
            val profit = money(d, "profit")
            val buyerNum = Json.str(d, "buyer_num", "0")
            val rechargeAmount = money(d, "recharge_amount")
            val userReg = Json.str(d, "user_register_num", "0")
            val cashPending = Json.str(d, "cash_status_0", "0")
            val unpaid = Json.str(d, "unpaid_order_num", "0")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("成交额", "¥" + turnover, Modifier.weight(1f))
                StatCard("订单数", orderNum, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("利润", "¥" + profit, Modifier.weight(1f))
                StatCard("买家数", buyerNum, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("充值金额", "¥" + rechargeAmount, Modifier.weight(1f))
                StatCard("新增用户", userReg, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("待提现", cashPending, Modifier.weight(1f))
                StatCard("未付款订单", unpaid, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("快捷管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            QuickGrid(nav)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickGrid(nav: NavController) {
    val items = listOf(
        Triple("订单管理", "orders", Icons.Default.ReceiptLong),
        Triple("商品管理", "commodities", Icons.Default.Inventory2),
        Triple("卡密管理", "cards", Icons.Default.Key),
        Triple("用户管理", "users", Icons.Default.People),
        Triple("提现审核", "cash", Icons.Default.AccountBalanceWallet),
        Triple("操作日志", "logs", Icons.Default.Article),
        Triple("插件管理", "plugins", Icons.Default.Extension),
        Triple("服务器设置", "server", Icons.Default.Settings)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    Card(onClick = { nav.navigate(item.second) }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(item.third, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(item.first, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun OrdersScreen(nav: NavController) {
    PagedScreen(nav, "订单管理", { p, l -> Api.orders(p, l) }, emptyText = "暂无订单") { o ->
        val amount = money(o, "amount")
        val t = Json.str(o, "create_time")
        val c = Json.str(o, "contact")
        InfoCard(
            title = Json.str(o, "trade_no", "订单"),
            badge = if (Json.int(o, "status") == 1) "已支付" else "待支付",
            rows = buildList {
                add("金额" to "¥$amount")
                if (t.isNotBlank()) add("时间" to t)
                if (c.isNotBlank()) add("联系" to c)
            }
        )
    }
}

@Composable
fun CommoditiesScreen(nav: NavController) {
    PagedScreen(nav, "商品管理", { p, l -> Api.commodities(p, l) }, emptyText = "暂无商品") { o ->
        val id = Json.int(o, "id")
        val price = money(o, "price")
        val stock = Json.int(o, "stock")
        InfoCard(
            title = Json.str(o, "name", "商品"),
            badge = if (Json.int(o, "status") == 1) "上架" else "下架",
            rows = listOf(
                "ID" to id.toString(),
                "价格" to "¥$price",
                "库存" to stock.toString()
            )
        )
    }
}

@Composable
fun CardsScreen(nav: NavController) {
    PagedScreen(nav, "卡密管理", { p, l -> Api.cards(p, l) }, emptyText = "暂无卡密") { o ->
        val id = Json.int(o, "id")
        val cid = Json.int(o, "commodity_id")
        InfoCard(
            title = "卡密 #$id",
            badge = if (Json.int(o, "status") == 0) "未售" else "已售",
            rows = listOf("商品ID" to cid.toString())
        )
    }
}

@Composable
fun UsersScreen(nav: NavController) {
    PagedScreen(nav, "用户管理", { p, l -> Api.users(p, l) }, emptyText = "暂无用户") { o ->
        val id = Json.int(o, "id")
        val balance = money(o, "balance")
        val recharge = money(o, "recharge")
        val t = Json.str(o, "create_time")
        InfoCard(
            title = Json.str(o, "username", "用户"),
            badge = "ID $id",
            rows = buildList {
                add("余额" to "¥$balance")
                add("累计充值" to "¥$recharge")
                if (t.isNotBlank()) add("注册" to t)
            }
        )
    }
}

@Composable
fun CashScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    key(reload) {
        PagedScreen(nav, "提现审核", { p, l -> Api.cashList(p, l) }, emptyText = "暂无提现申请") { o ->
            val id = Json.int(o, "id")
            val status = Json.int(o, "status")
            val amount = money(o, "amount")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#$id", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        AssistChip(onClick = {}, label = { Text(if (status == 0) "待处理" else if (status == 1) "已通过" else "已驳回") })
                    }
                    Text("金额 ¥$amount", style = MaterialTheme.typography.bodyMedium)
                    if (status == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        runCatching { withContext(Dispatchers.IO) { Api.cashDecide(id, 1) } }
                                        reload++
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("通过") }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { withContext(Dispatchers.IO) { Api.cashDecide(id, 2) } }
                                        reload++
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("驳回") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogsScreen(nav: NavController) {
    PagedScreen(nav, "操作日志", { p, l -> Api.logs(p, l) }, emptyText = "暂无日志") { o ->
        val c = Json.str(o, "content")
        val t = Json.str(o, "create_time")
        InfoCard(
            title = Json.str(o, "action", "操作"),
            badge = Json.str(o, "create_ip"),
            rows = buildList {
                if (c.isNotBlank()) add("内容" to c)
                if (t.isNotBlank()) add("时间" to t)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                list = withContext(Dispatchers.IO) { Api.plugins() }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("插件管理") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        LazyColumnOfPlugins(list, onToggle = { id, v ->
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { Api.pluginSetConfig(id, v) } }
                load()
            }
        })
    }
}

@Composable
private fun LazyColumnOfPlugins(list: List<JSONObject>, onToggle: (String, Boolean) -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(list.size) { i ->
            val o = list[i]
            val id = Json.str(o, "PLUGIN_NAME", Json.str(o, "id"))
            val name = Json.str(o, "NAME", id)
            val cfg = o.optJSONObject("PLUGIN_CONFIG") ?: JSONObject()
            val active = Json.int(cfg, "STATUS") == 1
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.SemiBold)
                        Text(id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = active, onCheckedChange = { v -> onToggle(id, v) })
                }
            }
        }
    }
}
