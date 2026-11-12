package com.ecalendar.reborn.ui

import android.content.Context
import com.ecalendar.reborn.data.AppDatabase
import com.ecalendar.reborn.data.SettingsRepository
import com.ecalendar.reborn.reminder.ReminderScheduler

/** 简单的依赖容器，替代原包的模块化 DI（原包用多模块 + 反射注入） */
class AppActions(context: Context) {
    val db: AppDatabase = AppDatabase.get(context)
    val settings: SettingsRepository = SettingsRepository(context)
    val scheduler: ReminderScheduler = ReminderScheduler(context)
}
