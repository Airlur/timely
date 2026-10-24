package com.airlur.timely.ui.date

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlur.timely.R
import com.airlur.timely.data.EventDao
import com.airlur.timely.data.Repeat
import com.airlur.timely.data.TimeUnit
import com.airlur.timely.domain.DateMath
import com.airlur.timely.ui.eventBg
import com.airlur.timely.ui.localDate
import com.airlur.timely.ui.prefixText
import com.airlur.timely.ui.theme.fontFamily
import kotlinx.coroutines.launch

/** 全屏沉浸详情：背景铺满，上中下三段，底部四个图标操作（对照旧版 event_detail_page） */
@Composable
fun DetailScreen(
    eventId: Long,
    dao: EventDao,
    snackbar: SnackbarHostState,
    onClose: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val events by dao.observeAll().collectAsState(initial = emptyList())
    val event = events.firstOrNull { it.id == eventId }
    if (event == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val bg = eventBg(event, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
    val today = localDate(System.currentTimeMillis())
    val display = DateMath.cardDisplay(localDate(event.targetDate), today, event.includeStartDay)
    val numbers = DateMath.detailNumbers(localDate(event.targetDate), today, event.includeStartDay)
    var unitIdx by remember(event.id) { mutableIntStateOf(0) }
    val num = numbers[unitIdx % numbers.size]

    LaunchedEffect(event.id) { /* placeholder for future animations */ }

    val bitmap = com.airlur.timely.ui.rememberImageBitmap(event.imagePath)

    Box(Modifier.fillMaxSize().background(bg.brush ?: androidx.compose.ui.graphics.SolidColor(bg.solid ?: Color.White))) {
        if (com.airlur.timely.data.BgType.valueOf(event.bgType) == com.airlur.timely.data.BgType.IMAGE && bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
        Column(Modifier.fillMaxSize()) {
            // 顶栏：返回 + 置顶
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹", color = bg.ink, fontSize = 26.sp,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (event.isPinned) "📌" else "📍",
                    fontSize = 20.sp,
                    color = bg.ink,
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                if (event.isPinned) dao.upsert(event.copy(isPinned = false, pinnedAt = null))
                                else dao.upsert(event.copy(isPinned = true, pinnedAt = System.currentTimeMillis()))
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // 中部内容：图标 + 标题前缀 → 大数字（点按切换单位）→ 日期
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (event.iconName != null) {
                    Text(event.iconName!!, fontSize = 30.sp, color = bg.ink)
                    Spacer(Modifier.height(10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = bg.ink,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontFamily = fontFamily(event.fontFamily)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        prefixText(
                            display.prefix,
                            stringResource(R.string.prefix_future), stringResource(R.string.prefix_past),
                            stringResource(R.string.prefix_today), stringResource(R.string.prefix_ordinal)
                        ),
                        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = bg.ink
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = when {
                        display.prefix == com.airlur.timely.domain.Prefix.TODAY && !event.includeStartDay ->
                            stringResource(R.string.today_word)
                        else -> num.value + " " + numUnitLabel(num.unitKey)
                    },
                    fontSize = 64.sp, fontWeight = FontWeight.Bold, color = bg.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { unitIdx++ }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    (if (DateMath.dayDiff(localDate(event.targetDate), today) >= 0)
                        stringResource(R.string.target_date) else stringResource(R.string.start_date)) +
                        "：" + dateTextOf(event) + " " + weekdayShort(localDate(event.targetDate)),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = bg.ink,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // 底部操作：分享 / 小组件 / 编辑 / 删除
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DetailAction("↗", stringResource(R.string.act_share), bg.ink) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.share_coming)) }
                }
                DetailAction("▦", stringResource(R.string.set_as_widget), bg.ink) {
                    scope.launch { snackbar.showSnackbar(ctx.getString(R.string.widget_coming)) }
                }
                DetailAction("✎", stringResource(R.string.act_edit), bg.ink) { onEdit(event.id) }
                DetailAction("🗑", stringResource(R.string.act_delete), bg.ink) {
                    scope.launch {
                        dao.delete(event.id)
                        onClose()
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailAction(icon: String, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(icon, fontSize = 24.sp, color = tint)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = tint)
    }
}

@Composable
private fun numUnitLabel(unitKey: String): String = when {
    unitKey == "day" -> stringResource(R.string.unit_days)
    unitKey == "year" -> stringResource(R.string.unit_year)
    unitKey.startsWith("year+") -> stringResource(R.string.unit_year) + " " +
        unitKey.substringAfter('+') + " " + stringResource(R.string.unit_month)
    unitKey == "month" -> stringResource(R.string.unit_months)
    unitKey.startsWith("month+") -> stringResource(R.string.unit_months) + " " +
        unitKey.substringAfter('+') + " " + stringResource(R.string.unit_day)
    unitKey == "week" -> stringResource(R.string.unit_week)
    unitKey.startsWith("week+") -> stringResource(R.string.unit_week) + " " +
        unitKey.substringAfter('+') + " " + stringResource(R.string.unit_day)
    else -> ""
}

fun weekdayShort(date: java.time.LocalDate): String =
    "周" + listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]
