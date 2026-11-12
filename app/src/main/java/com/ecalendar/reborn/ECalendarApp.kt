package com.ecalendar.reborn

import android.app.Application
import com.ecalendar.reborn.data.AppDatabase
import com.ecalendar.reborn.data.SettingsRepository
import com.ecalendar.reborn.reminder.Notifications
import com.ecalendar.reborn.reminder.ReminderScheduler

/**
 * 应用入口。对应原包 `cn.etouch.ecalendar.ECalendar`（Application 子类）
 * 中与自己业务相关的那部分初始化（数据库、设置、提醒）。
 * 原包在 Application 里初始化的广告/统计/推送 SDK 全部不实现。
 */
class ECalendarApp : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var scheduler: ReminderScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        database = AppDatabase.get(this)
        scheduler = ReminderScheduler(this)
        Notifications.createChannels(this)
    }
}
