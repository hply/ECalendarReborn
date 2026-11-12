package com.ecalendar.reborn.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ecalendar.reborn.ui.AppActions
import com.ecalendar.reborn.ui.screens.AlmanacScreen
import com.ecalendar.reborn.ui.screens.CalendarScreen
import com.ecalendar.reborn.ui.screens.DayDetailScreen
import com.ecalendar.reborn.ui.screens.ListsScreen
import com.ecalendar.reborn.ui.screens.SettingsScreen
import com.ecalendar.reborn.ui.screens.ToolsScreen
import com.ecalendar.reborn.ui.screens.WeatherScreen

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("calendar", "日历", Icons.Filled.CalendarMonth),
    Tab("almanac", "黄历", Icons.Filled.Today),
    Tab("lists", "清单", Icons.Filled.Checklist),
    Tab("tools", "工具", Icons.Filled.Handyman),
    Tab("settings", "我的", Icons.Filled.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(actions: AppActions) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("中华万年历 · 重建版") })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { navController.navigate(tab.route) { launchSingleTop = true } },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "calendar",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("calendar") {
                CalendarScreen(
                    actions = actions,
                    onOpenDayDetail = { epochDay -> navController.navigate("day/$epochDay") },
                    onOpenAlmanac = { navController.navigate("almanac") },
                )
            }
            composable("almanac") {
                AlmanacScreen(onBack = { navController.popBackStack() })
            }
            composable("lists") { ListsScreen(actions) }
            composable("tools") {
                ToolsScreen(onOpenWeather = { navController.navigate("weather") })
            }
            composable("settings") { SettingsScreen(actions) }
            composable("weather") { WeatherScreen() }
            composable(
                route = "day/{epochDay}",
                arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
            ) { entry ->
                val epochDay = entry.arguments?.getLong("epochDay") ?: 0L
                DayDetailScreen(actions = actions, epochDay = epochDay, onBack = { navController.popBackStack() })
            }
        }
    }
}
