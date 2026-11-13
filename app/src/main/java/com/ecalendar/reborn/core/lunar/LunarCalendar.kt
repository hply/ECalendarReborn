package com.ecalendar.reborn.core.lunar

import java.time.LocalDate

/**
 * 农历换算核心（公历 ⇄ 农历）。
 *
 * 算法与 `cn.etouch.ecalendar.nongliManager.CnNongLiManager` 暴露的方法族对应：
 * `calGongliToNongli`、`nongliToGongli`、`lYearDays`、`leapMonth`、`leapDays`、
 * `monthDays`、`calNongliByMonth`、`calNongliJiangeDays`、`AnimalsYear`、`cyclical`。
 * 编码方式采用业界通用的 1900–2100 年 `lunarInfo` 压缩表（一个 int 表示一年的
 * 闰月月份、闰月大小与 12 个朔望月大小），由本工程独立实现。
 */
object LunarCalendar {

    private const val MIN_YEAR = 1900
    private const val MAX_YEAR = 2100

    /** 1900–2100 年农历编码表 */
    private val lunarInfo = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520,
    )

    /** 基准日：1900-01-31 为农历 1900 年正月初一 */
    private val BASE_DATE: LocalDate = LocalDate.of(1900, 1, 31)

    fun isSupported(year: Int): Boolean = year in MIN_YEAR..MAX_YEAR

    /** 某农历年的闰月，0 表示无闰月 */
    fun leapMonth(year: Int): Int = lunarInfo[year - MIN_YEAR] and 0xf

    /** 某农历年闰月的天数，无闰月返回 0 */
    fun leapDays(year: Int): Int {
        if (leapMonth(year) == 0) return 0
        return if (lunarInfo[year - MIN_YEAR] and 0x10000 != 0) 30 else 29
    }

    /** 某农历年某月（非闰月）的天数 */
    fun monthDays(year: Int, month: Int): Int =
        if (lunarInfo[year - MIN_YEAR] and (0x10000 shr month) != 0) 30 else 29

    /** 某农历年总天数 */
    fun yearDays(year: Int): Int {
        var sum = 348
        var mask = 0x8000
        while (mask > 0x8) {
            if (lunarInfo[year - MIN_YEAR] and mask != 0) sum += 1
            mask = mask shr 1
        }
        return sum + leapDays(year)
    }

    /** 农历 → 公历；参数不合法时返回 null */
    fun lunarToSolar(year: Int, month: Int, day: Int, isLeap: Boolean = false): LocalDate? {
        if (!isSupported(year) || month !in 1..12) return null
        var offset = 0
        for (y in MIN_YEAR until year) offset += yearDays(y)
        val leap = leapMonth(year)
        for (m in 1 until month) {
            offset += monthDays(year, m)
            if (leap in 1..12 && m == leap) offset += leapDays(year)
        }
        if (isLeap && leap == month) offset += monthDays(year, month)
        val maxDay = if (isLeap && leap == month) leapDays(year) else monthDays(year, month)
        if (day !in 1..maxDay) return null
        return BASE_DATE.plusDays((offset + day - 1).toLong())
    }

    /**
     * 公历 → 农历（含节日）。
     *
     * 注意：节日聚合在 [Festivals] 内部，那里只能调用不含节日的 [raw]。
     * 若让 [Festivals] 回调本函数就会出现「solarToLunar → Festivals.of → solarToLunar」的
     * 无限递归（v10.0.10-reborn.1 启动崩溃即此问题，StackOverflowError）。
     */
    fun solarToLunar(date: LocalDate): LunarDate {
        val core = raw(date)
        val term = SolarTerm.nameOf(date)
        return LunarDate(
            lunarYear = core.year,
            lunarMonth = core.month,
            lunarDay = core.day,
            isLeapMonth = core.isLeap,
            monthDays = core.monthDays,
            leapMonth = core.leap,
            yearGanZhi = Ganzhi.yearGanZhi(core.year),
            zodiac = Ganzhi.zodiac(core.year),
            solarTerm = term,
            festivals = Festivals.of(date, core.month, core.day, core.isLeap, term),
        )
    }

    /** 农历原始换算结果（不含节日），供内部模块使用，绝不回调节日计算 */
    internal data class Raw(
        val year: Int,
        val month: Int,
        val day: Int,
        val isLeap: Boolean,
        val monthDays: Int,
        val leap: Int,
    )

    /** 只做公历→农历的纯算术换算，不做节日聚合，因此可安全地被 Festivals 调用 */
    internal fun raw(date: LocalDate): Raw {
        require(isSupported(date.year)) { "农历数据仅覆盖 1900–2100 年：$date" }
        var offset = java.time.temporal.ChronoUnit.DAYS.between(BASE_DATE, date).toInt()
        var year = MIN_YEAR
        while (year <= MAX_YEAR) {
            val days = yearDays(year)
            if (offset < days) break
            offset -= days
            year++
        }
        val leap = leapMonth(year)
        var month = 12
        var isLeap = false
        var resolved = false
        for (m in 1..12) {
            val regular = monthDays(year, m)
            if (offset < regular) {
                month = m
                isLeap = false
                resolved = true
                break
            }
            offset -= regular
            if (leap in 1..12 && m == leap) {
                val leapLen = leapDays(year)
                if (offset < leapLen) {
                    month = m
                    isLeap = true
                    resolved = true
                    break
                }
                offset -= leapLen
            }
        }
        require(resolved) { "农历换算越界：$date" }
        val safeMonth = month.coerceIn(1, 12)
        return Raw(
            year = year,
            month = safeMonth,
            day = offset + 1,
            isLeap = isLeap,
            monthDays = monthDays(year, safeMonth),
            leap = leap,
        )
    }
}
