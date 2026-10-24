package com.airlur.timely.ui.date

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import android.Manifest
import com.airlur.timely.R
import com.airlur.timely.data.BgType
import com.airlur.timely.data.CategoryDao
import com.airlur.timely.data.EventDao
import com.airlur.timely.data.EventEntity
import com.airlur.timely.data.PrefsRepository
import com.airlur.timely.data.Repeat
import com.airlur.timely.data.TimeUnit
import com.airlur.timely.notify.AlarmScheduler
import com.airlur.timely.ui.eventBackground
import com.airlur.timely.ui.theme.PresetColors
import com.airlur.timely.ui.theme.PresetGradients
import com.airlur.timely.ui.theme.Primary
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

private val EMOJI_ICONS = listOf(null, "🚩", "🎂", "💗", "🏠", "💼", "📚", "💰", "🐱", "⭐", "✈️", "🏃", "🎬", "🎁", "⚽", "📅")

/** 新建/编辑事件：对照旧版 add_event_page 的四块分卡结构 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEventScreen(
    eventId: Long, // -1 = 新建
    dao: EventDao,
    catDao: CategoryDao,
    prefs: PrefsRepository,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var loaded by remember { mutableStateOf<EventEntity?>(null) }
    LaunchedEffect(eventId) {
        if (eventId > 0) {
            loaded = dao.getById(eventId)
            if (loaded == null) onClose()
        }
    }
    val source = loaded

    var title by remember(source) { mutableStateOf(source?.title ?: "") }
    var date by remember(source) { mutableStateOf(source?.let { localDateOf(it) } ?: LocalDate.now().plusDays(30)) }
    var time by remember(source) { mutableStateOf(source?.let { localTimeOf(it) } ?: LocalTime.MIDNIGHT) }
    var lunar by remember(source) { mutableStateOf(source?.isLunar ?: false) }
    var categoryId by remember(source) { mutableStateOf(source?.categoryId) }
    var reminder by remember(source) { mutableStateOf(source?.reminderTime?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }) }
    var repeat by remember(source) { mutableStateOf(source?.let { Repeat.valueOf(it.repeat) } ?: Repeat.NONE) }
    var customInterval by remember(source) { mutableStateOf(2) }
    var customUnit by remember(source) { mutableStateOf("WEEK") }
    var customWeekdays by remember(source) { mutableStateOf(setOf<Int>()) }
    var endTime by remember(source) { mutableStateOf(source?.repeatEnd?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }) }
    var pinned by remember(source) { mutableStateOf(source?.isPinned ?: false) }
    var includeStart by remember(source) { mutableStateOf(source?.includeStartDay ?: false) }
    var bgType by remember(source) { mutableStateOf(source?.let { BgType.valueOf(it.bgType) } ?: BgType.NONE) }
    var bgColor by remember(source) { mutableStateOf(source?.bgColor) }
    var gradStart by remember(source) { mutableStateOf(source?.gradientStart) }
    var gradEnd by remember(source) { mutableStateOf(source?.gradientEnd) }
    var imagePath by remember(source) { mutableStateOf(source?.imagePath) }
    var icon by remember(source) { mutableStateOf(source?.iconName) }
    var font by remember(source) { mutableStateOf(source?.fontFamily) }

    val cats by catDao.observeAll().collectAsState(initial = emptyList())
    var sheet by remember { mutableStateOf<String?>(null) } // date/cat/rem/rep/end
    var styleOpen by remember { mutableStateOf(false) }
    var manageCats by remember { mutableStateOf(false) }

    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    val targetDateTime = LocalDateTime.of(date, time)
    val dateText = if (lunar) lunarTextOf(EventEntity(id = 0, title = "", targetDate = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), createdAt = 0, isLunar = true))
        else "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    val catName = cats.firstOrNull { it.id == categoryId }?.name

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 顶栏：返回 / 标题 / 保存（文字按钮，同旧版）
        Row(Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹", fontSize = 26.sp, color = Primary,
                modifier = Modifier.clickable { onClose() }.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            Text(
                if (eventId > 0) stringResource(R.string.edit_event) else stringResource(R.string.new_event),
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            TextButton(onClick = {
                if (title.isBlank()) return@TextButton
                if (reminder != null && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                scope.launch {
                    val entity = EventEntity(
                        id = source?.id ?: 0,
                        title = title.trim(),
                        targetDate = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        createdAt = source?.createdAt ?: System.currentTimeMillis(),
                        displayUnit = source?.displayUnit ?: TimeUnit.DAY.name,
                        isPinned = pinned,
                        pinnedAt = if (pinned) (source?.pinnedAt ?: System.currentTimeMillis()) else null,
                        bgType = bgType.name,
                        bgColor = bgColor,
                        gradientStart = gradStart,
                        gradientEnd = gradEnd,
                        imagePath = imagePath,
                        isLunar = lunar,
                        categoryId = categoryId,
                        sortOrder = source?.sortOrder ?: 0,
                        iconName = icon,
                        fontFamily = font,
                        reminderTime = reminder?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                        includeStartDay = includeStart,
                        repeat = repeat.name,
                        repeatEnd = endTime?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                        customRule = if (repeat == Repeat.CUSTOM)
                            "$customInterval,$customUnit" + customWeekdays.sorted().joinToString("", prefix = ",") { it.toString() }
                        else null
                    )
                    val id = dao.upsert(entity)
                    val saved = if (entity.id == 0L) entity.copy(id = id) else entity
                    if (pinned) dao.clearPinnedExcept(saved.id)
                    AlarmScheduler.rescheduleAll(context)
                    onClose()
                }
            }) { Text(stringResource(R.string.save), color = Primary, fontWeight = FontWeight.SemiBold) }
        }
        if (title.isBlank()) {
            Text(
                stringResource(R.string.title_required), color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp, modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            // 块1：基础信息
            CardBlock {
                FormRow(icon = "T", label = null) {
                    OutlinedTextField(
                        value = title, onValueChange = { if (it.length <= 20) title = it },
                        placeholder = { Text(stringResource(R.string.title_hint)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                FormRow(icon = "📅", label = stringResource(R.string.target_date), value = dateText, onClick = { sheet = "date" })
                FormRow(icon = "🗂", label = stringResource(R.string.category), value = catName, onClick = { sheet = "cat" })
            }
            Spacer(Modifier.height(12.dp))
            // 块2：时间设置
            CardBlock {
                FormRow(icon = "⏰", label = stringResource(R.string.reminder_time),
                    value = reminder?.let { "%04d-%02d-%02d %02d:%02d".format(it.year, it.monthValue, it.dayOfMonth, it.hour, it.minute) },
                    onClick = { sheet = "rem" })
                FormRow(icon = "🔁", label = stringResource(R.string.repeat),
                    value = repeatLabel(repeat), onClick = { sheet = "rep" })
                FormRow(icon = "⛔", label = stringResource(R.string.end_time),
                    value = endTime?.let { "${it.year}年${it.monthValue}月${it.dayOfMonth}日" }, onClick = { sheet = "end" })
            }
            Spacer(Modifier.height(12.dp))
            // 块3：其他设置
            CardBlock {
                SwitchRow(icon = "📌", label = stringResource(R.string.pin), checked = pinned) { pinned = it }
                SwitchRow(icon = "＋", label = stringResource(R.string.include_start_day), checked = includeStart) { includeStart = it }
            }
            Spacer(Modifier.height(12.dp))
            // 块4：样式设置
            CardBlock {
                FormRow(
                    icon = "🎨", label = stringResource(R.string.style_settings),
                    value = styleLabel(bgType, icon, font),
                    onClick = { styleOpen = true }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    /* ---- 弹层 ---- */
    if (sheet == "date") {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.is_lunar), modifier = Modifier.weight(1f))
                    Switch(checked = lunar, onCheckedChange = { lunar = it })
                }
                val dpState = rememberDatePickerState(
                    initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                )
                DatePicker(state = dpState, showModeToggle = false)
                val tpState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
                TimePicker(state = tpState)
                Text(stringResource(R.string.quick_pick), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    listOf(
                        stringResource(R.string.today_word) to 0L,
                        stringResource(R.string.tomorrow) to 1L,
                        stringResource(R.string.one_week_later) to 7L,
                        stringResource(R.string.one_month_later) to 30L,
                        stringResource(R.string.half_year_later) to 182L,
                        stringResource(R.string.one_year_later) to 365L
                    ).forEach { (label, days) ->
                        AssistChip(onClick = { dpState.selectedDateMillis =
                            LocalDate.now().plusDays(days).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                        }, label = { Text(label) })
                    }
                }
                TextButton(
                    onClick = {
                        dpState.selectedDateMillis?.let {
                            date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                        }
                        time = LocalTime.of(tpState.hour, tpState.minute)
                        sheet = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.ok), color = Primary, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (sheet == "cat") {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { categoryId = null; sheet = null },
                        label = { Text(stringResource(R.string.no_category)) },
                        border = if (categoryId == null) BorderStroke(2.dp, Primary) else null
                    )
                    cats.forEach { c ->
                        AssistChip(
                            onClick = { categoryId = c.id; sheet = null },
                            label = { Text((c.icon ?: "") + " " + c.name) },
                            border = if (categoryId == c.id) BorderStroke(2.dp, Primary) else null
                        )
                    }
                }
                TextButton(onClick = { sheet = null; manageCats = true }) {
                    Text(stringResource(R.string.manage_categories))
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (sheet == "rem") {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                val rDate = remember { mutableStateOf(reminder?.toLocalDate() ?: date) }
                val rTime = remember { mutableStateOf(reminder?.toLocalTime() ?: LocalTime.of(9, 0)) }
                val dpState = rememberDatePickerState(
                    initialSelectedDateMillis = rDate.value.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                )
                DatePicker(state = dpState, showModeToggle = false)
                val tpState = rememberTimePickerState(initialHour = rTime.value.hour, initialMinute = rTime.value.minute, is24Hour = true)
                TimePicker(state = tpState)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { reminder = null; sheet = null }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.clear))
                    }
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { rDate.value = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate() }
                        rTime.value = LocalTime.of(tpState.hour, tpState.minute)
                        reminder = LocalDateTime.of(rDate.value, rTime.value)
                        sheet = null
                    }, modifier = Modifier.weight(2f)) {
                        Text(stringResource(R.string.ok), color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (sheet == "rep") {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                listOf(
                    Repeat.NONE to stringResource(R.string.repeat_none),
                    Repeat.DAILY to stringResource(R.string.repeat_daily),
                    Repeat.WEEKLY to stringResource(R.string.repeat_weekly),
                    Repeat.MONTHLY to stringResource(R.string.repeat_monthly),
                    Repeat.YEARLY to stringResource(R.string.repeat_yearly),
                    Repeat.CUSTOM to stringResource(R.string.repeat_custom)
                ).forEach { (r, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            repeat = r; sheet = if (r == Repeat.CUSTOM) "rep" else null
                        }.padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f),
                            color = if (repeat == r) Primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (repeat == r) FontWeight.SemiBold else FontWeight.Normal)
                        if (repeat == r) Text("✓", color = Primary)
                    }
                }
                if (repeat == Repeat.CUSTOM) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("每", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = customInterval.toString(),
                            onValueChange = { customInterval = it.toIntOrNull()?.coerceIn(1, 99) ?: 1 },
                            modifier = Modifier.width(80.dp), singleLine = true
                        )
                        listOf("DAY" to "天", "WEEK" to "周", "MONTH" to "月", "YEAR" to "年").forEach { (u, label) ->
                            AssistChip(
                                onClick = { customUnit = u },
                                label = { Text(label) },
                                border = if (customUnit == u) BorderStroke(2.dp, Primary) else null
                            )
                        }
                    }
                    if (customUnit == "WEEK") {
                        Row(
                            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")
                                .forEach { (wd, label) ->
                                    AssistChip(
                                        onClick = {
                                            customWeekdays = if (wd in customWeekdays)
                                                customWeekdays - wd else customWeekdays + wd
                                        },
                                        label = { Text(label) },
                                        border = if (wd in customWeekdays) BorderStroke(2.dp, Primary) else null
                                    )
                                }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { sheet = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ok), color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (sheet == "end") {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.end_time_hint), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val dpState = rememberDatePickerState(
                    initialSelectedDateMillis = (endTime ?: LocalDate.now().plusYears(1))
                        .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                )
                DatePicker(state = dpState, showModeToggle = false)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { endTime = null; sheet = null }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.clear))
                    }
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let {
                            endTime = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                        }
                        sheet = null
                    }, modifier = Modifier.weight(2f)) {
                        Text(stringResource(R.string.ok), color = Primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (manageCats) {
        ManageCategoriesDialog(catDao, cats, dao) { manageCats = false }
    }

    if (styleOpen) {
        StylePage(
            title = title.ifBlank { stringResource(R.string.title_hint) }, dateText = dateText,
            bgType = bgType, bgColor = bgColor, gradStart = gradStart, gradEnd = gradEnd,
            imagePath = imagePath, icon = icon, font = font,
            prefs = prefs,
            onBg = { t, c, gs, ge, ip -> bgType = t; bgColor = c; gradStart = gs; gradEnd = ge; imagePath = ip },
            onIcon = { icon = it }, onFont = { font = it },
            onClose = { styleOpen = false }
        )
    }
}

@Composable
private fun repeatLabel(r: Repeat): String = when (r) {
    Repeat.NONE -> stringResource(R.string.repeat_none)
    Repeat.DAILY -> stringResource(R.string.repeat_daily)
    Repeat.WEEKLY -> stringResource(R.string.repeat_weekly)
    Repeat.MONTHLY -> stringResource(R.string.repeat_monthly)
    Repeat.YEARLY -> stringResource(R.string.repeat_yearly)
    Repeat.CUSTOM -> stringResource(R.string.repeat_custom)
}

@Composable
private fun styleLabel(bgType: BgType, icon: String?, font: String?): String {
    val parts = mutableListOf(
        when (bgType) {
            BgType.COLOR -> stringResource(R.string.solid_color)
            BgType.GRADIENT -> stringResource(R.string.gradient)
            BgType.IMAGE -> stringResource(R.string.image_bg)
            BgType.NONE -> stringResource(R.string.style_default)
        }
    )
    if (icon != null) parts.add(icon)
    return parts.joinToString(" · ")
}

private fun localDateOf(e: EventEntity): LocalDate =
    Instant.ofEpochMilli(e.targetDate).atZone(ZoneId.systemDefault()).toLocalDate()

private fun localTimeOf(e: EventEntity): LocalTime =
    Instant.ofEpochMilli(e.targetDate).atZone(ZoneId.systemDefault()).toLocalTime()

@Composable
private fun CardBlock(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) { content() }
}

@Composable
private fun FormRow(icon: String, label: String?, value: String? = null, onClick: (() -> Unit)? = null, content: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.width(30.dp))
        if (label != null) {
            Text(label, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
        }
        when {
            content != null -> Box(Modifier.weight(1f)) { content() }
            label != null -> Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value ?: stringResource(R.string.none),
                    fontSize = 15.sp,
                    color = if (value != null) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun SwitchRow(icon: String, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.width(30.dp))
        Text(label, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/* ---------- 样式设置页（纯色/渐变/图片 + 图标 + 字体 + 实时预览） ---------- */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StylePage(
    title: String, dateText: String,
    bgType: BgType, bgColor: Int?, gradStart: Int?, gradEnd: Int?, imagePath: String?,
    icon: String?, font: String?,
    prefs: PrefsRepository,
    onBg: (BgType, Int?, Int?, Int?, String?) -> Unit,
    onIcon: (String?) -> Unit,
    onFont: (String?) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by prefs.settings.collectAsState(initial = com.airlur.timely.data.UiSettings())
    var tab by remember { mutableIntStateOf(0) }
    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val copied = copyUriToFile(it.toString(), imagePath, context)
            onBg(BgType.IMAGE, null, null, null, copied)
        }
    }
    val previewEvent = EventEntity(
        id = 0, title = title, targetDate = 0, createdAt = 0,
        bgType = bgType.name, bgColor = bgColor,
        gradientStart = gradStart, gradientEnd = gradEnd, imagePath = imagePath,
        iconName = icon, fontFamily = font
    )

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", fontSize = 26.sp, color = Primary,
                    modifier = Modifier.clickable { onClose() }.padding(horizontal = 18.dp, vertical = 6.dp))
                Text(stringResource(R.string.style_settings), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(60.dp))
            }
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                listOf(stringResource(R.string.solid_color), stringResource(R.string.gradient), stringResource(R.string.image_bg))
                    .forEachIndexed { i, label -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) }) }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                // 实时预览
                val previewBg = com.airlur.timely.ui.eventBg(
                    previewEvent, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .eventBackground(previewEvent)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = previewBg.ink)
                        Spacer(Modifier.height(2.dp))
                        Text(dateText, fontSize = 14.sp, color = previewBg.ink)
                    }
                    if (icon != null) Text(icon, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterEnd))
                }
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    0 -> {
                        ColorGrid(
                            colors = PresetColors + settings.customSolids.map { Color(it) },
                            selected = if (bgType == BgType.COLOR) bgColor else null
                        ) { c ->
                            onBg(BgType.COLOR, c.toArgb(), null, null, null)
                        }
                        CustomColorAdd { c ->
                            onBg(BgType.COLOR, c.toArgb(), null, null, null)
                            scope.launch { prefs.addCustomSolid(c.toArgb()) }
                        }
                    }
                    1 -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            (PresetGradients + settings.customGradients.map { Color(it.first) to Color(it.second) })
                                .forEach { (s, e) ->
                                    val sel = bgType == BgType.GRADIENT && gradStart == s.toArgb() && gradEnd == e.toArgb()
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(s, e)))
                                            .clickable { onBg(BgType.GRADIENT, null, s.toArgb(), e.toArgb(), null) }
                                            .then(if (sel) Modifier.border2() else Modifier)
                                    )
                                }
                        }
                        Spacer(Modifier.height(10.dp))
                        CustomGradientAdd { s, e ->
                            onBg(BgType.GRADIENT, null, s.toArgb(), e.toArgb(), null)
                            scope.launch { prefs.addCustomGradient(s.toArgb(), e.toArgb()) }
                        }
                    }
                    2 -> {
                        Text(stringResource(R.string.pick_from_gallery), color = Primary, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    imageLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                                .padding(vertical = 8.dp))
                        if (imagePath != null && bgType == BgType.IMAGE) {
                            TextButton(onClick = { onBg(BgType.NONE, null, null, null, null) }) {
                                Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.event_icon), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    EMOJI_ICONS.forEach { e ->
                        AssistChip(
                            onClick = { onIcon(e) },
                            label = { Text(e ?: stringResource(R.string.none)) },
                            border = if (icon == e || (icon == null && e == null)) BorderStroke(2.dp, Primary) else null
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.font), fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf(
                        null to stringResource(R.string.font_default),
                        "serif" to stringResource(R.string.font_serif),
                        "mono" to stringResource(R.string.font_mono)
                    ).forEach { (f, label) ->
                        AssistChip(
                            onClick = { onFont(f) }, label = { Text(label) },
                            border = if (font == f) BorderStroke(2.dp, Primary) else null
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun Modifier.border2(): Modifier =
    this.then(Modifier.border(3.dp, Primary, RoundedCornerShape(8.dp)))

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorGrid(colors: List<Color>, selected: Int?, onPick: (Color) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        colors.forEach { c ->
            val sel = selected == c.toArgb()
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c)
                    .clickable { onPick(c) }
                    .then(if (sel) Modifier.border2() else Modifier)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomColorAdd(onAdd: (Color) -> Unit) {
    var hue by remember { mutableStateOf(200f) }
    var bright by remember { mutableStateOf(0.85f) }
    val color = Color.hsv(hue, 0.72f, bright)
    Column(Modifier.padding(top = 12.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(color))
        Text("色相", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
        Text("明度", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = bright, onValueChange = { bright = it }, valueRange = 0.4f..1f)
        TextButton(onClick = { onAdd(color) }) { Text("+ " + stringResource(R.string.add_custom_color)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomGradientAdd(onAdd: (Color, Color) -> Unit) {
    var h1 by remember { mutableStateOf(340f) }
    var h2 by remember { mutableStateOf(280f) }
    val c1 = Color.hsv(h1, 0.65f, 0.9f)
    val c2 = Color.hsv(h2, 0.65f, 0.75f)
    Column(Modifier.padding(top = 12.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c1, c2)))
        )
        Text("起色相", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = h1, onValueChange = { h1 = it }, valueRange = 0f..360f)
        Text("止色相", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value = h2, onValueChange = { h2 = it }, valueRange = 0f..360f)
        TextButton(onClick = { onAdd(c1, c2) }) { Text("+ " + stringResource(R.string.add_custom_color)) }
    }
}

private fun copyUriToFile(uriString: String, oldPath: String?, context: android.content.Context): String? {
    return try {
        val stream = context.contentResolver.openInputStream(android.net.Uri.parse(uriString)) ?: return null
        val bmp = android.graphics.BitmapFactory.decodeStream(stream) ?: return null
        stream.close()
        val dir = java.io.File(context.filesDir, "images").apply { mkdirs() }
        val f = java.io.File(dir, "bg_" + System.currentTimeMillis() + ".jpg")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
        oldPath?.let { java.io.File(it).delete() }
        f.absolutePath
    } catch (e: Exception) {
        null
    }
}
