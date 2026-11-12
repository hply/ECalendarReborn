package com.ecalendar.reborn.core.lunar

import java.time.LocalDate

/**
 * 干支、生肖、纳音、五行。
 *
 * 对应原包 `CnNongLiManager.cyclical` / `cyclicalm` / `AnimalsYear` /
 * `calDayTimeCyclical`（时辰干支）以及 `Gan` / `Zhi` / `Animals` 静态表。
 * 日柱以 1949-10-01（甲子日，通用历法锚点）为基准做 60 甲子循环。
 */
object Ganzhi {

    private val GAN = ChineseNames.gan
    private val ZHI = ChineseNames.zhi

    /** 60 甲子锚点：1949-10-01 为甲子日 */
    private val DAY_ANCHOR: LocalDate = LocalDate.of(1949, 10, 1)

    fun yearGanZhi(lunarYear: Int): String {
        val idx = (lunarYear - 4) % 60
        val g = GAN[idx % 10]
        val z = ZHI[idx % 12]
        return "$g$z"
    }

    fun zodiac(lunarYear: Int): String = ChineseNames.zodiacs[(lunarYear - 4) % 12]

    /** 日天干 */
    fun dayGan(date: LocalDate): String = dayGanZhi(date).substring(0, 1)

    /** 日地支 */
    fun dayZhi(date: LocalDate): String = dayGanZhi(date).substring(1)

    fun dayGanZhi(date: LocalDate): String {
        val days = java.time.temporal.ChronoUnit.DAYS.between(DAY_ANCHOR, date)
        val idx = ((days % 60 + 60) % 60).toInt()
        return GAN[idx % 10] + ZHI[idx % 12]
    }

    /** 日柱在 60 甲子中的序号 0..59 */
    fun dayIndex(date: LocalDate): Int {
        val days = java.time.temporal.ChronoUnit.DAYS.between(DAY_ANCHOR, date)
        return ((days % 60 + 60) % 60).toInt()
    }

    /** 时干支：给出时辰地支序号（0=子）与该日天干 */
    fun hourGanZhi(dayGanIdx: Int, hourBranch: Int): String {
        // 甲己还加甲、乙庚丙作初、丙辛从戊起、丁壬庚子居、戊癸何方发、壬子是真途
        val start = intArrayOf(0, 2, 4, 6, 8)[dayGanIdx % 5]
        val ganIdx = (start + hourBranch) % 10
        return GAN[ganIdx] + ZHI[hourBranch]
    }

    /** 时辰地支序号：23:00–00:59 为子时 */
    fun hourBranchOf(hour: Int): Int = ((hour + 1) / 2) % 12

    val hourLabels = listOf(
        "子时 23-01", "丑时 01-03", "寅时 03-05", "卯时 05-07",
        "辰时 07-09", "巳时 09-11", "午时 11-13", "未时 13-15",
        "申时 15-17", "酉时 17-19", "戌时 19-21", "亥时 21-23",
    )

    /** 六十甲子纳音 */
    private val naYinTable = listOf(
        "海中金", "炉中火", "大林木", "路旁土", "剑锋金", "山头火",
        "涧下水", "城头土", "白蜡金", "杨柳木", "泉中水", "屋上土",
        "霹雳火", "松柏木", "长流水", "砂中金", "山下火", "平地木",
        "壁上土", "金箔金", "覆灯火", "天河水", "大驿土", "钗钏金",
        "桑柘木", "大溪水", "沙中土", "天上火", "石榴木", "大海水",
    )

    fun naYin(ganZhi: String): String {
        val g = GAN.indexOf(ganZhi.substring(0, 1))
        val z = ZHI.indexOf(ganZhi.substring(1, 2))
        if (g < 0 || z < 0) return ""
        // 由干支反推 60 序号：同余方程求解
        for (i in 0 until 60) {
            if (i % 10 == g && i % 12 == z) return naYinTable[i / 2]
        }
        return ""
    }

    fun ganElement(gan: String): String = when (gan) {
        "甲", "乙" -> "木"
        "丙", "丁" -> "火"
        "戊", "己" -> "土"
        "庚", "辛" -> "金"
        "壬", "癸" -> "水"
        else -> ""
    }

    fun zhiElement(zhi: String): String = when (zhi) {
        "子", "亥" -> "水"
        "寅", "卯" -> "木"
        "巳", "午" -> "火"
        "申", "酉" -> "金"
        "辰", "戌", "丑", "未" -> "土"
        else -> ""
    }

    /** 旬空（空亡）两支 */
    fun xunKong(dayIdx: Int): String {
        val head = dayIdx - dayIdx % 10
        val kong = ((head + 10) % 12)
        return ZHI[kong] + ZHI[(kong + 1) % 12]
    }

    /** 年月日时四柱 */
    fun fourPillars(date: LocalDate, hour: Int): List<String> {
        val yearPillar = yearGanZhiOfSolarYear(date)
        val monthPillar = monthPillar(date)
        val dayPillar = dayGanZhi(date)
        val hourPillar = hourGanZhi(dayIndex(date) % 10, hourBranchOf(hour))
        return listOf(yearPillar, monthPillar, dayPillar, hourPillar)
    }

    /** 月柱：以节气分界，子月自大雪起，寅月自立春起 */
    fun monthPillar(date: LocalDate): String {
        val table = listOf(
            Triple(date.year - 1, "大雪", 0),
            Triple(date.year, "小寒", 1),
            Triple(date.year, "立春", 2),
            Triple(date.year, "惊蛰", 3),
            Triple(date.year, "清明", 4),
            Triple(date.year, "立夏", 5),
            Triple(date.year, "芒种", 6),
            Triple(date.year, "小暑", 7),
            Triple(date.year, "立秋", 8),
            Triple(date.year, "白露", 9),
            Triple(date.year, "寒露", 10),
            Triple(date.year, "立冬", 11),
        )
        var branch = 1 // 丑月兜底
        for ((y, term, b) in table) {
            val d = SolarTerm.dateOf(y, term) ?: continue
            if (date >= d) branch = b
        }
        val yearGanIdx = GAN.indexOf(yearGanZhiOfSolarYear(date).substring(0, 1))
        // 五虎遁：甲己之年丙作首，乙庚之岁戊为头，丙辛必定寻庚起，丁壬壬位顺行流，戊癸何方发，甲寅之上好追求
        val startGan = intArrayOf(2, 4, 6, 8, 0)[yearGanIdx % 5]
        val offset = (branch - 2 + 12) % 12
        val ganIdx = (startGan + offset) % 10
        return GAN[ganIdx] + ZHI[branch]
    }

    private fun yearGanZhiOfSolarYear(date: LocalDate): String {
        val lichun = SolarTerm.dateOf(date.year, "立春")
        val solarYear = if (lichun != null && date < lichun) date.year - 1 else date.year
        val idx = (solarYear - 4) % 60
        return GAN[idx % 10] + ZHI[idx % 12]
    }
}
