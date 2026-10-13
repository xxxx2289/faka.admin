package cn.xiaobai.acgfaka.merchant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.cards(p, pageSize) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(1) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("卡密管理") },
            actions = {
                IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") }
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "添加") }
            }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        if (list.isEmpty() && !loading && error == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无卡密") }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list.size) { i ->
                    val c = list[i]
                    val id = Json.int(c, "id")
                    val draft = Json.str(c, "draft")
                    val status = Json.int(c, "status")
                    val premium = Json.dbl(c, "draft_premium")
                    val commodityId = Json.int(c, "commodity_id")
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("#${id}  商品ID $commodityId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(draft, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            Row {
                                AssistChip(onClick = {}, label = { Text(if (status == 0) "未售" else "已售") })
                                if (premium > 0) { Spacer(Modifier.width(6.dp)); AssistChip(onClick = {}, label = { Text("+¥${fmt(premium)}") }) }
                                Spacer(Modifier.weight(1f))
                                if (status == 0) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            try { withContext(Dispatchers.IO) { Api.deleteCard(id) }; load(page) } catch (e: Exception) { error = e.message }
                                        }
                                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                                }
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

    if (showAdd) {
        AddCardDialog(
            onDismiss = { showAdd = false },
            onConfirm = { commodityId, draft, premium, race ->
                scope.launch {
                    loading = true; error = null
                    try {
                        withContext(Dispatchers.IO) { Api.addCards(commodityId, draft, premium, race) }
                        showAdd = false
                        load(1)
                    } catch (e: Exception) { error = e.message; showAdd = false } finally { loading = false }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onConfirm: (Int, String, Double, String) -> Unit) {
    var commodityId by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var premium by remember { mutableStateOf("0") }
    var race by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加卡密") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(commodityId, { commodityId = it }, label = { Text("商品 ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(draft, { draft = it }, label = { Text("卡密内容（每行一条）") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                OutlinedTextField(premium, { premium = it }, label = { Text("加价（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(race, { race = it }, label = { Text("规格/种族（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(commodityId.toIntOrNull() ?: 0, draft, premium.toDoubleOrNull() ?: 0.0, race) },
                enabled = commodityId.toIntOrNull() != null && draft.isNotBlank()
            ) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
