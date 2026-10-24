package com.airlur.timely.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.airlur.timely.data.EventEntity
import com.airlur.timely.data.Repeat
import com.airlur.timely.domain.RepeatRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 提醒调度：统一走 AlarmManager.setAlarmClock。
 * 闹钟级触发无需精确闹钟特殊授权，Doze 下照常响，状态栏显示闹钟图标——
 * 这是相对旧版（flutter_local_notifications + SCHEDULE_EXACT_ALARM）根治可靠性的关键。
 */
object AlarmScheduler {

    const val ACTION_FIRE = "com.airlur.timely.action.FIRE_REMINDER"
    const val EXTRA_EVENT_ID = "event_id"

    /** requestCode = eventId * 10 + slot；单提醒 v1.0 固定 slot 0，预留多提醒扩展 */
    fun requestCode(eventId: Long, slot: Int = 0): Int = (eventId * 10 + slot).toInt()

    fun schedule(context: Context, event: EventEntity, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = PendingIntent.getBroadcast(
            context, requestCode(event.id),
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE)
                .putExtra(EXTRA_EVENT_ID, event.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            context, requestCode(event.id),
            Intent(context, com.airlur.timely.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, show), fire)
    }

    fun cancel(context: Context, eventId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = PendingIntent.getBroadcast(
            context, requestCode(eventId),
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (fire != null) am.cancel(fire)
    }

    /** 计算某事件下一次应触发时刻；无提醒或已过期返回 null */
    fun nextTrigger(event: EventEntity, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val reminder = event.reminderTime ?: return null
        val now = System.currentTimeMillis()
        val repeat = Repeat.valueOf(event.repeat)
        if (repeat == Repeat.NONE) {
            return if (reminder > now) reminder else null
        }
        // 重复事件：保持「提醒相对目标时刻的偏移」滚动到下一次出现
        val targetDate = Instant.ofEpochMilli(event.targetDate).atZone(zone).toLocalDate()
        val reminderDateTime = Instant.ofEpochMilli(reminder).atZone(zone).toLocalDateTime()
        val offsetMinutes = java.time.Duration.between(
            targetDate.atStartOfDay(), reminderDateTime
        ).toMinutes()
        val today = LocalDate.now(zone)
        var next = RepeatRule.nextOccurrence(
            repeat, targetDate, if (reminder > now) LocalDate.now(zone) else today, event.customRule
        ) ?: return null
        if (event.repeatEnd != null && next.atStartOfDay(zone).toInstant().toEpochMilli() > event.repeatEnd) return null
        var trigger = next.atStartOfDay(zone).toInstant().toEpochMilli() + offsetMinutes * 60_000
        var guard = 0
        while (trigger <= now && guard < 3660) {
            next = RepeatRule.nextOccurrence(repeat, next.plusDays(1), next.plusDays(1), event.customRule) ?: return null
            trigger = next.atStartOfDay(zone).toInstant().toEpochMilli() + offsetMinutes * 60_000
            guard++
        }
        return trigger
    }

    /** 全量重排：启动、增删改事件、开机、换版本、时间/时区变更后调用（suspend，须在 IO 调用） */
    suspend fun rescheduleAll(context: Context) {
        val db = (context.applicationContext as com.airlur.timely.TimelyApp).db
        val events = db.eventDao().getAll()
        events.forEach { cancel(context, it.id) }
        events.forEach { event ->
            nextTrigger(event)?.let { schedule(context, event, it) }
        }
    }

    fun scheduleTest(context: Context, delayMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fire = PendingIntent.getBroadcast(
            context, requestCode(Notifier.TEST_EVENT_ID),
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE)
                .putExtra(EXTRA_EVENT_ID, Notifier.TEST_EVENT_ID),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            context, requestCode(Notifier.TEST_EVENT_ID),
            Intent(context, com.airlur.timely.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(System.currentTimeMillis() + delayMillis, show), fire
        )
    }
}
