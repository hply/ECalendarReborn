package com.ecalendar.reborn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 设置项，对应原包 `cn.etouch.ecalendar.settings` 下的设置页（主题、字体、日历显示、提醒） */
data class AppSettings(
    val weekStartMonday: Boolean = true,
    val showLunar: Boolean = true,
    val showFestival: Boolean = true,
    val fontScale: Float = 1.0f,
    val skin: String = "classic",
    val defaultRemindMinutes: Int = 10,
    val dailyRemindEnabled: Boolean = true,
    val dailyRemindHour: Int = 8,
    val dailyRemindMinute: Int = 0,
    val weatherEnabled: Boolean = true,
    val showYiJiInCard: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val weekStartMonday = booleanPreferencesKey("week_start_monday")
        val showLunar = booleanPreferencesKey("show_lunar")
        val showFestival = booleanPreferencesKey("show_festival")
        val fontScale = floatPreferencesKey("font_scale")
        val skin = stringPreferencesKey("skin")
        val defaultRemind = intPreferencesKey("default_remind")
        val dailyRemindEnabled = booleanPreferencesKey("daily_remind_enabled")
        val dailyRemindHour = intPreferencesKey("daily_remind_hour")
        val dailyRemindMinute = intPreferencesKey("daily_remind_minute")
        val weatherEnabled = booleanPreferencesKey("weather_enabled")
        val showYiJi = booleanPreferencesKey("show_yiji")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            weekStartMonday = p[Keys.weekStartMonday] ?: true,
            showLunar = p[Keys.showLunar] ?: true,
            showFestival = p[Keys.showFestival] ?: true,
            fontScale = p[Keys.fontScale] ?: 1.0f,
            skin = p[Keys.skin] ?: "classic",
            defaultRemindMinutes = p[Keys.defaultRemind] ?: 10,
            dailyRemindEnabled = p[Keys.dailyRemindEnabled] ?: true,
            dailyRemindHour = p[Keys.dailyRemindHour] ?: 8,
            dailyRemindMinute = p[Keys.dailyRemindMinute] ?: 0,
            weatherEnabled = p[Keys.weatherEnabled] ?: true,
            showYiJiInCard = p[Keys.showYiJi] ?: true,
        )
    }

    suspend fun setWeekStartMonday(value: Boolean) =
        context.dataStore.edit { it[Keys.weekStartMonday] = value }

    suspend fun setShowLunar(value: Boolean) =
        context.dataStore.edit { it[Keys.showLunar] = value }

    suspend fun setShowFestival(value: Boolean) =
        context.dataStore.edit { it[Keys.showFestival] = value }

    suspend fun setFontScale(value: Float) =
        context.dataStore.edit { it[Keys.fontScale] = value }

    suspend fun setSkin(value: String) =
        context.dataStore.edit { it[Keys.skin] = value }

    suspend fun setDefaultRemindMinutes(value: Int) =
        context.dataStore.edit { it[Keys.defaultRemind] = value }

    suspend fun setDailyRemind(enabled: Boolean, hour: Int, minute: Int) =
        context.dataStore.edit {
            it[Keys.dailyRemindEnabled] = enabled
            it[Keys.dailyRemindHour] = hour
            it[Keys.dailyRemindMinute] = minute
        }

    suspend fun setWeatherEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.weatherEnabled] = value }

    suspend fun setShowYiJi(value: Boolean) =
        context.dataStore.edit { it[Keys.showYiJi] = value }
}
