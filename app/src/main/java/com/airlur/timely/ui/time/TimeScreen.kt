package com.airlur.timely.ui.time

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airlur.timely.R
import com.airlur.timely.ui.theme.Primary
import kotlinx.coroutines.delay

/** 时间页：倒计时 + 秒表（顶部下划线切换） */
@Composable
fun TimeScreen() {
    var seg by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surface)) {
            listOf(stringResource(R.string.countdown), stringResource(R.string.stopwatch)).forEachIndexed { i, label ->
                Column(
                    Modifier.weight(1f).clickable { seg = i },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            label, fontSize = 16.sp,
                            color = if (seg == i) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth(0.4f)
                            .height(3.dp)
                            .background(if (seg == i) Primary else Color.Transparent, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
        when (seg) {
            0 -> CountdownPane()
            else -> StopwatchPane()
        }
    }
}

@Composable
private fun CountdownPane() {
    val ctx = LocalContext.current
    var h by remember { mutableStateOf(0) }
    var m by remember { mutableStateOf(10) }
    var s by remember { mutableStateOf(0) }
    var total by remember { mutableLongStateOf(600L) }
    var remain by remember { mutableLongStateOf(600L) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (running) {
            val end = System.currentTimeMillis() + remain * 1000
            while (running) {
                remain = ((end - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                if (remain <= 0L) {
                    running = false
                    com.airlur.timely.notify.Notifier.showTest(ctx)
                    val v = ctx.getSystemService(android.os.Vibrator::class.java)
                    @Suppress("MissingPermission")
                    if (android.os.Build.VERSION.SDK_INT >= 31) v.vibrate(
                        android.os.VibrationEffect.createWaveform(longArrayOf(200, 100, 200), -1)
                    ) else @Suppress("DEPRECATION") v.vibrate(400)
                    break
                }
                delay(200)
            }
        }
    }

    val set = h * 3600 + m * 60 + s
    val shown = when {
        running -> remain
        remain == 0L && total > 0 -> 0L
        else -> set.toLong().coerceAtLeast(0)
    }
    val progress = if (total > 0) remain.toFloat() / total else 0f

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(22.dp))
        Box(Modifier.size(212.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Primary,
                    startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                    style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%02d:%02d:%02d".format(shown / 3600, shown % 3600 / 60, shown % 60),
                    fontSize = 38.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (running) stringResource(R.string.pause) else stringResource(R.string.start),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        val hLabel = stringResource(R.string.hours)
        val mLabel = stringResource(R.string.minutes)
        val sLabel = stringResource(R.string.seconds)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                hLabel to { delta: Int -> h = (h + delta).coerceIn(0, 99) },
                mLabel to { delta: Int -> m = (m + delta).coerceIn(0, 59) },
                sLabel to { delta: Int -> s = (s + delta).coerceIn(0, 59) }
            ).forEach { (label, step) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepBtn("−") { step(-1) }
                        Text(
                            when (label) {
                                hLabel -> h.toString()
                                mLabel -> m.toString()
                                else -> s.toString()
                            },
                            fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(46.dp),
                            textAlign = TextAlign.Center
                        )
                        StepBtn("＋") { step(1) }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 25, 45).forEach { min ->
                Text(
                    stringResource(R.string.minutes_n, min),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            h = min / 60; m = min % 60; s = 0
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    running = false
                    remain = (h * 3600 + m * 60 + s).toLong()
                    total = remain.coerceAtLeast(1)
                },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
            ) { Text(stringResource(R.string.reset)) }
            Button(
                onClick = {
                    if (!running) {
                        val t = h * 3600 + m * 60 + s
                        if (remain <= 0) {
                            if (t <= 0) return@Button
                            remain = t.toLong()
                            total = t.toLong()
                        }
                        running = true
                    } else running = false
                },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
            ) {
                Text(
                    when {
                        running -> stringResource(R.string.pause)
                        remain in 1 until total -> stringResource(R.string.resume)
                        else -> stringResource(R.string.start)
                    }
                )
            }
        }
    }
}

@Composable
private fun StepBtn(symbol: String, onClick: () -> Unit) {
    Text(
        symbol, fontSize = 17.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .wrapContentSize(Alignment.Center)
    )
}

private data class Lap(val index: Int, val lapMs: Long, val totalMs: Long)

@Composable
private fun StopwatchPane() {
    var running by remember { mutableStateOf(false) }
    var acc by remember { mutableLongStateOf(0L) }
    var startAt by remember { mutableLongStateOf(0L) }
    var laps by remember { mutableStateOf(listOf<Lap>()) }
    val elapsed = acc + if (running) System.currentTimeMillis() - startAt else 0L
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        while (running) {
            tick = System.currentTimeMillis()
            delay(33)
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            fmtMs(elapsed), fontSize = 46.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { running = false; acc = 0; startAt = 0; laps = emptyList() },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
            ) { Text(stringResource(R.string.reset)) }
            OutlinedButton(
                onClick = {
                    if (running) laps = laps + Lap(
                        laps.size + 1,
                        elapsed - (laps.lastOrNull()?.totalMs ?: 0), elapsed
                    )
                },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
            ) { Text(stringResource(R.string.lap)) }
            Button(
                onClick = {
                    if (running) {
                        acc += System.currentTimeMillis() - startAt
                        running = false
                    } else {
                        startAt = System.currentTimeMillis()
                        running = true
                    }
                },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
            ) {
                Text(
                    when {
                        running -> stringResource(R.string.pause)
                        acc > 0 -> stringResource(R.string.resume)
                        else -> stringResource(R.string.start)
                    }
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (laps.isNotEmpty()) {
            val ints = laps.map { it.lapMs }
            val fastest = ints.minOrNull()
            val slowest = ints.maxOrNull()
            laps.reversed().forEach { lap ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#${lap.index}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(44.dp))
                    Text(
                        fmtMs(lap.lapMs), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (ints.size > 1 && lap.lapMs == fastest) {
                        Text(stringResource(R.string.lap_fast), fontSize = 10.5.sp, color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(end = 8.dp).background(Color(0x294CAF50), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    if (ints.size > 1 && lap.lapMs == slowest && fastest != slowest) {
                        Text(stringResource(R.string.lap_slow), fontSize = 10.5.sp, color = Color(0xFFF02419),
                            modifier = Modifier.padding(end = 8.dp).background(Color(0x1FF02419), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    Text(fmtMs(lap.totalMs), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun fmtMs(ms: Long): String {
    val h = ms / 3600000
    val m = ms % 3600000 / 60000
    val s = ms % 60000 / 1000
    val c = ms % 1000 / 10
    return (if (h > 0) "%02d:".format(h) else "") + "%02d:%02d.%02d".format(m, s, c)
}
