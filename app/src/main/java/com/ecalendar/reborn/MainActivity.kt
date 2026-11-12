package com.ecalendar.reborn

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecalendar.reborn.data.AppSettings
import com.ecalendar.reborn.ui.AppActions
import com.ecalendar.reborn.ui.nav.AppNavHost
import com.ecalendar.reborn.ui.theme.ECalendarTheme

/**
 * 单 Activity 承载全部页面。
 * 对应原包 `cn.etouch.ecalendar.ECalendar`（引导与入口）与 `module.main.ui.MainActivity`。
 * 原包的多进程/多 Activity 结构在本工程改为 Navigation Compose 单栈。
 */
class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val actions = AppActions(this)
        setContent {
            val settings by actions.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
            ECalendarTheme(skinId = settings.skin, fontScale = settings.fontScale) {
                AppNavHost(actions = actions)
            }
        }
    }
}
