package cn.xiaobai.acgfaka.merchant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.merchant.core.Api
import cn.xiaobai.acgfaka.merchant.core.Http
import cn.xiaobai.acgfaka.merchant.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun OrdersScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filterDelivery by remember { mutableStateOf<Int?>(null) }
    var keyword by remember { mutableStateOf("") }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.orders(p, pageSize, keyword, filterDelivery) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(1) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("订单管理") },
            actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
        )
        OutlinedTextField(
            value = keyword, onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            label = { Text("订单号") }, singleLine = true,
            trailingIcon = { TextButton(onClick = { load(1) }) { Text("搜索") } }
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filterDelivery == null, onClick = { filterDelivery = null; load(1) }, label = { Text("全部") })
            FilterChip(selected = filterDelivery == 0, onClick = { filterDelivery = 0; load(1) }, label = { Text("待发货") })
            FilterChip(selected = filterDelivery == 1, onClick = { filterDelivery = 1; load(1) }, label = { Text("已发货") })
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        if (list.isEmpty() && !loading && error == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无订单") }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list.size) { i ->
                    val o = list[i]
                    val tradeNo = Json.str(o, "trade_no")
                    val amount = Json.dbl(o, "amount")
                    val status = Json.int(o, "status")
                    val delivery = Json.int(o, "delivery_status")
                    val contact = Json.str(o, "contact")
                    val createTime = Json.str(o, "create_time")
                    val commodityName = o.optJSONObject("commodity")?.let { Json.str(it, "name") } ?: ""
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(commodityName.ifBlank { "商品" }, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                                AssistChip(onClick = {}, label = { Text(if (status == 1) "已支付" else "待支付") })
                            }
                            Text(tradeNo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row {
                                Text("¥${fmt(amount)}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                Text("发货：${if (delivery == 1) "已发" else "未发"}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (contact.isNotBlank()) Text("联系：$contact", style = MaterialTheme.typography.labelSmall)
                            Text(createTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (status == 1 && delivery != 1) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val id = Json.int(o, "id")
                                                withContext(Dispatchers.IO) { Api.deliver(id) }
                                                load(page)
                                            } catch (e: Exception) { error = e.message }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("手动发货") }
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("第 $page 页 · 共 $total 条", style = MaterialTheme.typography.labelMedium)
            Row {
                OutlinedButton(onClick = { load(page - 1) }, enabled = !loading && page > 1) { Text("上一页") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { load(page + 1) }, enabled = !loading && page * pageSize < total) { Text("下一页") }
            }
        }
    }
}
