package com.airlur.timely.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlur.timely.R
import com.airlur.timely.data.LangMode
import com.airlur.timely.data.PrefsRepository
import com.airlur.timely.data.SwipeAction
import com.airlur.timely.data.SwipeMode
import com.airlur.timely.data.ThemeMode
import com.airlur.timely.data.UiSettings
import com.airlur.timely.data.WmOpacity
import com.airlur.timely.data.WmPos
import com.airlur.timely.notify.AlarmScheduler
import com.airlur.timely.notify.Notifier
import com.airlur.timely.ui.Segmented
import com.airlur.timely.ui.SwipeCard
import com.airlur.timely.ui.Watermark
import com.airlur.timely.ui.slotsFor
import com.airlur.timely.ui.theme.Primary
import kotlinx.coroutines.launch

private fun actionLabel(ctx: android.content.Context, a: SwipeAction): String = when (a) {
    SwipeAction.PIN -> ctx.getString(R.string.act_pin)
    SwipeAction.DETAIL -> ctx.getString(R.string.act_detail)
    SwipeAction.TIMELINE -> ctx.getString(R.string.act_timeline)
    SwipeAction.EDIT -> ctx.getString(R.string.act_edit)
    SwipeAction.DELETE -> ctx.getString(R.string.act_delete)
    SwipeAction.SHARE -> ctx.getString(R.string.act_share)
}

/** 设置页：分组卡片布局；主题/语言行内分段；滑动与水印子页带示例卡 */
@Composable
fun SettingsScreen(
    prefs: PrefsRepository,
    snackbar: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val settings by prefs.settings.collectAsState(initial = UiSettings())
    var sub by remember { mutableStateOf<String?>(null) }

    // 系统返回：子页先回主设置页
    BackHandler(enabled = sub != null) { sub = null }

    if (sub != null) {
        when (sub) {
            "swipe" -> SwipePage(prefs, snackbar) { sub = null }
            "wm" -> WatermarkPage(prefs, settings) { sub = null }
            "noti" -> NotificationPage(snackbar) { sub = null }
            "help" -> HelpPage(snackbar) { sub = null }
            "about" -> AboutPage { sub = null }
        }
        return
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            stringResource(R.string.settings),
            fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionLabel(stringResource(R.string.sec_general))
            GroupCard {
                RowItem(
                    icon = Icons.Rounded.BrightnessMedium,
                    title = stringResource(R.string.theme),
                    trailing = {
                        Segmented(
                            listOf(
                                ThemeMode.SYSTEM.name to stringResource(R.string.follow_system),
                                ThemeMode.LIGHT.name to stringResource(R.string.light_mode),
                                ThemeMode.DARK.name to stringResource(R.string.dark_mode)
                            ),
                            settings.theme.name
                        ) { scope.launch { prefs.setTheme(ThemeMode.valueOf(it)) } }
                    }
                )
                RowItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.language),
                    trailing = {
                        Segmented(
                            listOf(
                                LangMode.SYSTEM.name to stringResource(R.string.follow_system),
                                LangMode.ZH.name to "中文",
                                LangMode.EN.name to "English"
                            ),
                            settings.lang.name
                        ) { scope.launch { prefs.setLang(LangMode.valueOf(it)) } }
                    }
                )
            }
            SectionLabel(stringResource(R.string.sec_gesture))
            GroupCard {
                NavigateRow(
                    icon = Icons.Rounded.Swipe,
                    title = stringResource(R.string.swipe_actions),
                    value = stringResource(
                        if (settings.swipeMode == SwipeMode.TRIGGER) R.string.swipe_trigger
                        else R.string.swipe_reveal
                    )
                ) { sub = "swipe" }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(
                    icon = Icons.Rounded.Layers,
                    title = stringResource(R.string.card_watermark),
                    value = stringResource(
                        if (settings.wmOn) R.string.turned_on else R.string.turned_off
                    )
                ) { sub = "wm" }
            }
            SectionLabel(stringResource(R.string.sec_support))
            GroupCard {
                NavigateRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.notification), value = null
                ) { sub = "noti" }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(
                    icon = Icons.Rounded.Help,
                    title = stringResource(R.string.help_and_feedback), value = null
                ) { sub = "help" }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.about), value = null
                ) { sub = "about" }
            }
            Spacer(Modifier.height(88.dp))
        }
    }
}

/* ---------- 通用行组件 ---------- */

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun GroupCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}

@Composable
private fun RowItem(
    icon: ImageVector,
    title: String,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun NavigateRow(
    icon: ImageVector,
    title: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(title, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Rounded.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .size(24.dp)
            )
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

/* ---------- 滑动操作子页（分段 + 可拖拽示例卡 + 六槽位） ---------- */

@Composable
private fun SwipePage(prefs: PrefsRepository, snackbar: SnackbarHostState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val settings by prefs.settings.collectAsState(initial = UiSettings())
    var pickSlot by remember { mutableStateOf<String?>(null) }

    SubPage(stringResource(R.string.swipe_actions), onBack) {
        Column(Modifier.padding(16.dp)) {
            Segmented(
                listOf(
                    "TRIGGER" to stringResource(R.string.swipe_trigger),
                    "REVEAL" to stringResource(R.string.swipe_reveal)
                ),
                settings.swipeMode.name,
                modifier = Modifier.fillMaxWidth()
            ) { scope.launch { prefs.setSwipeMode(SwipeMode.valueOf(it)) } }
            Spacer(Modifier.height(12.dp))
            Text(
                if (settings.swipeMode == SwipeMode.TRIGGER)
                    stringResource(R.string.swipe_trigger_desc)
                else stringResource(R.string.swipe_reveal_desc),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            SwipeCard(
                mode = settings.swipeMode,
                slots = slotsFor(settings) { actionLabel(ctx, it) },
                onAction = {
                    scope.launch {
                        snackbar.showSnackbar(ctx.getString(R.string.demo_triggered) + "：" + actionLabel(ctx, it))
                    }
                }
            ) { DemoCard(settings.wmOn, settings.wmPos, settings.wmOp) }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.swipe_demo_hint),
                fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
            SectionLabel(stringResource(R.string.swipe_slots_title))
            listOf(
                stringResource(R.string.swipe_slot_near, 1) to "R0",
                stringResource(R.string.swipe_slot_near, 2) to "R1",
                stringResource(R.string.swipe_slot_near, 3) to "R2",
                stringResource(R.string.swipe_slot_near_right, 1) to "L0",
                stringResource(R.string.swipe_slot_near_right, 2) to "L1",
                stringResource(R.string.swipe_slot_near_right, 3) to "L2"
            ).forEach { (label, key) ->
                val current = if (key[0] == 'R') settings.slotsRight[key[1] - '0']
                else settings.slotsLeft[key[1] - '0']
                GroupCard {
                    NavigateRow(icon = Icons.Rounded.Swipe, title = label, value = actionLabel(ctx, current)) {
                        pickSlot = key
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            pickSlot?.let { key ->
                AlertDialog(
                    onDismissRequest = { pickSlot = null },
                    confirmButton = {},
                    title = {
                        Text(stringResource(R.string.choose_action), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            SwipeAction.entries.forEach { a ->
                                Text(
                                    actionLabel(ctx, a),
                                    fontSize = 15.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                val l = settings.slotsLeft.toMutableList()
                                                val r = settings.slotsRight.toMutableList()
                                                if (key[0] == 'R') r[key[1] - '0'] = a
                                                else l[key[1] - '0'] = a
                                                prefs.setSlots(l, r)
                                            }
                                            pickSlot = null
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 示例卡（滑动与水印页共用） ---------- */

@Composable
private fun DemoCard(wmOn: Boolean, wmPos: WmPos, wmOp: WmOpacity) {
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color(0xFFCC2B5E), Color(0xFF753A88))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(3f)) {
                Text(
                    stringResource(R.string.demo_title),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.demo_date), fontSize = 14.sp, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.demo_since),
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }
        if (wmOn) {
            Watermark(wmPos, wmOp, Modifier.matchParentSize())
        }
    }
}

/* ---------- 水印子页 ---------- */

@Composable
private fun WatermarkPage(prefs: PrefsRepository, settings: UiSettings, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    SubPage(stringResource(R.string.card_watermark), onBack) {
        Column(Modifier.padding(16.dp)) {
            GroupCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Rounded.Layers, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)
                    )
                    Text(stringResource(R.string.show_watermark), fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.wmOn,
                        onCheckedChange = { scope.launch { prefs.setWatermark(on = it) } }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            GroupCard {
                RowItem(
                    icon = Icons.Rounded.Layers,
                    title = stringResource(R.string.watermark_position)
                ) {
                    Segmented(
                        listOf(
                            WmPos.BR.name to stringResource(R.string.pos_br),
                            WmPos.BL.name to stringResource(R.string.pos_bl),
                            WmPos.TR.name to stringResource(R.string.pos_tr),
                            WmPos.TL.name to stringResource(R.string.pos_tl)
                        ),
                        settings.wmPos.name
                    ) { scope.launch { prefs.setWatermark(pos = WmPos.valueOf(it)) } }
                }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                RowItem(
                    icon = Icons.Rounded.Layers,
                    title = stringResource(R.string.watermark_opacity)
                ) {
                    Segmented(
                        listOf(
                            WmOpacity.LOW.name to stringResource(R.string.opacity_low),
                            WmOpacity.MID.name to stringResource(R.string.opacity_mid),
                            WmOpacity.HIGH.name to stringResource(R.string.opacity_high)
                        ),
                        settings.wmOp.name
                    ) { scope.launch { prefs.setWatermark(op = WmOpacity.valueOf(it)) } }
                }
            }
            SectionLabel(stringResource(R.string.wm_sample))
            SwipeCard(
                mode = settings.swipeMode,
                slots = slotsFor(settings) { actionLabel(ctx, it) },
                onAction = {}
            ) { DemoCard(settings.wmOn, settings.wmPos, settings.wmOp) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 通知子页（权限守卫，杜绝 SecurityException 闪退） ---------- */

@Composable
private fun NotificationPage(snackbar: SnackbarHostState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember {
        mutableStateOf(
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    fun testNow() {
        if (!granted) {
            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.noti_need_permission)) }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        Notifier.showTest(ctx)
    }

    fun testDelayed() {
        if (!granted) {
            scope.launch { snackbar.showSnackbar(ctx.getString(R.string.noti_need_permission)) }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        AlarmScheduler.scheduleTest(ctx, 10_000)
        scope.launch { snackbar.showSnackbar(ctx.getString(R.string.test_scheduled)) }
    }

    SubPage(stringResource(R.string.notification), onBack) {
        Column(Modifier.padding(16.dp)) {
            GroupCard {
                RowItem(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.notification_permission)
                ) {
                    Text(
                        stringResource(if (granted) R.string.granted else R.string.not_granted),
                        fontSize = 13.sp,
                        color = if (granted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
                if (!granted) {
                    HorizontalDivider(Modifier.padding(start = 52.dp))
                    NavigateRow(
                        icon = Icons.Rounded.Notifications,
                        title = stringResource(R.string.open_system_settings), value = null
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.test_notification_now), value = null
                ) { testNow() }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.test_notification_delay), value = null
                ) { testDelayed() }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 帮助与反馈 ---------- */

@Composable
private fun HelpPage(snackbar: SnackbarHostState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    SubPage(stringResource(R.string.help_and_feedback), onBack) {
        Column(Modifier.padding(16.dp)) {
            GroupCard {
                NavigateRow(Icons.Rounded.Help, stringResource(R.string.faq), null) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(Icons.Rounded.Help, stringResource(R.string.feedback), null) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(Icons.Rounded.Help, stringResource(R.string.contact), null) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- 关于 ---------- */

@Composable
private fun AboutPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
    } catch (e: Exception) { "?" }
    SubPage(stringResource(R.string.about), onBack) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(Modifier.size(32.dp)) {
                        androidx.compose.ui.graphics.drawscope.rotate(degrees = -24f, pivot = center) {
                            drawOval(
                                color = Color.White,
                                topLeft = androidx.compose.ui.geometry.Offset(
                                    size.width * 0.125f, size.height * 0.375f
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width * 0.75f, size.height * 0.25f
                                ),
                                style = Stroke(width = size.width * 0.1f)
                            )
                        }
                    }
                }
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "v$version · " + stringResource(R.string.slogan),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            GroupCard {
                NavigateRow(Icons.Rounded.Info, stringResource(R.string.version_info), version) {}
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(Icons.Rounded.Info, stringResource(R.string.privacy_policy), null) {}
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(Icons.Rounded.Info, stringResource(R.string.user_agreement), null) {}
                HorizontalDivider(Modifier.padding(start = 52.dp))
                NavigateRow(Icons.Rounded.Info, stringResource(R.string.backup_restore), null) {}
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
