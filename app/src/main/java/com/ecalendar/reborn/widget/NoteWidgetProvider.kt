package com.ecalendar.reborn.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.ecalendar.reborn.R
import com.ecalendar.reborn.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 便签小组件（2x2）：显示最近一条记事。
 * 对应原包 `settings.ConfigureNote2x2Activity` 与 `Widget_Note_Dialog`。
 */
class NoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val latest = AppDatabase.get(context).noteDao().observeAll().first().firstOrNull()
                val views = RemoteViews(context.packageName, R.layout.widget_note)
                views.setTextViewText(R.id.widget_note_title, latest?.title ?: "还没有记事")
                views.setTextViewText(
                    R.id.widget_note_content,
                    latest?.content?.take(120) ?: "打开应用写第一条记事",
                )
                views.setOnClickPendingIntent(R.id.widget_note_title, CalendarWidgetProvider.openApp(context))
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }
}
