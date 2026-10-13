package cn.xiaobai.acgfaka.admin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cn.xiaobai.acgfaka.admin.core.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 锁屏界面。服务端闲置超时后会锁会话，所有接口返回 42010。
 * 输入登录密码调 unlockPassword 即可恢复。
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("后台已锁定", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("长时间未操作，请输入登录密码解锁", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("登录密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            msg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true; msg = null; isError = false
                        try {
                            val m = withContext(Dispatchers.IO) { Api.unlockPassword(password) }
                            msg = m
                            password = ""
                            onUnlocked()
                        } catch (e: Exception) {
                            msg = e.message ?: "解锁失败"
                            isError = true
                        } finally { busy = false }
                    }
                },
                enabled = !busy && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (busy) "验证中…" else "解锁") }
        }
    }
}
