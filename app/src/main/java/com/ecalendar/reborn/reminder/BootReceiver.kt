package com.ecalendar.reborn.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ecalendar.reborn.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 开机后重建闹钟，替代原包的守护进程重启逻辑 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).eventDao()
                val scheduler = ReminderScheduler(context)
                dao.observeAll().first().forEach { scheduler.schedule(it) }
                Notifications.createChannels(context)
            } finally {
                pending.finish()
            }
        }
    }
}
