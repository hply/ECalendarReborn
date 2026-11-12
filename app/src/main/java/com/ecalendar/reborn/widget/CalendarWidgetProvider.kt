package com.ecalendar.reborn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ecalendar.reborn.MainActivity
import com.ecalendar.reborn.R
import com.ecalendar.reborn.core.lunar.Almanac
import com.ecalendar.reborn.core.lunar.LunarCalendar
import com.ecalendar.reborn.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * 日历小组件（4x2）：公历日期 + 农历 + 今日宜忌 + 日程条数。
 * 对应原包 `settings.ConfigureCalendarActivity4x2` 与
 * `component.appwidget` / `widget` 下的桌面组件。
 */
class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val date = LocalDate.now()
                val lunar = runCatching { LunarCalendar.solarToLunar(date) }.getOrNull()
                val almanac = Almanac.of(date)
                val today = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val events = AppDatabase.get(context).eventDao().between(today, today + 86_400_000L)
                val views = RemoteViews(context.packageName, R.layout.widget_calendar)
                views.setTextViewText(
                    R.id.widget_date,
                    "${date.monthValue}月${date.dayOfMonth}日 星期" + listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1],
                )
                views.setTextViewText(
                    R.id.widget_lunar,
                    (lunar?.fullName ?: "") + if (lunar?.festivals?.isNotEmpty() == true) " · ${lunar.festivals}" else "",
                )
                views.setTextViewText(
                    R.id.widget_yiji,
                    "宜：" + almanac.yi.take(4).joinToString("、") + "\n忌：" + almanac.ji.take(4).joinToString("、"),
                )
                views.setTextViewText(
                    R.id.widget_events,
                    if (events.isEmpty()) "今日无日程" else "今日 ${events.size} 条日程",
                )
                views.setOnClickPendingIntent(R.id.widget_date, openApp(context))
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context, 100,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CalendarWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
