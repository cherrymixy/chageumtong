package com.pure.chageumtong.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.pure.chageumtong.R
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Pretendard
import com.pure.chageumtong.ui.theme.fig
import com.pure.chageumtong.ui.theme.figSp
import kotlin.math.min
import kotlin.math.roundToInt

// Figma 포인트 적립 규칙 팝업 (107:2 / 112:992~1227) 색. 앱 Primary(5EC1F2)와 다른 팝업 전용 값
private val Accent = Color(0xFF0DC4F5)
private val Ink = Color(0xFF1C1F24)
private val Sub = Color(0xFF6B7380)
private val Gray = Color(0xFFF3F4F6)
private val Disabled = Color(0xFFCCD1D9)
private val Road = Color(0xFF5C616B)
private val Red = Color(0xFFF04D4D)
private val Orange = Color(0xFFF58033)
private val Bar = Color(0xFFE0E3E8)
private val Green = Color(0xFF26B878)
private val DeepBlue = Color(0xFF0099D9)
private val BlueTint = Color(0xFFE6F7FF)
private val RedTint = Color(0xFFFCE0E0)
private val OrangeTint = Color(0xFFFFEDE0)

private const val W = 1000
private const val H = 816
private const val STEPS = 5

private fun ts(px: Number, weight: FontWeight, lineHeight: Float? = null) = TextStyle(
    fontFamily = Pretendard, fontWeight = weight, fontSize = figSp(px),
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
)

/** Figma 절대 좌표 텍스트. w가 있으면 그 폭 안에서 가운데 정렬 */
@Composable
private fun Txt(text: String, x: Number, y: Number, px: Number, weight: FontWeight, color: Color, w: Number? = null, lineHeight: Float? = null) {
    val m = Modifier.offset(fig(x), fig(y)).let { if (w != null) it.width(fig(w)) else it }
    Text(text, m, style = ts(px, weight, lineHeight), color = color, textAlign = if (w != null) TextAlign.Center else TextAlign.Start)
}

@Composable
private fun Rect(x: Number, y: Number, w: Number, h: Number, color: Color, radius: Number = 0) =
    Box(Modifier.offset(fig(x), fig(y)).size(fig(w), fig(h)).clip(RoundedCornerShape(fig(radius))).background(color))

@Composable
private fun Circle(x: Number, y: Number, d: Number, color: Color) =
    Box(Modifier.offset(fig(x), fig(y)).size(fig(d)).background(color, CircleShape))

/** SVG 아이콘: Figma 노드 위치에서 stroke 여백(inset)만큼 바깥으로 그려진 실제 viewBox 크기·위치 */
@Composable
private fun Icon(id: Int, x: Number, y: Number, w: Number, h: Number) =
    Image(painterResource(id), null, Modifier.offset(fig(x), fig(y)).size(fig(w), fig(h)))

/** 흰 도로(Figma 점선 22×4, 간격 40) */
@Composable
private fun RoadBar(x: Int, y: Int, w: Int, dashStart: Int, dashEnd: Int) {
    Rect(x, y, w, 52, Road, 10)
    var dx = dashStart
    while (dx <= dashEnd) { Rect(dx, y + 24, 22, 4, Color.White); dx += 40 }
}

@Composable
private fun Unicorn(x: Number, y: Number, rotation: Float = 0f) = Image(
    painterResource(R.drawable.unicorn_run), null,
    Modifier.offset(fig(x), fig(y)).size(fig(105), fig(74.7)).rotate(rotation),
    contentScale = ContentScale.Fit,
)

/**
 * Figma 팝업 공통 틀: 뒤 딤, 화면 가운데, 흰 카드(라운드 32·그림자), 왼쪽 위 X(56), 위 가운데 회색 제목.
 * 1000 폭 기준으로 그리고, 분할 화면처럼 창이 좁으면 통째로 줄인다.
 */
@Composable
fun FigPopup(title: String, onClose: () -> Unit, height: Int? = H, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        // 시스템 dim은 Pleos 멀티 윈도우에서 거의 안 보여서, 창을 꽉 채우고 딤을 직접 그린다
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        val conf = LocalConfiguration.current
        val scale = min(1f, min((conf.screenWidthDp - 48) / fig(W).value, (conf.screenHeightDp - 48) / fig(height ?: H).value))
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                .clickable(interactionSource = null, indication = null, onClick = onClose), // 바깥 누르면 닫기
            contentAlignment = Alignment.Center,
        ) {
        Box(
            Modifier.fitScale(scale)
                .clickable(interactionSource = null, indication = null) {} // 카드 안 누름은 바깥(닫기)으로 안 새게
                .width(fig(W)).let { if (height != null) it.height(fig(height)) else it }
                .shadow(fig(48), RoundedCornerShape(fig(32)), ambientColor = Color.Black.copy(alpha = 0.14f), spotColor = Color.Black.copy(alpha = 0.14f))
                .clip(RoundedCornerShape(fig(32))).background(Color.White),
        ) {
            content()
            Txt(title, 0, 46, 20, FontWeight.Bold, Sub, w = W)
            // 보이는 원은 Figma 56, 누르는 영역은 최소 76dp (PRD §7). 원 중심 (60, 60)
            Box(
                Modifier.offset(fig(60) - MIN_TOUCH / 2, fig(60) - MIN_TOUCH / 2).size(MIN_TOUCH).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(fig(56)).background(Gray, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_rule_close), "닫기", Modifier.size(fig(21)))
                }
            }
        }
        }
    }
}

/** 측정·그리기를 함께 줄여서, 줄어든 크기로 자리를 차지하게 한다 */
private fun Modifier.fitScale(s: Float) = if (s >= 1f) this else layout { m, _ ->
    val p = m.measure(Constraints())
    layout((p.width * s).roundToInt(), (p.height * s).roundToInt()) {
        p.placeWithLayer(0, 0) { scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 0f) }
    }
}

/** 팝업 아래 버튼 (Figma 200×86, 라운드 19, 글자 26 Bold) */
@Composable
fun FigPopupButton(text: String, container: Color, content: Color, enabled: Boolean = true, onClick: () -> Unit) = Box(
    Modifier.size(fig(200), fig(86)).clip(RoundedCornerShape(fig(19))).background(container)
        .clickable(enabled = enabled, onClick = onClick),
    contentAlignment = Alignment.Center,
) { Text(text, style = ts(26, FontWeight.Bold), color = content) }

/** 회색 보조 버튼 · 파란 주 버튼 */
@Composable fun FigPopupSecondary(text: String, onClick: () -> Unit) = FigPopupButton(text, Gray, Sub, onClick = onClick)
@Composable fun FigPopupPrimary(text: String, onClick: () -> Unit) = FigPopupButton(text, Accent, Color.White, onClick = onClick)

/** 짧은 안내·확인 팝업: 같은 틀에 굵은 제목 + 본문 + 버튼 줄 (높이는 내용에 맞춤) */
@Composable
fun FigMessagePopup(title: String, heading: String, body: String, onClose: () -> Unit, buttons: @Composable () -> Unit) {
    FigPopup(title, onClose, height = null) {
        Column(
            Modifier.width(fig(W)).padding(top = fig(116), bottom = fig(32), start = fig(100), end = fig(100)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(heading, style = ts(38, FontWeight.Bold), color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(fig(24)))
            Text(body, style = ts(22, FontWeight.Normal, 1.6f), color = Sub, textAlign = TextAlign.Center)
            Spacer(Modifier.height(fig(48)))
            Row(horizontalArrangement = Arrangement.spacedBy(fig(22))) { buttons() }
        }
    }
}

/** 포인트 적립 규칙 5단계 (Figma 112:992 / 1072 / 1110 / 1189 / 1227) */
@Composable
fun RulesPopup(onClose: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    FigPopup("포인트 적립 규칙", onClose) {
        Txt("STEP ${step + 1}", 0, 108, 18, FontWeight.Bold, Accent, w = W)
        when (step) {
            0 -> Step1()
            1 -> Step2()
            2 -> Step3()
            3 -> Step4()
            else -> Step5()
        }
        // 단계 점 (활성 36×12, 나머지 12×12, 간격 12)
        Row(Modifier.offset(fig(434), fig(663)), horizontalArrangement = Arrangement.spacedBy(fig(12))) {
            repeat(STEPS) { i ->
                Box(Modifier.size(fig(if (i == step) 36 else 12), fig(12)).clip(RoundedCornerShape(fig(6))).background(if (i == step) Accent else Disabled))
            }
        }
        Row(Modifier.offset(fig(289), fig(698)), horizontalArrangement = Arrangement.spacedBy(fig(22))) {
            FigPopupButton("이전", Gray, Disabled, enabled = step > 0) { step-- }
            if (step < STEPS - 1) {
                FigPopupButton("다음", Accent, Color.White) { step++ }
            } else {
                FigPopupButton("닫기", Ink, Color.White, onClick = onClose)
            }
        }
    }
}

@Composable
private fun Heading(title: String, sub: String) {
    Txt(title, 0, 136, 38, FontWeight.Bold, Ink, w = W)
    Txt(sub, 100, 196, 20, FontWeight.Normal, Sub, w = 800, lineHeight = 1.45f)
}

@Composable
private fun Step1() {
    Heading("안전하게 달린 1km = 1P", "차량이 직접 속도와 주행거리를 확인해서, 안전하게 달린 거리만큼 포인트를 드려요")
    Rect(72, 260, 856, 380, Gray, 24)
    RoadBar(120, 330, 760, 134, 854)
    // 113:1283~1319: 1km마다 당근 (P 동그라미 → 당근으로 바뀜)
    listOf(262.873, 390, 522, 654, 786).forEach { x -> Icon(R.drawable.carrot_small, x, 279, 40.255, 41.127) }
    listOf(260, 390, 520, 650, 780).forEach { x -> Txt("1km", x - 20, 392, 20, FontWeight.Medium, Sub, w = 80) }
    Unicorn(137, 287)
    Rect(120, 440, 760, 114, Color.White, 18)
    Txt("18km 주행 중 16km를 안전하게 달렸다면", 150, 462, 20, FontWeight.Normal, Sub)
    Txt("→  16P 적립", 150, 494, 36, FontWeight.Bold, Accent)
    Circle(769, 454, 85, BlueTint)
    Txt("16P", 769, 480, 28.333, FontWeight.Bold, DeepBlue, w = 85)
    Txt("• 0.3km 미만의 짧은 주행은 적립되지 않아요     • 소수점 아래 거리는 제외해요", 120, 588, 20, FontWeight.Normal, Sub, w = 760)
}

@Composable
private fun Step2() {
    Heading("이럴 때는 포인트가 쌓이지 않아요", "위험한 운전이 감지된 구간은 안전운전 거리에서 빠져요")
    data class Card(val x: Int, val tint: Color, val title: String, val body: String, val icon: @Composable () -> Unit)
    listOf(
        Card(72, RedTint, "과속", "제한속도보다\n10km/h 넘게\n3초 이상 달릴 때") {
            Icon(R.drawable.ic_rule_gauge, 140, 337, 66, 36)
            Icon(R.drawable.ic_rule_gauge_needle, 170, 341, 28, 32)
        },
        Card(290, OrangeTint, "급가속·급출발", "1초에 11km/h 이상\n갑자기 빨라질 때") { Icon(R.drawable.ic_rule_arrow_up, 366, 325, 50, 58) },
        Card(508, OrangeTint, "급감속·급정지", "1초에 7.5km/h 이상\n갑자기 느려질 때") { Icon(R.drawable.ic_rule_arrow_down, 584, 325, 50, 58) },
        Card(726, BlueTint, "급커브 미감속", "커브에서 속도를\n충분히 줄이지 않을 때") { Icon(R.drawable.ic_rule_curve, 796, 329, 62, 43.5) },
    ).forEach { c ->
        Rect(c.x, 264, 203, 380, Gray, 24)
        Circle(c.x + 51, 304, 100, c.tint)
        c.icon()
        Txt(c.title, c.x, 434, 24, FontWeight.Bold, Ink, w = 203)
        Txt(c.body, c.x, 476, 20, FontWeight.Normal, Sub, w = 203, lineHeight = 1.5f)
    }
}

@Composable
private fun Step3() {
    Heading("빠를수록 더 길게 빠져요", "위험 운전이 일어난 지점 앞뒤로, 그 속도에서 멈추는 데 필요한 거리만큼 적립되지 않아요")
    Rect(72, 264, 856, 380, Gray, 24)
    Txt("예) 60km/h로 달리다 급정지하면", 110, 298, 20, FontWeight.Bold, Ink)
    RoadBar(110, 378, 780, 124, 844)
    Rect(350, 378, 300, 52, Red.copy(alpha = 0.6f))
    Rect(499, 338, 2, 40, Red)
    Rect(440, 308, 120, 34, Red, 17)
    Txt("급정지!", 440, 314, 16, FontWeight.Bold, Color.White, w = 120)
    // 유니콘: Figma 그룹(469,313 128×117) 가운데에 30° 돌린 105×74.7
    Unicorn(533.5 - 52.5, 371.6 - 37.35, rotation = 30f)
    Rect(350, 444, 2, 12, Red); Rect(498, 444, 2, 12, Red); Rect(648, 444, 2, 12, Red); Rect(350, 454, 300, 2, Red)
    Txt("37.5m", 350, 464, 20, FontWeight.Bold, Red, w = 150)
    Txt("37.5m", 500, 464, 20, FontWeight.Bold, Red, w = 150)
    Txt("총 75m 미적립", 690, 306, 22, FontWeight.Bold, Red)
    listOf(Triple(110, "30km/h", "30m"), Triple(306, "60km/h", "75m"), Triple(502, "80km/h", "110m"), Triple(698, "100km/h", "155m"))
        .forEach { (x, speed, dist) ->
            val on = speed == "60km/h"
            Rect(x, 503, 180, 73, if (on) Red else Color.White, 14)
            Txt(speed, x, 511, 20, FontWeight.Normal, if (on) Color.White else Sub, w = 180)
            Txt(dist, x, 536, 26, FontWeight.Bold, if (on) Color.White else Ink, w = 180)
        }
    Txt("주행마다 첫 급감속 1건은 봐드려요 · 겹치는 구간은 한 번만 빠져요", 110, 594, 20, FontWeight.Medium, DeepBlue, w = 780)
}

@Composable
private fun Step4() {
    Heading("과속은 넘은 만큼 더 빠져요", "과속한 구간은 통째로 빠지고, 제한속도를 많이 넘을수록 앞뒤로 더 길게 빠져요")
    Rect(72, 264, 856, 380, Gray, 24)
    listOf(
        Triple("+10~20km/h", Accent, 75 to "과속한 구간만"),
        Triple("+20~40km/h", Orange, 150 to "과속 구간 + 앞뒤 거리 ×2"),
        Triple("+40~60km/h", Red, 225 to "과속 구간 + 앞뒤 거리 ×4"),
        Triple("+60km/h 초과", Ink, 300 to "그 주행은 0P"),
    ).forEachIndexed { i, (label, color, bar) ->
        val y = 292 + i * 84
        Rect(110, y, 780, 72, Color.White, 16)
        Txt(label, 134, y + 22, 20, FontWeight.Bold, color)
        Rect(320, y + 24, 300, 24, Bar, 12)
        Rect(320, y + 24, bar.first, 24, color, 12)
        Txt(bar.second, 640, y + 24, 20, FontWeight.Medium, Ink)
    }
}

@Composable
private fun Step5() {
    Heading("주차하면 알림으로 적립해요", "운전 중에는 아무것도 띄우지 않고, 안전하게 주차한 뒤에만 알려드려요")
    Rect(72, 264, 856, 380, Gray, 24)
    listOf(
        Triple(110, Ink, Triple("P", "P단으로 주차", "정차가 확인되면\n이번 운전이 끝나요")),
        Triple(380, Accent, Triple("!", "포인트 알림", "적립할 수 있는\n포인트를 알려드려요")),
        Triple(650, Green, Triple("✓", "눌러서 적립", "적립과 함께 놓친\n구간도 확인해요")),
    ).forEach { (x, color, t) ->
        Rect(x, 296, 240, 250, Color.White, 20)
        Circle(x + 80, 320, 80, color)
        Txt(t.first, x + 80, 336, 34, FontWeight.Bold, Color.White, w = 80)
        Txt(t.second, x, 420, 22, FontWeight.Bold, Ink, w = 240)
        Txt(t.third, x + 16, 458, 20, FontWeight.Normal, Sub, w = 208, lineHeight = 1.5f)
    }
    Icon(R.drawable.ic_rule_chevron, 354, 408, 20, 28)
    Icon(R.drawable.ic_rule_chevron, 624, 408, 20, 28)
    Txt(
        "알림을 놓쳐도 다음에 탈 때 출발 전에 다시 알려드려요\n모은 포인트는 상점에서 상품 가격만큼 사용할 수 있어요",
        110, 566, 20, FontWeight.Medium, Sub, w = 780, lineHeight = 1.6f,
    )
}
