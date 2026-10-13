package cn.xiaobai.acgfaka.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.xiaobai.acgfaka.admin.core.Api
import cn.xiaobai.acgfaka.admin.core.AppState
import cn.xiaobai.acgfaka.admin.core.Http
import cn.xiaobai.acgfaka.admin.core.Session
import cn.xiaobai.acgfaka.admin.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { AppRoot() } }

        // 每次回到前台检查会话是否被服务端锁定
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (!Session.isLoggedIn(this@MainActivity)) return
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val locked = Api.lockStatus()
                        withContext(Dispatchers.Main) { AppState.locked = locked }
                    } catch (_: Exception) {}
                }
            }
        })
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    LaunchedEffect(Unit) { Http.baseUrl = Session.server(ctx) }

    // 锁定则覆盖显示锁屏
    if (AppState.locked) {
        LockScreen(onUnlocked = { AppState.locked = false })
        return
    }

    val tabs = listOf(
        Tab("dashboard", "概览", Icons.Default.Dashboard),
        Tab("orders", "订单", Icons.Default.ReceiptLong),
        Tab("users", "用户", Icons.Default.People),
        Tab("more", "更多", Icons.Default.Apps)
    )
    val showBar = current in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) { popUpTo("dashboard") { saveState = true }; launchSingleTop = true; restoreState = true }
                        },
                        icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(nav) }
            composable("orders") { OrdersScreen(nav) }
            composable("commodities") { CommoditiesScreen(nav) }
            composable("cards") { CardsScreen(nav) }
            composable("users") { UsersScreen(nav) }
            composable("cash") { CashScreen(nav) }
            composable("logs") { LogsScreen(nav) }
            composable("plugins") { PluginsScreen(nav) }
            composable("store") { StoreScreen(nav) }
            composable("more") { MoreScreen(nav) }
            composable("login") { LoginScreen(nav) }
            composable("server") { ServerScreen(nav) }
        }
    }
}
