package com.ecalendar.reborn.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ecalendar.reborn.data.EventEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 提醒调度：用 AlarmManager 精确闹钟替代原包的常驻提醒进程
 * （原包依赖 `cn.etouch.ecalendar.remind.AlarmRemindActivity` + 保活服务，
 * 这里改为系统闹钟 + 广播接收器，不申请保活类权限）。
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntentFor(id: Long, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, id)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun schedule(event: EventEntity) {
        cancel(event.id)
        if (event.remindBefore < 0 || event.done) return
        val triggerAt = computeTrigger(event) ?: return
        if (triggerAt <= System.currentTimeMillis()) return
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntentFor(event.id, event.id.toInt()))
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntentFor(event.id, event.id.toInt()))
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntentFor(event.id, event.id.toInt()))
        }
    }

    fun cancel(id: Long) {
        alarmManager.cancel(pendingIntentFor(id, id.toInt()))
    }

    fun scheduleDaily(hour: Int, minute: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ReminderReceiver.ACTION_DAILY }
        val pi = PendingIntent.getBroadcast(
            context, DAILY_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = ZonedDateTime.of(LocalDate.now(), LocalTime.of(hour, minute), ZoneId.systemDefault())
            .let { if (it.isBefore(ZonedDateTime.now())) it.plusDays(1) else it }
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pi,
        )
    }

    fun cancelDaily() {
        val pi = PendingIntent.getBroadcast(
            context, DAILY_REQUEST,
            Intent(context, ReminderReceiver::class.java).apply { action = ReminderReceiver.ACTION_DAILY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
    }

    private fun computeTrigger(event: EventEntity): Long? {
        val startAt = event.startAt
        val remindAt = startAt - event.remindBefore * 60_000L
        if (event.repeatRule == "none") return remindAt
        var candidate = remindAt
        val now = System.currentTimeMillis()
        var guard = 0
        while (candidate <= now && guard < 400) {
            val date = Instant.ofEpochMilli(candidate).atZone(ZoneId.systemDefault()).toLocalDate()
            candidate += when (event.repeatRule) {
                "daily" -> 86_400_000L
                "weekly" -> 7 * 86_400_000L
                "monthly" -> java.time.temporal.ChronoUnit.DAYS.between(date, date.plusMonths(1)) * 86_400_000L
                "yearly" -> java.time.temporal.ChronoUnit.DAYS.between(date, date.plusYears(1)) * 86_400_000L
                else -> 86_400_000L
            }
            guard++
        }
        return candidate
    }

    private fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= 31) alarmManager.canScheduleExactAlarms() else true

    companion object {
        private const val DAILY_REQUEST = 9001
    }
}
