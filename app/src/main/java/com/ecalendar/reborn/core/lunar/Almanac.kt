package com.ecalendar.reborn.core.lunar

import java.time.LocalDate

/**
 * 黄历（老黄历）推算。
 *
 * 复刻对象：原包 `cn.etouch.ecalendar.tools.almanac` 下的
 * `AlmanacModernChineseActivity`、`QueryYiJiActivity`、`TimeJiXiongActivity`、
 * `ChooseDayActivity`，以及 `CnNongLiManager.calChongSha` / `calDayTimeCyclical`。
 *
 * 说明：宜忌为「建除十二神 + 黄道黑道 + 神煞」规则推导结果，属通书通行规则；
 * 原 APK 中由服务端下发的部分（如逐日长文、宜忌细目）不在静态证据范围内，
 * 本工程不做臆造。此处输出的是可离线复算的规则化黄历。
 */
data class AlmanacDay(
    val date: LocalDate,
    val lunar: LunarDate,
    val yearGanZhi: String,
    val monthGanZhi: String,
    val dayGanZhi: String,
    val naYin: String,
    val element: String,
    val zodiac: String,
    val constellation: String,
    val jianChu: String,
    val zhiShen: String,
    val isYellowAuspicious: Boolean,
    val chong: String,
    val sha: String,
    val pengZu: String,
    val xunKong: String,
    val yi: List<String>,
    val ji: List<String>,
    val jiShen: List<String>,
    val xiongSha: List<String>,
    val hours: List<AlmanacHour>,
    val festival: String,
    val solarTerm: String,
)

data class AlmanacHour(
    val branch: Int,
    val label: String,
    val ganZhi: String,
    val godName: String,
    val auspicious: Boolean,
    val yi: List<String>,
    val ji: List<String>,
)

object Almanac {

    private val jianChuNames = listOf(
        "建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭",
    )

    /** 建除十二神的宜忌模板 */
    private val jianChuYiJi: Map<String, Pair<List<String>, List<String>>> = mapOf(
        "建" to (listOf("祈福", "出行", "上任", "见贵", "上书", "纳财") to listOf("动土", "破土", "安葬", "开仓")),
        "除" to (listOf("扫舍", "治病", "解除", "沐浴", "祭祀") to listOf("赴任", "诉讼", "开市", "出行")),
        "满" to (listOf("祭祀", "祈福", "开市", "交易", "立券", "纳财") to listOf("安葬", "服药", "诉讼")),
        "平" to (listOf("嫁娶", "祭祀", "修饰垣墙", "修造") to listOf("开市", "出行", "动土", "栽种")),
        "定" to (listOf("订盟", "纳采", "嫁娶", "安床", "修造") to listOf("诉讼", "出行", "移徙", "开仓")),
        "执" to (listOf("祭祀", "捕捉", "狩猎", "修造", "嫁娶") to listOf("开市", "出行", "移徙", "安葬")),
        "破" to (listOf("求医", "治病", "破屋", "坏垣") to listOf("嫁娶", "开市", "出行", "安葬", "动土")),
        "危" to (listOf("祭祀", "安床", "纳财", "捕捉") to listOf("嫁娶", "出行", "移徙", "动土")),
        "成" to (listOf("嫁娶", "开市", "交易", "出行", "求财", "纳财", "安床", "祈福") to listOf("诉讼", "破土", "安葬")),
        "收" to (listOf("纳财", "收账", "进人口", "纳畜", "捕捉") to listOf("开市", "出行", "嫁娶", "动土")),
        "开" to (listOf("开市", "交易", "出行", "上任", "嫁娶", "修造", "祈福") to listOf("安葬", "动土", "破土")),
        "闭" to (listOf("祭祀", "安葬", "收账", "塞穴") to listOf("开市", "出行", "嫁娶", "动土", "开渠")),
    )

    private val twelveGods = listOf(
        "青龙", "明堂", "天刑", "朱雀", "金匮", "天德",
        "白虎", "玉堂", "天牢", "玄武", "司命", "勾陈",
    )

    private val twelveGodYiJi: Map<String, Pair<List<String>, List<String>>> = mapOf(
        "青龙" to (listOf("祈福", "出行", "嫁娶", "开市", "求财") to listOf("诉讼")),
        "明堂" to (listOf("祭祀", "纳财", "上任", "修造", "嫁娶") to listOf("破土", "安葬")),
        "天刑" to (listOf("祭祀", "祈福") to listOf("嫁娶", "开市", "出行", "诉讼")),
        "朱雀" to (listOf("祭祀") to listOf("出行", "嫁娶", "开市", "动土")),
        "金匮" to (listOf("嫁娶", "开市", "交易", "求财", "纳财") to listOf("安葬", "诉讼")),
        "天德" to (listOf("祈福", "嫁娶", "出行", "开市", "修造", "安葬") to listOf("诉讼")),
        "白虎" to (listOf("求医", "治病", "祭祀") to listOf("嫁娶", "出行", "开市", "动土")),
        "玉堂" to (listOf("开市", "交易", "出行", "嫁娶", "安床") to listOf("动土", "破土")),
        "天牢" to (listOf("祭祀", "修造") to listOf("出行", "诉讼", "开市")),
        "玄武" to (listOf("祭祀") to listOf("嫁娶", "开市", "出行", "纳财")),
        "司命" to (listOf("祈福", "嫁娶", "开市", "出行", "安葬") to listOf("诉讼")),
        "勾陈" to (listOf("祭祀", "修造") to listOf("嫁娶", "出行", "开市", "动土")),
    )

    private val pengZuGan = listOf(
        "甲不开仓财物耗散", "乙不栽植千株不长", "丙不修灶必见灾殃", "丁不剃头头必生疮",
        "戊不受田田主不祥", "己不破券二比并亡", "庚不经络织机虚张", "辛不合酱主人不尝",
        "壬不泱水更难提防", "癸不词讼理弱敌强",
    )

    private val pengZuZhi = listOf(
        "子不问卜自惹祸殃", "丑不冠带主不还乡", "寅不祭祀神鬼不尝", "卯不穿井水泉不香",
        "辰不哭泣必主重丧", "巳不远行财物伏藏", "午不苫盖屋主更张", "未不服药毒气入肠",
        "申不安床鬼祟入房", "酉不会客醉坐颠狂", "戌不吃犬作怪上床", "亥不嫁娶不利新郎",
    )

    /** 三煞方向：与日支三合局对应，顺序为 东 北 西 南 */
    private val shaDirections = listOf("东", "北", "西", "南")

    /** 时辰青龙起点：子午临申、丑未戌上、寅申加子、卯酉居寅、辰戌龙位、巳亥午上 */
    private val hourGodStart = intArrayOf(8, 10, 0, 2, 4, 6, 8, 10, 0, 2, 4, 6)

    fun of(date: LocalDate): AlmanacDay {
        val lunar = LunarCalendar.solarToLunar(date)
        val dayIdx = Ganzhi.dayIndex(date)
        val dayGz = Ganzhi.dayGanZhi(date)
        val yearGz = Ganzhi.yearGanZhi(lunar.lunarYear)
        val monthGz = Ganzhi.monthPillar(date)

        val monthZhiIdx = ChineseNames.zhi.indexOf(monthGz.substring(1, 2))
        val dayZhiIdx = dayIdx % 12
        // 建除：以月建为「建」，日支与月支同者为建，顺推十二神
        val jianOffset = ((dayZhiIdx - monthZhiIdx + 12) % 12)
        val jianChu = jianChuNames[jianOffset]

        // 黄道黑道值神：按月建起青龙（子月申日起、丑月戌日起、寅月子日起……）
        val godStart = hourGodStart[monthZhiIdx]
        val godIdx = ((dayZhiIdx - godStart + 12) % 12)
        val zhiShen = twelveGods[godIdx]

        val chongBranch = (dayZhiIdx + 6) % 12
        val chong = buildString {
            append("冲")
            append(ChineseNames.zodiacs[chongBranch])
            append("(")
            append(ChineseNames.gan[(dayIdx + 4) % 10])
            append(ChineseNames.zhi[chongBranch])
            append(")")
        }
        val sha = "煞" + shaDirections[(dayIdx + 3) % 4]

        val pengZu = pengZuGan[dayIdx % 10] + "，" + pengZuZhi[dayZhiIdx]

        val yiBase = jianChuYiJi[jianChu]!!.first
        val jiBase = jianChuYiJi[jianChu]!!.second
        val godYiJi = twelveGodYiJi[zhiShen]!!
        val yi = (yiBase + godYiJi.first).distinct().take(6)
        val ji = (jiBase + godYiJi.second).distinct().take(6)

        val jiShen = buildList {
            if (twelveGods[godIdx] in listOf("青龙", "明堂", "金匮", "天德", "玉堂", "司命")) {
                add("天德贵人"); add("月德")
            }
            if (jianChu in listOf("成", "开", "定")) add("三合")
            if (jianChu == "满") add("天医")
        }.distinct()
        val xiongSha = buildList {
            if (twelveGods[godIdx] in listOf("天刑", "朱雀", "白虎", "天牢", "玄武", "勾陈")) {
                add("劫煞"); add("灾煞")
            }
            if (jianChu == "破") add("月破")
            if (jianChu == "闭") add("灾煞")
            if (dayZhiIdx == monthZhiIdx) add("月建")
        }.distinct()

        val dayGanIdx = dayIdx % 10
        val startBranch = hourGodStart[dayZhiIdx]
        val hours = (0..11).map { branch ->
            val god = twelveGods[((branch - startBranch + 12) % 12)]
            val auspicious = god in listOf("青龙", "明堂", "金匮", "天德", "玉堂", "司命")
            val rule = twelveGodYiJi[god]!!
            AlmanacHour(
                branch = branch,
                label = Ganzhi.hourLabels[branch],
                ganZhi = Ganzhi.hourGanZhi(dayGanIdx, branch),
                godName = god,
                auspicious = auspicious,
                yi = if (auspicious) rule.first.take(3) else emptyList(),
                ji = if (!auspicious) rule.second.take(3) else emptyList(),
            )
        }

        return AlmanacDay(
            date = date,
            lunar = lunar,
            yearGanZhi = yearGz,
            monthGanZhi = monthGz,
            dayGanZhi = dayGz,
            naYin = Ganzhi.naYin(dayGz),
            element = Ganzhi.ganElement(dayGz.substring(0, 1)),
            zodiac = lunar.zodiac,
            constellation = Zodiac.constellationOf(date.monthValue, date.dayOfMonth),
            jianChu = jianChu,
            zhiShen = zhiShen,
            isYellowAuspicious = zhiShen in listOf("青龙", "明堂", "金匮", "天德", "玉堂", "司命"),
            chong = chong,
            sha = sha,
            pengZu = pengZu,
            xunKong = Ganzhi.xunKong(dayIdx),
            yi = yi,
            ji = ji,
            jiShen = jiShen,
            xiongSha = xiongSha,
            hours = hours,
            festival = lunar.festivals,
            solarTerm = lunar.solarTerm,
        )
    }

    /** 择日：在区间内按要求筛选宜做某事的日期 */
    fun chooseDays(from: LocalDate, to: LocalDate, activity: String): List<AlmanacDay> {
        val result = mutableListOf<AlmanacDay>()
        var d = from
        while (!d.isAfter(to)) {
            val day = of(d)
            if (day.yi.any { it.contains(activity) }) result += day
            d = d.plusDays(1)
        }
        return result
    }
}
