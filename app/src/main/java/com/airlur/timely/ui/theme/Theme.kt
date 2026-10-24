package com.airlur.timely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val Primary = Color(0xFF2196F3)
val PrimaryDeep = Color(0xFF1976D2)
val PrimarySoft = Color(0x1F2196F3)

val BgLight = Color(0xFFF5F5F5)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceAltLight = Color(0xFFF3F3F3)
val InkLight = Color(0xFF333333)
val InkGray = Color(0xFF757175)

val BgDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val SurfaceAltDark = Color(0xFF171717)
val InkDark = Color(0xFFE0E0E0)

// 列表滑动操作色（与旧版一致）
val ActionPin = Color(0xFF9C27B0)
val ActionDetail = Color(0xFFF33C76)
val ActionTimeline = Color(0xFF2196F3)
val ActionEdit = Color(0xFFFF9800)
val ActionDelete = Color(0xFFF02419)
val ActionShare = Color(0xFF4CAF50)

// 事件卡片预设纯色（20）
val PresetColors = listOf(
    0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
    0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
    0xFF8BC34A, 0xFFCDDC39, 0xFFFF0000, 0xFF00FFFF, 0xFF607D8B,
    0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFFFFEB3B
).map { Color(it) }

// 事件卡片预设渐变（23 组，start→end，135°）
val PresetGradients = listOf(
    0xFFFF416C to 0xFFFF4B2B, 0xFFF85032 to 0xFFE73827, 0xFFF12711 to 0xFFF5AF19,
    0xFFFF8008 to 0xFFFFC837, 0xFFFFE000 to 0xFFFFBD00, 0xFFF7971E to 0xFFFFD200,
    0xFF11998E to 0xFF38EF7D, 0xFF00B09B to 0xFF96C93D, 0xFF2193B0 to 0xFF6DD5ED,
    0xFF4364F7 to 0xFF6FB1FC, 0xFF834D9B to 0xFFD04ED6, 0xFF8E2DE2 to 0xFF4A00E0,
    0xFFEE9CA7 to 0xFFFFDDE1, 0xFFFF6B6B to 0xFFFF8E8E, 0xFF1A2980 to 0xFF26D0CE,
    0xFFFF512F to 0xFFDD2476, 0xFF614385 to 0xFF516395, 0xFF02AABD to 0xFF00CDAC,
    0xFFFF5F6D to 0xFFFFC371, 0xFF396AFC to 0xFF2948FF, 0xFFCC2B5E to 0xFF753A88,
    0xFF42275A to 0xFF734B6D, 0xFF141E30 to 0xFF243B55
).map { Color(it.first) to Color(it.second) }

private val LightScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = PrimaryDeep,
    background = BgLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceAltLight,
    onSurfaceVariant = InkGray,
    error = Color(0xFFF02419)
)

private val DarkScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF195582),
    onPrimaryContainer = Color(0xFFBBDEFB),
    background = BgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceAltDark,
    onSurfaceVariant = Color(0xFF9A9A9A),
    error = Color(0xFFF02419)
)

/** 天数等数字统一等宽，与原型一致 */
val NumStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum"
)

fun fontFamily(name: String?): FontFamily = when (name) {
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    else -> FontFamily.Default
}

@Composable
fun TimelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
