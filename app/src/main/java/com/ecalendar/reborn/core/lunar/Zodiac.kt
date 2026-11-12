package com.ecalendar.reborn.core.lunar

import java.time.LocalDate

/**
 * 星座与生肖速查 / 配对。
 *
 * 对应原包 `cn.etouch.ecalendar.tools.astro`（`AstroPairActivity`、
 * `AstroPairNextActivity`、`HoroscopeAdapter`）与 `module.calculate` 中的生肖入口。
 * 配对分值为本工程自建的可解释评分（按四象属性 + 元素关系计算），
 * 不使用原包的付费内容。
 */
object Zodiac {

    private val constellations = listOf(
        "摩羯座" to (12 to 22), "水瓶座" to (1 to 20), "双鱼座" to (2 to 19),
        "白羊座" to (3 to 21), "金牛座" to (4 to 20), "双子座" to (5 to 21),
        "巨蟹座" to (6 to 22), "狮子座" to (7 to 23), "处女座" to (8 to 23),
        "天秤座" to (9 to 23), "天蝎座" to (10 to 24), "射手座" to (11 to 23),
    )

    private val elements = mapOf(
        "白羊座" to "火", "狮子座" to "火", "射手座" to "火",
        "金牛座" to "土", "处女座" to "土", "摩羯座" to "土",
        "双子座" to "风", "天秤座" to "风", "水瓶座" to "风",
        "巨蟹座" to "水", "天蝎座" to "水", "双鱼座" to "水",
    )

    private val traits = mapOf(
        "白羊座" to "热情直率、行动力强、喜欢开疆拓土",
        "金牛座" to "稳健务实、重视安全感、耐力出众",
        "双子座" to "思维敏捷、好奇心强、沟通灵活",
        "巨蟹座" to "情感细腻、重视家庭、记忆力好",
        "狮子座" to "自信慷慨、表现力强、重视荣誉",
        "处女座" to "细致严谨、擅长分析、追求完善",
        "天秤座" to "重平衡与美感、善协调、忌冲突",
        "天蝎座" to "专注深刻、洞察力强、情感强烈",
        "射手座" to "乐观自由、爱探索、直言不讳",
        "摩羯座" to "目标坚定、责任感强、耐得住长期投入",
        "水瓶座" to "独立思考、重视自由、创新意识强",
        "双鱼座" to "共情力强、想象力丰富、感性主导",
    )

    fun constellationOf(month: Int, day: Int): String {
        val md = month * 100 + day
        var result = "摩羯座"
        for ((name, start) in constellations) {
            if (md >= start.first * 100 + start.second) result = name
        }
        // 1 月 1–19 日仍属摩羯座，上面的循环已正确处理（摩羯起点 12-22 在末尾）
        return result
    }

    fun elementOf(constellation: String): String = elements[constellation] ?: ""

    fun traitOf(constellation: String): String = traits[constellation] ?: ""

    /** 星座配对：同象 90 分，同行 72，相冲 52，同座 88 */
    fun matchScore(a: String, b: String): Int {
        val ea = elementOf(a)
        val eb = elementOf(b)
        if (a == b) return 88
        if (ea == eb) return 90
        val cycle = mapOf("火" to "土", "土" to "风", "风" to "水", "水" to "火")
        return when {
            cycle[ea] == eb || cycle[eb] == ea -> 52
            else -> 72
        }
    }

    val zodiacs = ChineseNames.zodiacs

    private val zodiacTraits = mapOf(
        "鼠" to "机敏善变，社交灵活，善于积累",
        "牛" to "踏实坚韧，重承诺，节奏稳",
        "虎" to "果敢有魄力，喜欢主导，行动快",
        "兔" to "温和谨慎，注重细节，善避风险",
        "龙" to "自信有格局，追求成就，气场强",
        "蛇" to "理性深沉，洞察力佳，谋定后动",
        "马" to "自由奔放，执行力强，不喜束缚",
        "羊" to "柔和有耐心，重感情，审美好",
        "猴" to "聪明灵活，学习快，善于变通",
        "鸡" to "条理清晰，要求高，表达直接",
        "狗" to "忠诚可靠，守原则，重信义",
        "猪" to "憨厚实在，包容心强，福气稳",
    )

    fun zodiacTrait(zodiac: String): String = zodiacTraits[zodiac] ?: ""

    /** 六合 / 三合关系（生肖配对常用口径） */
    private val liuHe = mapOf(
        "鼠" to "牛", "牛" to "鼠", "虎" to "猪", "兔" to "狗",
        "龙" to "鸡", "蛇" to "猴", "马" to "羊", "羊" to "马",
        "猴" to "蛇", "鸡" to "龙", "狗" to "兔", "猪" to "虎",
    )

    private val sanHe = listOf(
        setOf("鼠", "龙", "猴"), setOf("牛", "蛇", "鸡"),
        setOf("虎", "马", "狗"), setOf("兔", "羊", "猪"),
    )

    fun zodiacMatch(a: String, b: String): Pair<Int, String> = when {
        liuHe[a] == b -> 95 to "六合：彼此补足，关系稳定长线"
        sanHe.any { a in it && b in it } -> 88 to "三合：价值观接近，配合顺畅"
        a == b -> 72 to "同肖：节奏相似，需错开主导权"
        else -> 60 to "无固定刑冲关系，靠沟通与边界维持"
    }

    fun zodiacOfYear(year: Int): String = ChineseNames.zodiacs[(year - 4) % 12]

    fun yearOfZodiac(zodiac: String, nearYear: Int): Int {
        val idx = ChineseNames.zodiacs.indexOf(zodiac)
        if (idx < 0) return nearYear
        var y = nearYear - (nearYear - 4) % 12 + idx
        while (y < nearYear - 6) y += 12
        return y
    }

    fun dateRangeOf(constellation: String): String = when (constellation) {
        "摩羯座" -> "12.22 - 01.19"
        "水瓶座" -> "01.20 - 02.18"
        "双鱼座" -> "02.19 - 03.20"
        "白羊座" -> "03.21 - 04.19"
        "金牛座" -> "04.20 - 05.20"
        "双子座" -> "05.21 - 06.21"
        "巨蟹座" -> "06.22 - 07.22"
        "狮子座" -> "07.23 - 08.22"
        "处女座" -> "08.23 - 09.22"
        "天秤座" -> "09.23 - 10.23"
        "天蝎座" -> "10.24 - 11.22"
        "射手座" -> "11.23 - 12.21"
        else -> ""
    }

    fun constellationOfDate(date: LocalDate): String = constellationOf(date.monthValue, date.dayOfMonth)
}
