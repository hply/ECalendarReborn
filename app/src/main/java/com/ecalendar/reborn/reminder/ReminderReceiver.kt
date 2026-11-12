package com.ecalendar.reborn.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ecalendar.reborn.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 提醒广播：日程到点、每日黄历 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMIND -> {
                val id = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
                if (id <= 0L) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val event = AppDatabase.get(context).eventDao().find(id)
                        if (event != null && !event.done) {
                            Notifications.notifyEvent(context, id.toInt(), event.title, describe(event))
                        }
                    } catch (e: Exception) {
                        Log.w("ReminderReceiver", "读取日程失败", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_DAILY -> Notifications.notifyDaily(context)
        }
    }

    private fun describe(event: com.ecalendar.reborn.data.EventEntity): String {
        val whenText = java.time.Instant.ofEpochMilli(event.startAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
            .let { "%02d月%02d日 %02d:%02d".format(it.monthValue, it.dayOfMonth, it.hour, it.minute) }
        return if (event.note.isBlank()) whenText else "$whenText\n${event.note}"
    }

    companion object {
        const val ACTION_REMIND = "com.ecalendar.reborn.ACTION_REMIND"
        const val ACTION_DAILY = "com.ecalendar.reborn.ACTION_DAILY"
        const val EXTRA_EVENT_ID = "event_id"
    }
}
