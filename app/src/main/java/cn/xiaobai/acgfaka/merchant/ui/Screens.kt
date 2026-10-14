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
private fun htmlClean(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    var s = raw
    s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
    s = s.replace(Regex("<[^>]+>"), "")
    s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
    return s.trim()
}

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
                                        runCatching { withContext(Dispatchers.IO) { Api.cashDecide(id, false) } }
                                        reload++
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("通过") }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { withContext(Dispatchers.IO) { Api.cashDecide(id, true, "审核未通过") } }
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
    var detail by remember { mutableStateOf<JSONObject?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("插件管理") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
            if (list.isEmpty() && !loading && error == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无已安装插件") }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list.size) { i ->
                        val o = list[i]
                        val id = Json.str(o, "PLUGIN_NAME", Json.str(o, "id"))
                        val name = htmlClean(Json.str(o, "NAME", id))
                        val desc = htmlClean(Json.str(o, "DESCRIPTION"))
                        val ver = Json.str(o, "VERSION")
                        val cfg = o.optJSONObject("PLUGIN_CONFIG") ?: JSONObject()
                        val active = Json.int(cfg, "STATUS") == 1
                        Card(onClick = { detail = o }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                                    AssistChip(onClick = {}, label = { Text(if (active) "运行中" else "未启用") })
                                }
                                if (desc.isNotBlank()) Text(desc, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                Text("标识 " + id + (if (ver.isNotBlank()) " · v" + ver else ""), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { o ->
        PluginDetailDialog(
            plugin = o,
            onDismiss = { detail = null },
            onChanged = { detail = null; load() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginDetailDialog(
    plugin: JSONObject,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val id = Json.str(plugin, "PLUGIN_NAME", Json.str(plugin, "id"))
    val name = htmlClean(Json.str(plugin, "NAME", id))
    val desc = htmlClean(Json.str(plugin, "DESCRIPTION"))
    val ver = Json.str(plugin, "VERSION")
    val cfg = plugin.optJSONObject("PLUGIN_CONFIG") ?: JSONObject()
    var active by remember { mutableStateOf(Json.int(cfg, "STATUS") == 1) }
    var top by remember { mutableStateOf(Json.int(cfg, "top") == 1) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var showConfig by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (desc.isNotBlank()) Text(desc, style = MaterialTheme.typography.bodySmall)
                DetailRow("标识", id)
                DetailRow("版本", ver.ifBlank { "-" })
                DetailRow("状态", if (active) "运行中" else "未启用")
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("启用插件", Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { v ->
                        active = v
                        scope.launch {
                            busy = true
                            runCatching { withContext(Dispatchers.IO) { Api.pluginSetConfig(id, v) } }
                                .onSuccess { msg = if (v) "已启用" else "已停用" }
                                .onFailure { msg = it.message }
                            busy = false
                            onChanged()
                        }
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("置顶显示", Modifier.weight(1f))
                    Switch(checked = top, onCheckedChange = { v ->
                        top = v
                        scope.launch {
                            busy = true
                            runCatching { withContext(Dispatchers.IO) { Api.pluginSaveConfig(id, mapOf("top" to if (v) "1" else "0")) } }
                                .onSuccess { msg = "已更新置顶" }
                                .onFailure { msg = it.message }
                            busy = false
                        }
                    })
                }
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(6.dp))
                Text("可用操作", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showConfig = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("配置插件") }
                    OutlinedButton(onClick = { showLog = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("查看日志") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    if (showConfig) PluginConfigDialog(id, cfg, { showConfig = false }, onChanged)
    if (showLog) PluginLogDialog(id, { showLog = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginConfigDialog(handle: String, cfg: JSONObject, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    // 把配置项转成可编辑字段，排除内部键
    val skip = setOf("STATUS", "top", "VERSION", "name", "description")
    val fields = remember(cfg) {
        val m = linkedMapOf<String, String>()
        val keys = cfg.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k in skip) continue
            val v = cfg.opt(k)
            if (v is String || v is Number || v is Boolean) m[k] = v.toString()
        }
        m
    }
    val values = remember { mutableStateMapOf<String, String>().apply { putAll(fields) } }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置插件") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (values.isEmpty()) Text("该插件无可编辑配置")
                values.forEach { (k, v) ->
                    OutlinedTextField(
                        value = v,
                        onValueChange = { values[k] = it },
                        label = { Text(k) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        busy = true; msg = null
                        runCatching { withContext(Dispatchers.IO) { Api.pluginSaveConfig(handle, values.toMap()) } }
                            .onSuccess { msg = it; onSaved(); onDismiss() }
                            .onFailure { msg = it.message }
                        busy = false
                    }
                },
                enabled = !busy
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginLogDialog(handle: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var log by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try { log = withContext(Dispatchers.IO) { Api.pluginLog(handle) } }
            catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(handle) { load() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("插件日志") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (loading) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!loading && log.isBlank() && error == null) Text("暂无日志")
                if (log.isNotBlank()) Text(log, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { Api.pluginClearLog(handle) } }
                        load()
                    }
                }) { Text("清空") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

// ---------- 应用商店 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var group by remember { mutableIntStateOf(0) }
    var keyword by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var storeState by remember { mutableStateOf<JSONObject?>(null) }
    var showLogin by remember { mutableStateOf(false) }
    var showBind by remember { mutableStateOf(false) }
    val pageSize = 15

    fun loadStoreState() {
        scope.launch {
            storeState = withContext(Dispatchers.IO) { runCatching { Api.storeService() }.getOrNull() }
        }
    }

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.appStore(p, pageSize, keyword, group) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(group) { load(1); loadStoreState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用商店") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load(page); loadStoreState() }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // ---- 授权状态栏 ----
            StoreAuthBar(
                state = storeState,
                onLogin = { showLogin = true },
                onUpgrade = { msg = "请在网页端完成企业版购买" },
                onBind = { showBind = true }
            )
            ScrollableTabRow(selectedTabIndex = group) {
                listOf("插件" to 0, "支付" to 1, "主题" to 2).forEach { (label, g) ->
                    Tab(selected = group == g, onClick = { group = g }, text = { Text(label) })
                }
            }
            OutlinedTextField(
                value = keyword, onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                label = { Text("搜索") }, singleLine = true,
                trailingIcon = { TextButton(onClick = { load(1) }) { Text("搜索") } }
            )
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp)) }
            if (list.isEmpty() && !loading && error == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("商店暂无内容") }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list.size) { i ->
                        val o = list[i]
                        val name = htmlClean(Json.str(o, "plugin_name", Json.str(o, "name", "插件")))
                        val desc = htmlClean(Json.str(o, "description"))
                        val author = Json.str(o, "author")
                        val version = Json.str(o, "version")
                        val localVersion = Json.str(o, "local_version")
                        val installed = Json.int(o, "install") == 1
                        val price = Json.str(o, "price", Json.str(o, "amount"))
                        val paid = price.isNotBlank() && price != "0" && price != "0.00"

                        Card(onClick = { detail = o }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                                    AssistChip(onClick = {}, label = { Text(if (installed) "已安装" else "未安装") })
                                }
                                if (desc.isNotBlank()) Text(desc, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                Row {
                                    if (author.isNotBlank()) Text("作者：" + author, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                    if (version.isNotBlank()) Text("版本：" + version, style = MaterialTheme.typography.labelSmall)
                                }
                                if (paid) Text("价格：¥" + price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text("点击查看详情与授权管理", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("第 " + page + " 页 · 共 " + total + " 条", style = MaterialTheme.typography.labelMedium)
                Row {
                    OutlinedButton(onClick = { load(page - 1) }, enabled = !loading && page > 1) { Text("上一页") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { load(page + 1) }, enabled = !loading && page * pageSize < total) { Text("下一页") }
                }
            }
        }
    }

    detail?.let { o ->
        StoreDetailDialog(
            item = o, group = group,
            onDismiss = { detail = null },
            onChanged = { m -> msg = m; detail = null; load(page) },
            onError = { e -> error = e; detail = null }
        )
    }

    if (showLogin) {
        StoreLoginDialog(
            onDismiss = { showLogin = false },
            onDone = { m -> msg = m; showLogin = false; loadStoreState() }
        )
    }
    if (showBind) {
        StoreBindDialog(
            onDismiss = { showBind = false },
            onDone = { m -> msg = m; showBind = false; loadStoreState() }
        )
    }
}

@Composable
private fun StoreAuthBar(
    state: JSONObject?,
    onLogin: () -> Unit,
    onUpgrade: () -> Unit,
    onBind: () -> Unit
) {
    if (state == null) return
    val id = Json.int(state, "id")
    if (id <= 0) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("未登录应用商店账户", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onLogin) { Text("登录") }
            }
        }
        return
    }
    val level = Json.str(state, "level")
    val haveLevel = Json.int(state, "is_have_level") == 1
    val boundPro = level == "0"
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (boundPro) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("当前设备：专业版", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("升级企业版后全部插件免费", style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) { Text("升级企业版") }
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("当前设备：企业版", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("全部插件免费使用", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (haveLevel) {
                OutlinedButton(onClick = onBind, modifier = Modifier.fillMaxWidth()) { Text("绑定专业版/企业版") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreLoginDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登录应用商店账户") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("访问应用商店需要登录，可下载插件和模板。", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(username, { username = it }, label = { Text("用户名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("密码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        busy = true; msg = null
                        runCatching { withContext(Dispatchers.IO) { Api.storeLogin(username, password) } }
                            .onSuccess { onDone("商店账户已登录") }
                            .onFailure { msg = it.message }
                        busy = false
                    }
                },
                enabled = !busy && username.isNotBlank() && password.isNotBlank()
            ) { Text("登录") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreBindDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var levels by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            levels = withContext(Dispatchers.IO) { runCatching { Api.storeAuthLevels() }.getOrDefault(emptyList()) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("绑定专业版/企业版") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("转移已有授权，原设备将解除绑定。", style = MaterialTheme.typography.labelSmall)
                if (loading) CircularProgressIndicator()
                if (!loading && levels.isEmpty()) Text("账号名下暂无授权")
                levels.forEach { l ->
                    val authId = Json.int(l, "id")
                    val serverIp = Json.str(l, "server_ip")
                    val lv = Json.int(l, "level")
                    val expire = Json.str(l, "expire_date")
                    val appKey = Json.str(l, "app_key")
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (lv == 0) "专业版" else "企业版", fontWeight = FontWeight.SemiBold)
                            if (serverIp.isNotBlank()) Text("服务器IP：" + serverIp, style = MaterialTheme.typography.labelSmall)
                            if (appKey.isNotBlank()) Text("授权指纹：" + appKey, style = MaterialTheme.typography.labelSmall)
                            if (expire.isNotBlank()) Text("到期：" + expire, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        busy = true; msg = null
                                        runCatching { withContext(Dispatchers.IO) { Api.appBindLevel(authId) } }
                                            .onSuccess { onDone("授权已转移至本机") }
                                            .onFailure { msg = it.message }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("解绑授权至本机") }
                        }
                    }
                }
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreDetailDialog(
    item: JSONObject,
    group: Int,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val name = htmlClean(Json.str(item, "plugin_name", Json.str(item, "name", "应用")))
    val desc = htmlClean(Json.str(item, "description"))
    val author = Json.str(item, "author")
    val version = Json.str(item, "version")
    val localVersion = Json.str(item, "local_version")
    val installed = Json.int(item, "install") == 1
    val price = Json.str(item, "price", Json.str(item, "amount"))
    val pluginKey = Json.str(item, "plugin_key", Json.str(item, "key"))
    val pluginId = Json.int(item, "id", Json.int(item, "plugin_id"))
    val hasUpdate = version.isNotBlank() && localVersion.isNotBlank() && version != localVersion
    val paid = price.isNotBlank() && price != "0" && price != "0.00"
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var showAuth by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (desc.isNotBlank()) Text(desc, style = MaterialTheme.typography.bodySmall)
                if (author.isNotBlank()) DetailRow("开发者", author)
                DetailRow("版本", version.ifBlank { "-" })
                if (localVersion.isNotBlank()) DetailRow("本地版本", localVersion)
                DetailRow("状态", if (installed) "已安装" else "未安装")
                if (paid) DetailRow("价格", "¥" + price)
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(6.dp))
                Text("可用操作", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (!installed) {
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true; msg = null
                                runCatching { withContext(Dispatchers.IO) { Api.appInstall(pluginKey, group, pluginId) } }
                                    .onSuccess { onChanged("安装完成") }
                                    .onFailure { onError(it.message ?: "安装失败") }
                                busy = false
                            }
                        },
                        enabled = !busy, modifier = Modifier.fillMaxWidth()
                    ) { Text(if (paid) "购买并安装" else "安装") }
                } else {
                    if (hasUpdate) {
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true; msg = null
                                    runCatching { withContext(Dispatchers.IO) { Api.appUpgrade(pluginKey, group, pluginId) } }
                                        .onSuccess { onChanged("更新完成") }
                                        .onFailure { onError(it.message ?: "更新失败") }
                                    busy = false
                                }
                            },
                            enabled = !busy, modifier = Modifier.fillMaxWidth()
                        ) { Text("更新到 " + version) }
                    }
                    OutlinedButton(onClick = { showAuth = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("授权管理 / 解绑") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true; msg = null
                                runCatching { withContext(Dispatchers.IO) { Api.appUninstall(pluginKey, group) } }
                                    .onSuccess { onChanged("卸载完成") }
                                    .onFailure { onError(it.message ?: "卸载失败") }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("卸载应用") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    if (showAuth) AuthDialog(pluginId, name, { showAuth = false }, onChanged, onError)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthDialog(
    pluginId: Int,
    appName: String,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            records = withContext(Dispatchers.IO) { runCatching { Api.appPurchaseRecords(pluginId) }.getOrDefault(emptyList()) }
            loading = false
        }
    }
    LaunchedEffect(pluginId) { load() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("授权管理") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appName, style = MaterialTheme.typography.labelMedium)
                if (loading) CircularProgressIndicator()
                if (!loading && records.isEmpty()) Text("暂无授权记录")
                records.forEach { r ->
                    val authId = Json.int(r, "id")
                    val remark = Json.str(r, "remark", Json.str(r, "device"))
                    val time = Json.str(r, "create_time")
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("授权 #" + authId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (remark.isNotBlank()) Text("设备：" + remark, style = MaterialTheme.typography.labelSmall)
                            if (time.isNotBlank()) Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        busy = true; msg = null
                                        runCatching { withContext(Dispatchers.IO) { Api.appBindLevel(authId) } }
                                            .onSuccess { msg = "已绑定到本机"; load() }
                                            .onFailure { msg = it.message }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("绑定到本机") }
                        }
                    }
                }
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

// ---------- 支付设置 ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var showPlugins by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                list = withContext(Dispatchers.IO) { Api.payList(1, 50).list }
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("支付设置") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "刷新") }
                    IconButton(onClick = { showPlugins = true }) { Icon(Icons.Default.Add, "新增") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp)) }
            if (list.isEmpty() && !loading && error == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无支付接口") }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list.size) { i ->
                        val o = list[i]
                        val id = Json.int(o, "id")
                        val name = htmlClean(Json.str(o, "name"))
                        val handle = Json.str(o, "handle")
                        val commodity = Json.int(o, "commodity") == 1
                        val recharge = Json.int(o, "recharge") == 1
                        Card(onClick = { detail = o }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text("插件 " + handle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (commodity) AssistChip(onClick = {}, label = { Text("商品下单") })
                                    if (recharge) AssistChip(onClick = {}, label = { Text("余额充值") })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPlugins) {
        PayPluginPicker(
            onDismiss = { showPlugins = false },
            onPick = { handle, name ->
                scope.launch {
                    loading = true; msg = null
                    runCatching { withContext(Dispatchers.IO) { Api.paySave(mapOf("name" to name, "handle" to handle, "commodity" to "1")) } }
                        .onSuccess { msg = "已新增支付接口"; load() }
                        .onFailure { error = it.message }
                    loading = false
                }
                showPlugins = false
            }
        )
    }

    detail?.let { o ->
        PayDetailDialog(
            pay = o,
            onDismiss = { detail = null },
            onChanged = { m -> msg = m; detail = null; load() },
            onError = { e -> error = e; detail = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayPluginPicker(onDismiss: () -> Unit, onPick: (String, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            plugins = withContext(Dispatchers.IO) { runCatching { Api.payPlugins() }.getOrDefault(emptyList()) }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择支付插件") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (loading) CircularProgressIndicator()
                if (!loading && plugins.isEmpty()) Text("暂无可用支付插件，请先到应用商店安装")
                plugins.forEach { p ->
                    val handle = Json.str(p, "id", Json.str(p, "handle"))
                    val info = p.optJSONObject("info")
                    val name = htmlClean(info?.let { Json.str(it, "name") } ?: handle)
                    Surface(onClick = { onPick(handle, name) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, Modifier.weight(1f))
                            Text(handle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayDetailDialog(
    pay: JSONObject,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val id = Json.int(pay, "id")
    val handle = Json.str(pay, "handle")
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var showConfig by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(htmlClean(Json.str(pay, "name"))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("ID", id.toString())
                DetailRow("插件", handle)
                DetailRow("编码", Json.str(pay, "code", "-").ifBlank { "-" })
                DetailRow("商品下单", if (Json.int(pay, "commodity") == 1) "是" else "否")
                DetailRow("余额充值", if (Json.int(pay, "recharge") == 1) "是" else "否")
                DetailRow("排序", Json.str(pay, "sort", "0"))
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.height(6.dp))
                Text("可用操作", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showConfig = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("配置插件") }
                    OutlinedButton(onClick = { showLog = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("查看日志") }
                }
                Button(
                    onClick = {
                        scope.launch {
                            busy = true; msg = null
                            runCatching { withContext(Dispatchers.IO) { Api.payTest(id) } }
                                .onSuccess { msg = "拨测成功：" + it }
                                .onFailure { msg = "拨测失败：" + it.message }
                            busy = false
                        }
                    },
                    enabled = !busy, modifier = Modifier.fillMaxWidth()
                ) { Text("支付拨测") }
                if (id != 1) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                runCatching { withContext(Dispatchers.IO) { Api.payDel(listOf(id)) } }
                                    .onSuccess { onChanged("已删除") }
                                    .onFailure { onError(it.message ?: "删除失败") }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("删除接口") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    if (showConfig) PayPluginConfigDialog(handle, { showConfig = false })
    if (showLog) PluginLogDialogForPay(handle, { showLog = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayPluginConfigDialog(handle: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置支付插件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("插件 " + handle, style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(key, { key = it }, label = { Text("配置项名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value, { value = it }, label = { Text("配置值") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("示例：app_id / private_key / mch_id 等，按插件说明填写。", style = MaterialTheme.typography.labelSmall)
                msg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { Api.paySetPluginConfig(handle, mapOf(key to value)) } }
                            .onSuccess { msg = it }
                            .onFailure { msg = it.message }
                    }
                },
                enabled = key.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginLogDialogForPay(handle: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var log by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(handle) {
        loading = true
        log = withContext(Dispatchers.IO) { runCatching { Api.payPluginLog(handle) }.getOrDefault("") }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("支付插件日志") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (loading) CircularProgressIndicator()
                if (!loading && log.isBlank()) Text("暂无日志")
                if (log.isNotBlank()) Text(log, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}
