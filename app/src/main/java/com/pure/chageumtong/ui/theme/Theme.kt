package com.pure.chageumtong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Bg,
    surface = Surface,
    onBackground = TextTitle,
    onSurface = TextTitle,
)

/** 차량 화면은 라이트 고정 (Figma 기준) */
@Composable
fun ChageumtongTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = Typography, content = content)
}
