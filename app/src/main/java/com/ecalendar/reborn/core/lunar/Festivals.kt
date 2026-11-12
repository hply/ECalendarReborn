package com.ecalendar.reborn.core.lunar

import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * 节日聚合：农历节日、公历节日、按星期计算的浮动节日、节气。
 *
 * 对应原包 `cn.etouch.ecalendar.tools.notice` 系列页面
 * （`FestivalDetailActivity`、`FestivalAndJieqiFragment`、`JieqiListFragment`）。
 * 节日表为通用民俗数据，本工程独立整理。
 */
object Festivals {

    private val lunarFestivals = mapOf(
        (1 to 1) to "春节",
        (1 to 15) to "元宵节",
        (2 to 2) to "龙抬头",
        (3 to 3) to "上巳节",
        (5 to 5) to "端午节",
        (7 to 7) to "七夕",
        (7 to 15) to "中元节",
        (8 to 15) to "中秋节",
        (9 to 9) to "重阳节",
        (12 to 8) to "腊八节",
        (12 to 23) to "小年",
    )

    private val solarFestivals = mapOf(
        (1 to 1) to "元旦",
        (2 to 14) to "情人节",
        (3 to 8) to "妇女节",
        (3 to 12) to "植树节",
        (4 to 1) to "愚人节",
        (5 to 1) to "劳动节",
        (5 to 4) to "青年节",
        (6 to 1) to "儿童节",
        (7 to 1) to "建党节",
        (8 to 1) to "建军节",
        (9 to 10) to "教师节",
        (10 to 1) to "国庆节",
        (10 to 31) to "万圣夜",
        (11 to 11) to "光棍节",
        (12 to 25) to "圣诞节",
    )

    fun of(
        date: LocalDate,
        lunarMonth: Int,
        lunarDay: Int,
        isLeap: Boolean,
        solarTerm: String,
    ): String {
        val list = mutableListOf<String>()
        if (!isLeap) {
            lunarFestivals[lunarMonth to lunarDay]?.let { list += it }
        }
        solarFestivals[date.monthValue to date.dayOfMonth]?.let { list += it }
        floating(date)?.let { list += it }
        if (solarTerm == "清明") list += "清明节"
        if (solarTerm == "冬至") list += "冬至"
        if (isLunarNewYearEve(date)) list += "除夕"
        return list.joinToString("·")
    }

    /** 除夕：农历腊月最后一天 */
    private fun isLunarNewYearEve(date: LocalDate): Boolean {
        val lunar = LunarCalendar.solarToLunar(date)
        if (lunar.lunarMonth != 12) return false
        val tomorrow = LunarCalendar.solarToLunar(date.plusDays(1))
        return tomorrow.lunarMonth == 1 && tomorrow.lunarDay == 1
    }

    /** 按星期计算的浮动节日 */
    private fun floating(date: LocalDate): String? = when {
        date.monthValue == 5 && date.dayOfWeek.value == 7 && date.dayOfMonth <= 14 -> "母亲节"
        date.monthValue == 6 && date.dayOfWeek.value == 7 && date.dayOfMonth in 15..21 -> "父亲节"
        date.monthValue == 11 && date.dayOfWeek.value == 4 && date.dayOfMonth >= 22 -> "感恩节"
        else -> null
    }

    /** 某年的全部节日（用于「节日」列表页） */
    fun ofYear(year: Int): List<Triple<LocalDate, String, Boolean>> {
        val result = mutableListOf<Triple<LocalDate, String, Boolean>>()
        solarFestivals.forEach { (md, name) ->
            result += Triple(LocalDate.of(year, md.first, md.second), name, true)
        }
        // 农历节日按农历年遍历
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        var d = start
        while (!d.isAfter(end)) {
            val lunar = LunarCalendar.solarToLunar(d)
            if (!lunar.isLeapMonth) lunarFestivals[lunar.lunarMonth to lunar.lunarDay]?.let {
                result += Triple(d, it, false)
            }
            if (lunar.solarTerm.isNotEmpty()) result += Triple(d, lunar.solarTerm, true)
            d = d.plusDays(1)
        }
        // 母亲节 / 父亲节 / 感恩节
        result += Triple(
            LocalDate.of(year, 5, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, java.time.DayOfWeek.SUNDAY)),
            "母亲节", true,
        )
        result += Triple(
            LocalDate.of(year, 6, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, java.time.DayOfWeek.SUNDAY)),
            "父亲节", true,
        )
        result += Triple(
            LocalDate.of(year, 11, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, java.time.DayOfWeek.THURSDAY)),
            "感恩节", true,
        )
        return result.sortedBy { it.first }
    }
}
