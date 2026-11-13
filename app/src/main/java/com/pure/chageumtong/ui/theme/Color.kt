package com.pure.chageumtong.ui.theme

import androidx.compose.ui.graphics.Color

// Figma 95:2142(홈) · 95:2638/95:2689(분석)에서 추출 (STEP 6-A)
val Primary = Color(0xFF00AAFF)
val PrimaryDark = Color(0xFF0689CA)
val PrimaryTint = Color(0x1A00AAFF) // 10%
val Bg = Color(0xFFEDEEF0)
val Surface = Color(0xFFFDFEFF)
val SurfaceWarm = Color(0xFFFFFFFD) // 홈 메인 카드
val Stroke = Color(0xFFC2C2C2)
val Track = Color(0xFFE8E9E9)
val ChipOff = Color(0xFFE5E5E5)
val Warn = Color(0xFFF46E31) // 홈 레일 미적립
val Alert = Color(0xFFFE9126) // 분석 칩·말풍선·지도 미적립
val AlertTint = Color(0xFFFFF6ED)
val BubbleStroke = Color(0xFF858585)
val AdBadge = Color(0x52000000) // 32%

// 글자
val TextTitle = Color(0xFF000000)
val TextNumber = Color(0xFF2B2B2B)
val TextStat = Color(0xFF353535)
val TextUnit = Color(0xFF434343) // 50% 투명도로 사용
val TextLabel = Color(0xFF616161)
val TextTertiary = Color(0xFF5A6274) // Figma 변수 text/tertiary
val TextHint = Color(0xFF8D8D8D)
val TextLocation = Color(0xFF999999)
val TextBody = Color(0xFF404040)
val TextChipOff = Color(0xFF2F2F2F)
val TextSection = Color(0xFF3B3B3B)
val OnPrimary = Color(0xFFFDFEFF)

// STEP 5 회색박스 화면(지도·상점·내역)이 쓰는 이름 → Figma 토큰으로 연결 (STEP 8에서 정리)
val Blue = Primary
val Orange = Alert
val BoxGray = Track
val CardBg = Surface
val TextSub = TextTertiary
