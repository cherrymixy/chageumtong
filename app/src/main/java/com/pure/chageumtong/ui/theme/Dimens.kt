package com.pure.chageumtong.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Figma 프레임 1700×1184 → 에뮬레이터 2560×1440dp. 세로 기준으로 맞추고 남는 가로는 좌측 메인 카드가 흡수한다 (PRD §10)
const val FIG_SCALE = 1440f / 1184f

/** Figma px 값을 화면 dp로 변환. 레이아웃 수치는 전부 이걸 거친다. */
fun fig(px: Number): Dp = (px.toFloat() * FIG_SCALE).dp
