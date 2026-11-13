package com.pure.chageumtong.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.pure.chageumtong.R

/** Figma의 Google Sans Flex 자리도 Pretendard로 대체 (승아 결정, 2026-11-13). OFL: docs/PRETENDARD_LICENSE.txt */
val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

/** Figma px, 행간 배수 → TextStyle (크기는 figSp로 화면 배율 적용) */
private fun style(px: Number, weight: FontWeight, lineHeight: Float = 1.3f, trackingPx: Float = 0f) = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = figSp(px),
    lineHeight = lineHeight.em,
    letterSpacing = if (trackingPx == 0f) TextStyle.Default.letterSpacing else (trackingPx / px.toFloat()).em,
)

object T {
    val Hero = style(158, FontWeight.SemiBold, 1.1f) // 16P
    val TotalNumber = style(77.5, FontWeight.SemiBold, 1.1f) // 14,560P
    val CardNumber = style(50.5, FontWeight.SemiBold, 1.1f) // 38.9km
    val Stat = style(40, FontWeight.SemiBold) // 홈 15.4
    val StatLarge = style(52, FontWeight.SemiBold) // 분석 17.4
    val StatUnit = style(24, FontWeight.SemiBold)
    val GaugeSmall = style(37.8, FontWeight.SemiBold)
    val GaugeLarge = style(73.9, FontWeight.SemiBold)
    val Section = style(26, FontWeight.SemiBold)
    val SectionBold = style(26, FontWeight.Bold, trackingPx = -1.04f)
    val Label = style(21, FontWeight.SemiBold)
    val Button = style(24, FontWeight.SemiBold, 1.1f)
    val ButtonLarge = style(29, FontWeight.Bold, 1.55f, -0.58f) // 주행 분석하고 적립하기
    // STEP 10: Pleos UI 가이드 Typography 하한 적용 — Label Small 20(짧은 라벨), Body Small 26/34(문장). Figma 값이 더 작던 것만 올림
    val Month = style(20, FontWeight.Normal)
    val MonthBold = style(20, FontWeight.SemiBold)
    val RailTick = style(20, FontWeight.Medium, 1.2f)
    val Caption = style(20, FontWeight.Normal)
    val CaptionBold = style(20, FontWeight.Bold)
    val Badge = style(20, FontWeight.Medium)
    val BadgeLarge = style(27.4, FontWeight.Medium)
    val Hint = style(26, FontWeight.Medium, 34f / 26f)
    val Chip = style(28, FontWeight.Bold, trackingPx = -0.56f)
    val BubbleTitle = style(52, FontWeight.Bold)
    val BubbleBody = style(26.4, FontWeight.Medium, 46f / 26.4f, -1.0556f)
    val BubbleLocation = style(20, FontWeight.SemiBold)
    val HeaderSub = style(26, FontWeight.SemiBold)
    val AdBadge = style(20, FontWeight.SemiBold, 1.1f)

    // STEP 8: 상점(95:2870) · 지도(95:2751)
    val ToggleOn = style(30.5, FontWeight.Bold)
    val ToggleOff = style(30.5, FontWeight.Medium)
    val PointsLabel = style(22, FontWeight.SemiBold)
    val ProductName = style(23.15, FontWeight.SemiBold)
    val ProductPrice = style(28, FontWeight.SemiBold)
    val SmallButton = style(21, FontWeight.SemiBold, 1.1f)
    val Search = style(26, FontWeight.Medium) // Pleos Body Small (Figma 20)
    val PlaceName = style(22, FontWeight.SemiBold)
    val PlaceSub = style(20, FontWeight.Medium)
    val PlaceMeta = style(20, FontWeight.Medium, 1.1f)

    // STEP 8: Figma에 없는 화면(구매 팝업·적립 내역)은 차량 가독성 기준 24sp 이상
    val DialogTitle = style(30, FontWeight.Bold)
    val DialogName = style(28, FontWeight.Bold)
    val DialogPrice = style(36, FontWeight.Bold)
    val DialogBody = style(26, FontWeight.Medium) // Pleos Body Small
    val RowTitle = style(26, FontWeight.SemiBold)
    val RowAmount = style(32, FontWeight.Bold)

    // 팝업·위젯 (Figma 97:2250). 프레임이 0.5배로 그려져 있어 px×2 (버튼 38→76 = MIN_TOUCH, 본문 14→28 ≥ Pleos 하한)
    val PopupTitle = style(48, FontWeight.SemiBold, trackingPx = -0.96f)
    val PopupSub = style(28, FontWeight.Medium, trackingPx = -0.56f)
    val PopupButton = style(31.1, FontWeight.SemiBold, trackingPx = -0.62f)
    val WidgetLabel = style(26, FontWeight.SemiBold, trackingPx = -0.52f)
    val WidgetNumber = style(96.5, FontWeight.SemiBold, 1.1f)
}

val Typography = Typography(
    bodyLarge = style(24, FontWeight.Normal),
    bodyMedium = style(24, FontWeight.Normal),
    labelLarge = style(24, FontWeight.SemiBold),
    titleLarge = style(32, FontWeight.Bold),
)
