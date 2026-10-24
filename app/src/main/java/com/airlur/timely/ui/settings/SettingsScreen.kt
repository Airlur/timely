package com.airlur.timely.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlur.timely.R
import com.airlur.timely.data.LangMode
import com.airlur.timely.data.PrefsRepository
import com.airlur.timely.data.SwipeAction
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

/** 设置页：主题/语言行内分段（交互统一），滑动操作与水印带可交互示例卡 */
@Composable
fun SettingsScreen(
    prefs: PrefsRepository,
    snackbar: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val settings by prefs.settings.collectAsState(initial = UiSettings())
    var sub by remember { mutableStateOf<String?>(null) }
    var pickSlot by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            stringResource(R.string.settings),
            fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SettingItem(stringResource(R.string.theme)) {
                Segmented(
                    listOf(
                        ThemeMode.SYSTEM.name to stringResource(R.string.follow_system),
                        ThemeMode.LIGHT.name to stringResource(R.string.light_mode),
                        ThemeMode.DARK.name to stringResource(R.string.dark_mode)
                    ),
                    settings.theme.name
                ) { scope.launch { prefs.setTheme(ThemeMode.valueOf(it)) } }
            }
            SettingItem(stringResource(R.string.language)) {
                Segmented(
                    listOf(
                        LangMode.SYSTEM.name to stringResource(R.string.follow_system),
                        LangMode.ZH.name to "中文",
                        LangMode.EN.name to "English"
                    ),
                    settings.lang.name
                ) { scope.launch { prefs.setLang(LangMode.valueOf(it)) } }
            }
            SettingItem(stringResource(R.string.swipe_actions), trailing = swipeLabel(settings), onClick = { sub = "swipe" })
            SettingItem(stringResource(R.string.card_watermark), trailing = if (settings.wmOn) stringResource(R.string.granted) else stringResource(R.string.not_granted), onClick = { sub = "wm" })
            SettingItem(stringResource(R.string.notification), onClick = { sub = "noti" })
            SettingItem(stringResource(R.string.help_and_feedback), onClick = { sub = "help" })
            SettingItem(stringResource(R.string.about), onClick = { sub = "about" })
            Spacer(Modifier.height(88.dp))
        }
    }

    when (sub) {
        "swipe" -> SubPage(stringResource(R.string.swipe_actions), { sub = null }) {
            Column(Modifier.padding(16.dp)) {
                Segmented(
                    listOf(
                        "TRIGGER" to stringResource(R.string.swipe_trigger),
                        "REVEAL" to stringResource(R.string.swipe_reveal)
                    ),
                    settings.swipeMode.name,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    scope.launch { prefs.setSwipeMode(com.airlur.timely.data.SwipeMode.valueOf(it)) }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (settings.swipeMode == com.airlur.timely.data.SwipeMode.TRIGGER)
                        stringResource(R.string.swipe_trigger_desc)
                    else stringResource(R.string.swipe_reveal_desc),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                SwipeCard(
                    mode = settings.swipeMode,
                    slots = slotsFor(settings) { a ->
                        when (a) {
                            SwipeAction.PIN -> ctx.getString(R.string.act_pin)
                            SwipeAction.DETAIL -> ctx.getString(R.string.act_detail)
                            SwipeAction.TIMELINE -> ctx.getString(R.string.act_timeline)
                            SwipeAction.EDIT -> ctx.getString(R.string.act_edit)
                            SwipeAction.DELETE -> ctx.getString(R.string.act_delete)
                            SwipeAction.SHARE -> ctx.getString(R.string.act_share)
                        }
                    },
                    onAction = {
                        scope.launch {
                            snackbar.showSnackbar(
                                ctx.getString(R.string.demo_triggered) + " " + it.name
                            )
                        }
                    }
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFFCC2B5E), Color(0xFF753A88))
                            )
                        ).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💗", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                        Column(Modifier.weight(3f)) {
                            Text("和可颂在一起", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(2.dp))
                            Text("2024年6月1日 周六", fontSize = 14.sp, color = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("已经 876 天", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.swipe_demo_hint),
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.swipe_slots_title),
                    fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                listOf(
                    stringResource(R.string.swipe_slot_near, 1) to 0,
                    stringResource(R.string.swipe_slot_near, 2) to 1,
                    stringResource(R.string.swipe_slot_near, 3) to 2
                ).forEach { (label, idx) ->
                    SettingItem(label, trailing = settings.slotsRight[idx].name.lowercase(), onClick = { pickSlot = "R" + idx })
                }
                listOf(
                    stringResource(R.string.swipe_slot_near_right, 1) to 0,
                    stringResource(R.string.swipe_slot_near_right, 2) to 1,
                    stringResource(R.string.swipe_slot_near_right, 3) to 2
                ).forEach { (label, idx) ->
                    SettingItem(label, trailing = settings.slotsLeft[idx].name.lowercase(), onClick = { pickSlot = "L" + idx })
                }
                pickSlot?.let { key ->
                    ActionPickDialog(
                        slotKey = key,
                        onPick = { a ->
                            scope.launch {
                                val l = settings.slotsLeft.toMutableList()
                                val r = settings.slotsRight.toMutableList()
                                if (key[0] == 'R') r[key[1].digitToInt()] = a else l[key[1].digitToInt()] = a
                                prefs.setSlots(l, r)
                            }
                        },
                        onDismiss = { pickSlot = null }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        "wm" -> SubPage(stringResource(R.string.card_watermark), { sub = null }) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.show_watermark), modifier = Modifier.weight(1f))
                    Switch(checked = settings.wmOn, onCheckedChange = { scope.launch { prefs.setWatermark(on = it) } })
                }
                SettingItem(stringResource(R.string.watermark_position)) {
                    Segmented(
                        listOf(
                            WmPos.BR.name to stringResource(R.string.pos_br),
                            WmPos.BL.name to stringResource(R.string.pos_bl),
                            WmPos.TR.name to stringResource(R.string.pos_tr),
                            WmPos.TL.name to stringResource(R.string.pos_tl)
                        ), settings.wmPos.name
                    ) { scope.launch { prefs.setWatermark(pos = WmPos.valueOf(it)) } }
                }
                SettingItem(stringResource(R.string.watermark_opacity)) {
                    Segmented(
                        listOf(
                            WmOpacity.LOW.name to stringResource(R.string.opacity_low),
                            WmOpacity.MID.name to stringResource(R.string.opacity_mid),
                            WmOpacity.HIGH.name to stringResource(R.string.opacity_high)
                        ), settings.wmOp.name
                    ) { scope.launch { prefs.setWatermark(op = WmOpacity.valueOf(it)) } }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.wm_sample),
                    fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth()) {
                    SwipeCard(
                        mode = settings.swipeMode,
                        slots = slotsFor(settings) { it.name.lowercase() },
                        onAction = {}
                    ) {
                        Row(
                            Modifier.fillMaxWidth().background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFFCC2B5E), Color(0xFF753A88))
                                )
                            ).padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(3f)) {
                                Text("和可颂在一起", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("2024年6月1日 周六", fontSize = 14.sp, color = Color.White)
                            }
                            Text("已经 876 天", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (settings.wmOn) {
                            Watermark(settings.wmPos, settings.wmOp, Modifier.matchParentSize())
                        }
                    }
                }
            }
        }
        "noti" -> SubPage(stringResource(R.string.notification), { sub = null }) {
            val granted = ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            Column(Modifier.padding(16.dp)) {
                SettingItem(stringResource(R.string.notification_permission), trailing = if (granted) stringResource(R.string.granted) else stringResource(R.string.not_granted)) {
                    if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!granted) {
                    Text(
                        stringResource(R.string.open_system_settings),
                        color = Primary,
                        modifier = Modifier
                            .clickable {
                                ctx.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                    ).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                )
                            }
                            .padding(16.dp)
                    )
                }
                SettingItem(stringResource(R.string.test_notification_now)) {
                    Notifier.showTest(ctx)
                }
                SettingItem(stringResource(R.string.test_notification_delay)) {
                    AlarmScheduler.scheduleTest(ctx, 10_000)
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.test_scheduled)) }
                }
            }
        }
        "help" -> SubPage(stringResource(R.string.help_and_feedback), { sub = null }) {
            Column(Modifier.padding(16.dp)) {
                SettingItem(stringResource(R.string.faq)) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
                SettingItem(stringResource(R.string.feedback)) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
                SettingItem(stringResource(R.string.contact)) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
            }
        }
        "about" -> SubPage(stringResource(R.string.about), { sub = null }) {
            val version = try {
                ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
            } catch (e: Exception) { "?" }
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(Modifier.size(30.dp)) {
                            rotate(degrees = -24f, pivot = center) {
                                drawOval(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(
                                        size.width * 0.125f, size.height * 0.375f
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        size.width * 0.75f, size.height * 0.25f
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = size.width * 0.1f
                                    )
                                )
                            }
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "v$version · " + stringResource(R.string.slogan),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SettingItem(stringResource(R.string.version_info), trailing = version ?: "?") {}
                SettingItem(stringResource(R.string.privacy_policy)) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
                SettingItem(stringResource(R.string.user_agreement)) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
                SettingItem(stringResource(R.string.backup_restore)) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.coming_soon)) }
                }
            }
        }
    }
}

private fun swipeLabel(settings: UiSettings): String = settings.swipeMode.name.lowercase()

@Composable
private fun SettingItem(
    title: String,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, modifier = if (content != null) Modifier else Modifier.weight(1f))
        when {
            content != null -> content()
            trailing != null -> {
                Text(trailing, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
            }
        }
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
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹", fontSize = 26.sp, color = Primary,
                modifier = Modifier.clickable { onBack() }.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(60.dp))
        }
        content()
    }
}

@Composable
private fun ActionPickDialog(slotKey: String, onPick: (SwipeAction) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.choose_action), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SwipeAction.entries.forEach { a ->
                    Text(
                        a.name.lowercase(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(a); onDismiss() }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    )
}
