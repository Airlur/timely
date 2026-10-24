package com.airlur.timely.ui.date

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlur.timely.R
import com.airlur.timely.data.CategoryDao
import com.airlur.timely.data.CategoryEntity
import com.airlur.timely.data.EventDao
import com.airlur.timely.data.EventEntity
import com.airlur.timely.data.PrefsRepository
import com.airlur.timely.data.PinMode
import com.airlur.timely.data.SortMode
import com.airlur.timely.data.SwipeAction
import com.airlur.timely.data.UiSettings
import com.airlur.timely.data.ViewMode
import com.airlur.timely.domain.CardDisplay
import com.airlur.timely.domain.DateMath
import com.airlur.timely.domain.Prefix
import com.airlur.timely.ui.SwipeCard
import com.airlur.timely.ui.Watermark
import com.airlur.timely.ui.eventBackground
import com.airlur.timely.ui.eventBg
import com.airlur.timely.ui.localDate
import com.airlur.timely.ui.prefixText
import com.airlur.timely.ui.slotsFor
import com.airlur.timely.ui.theme.Primary
import com.airlur.timely.ui.theme.fontFamily
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DateScreen(
    dao: EventDao,
    catDao: CategoryDao,
    prefs: PrefsRepository,
    snackbar: SnackbarHostState,
    onOpenDetail: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by prefs.settings.collectAsState(initial = UiSettings())
    val events by dao.observeAll().collectAsState(initial = emptyList())
    val cats by catDao.observeAll().collectAsState(initial = emptyList())

    var tab by remember { mutableIntStateOf(0) }
    var selCat by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var sortSheet by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var manageCats by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val labels: (SwipeAction) -> String = { a ->
        when (a) {
            SwipeAction.PIN -> ctx.getString(R.string.act_pin)
            SwipeAction.DETAIL -> ctx.getString(R.string.act_detail)
            SwipeAction.TIMELINE -> ctx.getString(R.string.act_timeline)
            SwipeAction.EDIT -> ctx.getString(R.string.act_edit)
            SwipeAction.DELETE -> ctx.getString(R.string.act_delete)
            SwipeAction.SHARE -> ctx.getString(R.string.act_share)
        }
    }

    fun act(a: SwipeAction, event: EventEntity) {
        when (a) {
            SwipeAction.PIN -> scope.launch {
                if (event.isPinned) {
                    dao.upsert(event.copy(isPinned = false, pinnedAt = null))
                } else {
                    dao.clearPinnedExcept(event.id)
                    dao.upsert(event.copy(isPinned = true, pinnedAt = System.currentTimeMillis()))
                }
            }
            SwipeAction.DELETE -> scope.launch {
                dao.delete(event.id)
                val r = snackbar.showSnackbar(
                    message = ctx.getString(R.string.deleted_with_undo, event.title),
                    actionLabel = ctx.getString(R.string.undo),
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
                if (r == SnackbarResult.ActionPerformed) dao.upsert(event)
            }
            SwipeAction.EDIT -> onEdit(event.id)
            SwipeAction.DETAIL -> onOpenDetail(event.id)
            SwipeAction.TIMELINE, SwipeAction.SHARE -> scope.launch {
                snackbar.showSnackbar(ctx.getString(R.string.coming_soon))
            }
        }
    }

    val filtered = events.filter { e ->
        val dd = DateMath.dayDiff(localDate(e.targetDate), today)
        (tab == 0 || (tab == 1 && dd >= 0) || (tab == 2 && dd < 0)) &&
            (selCat == null || e.categoryId == cats.firstOrNull { it.name == selCat }?.id)
    }
    val sorted = when (settings.sort) {
        SortMode.CREATED -> filtered.sortedBy { it.createdAt }
        SortMode.DATE -> filtered.sortedBy { kotlin.math.abs(DateMath.dayDiff(localDate(it.targetDate), today)) }
        SortMode.MANUAL -> filtered.sortedBy { it.sortOrder }
    }
    val pinned = filtered.filter { it.isPinned }
    val listItems = sorted.filter { !it.isPinned }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEdit(-1) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) { Text("+", fontSize = 26.sp) }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f).padding(start = 4.dp)) {
                    listOf(
                        stringResource(R.string.filter_all),
                        stringResource(R.string.filter_countdown),
                        stringResource(R.string.filter_countup),
                        stringResource(R.string.filter_calendar)
                    ).forEachIndexed { i, label ->
                        val on = tab == i
                        Column(
                            Modifier
                                .clickable { tab = i }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Text(
                                label, fontSize = 16.sp,
                                color = if (on) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .size(width = 24.dp, height = 3.dp)
                                    .background(if (on) Primary else Color.Transparent, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text("⋯", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort)) },
                            onClick = { menuOpen = false; sortSheet = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.switch_view)) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    prefs.setView(
                                        if (settings.view == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pin_mode)) },
                            onClick = { menuOpen = false; pinDialog = true }
                        )
                    }
                }
            }

            when (tab) {
                3 -> CalendarPane(events, today, onOpenDetail)
                else -> Column(Modifier.fillMaxSize()) {
                    if (pinned.isNotEmpty()) {
                        if (settings.pinMode == PinMode.CAROUSEL && pinned.size > 1) {
                            val pagerState = rememberPagerState { pinned.size }
                            LaunchedEffect(pagerState, pinned.size) {
                                while (true) {
                                    delay(3000)
                                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % pinned.size)
                                }
                            }
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth().height(126.dp)
                            ) { page ->
                                Column {
                                    Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                        EventCardView(
                                            event = pinned.getOrElse(page) { pinned.first() }, settings = settings, big = true, labels = labels,
                                            onOpenDetail = onOpenDetail, onAction = ::act
                                        )
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        pinned.indices.forEach { i ->
                                            Box(
                                                Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .size(if (pagerState.currentPage == i) 12.dp else 5.dp, 5.dp)
                                                    .background(
                                                        if (pagerState.currentPage == i) Primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                        RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.padding(16.dp, 6.dp)) {
                                EventCardView(
                                    event = pinned.first(), settings = settings, big = true, labels = labels,
                                    onOpenDetail = onOpenDetail, onAction = ::act
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    if (listItems.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("◷", fontSize = 40.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    when {
                                        selCat != null -> stringResource(
                                            R.string.empty_in_category,
                                            if (tab == 1) stringResource(R.string.filter_countdown)
                                            else if (tab == 2) stringResource(R.string.filter_countup)
                                            else stringResource(R.string.filter_all),
                                            selCat!!
                                        )
                                        tab == 1 -> stringResource(R.string.empty_countdown)
                                        tab == 2 -> stringResource(R.string.empty_countup)
                                        else -> stringResource(R.string.empty_first)
                                    },
                                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (settings.view == ViewMode.GRID) {
                        LazyColumn(
                            Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(listItems.chunked(2)) { _, row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { e ->
                                        Box(Modifier.weight(1f)) {
                                            EventCardView(e, settings, false, labels, onOpenDetail, ::act, grid = true)
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    } else {
                        DragReorderList(listItems, settings, labels, onOpenDetail, ::act) { ids ->
                            scope.launch { ids.forEachIndexed { i, id -> dao.updateOrder(id, i) } }
                        }
                    }
                }
            }
        }

        if (sortSheet) {
            ModalBottomSheet(onDismissRequest = { sortSheet = false }) {
                Column(Modifier.padding(bottom = 24.dp)) {
                    Text(
                        stringResource(R.string.sort), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
                    )
                    listOf(
                        SortMode.CREATED to stringResource(R.string.sort_created),
                        SortMode.DATE to stringResource(R.string.sort_date),
                        SortMode.MANUAL to stringResource(R.string.sort_manual)
                    ).forEach { (mode, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { scope.launch { prefs.setSort(mode) } }
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label, fontSize = 14.5.sp,
                                color = if (settings.sort == mode) Primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (settings.sort == mode) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (settings.sort == mode) Text("✓", color = Primary)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Text(
                        stringResource(R.string.filter_by_category),
                        fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                    FlowRow(
                        Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { selCat = null },
                            label = { Text(stringResource(R.string.filter_all)) },
                            border = if (selCat == null) BorderStroke(2.dp, Primary) else null
                        )
                        cats.forEach { c ->
                            AssistChip(
                                onClick = { selCat = c.name },
                                label = { Text((c.icon ?: "") + " " + c.name) },
                                border = if (selCat == c.name) BorderStroke(2.dp, Primary) else null
                            )
                        }
                    }
                    TextButton(
                        onClick = { sortSheet = false; manageCats = true },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    ) { Text(stringResource(R.string.manage_categories)) }
                }
            }
        }

        if (pinDialog) {
            AlertDialog(
                onDismissRequest = { pinDialog = false },
                confirmButton = {},
                title = { Text(stringResource(R.string.pin_mode), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        listOf(
                            PinMode.SINGLE to stringResource(R.string.pin_mode_single),
                            PinMode.CAROUSEL to stringResource(R.string.pin_mode_carousel)
                        ).forEach { (m, label) ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    scope.launch { prefs.setPinMode(m) }
                                    pinDialog = false
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, modifier = Modifier.weight(1f))
                                if (settings.pinMode == m) Text("✓", color = Primary)
                            }
                        }
                    }
                }
            )
        }

        if (manageCats) {
            ManageCategoriesDialog(catDao, cats, dao) { manageCats = false }
        }
    }
}

/* ---------- 事件卡片：布局与旧版 EventCard 一致（一行式，圆角 8dp） ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCardView(
    event: EventEntity,
    settings: UiSettings,
    big: Boolean,
    labels: (SwipeAction) -> String,
    onOpenDetail: (Long) -> Unit,
    onAction: (SwipeAction, EventEntity) -> Unit,
    grid: Boolean = false
) {
    val bg = eventBg(event, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
    val display = DateMath.cardDisplay(localDate(event.targetDate), LocalDate.now(), event.includeStartDay)
    val dateText = dateTextOf(event)

    val boxed: @Composable () -> Unit = {
        Box(Modifier.clip(RoundedCornerShape(8.dp))) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { onOpenDetail(event.id) })
                    .eventBackground(event)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (event.iconName != null) {
                    Text(event.iconName!!, fontSize = if (big) 22.sp else 18.sp, modifier = Modifier.padding(end = 8.dp))
                }
                Column(Modifier.weight(3f)) {
                    Text(
                        event.title, fontSize = if (big) 24.sp else 20.sp, fontWeight = FontWeight.Bold,
                        color = bg.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontFamily = fontFamily(event.fontFamily)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateText, fontSize = if (big) 16.sp else 14.sp, color = bg.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(16.dp))
                if (grid) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(display.number.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = bg.ink)
                        Text(
                            cardPrefix(display) + " " + stringResource(R.string.unit_days),
                            fontSize = 12.sp, color = bg.ink
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!(display.prefix == Prefix.TODAY && !event.includeStartDay)) {
                            Text(cardPrefix(display), fontSize = (if (big) 24 else 20).sp, fontWeight = FontWeight.Bold, color = bg.ink)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (display.prefix == Prefix.TODAY && !event.includeStartDay) stringResource(R.string.today_word)
                            else display.number.toString(),
                            fontSize = (if (big) 28 else 24).sp, fontWeight = FontWeight.Bold, color = bg.ink
                        )
                        if (!(display.prefix == Prefix.TODAY && !event.includeStartDay)) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (display.prefix == Prefix.ORDINAL) stringResource(R.string.unit_day)
                                else stringResource(R.string.unit_days),
                                fontSize = (if (big) 24 else 20).sp, fontWeight = FontWeight.Bold, color = bg.ink
                            )
                        }
                    }
                }
            }
            if (settings.wmOn) {
                Watermark(settings.wmPos, settings.wmOp, Modifier.align(Alignment.BottomEnd), tint = bg.ink)
            }
        }
    }

    if (grid || big) {
        boxed()
    } else {
        SwipeCard(
            mode = settings.swipeMode,
            slots = slotsFor(settings, labels),
            onAction = { a -> onAction(a, event) },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) { boxed() }
    }
}

@Composable
private fun cardPrefix(display: CardDisplay): String = prefixText(
    display.prefix,
    stringResource(R.string.prefix_future), stringResource(R.string.prefix_past),
    stringResource(R.string.prefix_today), stringResource(R.string.prefix_ordinal)
)

fun dateTextOf(event: EventEntity): String =
    if (event.isLunar) lunarTextOf(event)
    else localDate(event.targetDate).let { "${it.year}年${it.monthValue}月${it.dayOfMonth}日" }

fun lunarTextOf(event: EventEntity): String = try {
    val d = java.util.Date.from(
        localDate(event.targetDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
    )
    val lunar = com.nlf.calendar.Lunar.fromDate(d)
    "${lunar.yearInGanZhi}年${lunar.monthInChinese}月${lunar.dayInChinese}"
} catch (e: Exception) {
    "农历"
}

/* ---------- 长按拖拽排序（长按提起 → 越过卡位交换 → 松手落库） ---------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DragReorderList(
    items: List<EventEntity>,
    settings: UiSettings,
    labels: (SwipeAction) -> String,
    onOpenDetail: (Long) -> Unit,
    onAction: (SwipeAction, EventEntity) -> Unit,
    onReorder: (List<Long>) -> Unit
) {
    val order = remember(items.map { it.id }) { mutableStateOf(items.map { it.id }) }
    val byId = remember(items) { items.associateBy { it.id } }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val itemH = with(LocalDensity.current) { 88.dp.toPx() }

    val ordered = order.value.mapNotNull { byId[it] }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(ordered, key = { _, e -> e.id }) { _, event ->
            val isDragging = draggingId == event.id
            Box(
                Modifier
                    .animateItemPlacement()
                    .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                    .pointerInput(event.id, ordered.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingId = event.id; dragOffset = 0f },
                            onDrag = { _, dy ->
                                dragOffset += dy
                                while (kotlin.math.abs(dragOffset) > itemH) {
                                    val dir = if (dragOffset > 0) 1 else -1
                                    val from = order.value.indexOf(event.id)
                                    val to = from + dir
                                    if (to in order.value.indices) {
                                        val cur = order.value.toMutableList()
                                        cur[from] = cur[to]; cur[to] = event.id
                                        order.value = cur
                                        dragOffset -= dir * itemH
                                    } else {
                                        dragOffset = 0f
                                        break
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggingId == event.id) {
                                    onReorder(order.value)
                                    draggingId = null; dragOffset = 0f
                                }
                            },
                            onDragCancel = { draggingId = null; dragOffset = 0f }
                        )
                    }
            ) {
                SwipeCard(
                    mode = settings.swipeMode,
                    slots = slotsFor(settings, labels),
                    onAction = { a -> onAction(a, event) }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = { onOpenDetail(event.id) })
                            .eventBackground(event)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CardTexts(event, settings)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(88.dp)) }
    }
}

@Composable
fun CardTexts(event: EventEntity, settings: UiSettings) {
    val bg = eventBg(event, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
    val display = DateMath.cardDisplay(localDate(event.targetDate), LocalDate.now(), event.includeStartDay)
    val dateText = dateTextOf(event)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (event.iconName != null) {
            Text(event.iconName!!, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Column(Modifier.weight(3f)) {
            Text(
                event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bg.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = fontFamily(event.fontFamily)
            )
            Spacer(Modifier.height(2.dp))
            Text(dateText, fontSize = 14.sp, color = bg.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!(display.prefix == Prefix.TODAY && !event.includeStartDay)) {
                Text(cardPrefix(display), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bg.ink)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (display.prefix == Prefix.TODAY && !event.includeStartDay) stringResource(R.string.today_word)
                else display.number.toString(),
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = bg.ink
            )
            if (!(display.prefix == Prefix.TODAY && !event.includeStartDay)) {
                Spacer(Modifier.width(8.dp))
                Text(
                    if (display.prefix == Prefix.ORDINAL) stringResource(R.string.unit_day)
                    else stringResource(R.string.unit_days),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = bg.ink
                )
            }
        }
    }
}

/* ---------- 日历（月视图 + 当日事件） ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarPane(
    events: List<EventEntity>,
    today: LocalDate,
    onOpenDetail: (Long) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val byDate = events.groupBy { localDate(it.targetDate) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", Modifier.clickable { month = month.minusMonths(1) }.padding(8.dp), fontSize = 18.sp)
            Text(
                "${month.year}年${month.monthValue}月",
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            Text("›", Modifier.clickable { month = month.plusMonths(1) }.padding(8.dp), fontSize = 18.sp)
        }
        Row(Modifier.padding(horizontal = 12.dp)) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(
                    it, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center
                )
            }
        }
        val first = month.atDay(1)
        val offset = (first.dayOfWeek.value + 6) % 7 // 周一开头
        val cells = mutableListOf<LocalDate?>()
        repeat(offset) { cells.add(null) }
        for (d in 1..month.lengthOfMonth()) cells.add(month.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        cells.chunked(7).forEach { week ->
            Row(Modifier.padding(horizontal = 12.dp)) {
                week.forEach { day ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected == day) Primary else Color.Transparent)
                            .clickable(enabled = day != null) { selected = day },
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    day.dayOfMonth.toString(), fontSize = 14.sp,
                                    color = when {
                                        selected == day -> Color.White
                                        day == today -> Primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                                )
                                (byDate[day] ?: emptyList()).take(3).forEach { e ->
                                    Box(
                                        Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .background(
                                                if (selected == day) Color.White
                                                else Color(e.bgColor ?: 0xFF2196F3.toInt()),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        selected?.let { day ->
            val dayEvents = byDate[day].orEmpty()
            Text(
                "$day · " + stringResource(R.string.events_on_day, dayEvents.size),
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            if (dayEvents.isEmpty()) {
                Text(
                    stringResource(R.string.no_events_that_day),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            dayEvents.forEach { e ->
                val bg = eventBg(e, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpenDetail(e.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).background(bg.solid ?: Primary, RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(10.dp))
                    Text(e.title, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    val disp = DateMath.cardDisplay(localDate(e.targetDate), today, e.includeStartDay)
                    Text(
                        cardPrefix(disp) + disp.number.toString() + stringResource(R.string.unit_days),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(88.dp))
    }
}

/* ---------- 分类管理 ---------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageCategoriesDialog(
    catDao: CategoryDao,
    cats: List<CategoryEntity>,
    eventDao: EventDao,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { adding = true }) { Text("+ " + stringResource(R.string.manage_categories)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
        title = { Text(stringResource(R.string.manage_categories), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (cats.isEmpty()) Text(stringResource(R.string.none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                cats.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text((c.icon ?: "") + " " + c.name, modifier = Modifier.weight(1f))
                        Text(
                            "✕", color = MaterialTheme.colorScheme.error,
                            Modifier.clickable {
                                scope.launch {
                                    if (eventDao.getAll().none { it.categoryId == c.id }) catDao.delete(c.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    )
    if (adding) {
        var name by remember { mutableStateOf("") }
        val emojiSet = listOf("📚", "💼", "🏠", "💗", "🎂", "✈️", "🎓", "⚽", "🎬", "🎵", "🐱", "💰", "🏥", "📅", "⭐", "🏃")
        var emoji by remember { mutableStateOf(emojiSet.first()) }
        AlertDialog(
            onDismissRequest = { adding = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (name.isNotBlank() && cats.size < 8 && cats.none { it.name == name.trim() }) {
                            catDao.upsert(
                                CategoryEntity(name = name.trim(), icon = emoji, color = 0xFF2196F3.toInt())
                            )
                            adding = false
                        }
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.clear)) } },
            title = { Text(stringResource(R.string.manage_categories)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        placeholder = { Text(stringResource(R.string.title_hint)) }, singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        emojiSet.forEach { e ->
                            Text(
                                e, fontSize = 20.sp,
                                color = if (emoji == e) Primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable { emoji = e }.padding(4.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}
