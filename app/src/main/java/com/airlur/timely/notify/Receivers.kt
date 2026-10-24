package com.airlur.timely.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.airlur.timely.TimelyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 到点触发：发通知；重复事件排下一次 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1)
                if (id == Notifier.TEST_EVENT_ID) {
                    Notifier.showTest(context)
                    return@launch
                }
                if (id <= 0) return@launch
                val db = (context.applicationContext as TimelyApp).db
                val event = db.eventDao().getById(id) ?: return@launch
                Notifier.show(context, event)
                // 重复事件：滚动到下一次出现并保持提醒偏移
                AlarmScheduler.nextTrigger(event)?.let { AlarmScheduler.schedule(context, event, it) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** 开机 / 升级覆盖安装 / 时间或时区变更后全量重排 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AlarmScheduler.rescheduleAll(context)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
