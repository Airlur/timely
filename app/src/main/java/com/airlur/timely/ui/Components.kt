package com.airlur.timely.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlur.timely.data.BgType
import com.airlur.timely.data.EventEntity
import com.airlur.timely.data.SwipeAction
import com.airlur.timely.data.SwipeMode
import com.airlur.timely.data.UiSettings
import com.airlur.timely.data.WmOpacity
import com.airlur.timely.data.WmPos
import com.airlur.timely.domain.CardDisplay
import com.airlur.timely.domain.Prefix
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/* ---------- 事件背景与文字适配色（对照旧版 BackgroundUtils） ---------- */

data class EventBg(val brush: Brush?, val solid: Color?, val ink: Color)

fun eventBg(event: EventEntity, surface: Color, onSurface: Color): EventBg =
    when (BgType.valueOf(event.bgType)) {
        BgType.COLOR -> {
            val c = Color(event.bgColor ?: 0xFFFFFFFF.toInt())
            EventBg(null, c, inkFor(c))
        }
        BgType.GRADIENT -> {
            val s = Color(event.gradientStart ?: 0xFF2196F3.toInt())
            val e = Color(event.gradientEnd ?: event.gradientStart ?: 0xFF2196F3.toInt())
            EventBg(Brush.linearGradient(listOf(s, e)), null, inkFor(s))
        }
        else -> EventBg(null, surface, onSurface)
    }

private fun inkFor(c: Color): Color =
    if (luminanceLike(c) > 0.62f) Color(0xFF1F1F1F) else Color.White

private fun luminanceLike(c: Color): Float =
    (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue) * c.alpha

fun localDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** 图片背景解码（已把相册图复制到应用私有目录） */
@Composable
fun rememberImageBitmap(path: String?): android.graphics.Bitmap? =
    androidx.compose.runtime.remember(path) {
        try {
            if (path.isNullOrBlank()) null
            else android.graphics.BitmapFactory.decodeFile(path)
        } catch (e: Exception) { null }
    }

/**
 * 事件卡片背景 Modifier：纯色 / 渐变 / 图片（NONE 用主题表面色），
 * 文字适配色与旧版 BackgroundUtils 一致；图片按 cover 裁剪绘制。
 */
@Composable
fun Modifier.eventBackground(event: EventEntity): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val bg = eventBg(event, surface, MaterialTheme.colorScheme.onSurface)
    val bitmap = rememberImageBitmap(event.imagePath)
    val isImage = BgType.valueOf(event.bgType) == BgType.IMAGE && bitmap != null
    return this
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        .background(bg.brush ?: SolidColor(bg.solid ?: surface))
        .drawBehind {
            if (isImage && bitmap != null) {
                val img = bitmap.asImageBitmap()
                val scale = maxOf(size.width / img.width, size.height / img.height)
                val w = img.width * scale
                val h = img.height * scale
                translate((size.width - w) / 2f, (size.height - h) / 2f) {
                    drawImage(img, dstSize = androidx.compose.ui.unit.IntSize(w.roundToInt(), h.roundToInt()))
                }
            }
        }
}

fun prefixText(prefix: Prefix, future: String, past: String, today: String, ordinal: String): String =
    when (prefix) {
        Prefix.FUTURE -> future
        Prefix.PAST -> past
        Prefix.TODAY -> today
        Prefix.ORDINAL -> ordinal
    }

/* ---------- 滑动动作槽位 ---------- */

data class SwipeSlot(val action: SwipeAction, val color: Color, val label: String)

/** slots[0] = 左滑露出（卡片右侧），slots[1] = 右滑露出（卡片左侧），近→远 */
fun slotsFor(settings: UiSettings, labels: (SwipeAction) -> String): List<List<SwipeSlot>> =
    listOf(
        settings.slotsRight.map { it.toSlot(labels) },
        settings.slotsLeft.map { it.toSlot(labels) }
    )

fun SwipeAction.toSlot(labels: (SwipeAction) -> String): SwipeSlot = when (this) {
    SwipeAction.PIN -> SwipeSlot(this, com.airlur.timely.ui.theme.ActionPin, labels(this))
    SwipeAction.DETAIL -> SwipeSlot(this, com.airlur.timely.ui.theme.ActionDetail, labels(this))
    SwipeAction.TIMELINE -> SwipeSlot(this, com.airlur.timely.ui.theme.ActionTimeline, labels(this))
    SwipeAction.EDIT -> SwipeSlot(this, com.airlur.timely.ui.theme.ActionEdit, labels(this))
    SwipeAction.DELETE -> SwipeSlot(this, com.airlur.timely.ui.theme.ActionDelete, labels(this))
    SwipeAction.SHARE -> SwipeSlot(this, com.airlur.timely.ui.theme.ActionShare, labels(this))
}

/** 松手触发模式的分段距离（dp），近→远 */
val TRIGGER_ZONES_DP = listOf(52, 112, 176, 246)

/**
 * 双模式滑动卡片容器（对应旧版 flutter_slidable + 新增的松手触发）：
 * REVEAL：滑开露出整排按钮，点按钮执行；
 * TRIGGER：按距离分段渐显彩色层、变色锁定，松手即执行。
 */
@Composable
fun SwipeCard(
    mode: SwipeMode,
    slots: List<List<SwipeSlot>>,
    onAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val openWidthPx = with(density) { 252.dp.toPx() }
    val zonePx = TRIGGER_ZONES_DP.map { with(density) { it.dp.toPx() } }
    val offsetX = remember { mutableStateOf(0f) }
    val armed = remember { mutableStateOf(-1) } // 0..2 = 命中的段
    val armedSide = remember { mutableStateOf(0) } // 0 = 左滑（右侧槽） 1 = 右滑（左侧槽）
    val open = remember { mutableStateOf(false) }

    fun reset() {
        offsetX.value = 0f; armed.value = -1; open.value = false
    }

    Box(modifier.clip(RoundedCornerShape(8.dp))) {
        // 底层动作（REVEAL 模式常驻，卡片移开后露出；左滑右侧 + 右滑左侧）
        if (mode == SwipeMode.REVEAL) {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(252.dp)
                    .fillMaxHeight()
            ) {
                slots[1].forEach { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(s.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.label, color = Color.White, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(252.dp)
                    .fillMaxHeight()
            ) {
                slots[0].forEach { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(s.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.label, color = Color.White, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        // 卡片本体
        Box(
            Modifier
                .matchParentSize()
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(mode, slots) {
                    detectDragGestures(
                        onDragEnd = {
                            if (mode == SwipeMode.TRIGGER) {
                                val hit = armed.value
                                if (hit >= 0) {
                                    onAction(slots[armedSide.value][hit.coerceAtMost(2)].action)
                                }
                                reset()
                            } else {
                                when {
                                    offsetX.value <= -openWidthPx * 0.35f -> { offsetX.value = -openWidthPx; open.value = true }
                                    offsetX.value >= openWidthPx * 0.35f -> { offsetX.value = openWidthPx; open.value = true }
                                    else -> reset()
                                }
                            }
                        },
                        onDragCancel = { reset() }
                    ) { change, drag ->
                        change.consume()
                        if (mode == SwipeMode.REVEAL) {
                            offsetX.value = (offsetX.value + drag.x).coerceIn(-openWidthPx - 40f, openWidthPx + 40f)
                        } else {
                            val nx = offsetX.value + drag.x * 0.82f
                            offsetX.value = nx.coerceIn(-zonePx.last(), zonePx.last())
                            val dist = kotlin.math.abs(nx)
                            armed.value = when {
                                dist >= zonePx[2] -> 2
                                dist >= zonePx[1] -> 1
                                dist >= zonePx[0] -> 0
                                else -> -1
                            }
                            armedSide.value = if (nx < 0) 0 else 1
                        }
                    }
                }
        ) { content() }

        // TRIGGER 模式：渐显彩色层（左滑右侧槽 / 右滑左侧槽）
        if (mode == SwipeMode.TRIGGER) {
            val idx = armed.value
            if (idx >= 0) {
                val isLeft = offsetX.value < 0
                val s = slots[armedSide.value][idx.coerceAtMost(2)]
                Box(
                    Modifier
                        .align(if (isLeft) Alignment.CenterEnd else Alignment.CenterStart)
                        .width(with(density) { kotlin.math.abs(offsetX.value).toDp() })
                        .fillMaxHeight()
                        .background(s.color),
                    contentAlignment = if (isLeft) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(s.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // REVEAL 模式：展开后的点击层（两侧）
        if (mode == SwipeMode.REVEAL && open.value) {
            val showLeft = offsetX.value < 0
            Row(
                Modifier
                    .align(if (showLeft) Alignment.CenterEnd else Alignment.CenterStart)
                    .width(252.dp)
                    .fillMaxHeight()
            ) {
                slots[if (showLeft) 0 else 1].forEach { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pointerInput(s.action) {
                                detectTapGestures(onTap = { onAction(s.action); reset() })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.label, color = Color.White, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/* ---------- 水印（引力环，位置/透明度可配） ---------- */

@Composable
fun Watermark(
    pos: WmPos,
    opacity: WmOpacity,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val alpha = when (opacity) {
        WmOpacity.LOW -> 0.10f
        WmOpacity.MID -> 0.18f
        WmOpacity.HIGH -> 0.30f
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(64.dp).alpha(alpha)) {
            rotate(degrees = -24f, pivot = center) {
                drawOval(
                    color = tint,
                    topLeft = Offset(size.width * 0.125f, size.height * 0.375f),
                    size = Size(size.width * 0.75f, size.height * 0.25f),
                    style = Stroke(width = size.width * 0.09f)
                )
            }
        }
    }
}

/* ---------- 圆形扩散容器（详情页进入动画，中心 = 被点卡片中心） ---------- */

@Composable
fun CircularReveal(
    center: Offset,
    revealed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(550),
        label = "reveal"
    )
    Box(modifier.drawWithContent {
        val r = size.minDimension * 0.12f + progress * (size.width + size.height)
        val path = Path().apply { addOval(Rect(center = center, radius = r)) }
        clipPath(path) { this@drawWithContent.drawContent() }
    }) { content() }
}

/* ---------- 行内分段条（主题/语言/滑动模式等） ---------- */

@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    current: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    Row(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9.dp))
            .padding(3.dp)
    ) {
        options.forEach { (v, label) ->
            val on = v == current
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .pointerInput(v) { detectTapGestures(onTap = { onSelect(v) }) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    label,
                    fontSize = 11.5.sp,
                    color = if (on) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
