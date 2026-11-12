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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecalendar.reborn.core.lunar.Almanac
import com.ecalendar.reborn.core.lunar.AlmanacDay
import com.ecalendar.reborn.ui.components.AlmanacHeaderCard
import com.ecalendar.reborn.ui.components.SectionCard
import com.ecalendar.reborn.ui.components.SectionTitle
import com.ecalendar.reborn.ui.components.TagFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 黄历页：今日完整黄历 + 择日查询。
 * 对应原包 `HuangLiFragment`、`QueryYiJiActivity`、`ChooseDayActivity`、`TimeJiXiongActivity`。
 */
@Composable
fun AlmanacScreen(onBack: () -> Unit) {
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    var chooseMode by remember { mutableStateOf(false) }
    var selectedActivity by remember { mutableStateOf("嫁娶") }
    var results by remember { mutableStateOf<List<AlmanacDay>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val day = remember(anchor) { Almanac.of(anchor) }

    val activities = listOf("嫁娶", "开市", "出行", "动土", "安葬", "祈福", "入学", "签约")

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("黄历 · ${anchor.year}年${anchor.monthValue}月${anchor.dayOfMonth}日", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(day.lunar.fullName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { anchor = anchor.minusDays(1); results = emptyList() }) { Text("前一天") }
                    OutlinedButton(onClick = { anchor = anchor.plusDays(1); results = emptyList() }) { Text("后一天") }
                }
            }
        }
        item { AlmanacHeaderCard(day = day) }
        item {
            SectionCard("今日概览") {
                Text("值神 ${day.zhiShen} · 建除 ${day.jianChu}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("${day.yearGanZhi}年 ${day.monthGanZhi}月 ${day.dayGanZhi}日 · ${day.naYin}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("${day.chong} ${day.sha} · 空亡 ${day.xunKong}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("彭祖百忌：${day.pengZu}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (day.solarTerm.isNotEmpty() || day.festival.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(listOfNotNull(day.solarTerm.ifEmpty { null }, day.festival.ifEmpty { null }).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            SectionCard("吉神与凶煞") {
                TagFlow(day.jiShen.ifEmpty { listOf("无明显吉神") }, color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(8.dp))
                TagFlow(day.xiongSha.ifEmpty { listOf("无明显凶煞") }, color = MaterialTheme.colorScheme.error)
            }
        }
        item { SectionTitle("时辰吉凶") }
        items(day.hours) { hour ->
            SectionCard(title = "${hour.label}　${hour.ganZhi}　${hour.godName}") {
                Text(
                    if (hour.auspicious) "吉 · 宜 " + hour.yi.joinToString("、") else "凶 · 忌 " + hour.ji.joinToString("、"),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Button(onClick = { chooseMode = !chooseMode }) {
                Text(if (chooseMode) "收起择日" else "择日：挑个好日子")
            }
        }
        if (chooseMode) {
            item {
                SectionCard("择日查询") {
                    Text("选择要做的事，在最近 60 天里筛出宜做的日子", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        activities.take(4).forEach { act ->
                            FilterChip(selected = selectedActivity == act, onClick = { selectedActivity = act }, label = { Text(act) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        activities.drop(4).forEach { act ->
                            FilterChip(selected = selectedActivity == act, onClick = { selectedActivity = act }, label = { Text(act) })
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        enabled = !searching,
                        onClick = {
                            searching = true
                            scope.launch {
                                val list = withContext(Dispatchers.Default) {
                                    Almanac.chooseDays(LocalDate.now(), LocalDate.now().plusDays(59), selectedActivity)
                                }
                                results = list
                                searching = false
                            }
                        },
                    ) { Text(if (searching) "推算中…" else "开始推算") }
                }
            }
            items(results) { item ->
                SectionCard(title = "${item.date.monthValue}月${item.date.dayOfMonth}日 · ${item.lunar.dayName} · ${item.dayGanZhi}") {
                    Text("宜：" + item.yi.joinToString("、"), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text("${item.zhiShen} · ${item.chong} ${item.sha}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
