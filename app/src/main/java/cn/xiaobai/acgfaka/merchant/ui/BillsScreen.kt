package cn.xiaobai.acgfaka.merchant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun BillsScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.bills(p, pageSize) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(1) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("账单明细") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        if (list.isEmpty() && !loading && error == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无账单") }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list.size) { i ->
                    val b = list[i]
                    val amount = Json.dbl(b, "amount")
                    val type = Json.str(b, "type", Json.str(b, "remark"))
                    val time = Json.str(b, "create_time")
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(type.ifBlank { "交易" }, style = MaterialTheme.typography.bodyMedium)
                                Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                (if (amount >= 0) "+" else "") + fmt(amount),
                                color = if (amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
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
