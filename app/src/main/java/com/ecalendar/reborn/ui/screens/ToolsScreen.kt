package com.ecalendar.reborn.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecalendar.reborn.core.lunar.Bazi
import com.ecalendar.reborn.core.lunar.BaziChart
import com.ecalendar.reborn.core.lunar.Festivals
import com.ecalendar.reborn.core.lunar.Ganzhi
import com.ecalendar.reborn.core.lunar.LunarCalendar
import com.ecalendar.reborn.core.lunar.Zodiac
import com.ecalendar.reborn.ui.components.SectionCard
import com.ecalendar.reborn.ui.components.SectionTitle
import com.ecalendar.reborn.ui.components.TagFlow
import java.time.LocalDate

/**
 * 工具页：八字排盘、星座配对、生肖速查、节日节气。
 * 对应原包 `tools.mc.MCActivity`、`tools.astro.AstroPairActivity`、
 * `module.calculate`、`tools.notice.FestivalAndJieqiFragment`。
 */
@Composable
fun ToolsScreen(onOpenWeather: () -> Unit) {
    var tool by remember { mutableStateOf(0) }
    val year = LocalDate.now().year
    val festivals = remember(year) { Festivals.ofYear(year) }
    val tools = listOf("八字排盘", "星座配对", "生肖速查", "节日节气")
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                tools.forEachIndexed { index, name ->
                    FilterChip(selected = tool == index, onClick = { tool = index }, label = { Text(name) })
                }
            }
        }
        item {
            SectionCard("天气") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("当前天气与未来 7 天（公开气象接口，无广告 SDK）", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onOpenWeather) { Text("查看") }
                }
            }
        }
        when (tool) {
            0 -> baziItem()
            1 -> astrologyItem()
            2 -> zodiacItem()
            else -> festivalItems(festivals)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.baziItem() {
    item {
        var date by mutableStateOf(LocalDate.now())
        var hour by mutableStateOf(12)
        val chart: BaziChart = remember(date, hour) { Bazi.chart(date, hour) }
        val context = LocalContext.current
        SectionCard("八字排盘（四柱）") {
            Column {
                TextButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                        date.year, date.monthValue - 1, date.dayOfMonth,
                    ).show()
                }) { Text("出生日期：${date.year}-${date.monthValue}-${date.dayOfMonth}") }
                Text("出生时辰：${Ganzhi.hourLabels[Ganzhi.hourBranchOf(hour)]}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 6, 12, 18).forEach { h -> AssistChip(onClick = { hour = h }, label = { Text("%02d:00".format(h)) }) }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    chart.pillars.forEachIndexed { i, p ->
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(chart.pillarNames[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(p, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(chart.naYin[i], style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("五行：" + chart.elementCount.entries.joinToString(" ") { "${it.key}${it.value}" }, style = MaterialTheme.typography.bodyMedium)
                Text("日主：${chart.dayMaster} · 生肖：${chart.zodiac} · 空亡：${chart.xunKong}", style = MaterialTheme.typography.bodySmall)
                val missing = Bazi.missingElements(chart)
                if (missing.isNotEmpty()) {
                    Text("五行缺：${missing.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "农历 ${chart.lunar.fullName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.astrologyItem() {
    item {
        var a by mutableStateOf("白羊座")
        var b by mutableStateOf("狮子座")
        val score = remember(a, b) { Zodiac.matchScore(a, b) }
        val all = listOf("白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座")
        SectionCard("星座配对") {
            Column {
                Text("第一方：$a  ${Zodiac.dateRangeOf(a)}", style = MaterialTheme.typography.bodyMedium)
                TagFlow(all.take(6), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    all.take(6).forEach { s -> AssistChip(onClick = { a = s }, label = { Text(s.take(2)) }) }
                }
                Spacer(Modifier.height(10.dp))
                Text("第二方：$b  ${Zodiac.dateRangeOf(b)}", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    all.drop(6).forEach { s -> AssistChip(onClick = { b = s }, label = { Text(s.take(2)) }) }
                    all.take(2).forEach { s -> AssistChip(onClick = { b = s }, label = { Text(s.take(2)) }) }
                }
                Spacer(Modifier.height(12.dp))
                Text("匹配度 $score 分", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(Zodiac.traitOf(a), style = MaterialTheme.typography.bodySmall)
                Text(Zodiac.traitOf(b), style = MaterialTheme.typography.bodySmall)
                Text(
                    "元素：${Zodiac.elementOf(a)} × ${Zodiac.elementOf(b)}　（同象最顺，相冲需磨合）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.zodiacItem() {
    item {
        var zodiac by mutableStateOf("鼠")
        val current = LunarCalendar.solarToLunar(LocalDate.now()).zodiac
        SectionCard("生肖速查") {
            Column {
                Text("今年生肖：$current · 农历 ${LunarCalendar.solarToLunar(LocalDate.now()).yearGanZhi} 年",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Zodiac.zodiacs.take(6).forEach { z -> AssistChip(onClick = { zodiac = z }, label = { Text(z) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Zodiac.zodiacs.drop(6).forEach { z -> AssistChip(onClick = { zodiac = z }, label = { Text(z) }) }
                }
                Spacer(Modifier.height(10.dp))
                Text("$zodiac：${Zodiac.zodiacTrait(zodiac)}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("年份：" + (0..8).map { Zodiac.yearOfZodiac(zodiac, LocalDate.now().year - it * 12) }.joinToString("、"),
                    style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("配对参考", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                val best = Zodiac.zodiacs.filter { Zodiac.zodiacMatch(zodiac, it).first >= 88 }
                Text("六合 / 三合高分组合：" + best.joinToString("、"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.festivalItems(list: List<Triple<LocalDate, String, Boolean>>) {
    item { SectionTitle("${LocalDate.now().year} 年节日与节气（共 ${list.size} 条）") }
    items(list) { (date, name, solar) ->
        SectionCard(title = "$name　${date.monthValue}月${date.dayOfMonth}日") {
            val lunar = remember(date) { runCatching { LunarCalendar.solarToLunar(date) }.getOrNull() }
            Text(
                (if (solar) "公历" else "农历") + " · " + (lunar?.fullName ?: "") +
                    " · " + listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1] + "曜",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
