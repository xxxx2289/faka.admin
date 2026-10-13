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
fun CommoditiesScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var keyword by remember { mutableStateOf("") }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.commodities(p, pageSize, keyword) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(1) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("商品管理") },
            actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
        )
        OutlinedTextField(
            value = keyword, onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            label = { Text("商品名称") }, singleLine = true,
            trailingIcon = { TextButton(onClick = { load(1) }) { Text("搜索") } }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        if (list.isEmpty() && !loading && error == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无商品") }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list.size) { i ->
                    val c = list[i]
                    val id = Json.int(c, "id")
                    val name = Json.str(c, "name")
                    val price = Json.dbl(c, "price")
                    val stock = Json.int(c, "stock")
                    val status = Json.int(c, "status")
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1)
                                AssistChip(onClick = {}, label = { Text(if (status == 1) "上架" else "下架") })
                            }
                            Text("#${id} · ¥${fmt(price)} · 库存 $stock", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { Api.setCommodityStatus(id, if (status == 1) 0 else 1) }
                                                load(page)
                                            } catch (e: Exception) { error = e.message }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (status == 1) "下架" else "上架") }
                                OutlinedButton(
                                    onClick = { nav.navigate("cards") },
                                    modifier = Modifier.weight(1f)
                                ) { Text("管理卡密") }
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
