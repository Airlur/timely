package com.airlur.timely.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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

private sealed interface Overlay {
    data object None : Overlay
    data class Detail(val eventId: Long) : Overlay
    data class Edit(val eventId: Long) : Overlay // -1 = 新建
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

    // 跟随系统的明暗
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // 语言切换（AppCompat per-app locale，切换后自动重建）
    LaunchedEffect(settings.lang) {
        val tag = when (settings.lang) {
            LangMode.SYSTEM -> ""
            LangMode.ZH -> "zh"
            LangMode.EN -> "en"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    // 启动与回到前台时全量重排提醒（时间/时区变更也有 BootReceiver 兜底）
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            AlarmScheduler.rescheduleAll(ctx)
        }
    }

    TimelyTheme(darkTheme = dark) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (overlay == Overlay.None) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        listOf(
                            stringResource(R.string.tab_date),
                            stringResource(R.string.tab_time),
                            stringResource(R.string.tab_settings)
                        ).forEachIndexed { i, label ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = {
                                    Text(
                                        listOf("🗓", "⏱", "⚙")[i],
                                        fontSize = 18.sp,
                                        color = if (tab == i) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
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
                            detailRevealed = false // 先归零，下一帧扩散展开
                        },
                        onEdit = { id -> overlay = Overlay.Edit(id) }
                    )
                    1 -> TimeScreen()
                    else -> SettingsScreen(prefs, snackbar)
                }

                val o = overlay
                when (o) {
                    is Overlay.Detail -> {
                        // 详情：从屏幕上部中心圆形扩散展开（对应旧版 CircleRevealRoute）
                        LaunchedEffect(o.eventId) {
                            kotlinx.coroutines.delay(30)
                            detailRevealed = true
                        }
                        val dm = ctx.resources.displayMetrics
                        val center = androidx.compose.ui.geometry.Offset(dm.widthPixels / 2f, dm.heightPixels / 3f)
                        CircularReveal(center = center, revealed = detailRevealed, modifier = Modifier.fillMaxSize()) {
                            DetailScreen(
                                eventId = o.eventId,
                                dao = app.db.eventDao(),
                                snackbar = snackbar,
                                onClose = { detailRevealed = false },
                                onEdit = { id -> overlay = Overlay.Edit(id) }
                            )
                        }
                        LaunchedEffect(detailRevealed, o.eventId) {
                            if (!detailRevealed) {
                                kotlinx.coroutines.delay(580)
                                overlay = Overlay.None
                            }
                        }
                    }
                    is Overlay.Edit -> {
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
                    Overlay.None -> {}
                }
            }
        }
    }
}
