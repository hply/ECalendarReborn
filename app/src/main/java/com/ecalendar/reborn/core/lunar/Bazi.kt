package com.ecalendar.reborn.core.lunar

import java.time.LocalDate

/**
 * 四柱（八字）排盘。
 *
 * 对应原包 `cn.etouch.ecalendar.tools.mc.MCActivity` / `MCResult` 与
 * `module.calculate` 的命理入口。方法族与 `CnNongLiManager` 的
 * `calDayTimeCyclical`（时辰干支）一致，此处做规则化实现。
 */
data class BaziChart(
    val date: LocalDate,
    val hourBranch: Int,
    val lunar: LunarDate,
    val pillars: List<String>,
    val pillarNames: List<String>,
    val naYin: List<String>,
    val elementCount: Map<String, Int>,
    val dayMaster: String,
    val xunKong: String,
    val zodiac: String,
)

object Bazi {

    private val pillarNames = listOf("年柱", "月柱", "日柱", "时柱")

    fun chart(date: LocalDate, hour: Int): BaziChart {
        val lunar = LunarCalendar.solarToLunar(date)
        val pillars = Ganzhi.fourPillars(date, hour)
        val naYin = pillars.map { Ganzhi.naYin(it) }
        val counts = mutableMapOf("木" to 0, "火" to 0, "土" to 0, "金" to 0, "水" to 0)
        for (p in pillars) {
            val ganEl = Ganzhi.ganElement(p.substring(0, 1))
            val zhiEl = Ganzhi.zhiElement(p.substring(1, 2))
            if (ganEl.isNotEmpty()) counts[ganEl] = counts[ganEl]!! + 1
            if (zhiEl.isNotEmpty()) counts[zhiEl] = counts[zhiEl]!! + 1
        }
        return BaziChart(
            date = date,
            hourBranch = Ganzhi.hourBranchOf(hour),
            lunar = lunar,
            pillars = pillars,
            pillarNames = pillarNames,
            naYin = naYin,
            elementCount = counts,
            dayMaster = Ganzhi.ganElement(pillars[2].substring(0, 1)),
            xunKong = Ganzhi.xunKong(Ganzhi.dayIndex(date)),
            zodiac = lunar.zodiac,
        )
    }

    /** 五行缺失提示（仅作参考，不做命理结论） */
    fun missingElements(chart: BaziChart): List<String> =
        chart.elementCount.filterValues { it == 0 }.keys.toList()
}
