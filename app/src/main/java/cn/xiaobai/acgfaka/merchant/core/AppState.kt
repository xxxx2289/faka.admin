package cn.xiaobai.acgfaka.admin.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 全局界面状态：会话是否被服务端锁定。锁定时前台弹锁屏。 */
object AppState {
    var locked by mutableStateOf(false)
}
