package cn.xiaobai.acgfaka.admin.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1A237E),
    secondary = Color(0xFF3949AB),
    tertiary = Color(0xFF5C6BC0)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF7986CB),
    tertiary = Color(0xFF5C6BC0)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
