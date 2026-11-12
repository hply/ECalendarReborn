package com.ecalendar.reborn.core.lunar

/**
 * 农历日期模型。
 *
 * 来源说明：字段划分与 `cn.etouch.ecalendar.nongliManager.CnNongLiManager`
 * （REA evidence `ev_a07e2264aed9a2888e103e4b5651c285bd0614b0d42b88ec3ed3218f14a66eaf`
 * 中的类清单）一致：农历年/月/日、是否闰月、该月大小、当年闰月、干支、生肖、节气。
 * 数值由本工程独立实现，不含原 APK 代码。
 */
data class LunarDate(
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeapMonth: Boolean,
    /** 该农历月的大小：30 或 29 天 */
    val monthDays: Int,
    /** 当年闰月，0 表示无闰月 */
    val leapMonth: Int,
    /** 年的干支，如「甲子」 */
    val yearGanZhi: String,
    /** 生肖，如「鼠」 */
    val zodiac: String,
    /** 当日节气名，空串表示当日无节气 */
    val solarTerm: String,
    /** 当日农历节日或公历节日，可多个，用「·」连接 */
    val festivals: String,
) {
    val monthName: String
        get() = ChineseNames.lunarMonths[lunarMonth - 1].let { if (isLeapMonth) "闰$it" else it }

    val dayName: String
        get() = ChineseNames.lunarDays[lunarDay - 1]

    /** 「二〇二五年九月初九」这类完整写法 */
    val fullName: String
        get() = "${ChineseNames.yearName(lunarYear)}年$monthName$dayName"

    /** 日历格子里的短显示：初一显示月份名，其余显示日名，有节气优先显示节气 */
    val shortLabel: String
        get() = when {
            solarTerm.isNotEmpty() -> solarTerm
            lunarDay == 1 -> monthName
            else -> dayName
        }
}

internal object ChineseNames {
    val lunarMonths = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月",
    )
    val lunarDays = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十",
    )
    private val digits = listOf("〇", "一", "二", "三", "四", "五", "六", "七", "八", "九")

    fun yearName(year: Int): String = year.toString().map { digits[it - '0'] }.joinToString("")

    val gan = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val zhi = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val zodiacs = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    val numbers = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二")
}
