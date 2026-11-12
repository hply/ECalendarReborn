package com.ecalendar.reborn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecalendar.reborn.core.lunar.Almanac
import com.ecalendar.reborn.core.lunar.LunarCalendar
import com.ecalendar.reborn.core.lunar.SolarTerm
import com.ecalendar.reborn.data.EventEntity
import com.ecalendar.reborn.ui.AppActions
import com.ecalendar.reborn.ui.components.AlmanacHeaderCard
import com.ecalendar.reborn.ui.components.MonthGrid
import com.ecalendar.reborn.ui.components.SectionCard
import com.ecalendar.reborn.ui.components.SectionTitle
import com.ecalendar.reborn.ui.components.TagFlow
import com.ecalendar.reborn.ui.components.WeekStrip
import java.time.LocalDate
import java.time.YearMonth

/**
 * 主日历页：月视图 / 周视图切换 + 当日黄历卡片 + 当日日程列表。
 * 对应原包 `module.main.ui.MainActivity` + `HuangLiFragment` + `WeekActivity`。
 */
@Composable
fun CalendarScreen(
    actions: AppActions,
    onOpenDayDetail: (Long) -> Unit,
    onOpenAlmanac: () -> Unit,
) {
    val settings by actions.settings.flow.collectAsStateWithLifecycle(initialValue = com.ecalendar.reborn.data.AppSettings())
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var weekMode by remember { mutableStateOf(false) }

    val almanac = remember(selected) { Almanac.of(selected) }
    val events by remember(selected) {
        val start = selected.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        actions.db.eventDao().observeBetween(start, start + 86_400_000L)
    }.collectAsStateWithLifecycle(initialValue = emptyList<EventEntity>())

    val marks by actions.db.dayMarkDao().observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val markerMap = remember(marks) { marks.associate { it.epochDay to 1 } }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${month.year} 年 ${month.monthValue} 月",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        LunarCalendar.solarToLunar(month.atDay(1)).let { "${it.yearGanZhi}年" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { weekMode = !weekMode }) {
                    Text(if (weekMode) "切换月视图" else "切换周视图")
                }
                TextButton(onClick = { selected = LocalDate.now(); month = YearMonth.now() }) {
                    Text("回到今天")
                }
            }
        }
        if (weekMode) {
            item {
                WeekStrip(
                    anchor = selected,
                    selected = selected,
                    weekStartMonday = settings.weekStartMonday,
                    onSelect = { selected = it; month = YearMonth.of(it.year, it.monthValue) },
                )
            }
        } else {
            item {
                MonthGrid(
                    month = month,
                    selected = selected,
                    weekStartMonday = settings.weekStartMonday,
                    showLunar = settings.showLunar,
                    markers = markerMap,
                    onSelect = {
                        selected = it
                        if (YearMonth.of(it.year, it.monthValue) != month) month = YearMonth.of(it.year, it.monthValue)
                    },
                )
            }
        }
        item {
            AlmanacHeaderCard(day = almanac, showYiJi = settings.showYiJiInCard)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { onOpenDayDetail(selected.toEpochDay()) }) { Text("查看当日详情") }
                TextButton(onClick = onOpenAlmanac) { Text("完整黄历") }
            }
        }
        item { SectionTitle("当日日程 · ${selected.monthValue}月${selected.dayOfMonth}日") }
        if (events.isEmpty()) {
            item {
                Text(
                    "这一天还没有安排，去「清单」里加一条吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        } else {
            items(events) { event ->
                SectionCard(title = event.title) {
                    Text(
                        "%02d:%02d".format(
                            java.time.Instant.ofEpochMilli(event.startAt)
                                .atZone(java.time.ZoneId.systemDefault()).hour,
                            java.time.Instant.ofEpochMilli(event.startAt)
                                .atZone(java.time.ZoneId.systemDefault()).minute,
                        ) + if (event.note.isBlank()) "" else " · ${event.note}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/**
 * 单日详情页：农历、干支、节气、节日、宜忌、吉神凶煞、冲煞、彭祖百忌。
 * 对应原包 `module.calendar` 的日期详情卡片与 `tools.almanac.AlmanacModernChineseActivity`。
 */
@Composable
fun DayDetailScreen(actions: AppActions, epochDay: Long, onBack: () -> Unit) {
    val date = LocalDate.ofEpochDay(epochDay)
    val almanac = remember(date) { Almanac.of(date) }
    val settings by actions.settings.flow.collectAsStateWithLifecycle(initialValue = com.ecalendar.reborn.data.AppSettings())
    val fu = remember(date.year) { SolarTerm.fuPeriods(date.year) }
    val shuJiu = remember(date) { SolarTerm.shuJiuDay(date) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "${date.year}年${date.monthValue}月${date.dayOfMonth}日 星期" +
                    listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1],
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                almanac.lunar.fullName + " · " + almanac.lunar.zodiac + "年",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { AlmanacHeaderCard(day = almanac, showYiJi = settings.showYiJiInCard) }
        if (almanac.solarTerm.isNotEmpty() || shuJiu > 0 || fu.isNotEmpty()) {
            item {
                SectionCard("节气与物候") {
                    if (almanac.solarTerm.isNotEmpty()) {
                        Text("今日 ${almanac.solarTerm}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    if (shuJiu > 0) {
                        val jiu = (shuJiu - 1) / 9 + 1
                        val dayInJiu = (shuJiu - 1) % 9 + 1
                        Text("数九：${'一'}九第 $dayInJiu 天", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    val current = fu.firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }
                    if (current != null) {
                        Text("三伏：${current.name}（${current.start.monthValue}月${current.start.dayOfMonth}日 起）", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            SectionCard("干支与五行") {
                Text("年柱 ${almanac.yearGanZhi}　月柱 ${almanac.monthGanZhi}　日柱 ${almanac.dayGanZhi}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("纳音 ${almanac.naYin} · 日五行 ${almanac.element} · 空亡 ${almanac.xunKong}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("${almanac.chong} ${almanac.sha}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("彭祖百忌：${almanac.pengZu}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (almanac.jiShen.isNotEmpty() || almanac.xiongSha.isNotEmpty()) {
            item {
                SectionCard("神煞") {
                    if (almanac.jiShen.isNotEmpty()) {
                        Text("吉神宜趋", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        TagFlow(almanac.jiShen, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (almanac.xiongSha.isNotEmpty()) {
                        Text("凶煞宜忌", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        TagFlow(almanac.xiongSha, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item { SectionTitle("十二时辰吉凶") }
        items(almanac.hours) { hour ->
            SectionCard(title = "${hour.label} · ${hour.ganZhi} · ${hour.godName}" +
                if (hour.auspicious) "（吉）" else "（凶）") {
                Text(
                    if (hour.auspicious) "宜：" + hour.yi.joinToString("、") else "忌：" + hour.ji.joinToString("、"),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
