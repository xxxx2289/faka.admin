package cn.xiaobai.acgfaka.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cn.xiaobai.acgfaka.admin.BuildConfig
import cn.xiaobai.acgfaka.admin.core.Api
import cn.xiaobai.acgfaka.admin.core.Http
import cn.xiaobai.acgfaka.admin.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(Http.baseUrl.ifBlank { Session.server(ctx) }) }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("服务器设置") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("默认服务器地址（构建时固定）", style = MaterialTheme.typography.titleMedium)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Text(BuildConfig.DEFAULT_SERVER, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Divider()
            Text("手动覆盖地址（可选）", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("地址") }, singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true; msg = null; isError = false
                            try {
                                val norm = Http.normalize(url)
                                Http.baseUrl = norm
                                withContext(Dispatchers.IO) { Api.dashboardData(0) }
                                Session.saveServer(ctx, norm)
                                msg = "连接成功"; isError = false
                            } catch (e: Exception) { msg = "连接失败：${e.message}"; isError = true } finally { busy = false }
                        }
                    },
                    enabled = !busy && url.isNotBlank()
                ) { Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(6.dp)); Text(if (busy) "测试中…" else "保存并测试") }
                OutlinedButton(
                    onClick = {
                        val norm = Http.normalize(url)
                        Http.baseUrl = norm; Session.saveServer(ctx, norm)
                        msg = "已保存"; isError = false
                    },
                    enabled = !busy
                ) { Text("仅保存") }
                OutlinedButton(
                    onClick = {
                        Session.saveServer(ctx, "")
                        Http.baseUrl = BuildConfig.DEFAULT_SERVER
                        url = BuildConfig.DEFAULT_SERVER
                        msg = "已恢复默认"; isError = false
                    },
                    enabled = !busy
                ) { Text("恢复默认") }
            }
            msg?.let { Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
            Divider()
            Text("提示：测试「保存并测试」会调用经营概览接口，未登录会提示会话失效，属正常。", style = MaterialTheme.typography.labelSmall)
        }
    }
}
