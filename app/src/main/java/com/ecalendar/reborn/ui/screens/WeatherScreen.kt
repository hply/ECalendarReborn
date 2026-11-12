package com.ecalendar.reborn.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecalendar.reborn.data.WeatherBundle
import com.ecalendar.reborn.data.WeatherRepository
import com.ecalendar.reborn.ui.components.SectionCard
import com.ecalendar.reborn.ui.components.SectionTitle

/**
 * 天气页：当前天气 + 7 天预报。
 * 对应原包 `module.weather.ui` 系列页面（WeatherForecastActivity、Weather15dayDetailActivity）。
 * 数据源为 Open-Meteo 公开接口，不接任何广告/统计 SDK。
 */
@Composable
fun WeatherScreen() {
    var city by remember { mutableStateOf(WeatherRepository.presetCities.first()) }
    var bundle by remember { mutableStateOf<WeatherBundle?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }

    LaunchedEffect(city, reloadTick) {
        loading = true
        error = null
        WeatherRepository.fetch(city.first, city.second, city.third)
            .onSuccess { bundle = it; error = null }
            .onFailure { error = it.message ?: "拉取失败" }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                WeatherRepository.presetCities.take(6).forEach { c ->
                    AssistChip(
                        onClick = { city = c },
                        label = { Text(if (city.first == c.first) "● ${c.first}" else c.first) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                WeatherRepository.presetCities.drop(6).forEach { c ->
                    AssistChip(
                        onClick = { city = c },
                        label = { Text(if (city.first == c.first) "● ${c.first}" else c.first) },
                    )
                }
            }
        }
        if (loading) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        error?.let { msg ->
            item {
                SectionCard("天气未能加载") {
                    Column {
                        Text(msg, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(6.dp))
                        Button(onClick = { reloadTick += 1 }) { Text("重试") }
                    }
                }
            }
        }
        bundle?.let { data ->
            item {
                SectionCard("${data.now.city} 当前") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(WeatherRepository.emoji(data.now.weatherCode), style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("${data.now.temperature}°C  ${data.now.description}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "体感 ${data.now.apparent}°C · 湿度 ${data.now.humidity}% · 风速 ${data.now.windSpeed} km/h",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item { SectionTitle("未来 7 天") }
            items(data.days) { day ->
                SectionCard(title = "${day.date.monthValue}月${day.date.dayOfMonth}日 · ${WeatherRepository.emoji(day.weatherCode)} ${day.description}") {
                    Text("${day.min}°C ~ ${day.max}°C", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
