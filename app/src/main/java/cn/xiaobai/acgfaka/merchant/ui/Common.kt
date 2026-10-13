package cn.xiaobai.acgfaka.merchant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.xiaobai.acgfaka.merchant.core.Http
import org.json.JSONObject

fun fullImage(path: String?): String? {
    if (path.isNullOrBlank()) return null
    if (path.startsWith("http")) return path
    val base = Http.baseUrl.trimEnd('/')
    return if (path.startsWith("/")) base + path else "$base/$path"
}

fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.2f", v)

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FieldRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(88.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

@Composable
fun PagedList(
    title: String,
    page: Int,
    total: Int,
    pageSize: Int,
    loading: Boolean,
    error: String?,
    list: List<JSONObject>,
    render: @Composable (JSONObject) -> Unit,
    onRefresh: () -> Unit,
    onPage: (Int) -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
            },
            actions = { IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "刷新") } }
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 12.dp)) }
        if (list.isEmpty() && !loading && error == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("暂无数据") }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list.size) { i -> render(list[i]) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("第 $page 页 · 共 $total 条", style = MaterialTheme.typography.labelMedium)
            Row {
                OutlinedButton(onClick = { onPage(page - 1) }, enabled = !loading && page > 1) { Text("上一页") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onPage(page + 1) }, enabled = !loading && page * pageSize < total) { Text("下一页") }
            }
        }
    }
}
