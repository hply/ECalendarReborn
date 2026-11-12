package com.ecalendar.reborn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 皮肤定义，对应原包 `settings.skin` 下的换肤能力（原包通过服务端主题包下发，这里内置 4 套） */
data class Skin(
    val id: String,
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val container: Color,
    val surfaceTint: Color,
    val accentText: Color,
)

object Skins {
    val classic = Skin(
        id = "classic", label = "经典朱红",
        primary = Color(0xFFB23A2E), secondary = Color(0xFF8A6A4F),
        tertiary = Color(0xFF3C6E57), container = Color(0xFFFFE9E2),
        surfaceTint = Color(0xFFFFF7F2), accentText = Color(0xFF7A2A21),
    )
    val ink = Skin(
        id = "ink", label = "水墨黛青",
        primary = Color(0xFF2E4A5A), secondary = Color(0xFF5B6E78),
        tertiary = Color(0xFF7A6A55), container = Color(0xFFE4EDF2),
        surfaceTint = Color(0xFFF5F8FA), accentText = Color(0xFF1E333F),
    )
    val jade = Skin(
        id = "jade", label = "青瓷碧玉",
        primary = Color(0xFF3F7A62), secondary = Color(0xFF7A8F6A),
        tertiary = Color(0xFFB08A4A), container = Color(0xFFE6F1EA),
        surfaceTint = Color(0xFFF6FBF7), accentText = Color(0xFF275645),
    )
    val night = Skin(
        id = "night", label = "夜阑鎏金",
        primary = Color(0xFFD9B46A), secondary = Color(0xFF9C8258),
        tertiary = Color(0xFF7FA6B8), container = Color(0xFF2B2620),
        surfaceTint = Color(0xFF15120F), accentText = Color(0xFFEDD79A),
    )

    val all = listOf(classic, ink, jade, night)

    fun of(id: String): Skin = all.firstOrNull { it.id == id } ?: classic
}

private fun lightColors(skin: Skin) = lightColorScheme(
    primary = skin.primary,
    onPrimary = Color.White,
    primaryContainer = skin.container,
    onPrimaryContainer = skin.accentText,
    secondary = skin.secondary,
    tertiary = skin.tertiary,
    background = skin.surfaceTint,
    surface = Color.White,
    surfaceVariant = skin.container,
)

private fun darkColors(skin: Skin) = darkColorScheme(
    primary = if (skin.id == "night") skin.primary else skin.primary.copy(alpha = 0.85f),
    onPrimary = if (skin.id == "night") Color(0xFF241E14) else Color.White,
    primaryContainer = if (skin.id == "night") skin.container else skin.primary.copy(alpha = 0.24f),
    onPrimaryContainer = if (skin.id == "night") skin.accentText else Color(0xFFFDE7E2),
    secondary = skin.secondary,
    tertiary = skin.tertiary,
    background = Color(0xFF121110),
    surface = Color(0xFF1B1917),
    surfaceVariant = Color(0xFF262320),
)

private fun typography(scale: Float): Typography {
    val base = Typography()
    fun TextStyle.scaled() = copy(
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
    )
    return Typography(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled().copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled().copy(fontSize = (base.bodySmall.fontSize * scale * 0.95f).let { if (it < 10.sp) 10.sp else it }),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled(),
    )
}

@Composable
fun ECalendarTheme(
    skinId: String = "classic",
    fontScale: Float = 1.0f,
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val skin = Skins.of(skinId)
    MaterialTheme(
        colorScheme = if (dark) darkColors(skin) else lightColors(skin),
        typography = typography(fontScale),
        content = content,
    )
}
