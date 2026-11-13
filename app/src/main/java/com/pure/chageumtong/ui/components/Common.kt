package com.pure.chageumtong.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.pure.chageumtong.R
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.StrokeGray
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextHint
import com.pure.chageumtong.ui.theme.TextTertiary
import com.pure.chageumtong.ui.theme.Track
import com.pure.chageumtong.ui.theme.Warn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.pure.chageumtong.domain.Violation
import com.pure.chageumtong.ui.theme.BODY
import com.pure.chageumtong.ui.theme.Blue
import com.pure.chageumtong.ui.theme.BoxGray
import com.pure.chageumtong.ui.theme.CardBg
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.Orange
import com.pure.chageumtong.ui.theme.TextSub
import com.pure.chageumtong.ui.theme.fig
import com.pure.chageumtong.ui.theme.figSp
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun fmtKm(km: Double) = "%.1f".format(km)
fun fmtP(p: Int) = "%,d".format(p)

/** 일러스트·이미지 자리 (PNG가 들어오면 STEP 6에서 교체) */
@Composable
fun GrayBox(label: String, modifier: Modifier = Modifier) {
    Box(modifier.background(BoxGray, RoundedCornerShape(fig(16))), contentAlignment = Alignment.Center) {
        Text(label, fontSize = BODY, color = TextSub)
    }
}

/** Figma 카드: Surface + 1px 테두리 #C2C2C2 + 라운드 24 */
@Composable
fun SectionCard(modifier: Modifier = Modifier, color: Color = Surface, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(D.RadiusCard)
    Column(
        modifier.clip(shape).background(color).border(D.StrokeCard, StrokeGray, shape).padding(fig(24)),
        verticalArrangement = Arrangement.spacedBy(fig(8)),
        content = content,
    )
}

/** 회색박스 화면용 버튼. 터치 타깃 최소 76dp (PRD §7) */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Blue,
    content: Color = Color.White,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MIN_TOUCH),
        shape = RoundedCornerShape(D.RadiusButton),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) { Text(text, style = T.Button) }
}

/** Figma 버튼 상자: 고정 크기 + 라운드 19. 비활성이면 회색. */
@Composable
fun FigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = Primary,
    content: Color = Color.White,
    style: TextStyle = T.Button,
) {
    Box(
        modifier.heightIn(min = MIN_TOUCH).clip(RoundedCornerShape(D.RadiusButton))
            .background(if (enabled) container else Track)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = style, color = if (enabled) content else TextHint, maxLines = 1) }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = TextSub) =
    Text(text, modifier, fontSize = BODY, color = color)

/** 반원 게이지: Track 트랙 + Primary 진행, 끝 라운드. 높이 = 폭/2 + 두께/2 */
@Composable
fun SemiGauge(ratio: Int, modifier: Modifier = Modifier, stroke: Dp = fig(28)) {
    Canvas(modifier) {
        val s = stroke.toPx()
        val d = size.width - s
        val topLeft = Offset(s / 2, s / 2)
        val style = Stroke(s, cap = StrokeCap.Round)
        drawArc(Track, 180f, 180f, false, topLeft, Size(d, d), style = style)
        if (ratio > 0) drawArc(Primary, 180f, 180f * ratio.coerceIn(0, 100) / 100f, false, topLeft, Size(d, d), style = style)
    }
}

/**
 * 진행 레일 (Figma 95:2229): 높이 12 트랙, 채움 Primary, 미적립 Warn, 5km 점, 차 배지 41.
 * 0~25km, 초과 시 5km 단위 확장 (PRD §6.1).
 */
@Composable
fun ProgressRail(distanceKm: Double, violations: List<Violation>, modifier: Modifier = Modifier) {
    val maxKm = max(25.0, ceil(distanceKm / 5) * 5)
    val ticks = (maxKm / 5).toInt()
    val badge = fig(41)
    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(badge)) {
            val density = LocalDensity.current
            val w = with(density) { maxWidth.toPx() }
            fun x(km: Double) = (km / maxKm).toFloat().coerceIn(0f, 1f) * w
            Canvas(Modifier.fillMaxSize()) {
                val h = fig(12).toPx()
                val top = (size.height - h) / 2
                val r = CornerRadius(fig(6).toPx())
                drawRoundRect(Track, Offset(0f, top), Size(w, h), r)
                if (distanceKm > 0) drawRoundRect(Primary, Offset(0f, top), Size(x(distanceKm), h), r)
                violations.forEach {
                    val end = min(it.segmentEndKm, distanceKm)
                    if (end > it.segmentStartKm) drawRect(Warn, Offset(x(it.segmentStartKm), top), Size(x(end) - x(it.segmentStartKm), h))
                }
                for (i in 1 until ticks) {
                    val km = i * 5.0
                    drawCircle(if (km <= distanceKm) Color.White else Primary.copy(alpha = 0.6f), fig(4).toPx(), Offset(x(km), size.height / 2))
                }
            }
            val badgePx = with(density) { badge.toPx() }
            Image(
                painterResource(R.drawable.ic_car_badge),
                contentDescription = "현재 위치",
                modifier = Modifier.offset { IntOffset((x(distanceKm) - badgePx / 2).coerceIn(0f, w - badgePx).roundToInt(), 0) }.size(badge),
            )
        }
        Spacer(Modifier.height(fig(10)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..ticks) Text(if (i == 0) "0" else "${i * 5}km", style = T.RailTick, color = TextTertiary)
        }
    }
}

private val WEEK_DAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 이번 주 월~일 막대 (Figma week-chart): 막대 간격 10, 라운드 8, 오늘 = Primary */
@Composable
fun WeekBars(values: List<Double>, todayIndex: Int, modifier: Modifier = Modifier, barsHeight: Dp = fig(125)) {
    val maxV = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val gap = fig(10)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(fig(8))) {
        Row(Modifier.fillMaxWidth().height(barsHeight), horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Bottom) {
            values.forEachIndexed { i, v ->
                Box(
                    Modifier.weight(1f).height((barsHeight * (v / maxV).toFloat()).coerceAtLeast(fig(8)))
                        .background(if (i == todayIndex) Primary else Track, RoundedCornerShape(D.RadiusBar)),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            WEEK_DAYS.forEachIndexed { i, d ->
                Text(
                    d,
                    Modifier.weight(1f),
                    style = if (i == todayIndex) T.CaptionBold else T.Caption,
                    color = if (i == todayIndex) Primary else TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 정규화 폴리라인을 길이 비율 f(0~1) 위치의 점으로 */
private fun pointAt(points: List<Offset>, f: Float): Offset {
    if (points.size < 2) return points.firstOrNull() ?: Offset.Zero
    val lens = points.zipWithNext { a, b -> hypot(b.x - a.x, b.y - a.y) }
    var target = lens.sum() * f.coerceIn(0f, 1f)
    for (i in lens.indices) {
        if (target <= lens[i] || i == lens.lastIndex) {
            val t = if (lens[i] == 0f) 0f else (target / lens[i]).coerceIn(0f, 1f)
            return points[i] + (points[i + 1] - points[i]) * t
        }
        target -= lens[i]
    }
    return points.last()
}

/**
 * 분석 화면 지도: 경로(파랑) + 미적립 구간(주황) + 선택 이벤트 말풍선.
 * shortcut: 위치는 km 비율로 데모 폴리라인에 투영, 실주행 GPS 경로 기록은 범위 밖(PRD §11 Won't).
 */
@Composable
fun RouteMap(
    polyline: List<List<Float>>,
    distanceKm: Double,
    violations: List<Violation>,
    selected: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.background(BoxGray, RoundedCornerShape(fig(24)))) {
        val density = LocalDensity.current
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val pts = polyline.map { Offset(it[0] * wPx, it[1] * hPx) }
        fun f(km: Double) = if (distanceKm <= 0) 0f else (km / distanceKm).toFloat()

        Label("지도 이미지 자리", Modifier.align(Alignment.TopStart).padding(fig(16)))
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(fig(10).toPx(), cap = StrokeCap.Round)
            drawPath(Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }, Blue, style = stroke)
            violations.forEachIndexed { i, v ->
                val seg = Path()
                for (k in 0..20) {
                    val p = pointAt(pts, f(v.segmentStartKm) + (f(v.segmentEndKm) - f(v.segmentStartKm)) * k / 20f)
                    if (k == 0) seg.moveTo(p.x, p.y) else seg.lineTo(p.x, p.y)
                }
                drawPath(seg, Orange, style = Stroke(fig(if (i == selected) 18 else 12).toPx(), cap = StrokeCap.Round))
            }
        }
        violations.getOrNull(selected)?.let { v ->
            val p = pointAt(pts, f(v.atKm))
            val cardW = with(density) { fig(380).toPx() }
            val x = (p.x - cardW / 2).coerceIn(0f, max(0f, wPx - cardW))
            val y = (p.y - with(density) { fig(260).toPx() }).coerceAtLeast(0f)
            SectionCard(
                Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }.width(fig(380)),
                color = Color.White,
            ) {
                Label(v.locationLabel)
                Text(v.title, fontSize = BODY, fontWeight = FontWeight.Bold, color = Orange)
                Text(v.description, fontSize = BODY, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
