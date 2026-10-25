package com.airlur.timely.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import com.airlur.timely.R
import com.airlur.timely.TimelyApp
import com.airlur.timely.data.LangMode
import com.airlur.timely.data.PrefsRepository
import com.airlur.timely.data.ThemeMode
import com.airlur.timely.data.UiSettings
import com.airlur.timely.notify.AlarmScheduler
import com.airlur.timely.ui.date.DateScreen
import com.airlur.timely.ui.date.DetailScreen
import com.airlur.timely.ui.date.EditEventScreen
import com.airlur.timely.ui.settings.SettingsScreen
import com.airlur.timely.ui.theme.TimelyTheme
import com.airlur.timely.ui.time.TimeScreen
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface Overlay {
    data object None : Overlay
    data class Detail(val eventId: Long) : Overlay
    data class Edit(val eventId: Long) : Overlay // -1 = 新建
}

/** 编辑/样式页进场：从右滑入（对应旧版 RouteUtils 的 300ms 右滑路由） */
@Composable
private fun SlideIn(content: @Composable () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(300)) { v, _ -> t = v }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        Box(Modifier.offset { androidx.compose.ui.unit.IntOffset(((1f - t) * w).roundToInt(), 0) }) {
            content()
        }
    }
}

@Composable
fun TimelyAppRoot() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TimelyApp
    val prefs = remember { PrefsRepository(ctx) }
    val settings by prefs.settings.collectAsState(initial = UiSettings())
    val snackbar = remember { SnackbarHostState() }

    var tab by remember { mutableStateOf(0) }
    var overlay by remember { mutableStateOf<Overlay>(Overlay.None) }
    var detailRevealed by remember { mutableStateOf(true) }

    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // 语言切换（AppCompat per-app locale）
    LaunchedEffect(settings.lang) {
        val tag = when (settings.lang) {
            LangMode.SYSTEM -> ""
            LangMode.ZH -> "zh"
            LangMode.EN -> "en"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    // 状态栏图标颜色跟随应用内主题（而非系统）
    LaunchedEffect(dark) {
        (ctx as? Activity)?.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView).isAppearanceLightStatusBars = !dark
        }
    }

    // 启动时后台全量重排提醒
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { AlarmScheduler.rescheduleAll(ctx) }
    }

    // 系统返回：先收 overlay（详情/编辑），设置子页由 SettingsScreen 自己的 BackHandler 接
    BackHandler(enabled = overlay != Overlay.None) {
        when (val o = overlay) {
            is Overlay.Detail -> detailRevealed = false // 现有 LaunchedEffect 会在动画后置 None
            is Overlay.Edit -> overlay = Overlay.None
            Overlay.None -> {}
        }
    }

    TimelyTheme(darkTheme = dark) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (overlay == Overlay.None) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val items = listOf(
                            Triple(Icons.Rounded.CalendarMonth, stringResource(R.string.tab_date), 0),
                            Triple(Icons.Rounded.Timer, stringResource(R.string.tab_time), 1),
                            Triple(Icons.Rounded.Settings, stringResource(R.string.tab_settings), 2)
                        )
                        items.forEach { (icon, label, idx) ->
                            NavigationBarItem(
                                selected = tab == idx,
                                onClick = { tab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.5.sp) }
                            )
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (tab) {
                    0 -> DateScreen(
                        dao = app.db.eventDao(),
                        catDao = app.db.categoryDao(),
                        prefs = prefs,
                        snackbar = snackbar,
                        onOpenDetail = { id ->
                            overlay = Overlay.Detail(id)
                            detailRevealed = false // 下一帧扩散展开
                        },
                        onEdit = { id -> overlay = Overlay.Edit(id) }
                    )
                    1 -> TimeScreen()
                    else -> SettingsScreen(prefs, snackbar)
                }

                when (val o = overlay) {
                    is Overlay.Detail -> {
                        LaunchedEffect(detailRevealed, o.eventId) {
                            if (!detailRevealed) {
                                delay(580)
                                overlay = Overlay.None
                            } else {
                                delay(30)
                                detailRevealed = true
                            }
                        }
                        val dm = ctx.resources.displayMetrics
                        val center = androidx.compose.ui.geometry.Offset(
                            dm.widthPixels / 2f, dm.heightPixels / 3f
                        )
                        CircularReveal(center = center, revealed = detailRevealed, modifier = Modifier.fillMaxSize()) {
                            DetailScreen(
                                eventId = o.eventId,
                                dao = app.db.eventDao(),
                                snackbar = snackbar,
                                onClose = { detailRevealed = false },
                                onEdit = { id -> overlay = Overlay.Edit(id) }
                            )
                        }
                    }
                    is Overlay.Edit -> {
                        SlideIn {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                EditEventScreen(
                                    eventId = o.eventId,
                                    dao = app.db.eventDao(),
                                    catDao = app.db.categoryDao(),
                                    prefs = prefs,
                                    onClose = { overlay = Overlay.None }
                                )
                            }
                        }
                    }
                    Overlay.None -> {}
                }
            }
        }
    }
}
