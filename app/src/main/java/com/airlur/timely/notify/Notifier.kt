package com.airlur.timely.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.airlur.timely.MainActivity
import com.airlur.timely.R
import com.airlur.timely.TimelyApp

object Notifier {

    const val TEST_EVENT_ID = 999999L
    const val TIMER_ID = 1001L

    /** Android 13+ 必须已授 POST_NOTIFICATIONS 才能发通知，否则系统抛 SecurityException */
    fun canNotify(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showTest(context: Context) = showCustom(
        context,
        title = context.getString(R.string.test_notification_title),
        text = context.getString(R.string.test_notification_body),
        id = TEST_EVENT_ID
    )

    fun showTimerDone(context: Context) = showCustom(
        context,
        title = context.getString(R.string.timer_notif_title),
        text = context.getString(R.string.timer_notif_body),
        id = TIMER_ID
    )

    fun showCustom(context: Context, title: String, text: String, id: Long) {
        if (!canNotify(context)) return
        val tap = PendingIntent.getActivity(
            context, id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, TimelyApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_ring)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id.toInt(), n)
        } catch (_: SecurityException) {
            // 权限在检查与发送之间被收回的极端情况，静默放弃
        }
    }

    fun show(context: Context, event: com.airlur.timely.data.EventEntity) {
        if (!canNotify(context)) return
        val whenText = java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm")
            .format(java.time.Instant.ofEpochMilli(event.targetDate).atZone(java.time.ZoneId.systemDefault()))
        showCustom(context, title = event.title, text = whenText, id = event.id)
    }
}
