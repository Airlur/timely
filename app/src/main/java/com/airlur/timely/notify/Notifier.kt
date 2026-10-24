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
import com.airlur.timely.data.EventEntity

object Notifier {

    const val TEST_EVENT_ID = 999999L

    fun canNotify(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, event: EventEntity) {
        if (!canNotify(context)) return
        val tap = PendingIntent.getActivity(
            context, event.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, TimelyApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_ring)
            .setContentTitle(event.title)
            .setContentText(
                java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm")
                    .format(java.time.Instant.ofEpochMilli(event.targetDate).atZone(java.time.ZoneId.systemDefault()))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(event.id.toInt(), n)
    }

    fun showTest(context: Context) {
        if (!canNotify(context)) return
        val tap = PendingIntent.getActivity(
            context, TEST_EVENT_ID.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, TimelyApp.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_ring)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(TEST_EVENT_ID.toInt(), n)
    }
}
