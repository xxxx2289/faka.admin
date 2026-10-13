package cn.xiaobai.acgfaka.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.admin.core.Api
import cn.xiaobai.acgfaka.admin.core.Http
import cn.xiaobai.acgfaka.admin.core.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<JSONObject?>(null) }
    val pageSize = 15

    fun load(p: Int) {
        scope.launch {
            loading = true; error = null
            try {
                if (Http.baseUrl.isBlank()) { error = "请先配置服务器地址"; loading = false; return@launch }
                val res = withContext(Dispatchers.IO) { Api.users(p, pageSize) }
                list = res.list; total = res.total; page = p
            } catch (e: Exception) { error = e.message } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { load(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("会员管理") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { IconButton(onClick = { load(page) }) { Icon(Icons.Default.Refresh, "刷新") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
            msg?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp)) }
            if (list.isEmpty() && !loading && error == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无会员") }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(list) { _, o ->
                        val id = Json.int(o, "id")
                        val name = Json.str(o, "username", "用户")
                        val balance = Json.str(o, "balance", "0")
                        val status = Json.int(o, "status")
                        Card(onClick = { detail = o }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    AssistChip(onClick = {}, label = { Text(if (status == 1) "正常" else "已禁用") })
                                }
                                Text("ID " + id + " · 余额 ¥" + balance, style = MaterialTheme.typography.bodySmall)
                                Text("元气 " + Json.str(o, "recharge", "0") + " · 硬币 " + Json.str(o, "coin", "0"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    detail?.let { u ->
        UserDetailDialog(
            user = u,
            onDismiss = { detail = null },
            onChanged = { m -> msg = m; detail = null; load(page) },
            onError = { e -> error = e }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDetailDialog(
    user: JSONObject,
    onDismiss: () -> Unit,
    onChanged: (String) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val id = Json.int(user, "id")
    var busy by remember { mutableStateOf(false) }
    var showBalance by remember { mutableStateOf(false) }
    var showCoin by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Json.str(user, "username", "会员")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("ID", id.toString())
                DetailRow("状态", if (Json.int(user, "status") == 1) "正常" else "已禁用")
                DetailRow("余额", "¥" + Json.str(user, "balance", "0"))
                DetailRow("硬币", Json.str(user, "coin", "0"))
                DetailRow("元气", Json.str(user, "recharge", "0"))
                DetailRow("总硬币", Json.str(user, "total_coin", "0"))
                DetailRow("邮箱", Json.str(user, "email", "-").ifBlank { "-" })
                DetailRow("手机", Json.str(user, "phone", "-").ifBlank { "-" })
                DetailRow("QQ", Json.str(user, "qq", "-").ifBlank { "-" })
                DetailRow("注册时间", Json.str(user, "create_time", "-"))
                Spacer(Modifier.height(6.dp))
                Text("可用操作", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showBalance = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("余额操作") }
                    OutlinedButton(onClick = { showCoin = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("硬币操作") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showEdit = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("编辑会员") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val newStatus = if (Json.int(user, "status") == 1) 0 else 1
                                    val m = withContext(Dispatchers.IO) { Api.userSetStatus(id, newStatus) }
                                    onChanged(m)
                                } catch (e: Exception) { onError(e.message ?: "操作失败") } finally { busy = false }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (Json.int(user, "status") == 1) "禁用" else "启用") }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                val m = withContext(Dispatchers.IO) { Api.userDelete(listOf(id)) }
                                onChanged(m)
                            } catch (e: Exception) { onError(e.message ?: "删除失败") } finally { busy = false }
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("删除会员") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )

    if (showBalance) {
        AssetDialog(
            title = "余额操作",
            onDismiss = { showBalance = false },
            onSubmit = { add, amount, log ->
                scope.launch {
                    busy = true
                    try {
                        val m = withContext(Dispatchers.IO) { Api.userBalance(id, add, amount, log) }
                        onChanged(m)
                    } catch (e: Exception) { onError(e.message ?: "操作失败") } finally { busy = false }
                }
            }
        )
    }
    if (showCoin) {
        AssetDialog(
            title = "硬币操作",
            onDismiss = { showCoin = false },
            onSubmit = { add, amount, log ->
                scope.launch {
                    busy = true
                    try {
                        val m = withContext(Dispatchers.IO) { Api.userCoin(id, add, amount, log) }
                        onChanged(m)
                    } catch (e: Exception) { onError(e.message ?: "操作失败") } finally { busy = false }
                }
            }
        )
    }
    if (showEdit) {
        EditUserDialog(
            user = user,
            onDismiss = { showEdit = false },
            onSubmit = { fields ->
                scope.launch {
                    busy = true
                    try {
                        val m = withContext(Dispatchers.IO) { Api.userSave(fields) }
                        onChanged(m)
                    } catch (e: Exception) { onError(e.message ?: "保存失败") } finally { busy = false }
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(76.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AssetDialog(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: (Boolean, Double, String) -> Unit
) {
    var add by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var log by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = add, onClick = { add = true }, label = { Text("增加") })
                    FilterChip(selected = !add, onClick = { add = false }, label = { Text("扣减") })
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("数量") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(log, { log = it }, label = { Text("操作原因（2-64字）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(add, amount.toDoubleOrNull() ?: 0.0, log) },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0 && log.length in 2..64
            ) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditUserDialog(
    user: JSONObject,
    onDismiss: () -> Unit,
    onSubmit: (Map<String, String>) -> Unit
) {
    val id = Json.int(user, "id")
    var email by remember { mutableStateOf(Json.str(user, "email")) }
    var phone by remember { mutableStateOf(Json.str(user, "phone")) }
    var qq by remember { mutableStateOf(Json.str(user, "qq")) }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑会员") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(email, { email = it }, label = { Text("邮箱") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("手机") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(qq, { qq = it }, label = { Text("QQ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("新密码（留空不改）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("提示：密码至少 6 位。", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = mutableMapOf("id" to id.toString(), "email" to email, "phone" to phone, "qq" to qq)
                    if (password.isNotBlank()) m["password"] = password
                    onSubmit(m)
                },
                enabled = password.isBlank() || password.length >= 6
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
