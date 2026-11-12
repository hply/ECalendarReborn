package com.ecalendar.reborn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecalendar.reborn.core.lunar.LunarCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 月视图日历网格。
 *
 * 对应原包 `module.calendar` 的月历卡片与 `cn.etouch.ecalendar.WeekActivity` 的周视图。
 * 每个格子显示：公历日 + 农历/节气 + 节日标记 + 事件圆点。
 */
@Composable
fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    weekStartMonday: Boolean,
    showLunar: Boolean,
    markers: Map<Long, Int>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekdays = remember(weekStartMonday) {
        val base = listOf("一", "二", "三", "四", "五", "六", "日")
        val order = if (weekStartMonday) base else base.dropLast(1).let { listOf("日") + it }
        order
    }
    val first = month.atDay(1)
    val leading = {
        val dow = first.dayOfWeek.value // 1=周一 .. 7=周日
        if (weekStartMonday) dow - 1 else dow % 7
    }()
    val days = month.lengthOfMonth()
    val total = ((leading + days + 6) / 7) * 7
    val today = LocalDate.now()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { index, label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        var cell = 0
        while (cell < total) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { slot ->
                    val dayNumber = cell - leading + 1
                    val date = if (dayNumber in 1..days) month.atDay(dayNumber) else null
                    DayCell(
                        date = date,
                        selected = date == selected,
                        isToday = date == today,
                        showLunar = showLunar,
                        markers = markers[date?.toEpochDay() ?: -1L] ?: 0,
                        onClick = { date?.let(onSelect) },
                        modifier = Modifier.weight(1f),
                    )
                    cell++
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    selected: Boolean,
    isToday: Boolean,
    showLunar: Boolean,
    markers: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (date == null) {
        Box(modifier.aspectRatio(0.82f))
        return
    }
    val lunar = remember(date) { runCatching { LunarCalendar.solarToLunar(date) }.getOrNull() }
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    Column(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                },
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        ) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 16.sp,
            fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Medium,
            color = when {
                selected -> MaterialTheme.colorScheme.onPrimary
                weekend -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (showLunar && lunar != null) {
            Text(
                lunar.shortLabel,
                fontSize = 10.sp,
                maxLines = 1,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    lunar.solarTerm.isNotEmpty() -> MaterialTheme.colorScheme.tertiary
                    lunar.lunarDay == 1 || lunar.festivals.isNotEmpty() -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (markers > 0) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                    ),
            )
        }
    }
}

/** 周视图：一周 7 天横排，对应原包 WeekActivity */
@Composable
fun WeekStrip(
    anchor: LocalDate,
    selected: LocalDate,
    weekStartMonday: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = if (weekStartMonday) {
        anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    } else {
        anchor.minusDays((anchor.dayOfWeek.value % 7).toLong())
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (0..6).forEach { i ->
            val date = start.plusDays(i.toLong())
            val lunar = remember(date) { runCatching { LunarCalendar.solarToLunar(date) }.getOrNull() }
            val active = date == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable { onSelect(date) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    lunar?.shortLabel ?: "",
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
