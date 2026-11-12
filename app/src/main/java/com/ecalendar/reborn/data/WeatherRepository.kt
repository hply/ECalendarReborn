package com.ecalendar.reborn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * 天气数据。
 *
 * 原包天气走自家服务端（`module.weather` 系列页面 + `com.amap` 定位），
 * 同时夹带广告 SDK。本工程改为直连公开气象接口 Open-Meteo（无需 AppKey、无 SDK），
 * 只保留「当前天气 + 未来 7 天」这件自研功能本身。
 */
data class WeatherNow(
    val city: String,
    val temperature: Int,
    val apparent: Int,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Int,
    val description: String,
)

data class WeatherDay(
    val date: LocalDate,
    val max: Int,
    val min: Int,
    val weatherCode: Int,
    val description: String,
)

data class WeatherBundle(
    val now: WeatherNow,
    val days: List<WeatherDay>,
    val fetchedAt: Long = System.currentTimeMillis(),
)

object WeatherRepository {

    suspend fun fetch(city: String, lat: Double, lon: Double): Result<WeatherBundle> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                    "&timezone=Asia%2FShanghai&forecast_days=7",
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(body)
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            val codes = daily.getJSONArray("weather_code")
            val maxs = daily.getJSONArray("temperature_2m_max")
            val mins = daily.getJSONArray("temperature_2m_min")
            val dates = daily.getJSONArray("time")
            val days = (0 until dates.length()).map { i ->
                val code = codes.getInt(i)
                WeatherDay(
                    date = LocalDate.parse(dates.getString(i)),
                    max = maxs.getDouble(i).toInt(),
                    min = mins.getDouble(i).toInt(),
                    weatherCode = code,
                    description = describe(code),
                )
            }
            val code = current.getInt("weather_code")
            WeatherBundle(
                now = WeatherNow(
                    city = city,
                    temperature = current.getDouble("temperature_2m").toInt(),
                    apparent = current.getDouble("apparent_temperature").toInt(),
                    weatherCode = code,
                    humidity = current.getInt("relative_humidity_2m"),
                    windSpeed = current.getDouble("wind_speed_10m").toInt(),
                    description = describe(code),
                ),
                days = days,
            )
        }
    }

    fun describe(code: Int): String = when (code) {
        0 -> "晴"
        1, 2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63, 65, 66, 67 -> "雨"
        71, 73, 75, 77 -> "雪"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95, 96, 99 -> "雷阵雨"
        else -> "未知"
    }

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌤️"
    }

    /** 内置常用城市，避免依赖定位 SDK；用户可自行添加 */
    val presetCities = listOf(
        Triple("北京", 39.9042, 116.4074),
        Triple("上海", 31.2304, 121.4737),
        Triple("广州", 23.1291, 113.2644),
        Triple("深圳", 22.5431, 114.0579),
        Triple("成都", 30.5728, 104.0668),
        Triple("杭州", 30.2741, 120.1551),
        Triple("西安", 34.3416, 108.9398),
        Triple("武汉", 30.5928, 114.3055),
        Triple("南京", 32.0603, 118.7969),
        Triple("重庆", 29.5630, 106.5516),
        Triple("哈尔滨", 45.8038, 126.5350),
        Triple("昆明", 25.0389, 102.7183),
        Triple("乌鲁木齐", 43.8256, 87.6168),
        Triple("拉萨", 29.6520, 91.1721),
    )
}
