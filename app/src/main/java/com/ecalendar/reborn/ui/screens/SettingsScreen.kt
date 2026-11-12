package com.ecalendar.reborn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecalendar.reborn.BuildConfig
import com.ecalendar.reborn.data.AppSettings
import com.ecalendar.reborn.ui.AppActions
import com.ecalendar.reborn.ui.components.SectionCard
import com.ecalendar.reborn.ui.theme.Skins
import kotlinx.coroutines.launch

/**
 * 设置页：皮肤、字体、日历显示、提醒、天气开关。
 * 对应原包 `settings.SettingsActivity`、`CalendarSettingActivity`、
 * `WeatherSettingActivity`、`RemindSettingActivity`、`settings.skin.*`。
 */
@Composable
fun SettingsScreen(actions: AppActions) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by actions.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionCard("皮肤") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Skins.all.forEach { skin ->
                        AssistChip(
                            onClick = { scope.launch { actions.settings.setSkin(skin.id) } },
                            label = { Text(if (settings.skin == skin.id) "● ${skin.label}" else skin.label) },
                        )
                    }
                }
            }
        }
        item {
            SectionCard("字体大小") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.9f to "小", 1.0f to "标准", 1.12f to "大", 1.25f to "特大").forEach { (scale, label) ->
                        AssistChip(
                            onClick = { scope.launch { actions.settings.setFontScale(scale) } },
                            label = { Text(if (kotlin.math.abs(settings.fontScale - scale) < 0.01f) "● $label" else label) },
                        )
                    }
                }
            }
        }
        item {
            SectionCard("日历显示") {
                Column {
                    SwitchRow("周一作为一周开始", settings.weekStartMonday) { v ->
                        scope.launch { actions.settings.setWeekStartMonday(v) }
                    }
                    SwitchRow("格子显示农历/节气", settings.showLunar) { v ->
                        scope.launch { actions.settings.setShowLunar(v) }
                    }
                    SwitchRow("黄历卡片显示宜忌", settings.showYiJiInCard) { v ->
                        scope.launch { actions.settings.setShowYiJi(v) }
                    }
                }
            }
        }
        item {
            SectionCard("提醒") {
                Column {
                    SwitchRow("每天推送当日黄历（${settings.dailyRemindHour}:%02d）".format(settings.dailyRemindMinute), settings.dailyRemindEnabled) { v ->
                        scope.launch {
                            actions.settings.setDailyRemind(v, settings.dailyRemindHour, settings.dailyRemindMinute)
                            if (v) actions.scheduler.scheduleDaily(settings.dailyRemindHour, settings.dailyRemindMinute)
                            else actions.scheduler.cancelDaily()
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(7, 8, 9, 12, 20).forEach { hour ->
                            AssistChip(
                                onClick = {
                                    scope.launch {
                                        actions.settings.setDailyRemind(true, hour, 0)
                                        actions.scheduler.scheduleDaily(hour, 0)
                                    }
                                },
                                label = { Text("${hour}:00") },
                            )
                        }
                    }
                    Text(
                        "日程提醒用系统精确闹钟；若系统限制了精确闹钟，会退化为普通闹钟。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            SectionCard("其他") {
                Column {
                    SwitchRow("天气功能", settings.weatherEnabled) { v ->
                        scope.launch { actions.settings.setWeatherEnabled(v) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("数据与隐私", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "本工程不含第三方广告、统计、推送 SDK；数据只存本地数据库；天气请求直连公开气象接口。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            SectionCard("关于") {
                Column {
                    Text("中华万年历 · 重建版", style = MaterialTheme.typography.bodyMedium)
                    Text("版本 ${BuildConfig.VERSION_NAME}（versionCode ${BuildConfig.VERSION_CODE}）", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "基于对 v10.0.10 安装包的静态逆向证据独立重写，不含原包代码与资源。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("设备：${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.SDK_INT}", style = MaterialTheme.typography.labelSmall)
                    Text(context.packageName, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
