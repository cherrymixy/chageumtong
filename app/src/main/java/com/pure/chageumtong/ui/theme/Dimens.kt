package com.pure.chageumtong.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Figma 프레임 1700×1184 → 실제 앱 영역 1682×1210dp(Pleos 오른쪽 패널, 실측). 세로 기준, 남는/모자란 가로는 좌측 메인 카드가 흡수 (PRD §10)
const val FIG_SCALE = 1210f / 1184f

/** Figma px 값을 화면 dp로 변환. 레이아웃 수치는 전부 이걸 거친다. */
fun fig(px: Number): Dp = (px.toFloat() * FIG_SCALE).dp
