package com.ecalendar.reborn.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ecalendar.reborn.MainActivity
import com.ecalendar.reborn.R
import java.time.LocalDate
import com.ecalendar.reborn.core.lunar.Almanac
import com.ecalendar.reborn.core.lunar.LunarCalendar

/** 通知渠道与通知构建，对应原包 `remind.AlarmRemindActivity` / `DailyRemindActivity` 的提醒能力 */
object Notifications {

    const val CHANNEL_REMIND = "remind"
    const val CHANNEL_DAILY = "daily"
    const val CHANNEL_BIRTHDAY = "birthday"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMIND, "日程提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "日程与待办到点提醒"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "每日黄历", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "每天早上推送当日农历与宜忌"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BIRTHDAY, "生日提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "生日与纪念日提前提醒"
            },
        )
    }

    private fun contentIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun notifyEvent(context: Context, id: Int, title: String, text: String) {
        if (!canPost(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REMIND)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun notifyDaily(context: Context) {
        if (!canPost(context)) return
        val today = LocalDate.now()
        val lunar = LunarCalendar.solarToLunar(today)
        val almanac = Almanac.of(today)
        val text = buildString {
            append(lunar.fullName)
            if (lunar.festivals.isNotEmpty()) append(" · ").append(lunar.festivals)
            append("\n宜：").append(almanac.yi.joinToString("、"))
            append("\n忌：").append(almanac.ji.joinToString("、"))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("今日黄历 · ${almanac.zhiShen}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(2001, notification)
    }

    fun notifyBirthday(context: Context, id: Int, name: String, daysLeft: Int) {
        if (!canPost(context)) return
        val title = if (daysLeft == 0) "$name 今天生日" else "$name 还有 $daysLeft 天生日"
        val notification = NotificationCompat.Builder(context, CHANNEL_BIRTHDAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("点开可以写祝福或准备礼物")
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun canPost(context: Context): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return granted
    }
}
