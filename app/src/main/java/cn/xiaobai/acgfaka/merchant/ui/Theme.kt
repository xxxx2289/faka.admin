package cn.xiaobai.acgfaka.merchant.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF00695C),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFF00897B)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF80CBC4),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFF00897B)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
