package com.ecalendar.reborn.core.lunar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 二十四节气与「数九 / 三伏」推算。
 *
 * 对应原包中 `cn.etouch.ecalendar.nongliManager.CnJieQiManager`
 * （方法 `getJieQi`、`getShuJiuOrShufu`、`isHaveShuJiuOrShufu`）。
 * 这里采用通行的节气分钟偏移近似式（基准 1900-01-06 02:05，东八区），
 * 覆盖 1900–2100 年，与常见万年历实现一致。
 */
object SolarTerm {

    val names = listOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
    )

    private val minuteOffsets = intArrayOf(
        0, 21208, 42467, 63836, 85337, 107014,
        128867, 150921, 173149, 195551, 218072, 240693,
        263343, 285989, 308563, 331033, 353350, 375494,
        397447, 419210, 440795, 462224, 483532, 504758,
    )

    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val base: Instant = LocalDate.of(1900, 1, 6).atTime(2, 5).atZone(zone).toInstant()
    private const val TROPICAL_YEAR_MS = 31556925974.7

    private val cache = HashMap<Int, Map<String, LocalDate>>()

    private fun termsOfYear(year: Int): Map<String, LocalDate> = cache.getOrPut(year) {
        val result = LinkedHashMap<String, LocalDate>(24)
        for (i in names.indices) {
            val ms = TROPICAL_YEAR_MS * (year - 1900) + minuteOffsets[i] * 60000.0
            val date = base.plusMillis(ms.toLong()).atZone(zone).toLocalDate()
            result[names[i]] = date
        }
        result
    }

    /** 某年某个节气的公历日期，找不到返回 null */
    fun dateOf(year: Int, term: String): LocalDate? = termsOfYear(year)[term]

    /** 某年全部节气日期 */
    fun all(year: Int): List<Pair<String, LocalDate>> =
        names.mapNotNull { name -> dateOf(year, name)?.let { name to it } }

    /** 指定公历日是否为节气日，是则返回节气名 */
    fun nameOf(date: LocalDate): String {
        val inYear = termsOfYear(date.year)
        inYear.entries.firstOrNull { it.value == date }?.let { return it.key }
        // 极少数年份节气会落在相邻年，检查前后一年
        for (y in intArrayOf(date.year - 1, date.year + 1)) {
            if (y !in 1900..2100) continue
            termsOfYear(y).entries.firstOrNull { it.value == date }?.let { return it.key }
        }
        return ""
    }

    /** 该日期属于「三九」的第几天（1..81），不属于数九期返回 0 */
    fun shuJiuDay(date: LocalDate): Int {
        val winter = dateOf(date.year - 1, "冬至") ?: return 0
        val days = java.time.temporal.ChronoUnit.DAYS.between(winter, date).toInt()
        return if (days in 0..80) days + 1 else 0
    }

    /** 三伏信息：入伏 / 中伏 / 末伏 的起止日期 */
    data class FuPeriod(val name: String, val start: LocalDate, val end: LocalDate)

    fun fuPeriods(year: Int): List<FuPeriod> {
        val summerSolstice = dateOf(year, "夏至") ?: return emptyList()
        val autumnStart = dateOf(year, "立秋") ?: return emptyList()
        val firstFu = nthGengDay(summerSolstice, 3)
        val midFu = firstFu.plusDays(10)
        val lastFu = nthGengDay(autumnStart, 1)
        return listOf(
            FuPeriod("初伏", firstFu, firstFu.plusDays(9)),
            FuPeriod("中伏", midFu, lastFu.minusDays(1)),
            FuPeriod("末伏", lastFu, lastFu.plusDays(9)),
        )
    }

    private fun nthGengDay(from: LocalDate, n: Int): LocalDate {
        var date = from
        var count = 0
        while (true) {
            if (Ganzhi.dayGan(date) == "庚") {
                count++
                if (count == n) return date
            }
            date = date.plusDays(1)
        }
    }
}
