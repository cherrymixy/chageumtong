package com.pure.chageumtong.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Figma 프레임 1700×1184 → 실제 앱 영역 1682×1210dp(Pleos 오른쪽 패널, 실측). 세로 기준, 남는/모자란 가로는 좌측 메인 카드가 흡수 (PRD §10)
const val FIG_SCALE = 1210f / 1184f

/** Figma px 값을 화면 dp로 변환. 레이아웃 수치는 전부 이걸 거친다. */
fun fig(px: Number): Dp = (px.toFloat() * FIG_SCALE).dp

/** Figma 글자 크기 px → sp */
fun figSp(px: Number): TextUnit = (px.toFloat() * FIG_SCALE).sp

/** 차량 가독성 최소값 (PRD §7) */
val MIN_TOUCH = 76.dp
val BODY = 24.sp

/** Pleos UI 가이드 Toast: 앱 영역 아래, 양옆·아래 여백 24 (dp 기준이라 fig() 아님) */
val POPUP_EDGE = 24.dp

// Figma 레이아웃 토큰 (px → fig())
object D {
    val ScreenPad = fig(30)
    val ColumnGap = fig(23)
    val CardGap = fig(19)
    val SmallCardGap = fig(26)
    val CardPad = fig(36)
    val RightColumn = fig(612)
    val ContentHeight = fig(1023) // 메인 카드·분석 프레임 높이
    val BottomGap = fig(20) // 카드 ↔ 탭바/확인 버튼
    val BottomBarHeight = fig(85)

    val RadiusScreen = fig(40)
    val RadiusCard = fig(24)
    val RadiusButton = fig(19)
    val RadiusBar = fig(8)
    val RadiusRail = fig(6)
    val StrokeCard = fig(1)
}
