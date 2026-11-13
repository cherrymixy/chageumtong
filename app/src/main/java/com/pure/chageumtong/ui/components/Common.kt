package com.pure.chageumtong.ui.components

import androidx.compose.foundation.Canvas
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

@Composable
fun SectionCard(modifier: Modifier = Modifier, color: Color = CardBg, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.background(color, RoundedCornerShape(fig(24))).padding(fig(24)),
        verticalArrangement = Arrangement.spacedBy(fig(8)),
        content = content,
    )
}

/** 터치 타깃 최소 76dp (PRD §7) */
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
        shape = RoundedCornerShape(fig(16)),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) { Text(text, fontSize = BODY, fontWeight = FontWeight.Bold) }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier, color: Color = TextSub) =
    Text(text, modifier, fontSize = BODY, color = color)

/** 반원 게이지: 회색 트랙 + 파랑 진행, 끝 라운드 */
@Composable
fun SemiGauge(ratio: Int, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(2f), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = fig(24).toPx()
            val d = min(size.width, size.height * 2) - stroke
            val topLeft = Offset((size.width - d) / 2, stroke / 2)
            val style = Stroke(stroke, cap = StrokeCap.Round)
            drawArc(BoxGray, 180f, 180f, false, topLeft, Size(d, d), style = style)
            if (ratio > 0) drawArc(Blue, 180f, 180f * ratio.coerceIn(0, 100) / 100f, false, topLeft, Size(d, d), style = style)
        }
        Text("$ratio%", fontSize = figSp(40), fontWeight = FontWeight.Bold)
    }
}

/** 진행 레일: 0~25km(초과 시 5km 단위 확장), 파랑 = 이동, 주황 = 미적립, 원 = 현재 위치 (PRD §6.1) */
@Composable
fun ProgressRail(distanceKm: Double, violations: List<Violation>, modifier: Modifier = Modifier) {
    val maxKm = max(25.0, ceil(distanceKm / 5) * 5)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(fig(48))) {
            val r = fig(22).toPx()
            val w = size.width - 2 * r
            val y = size.height / 2
            val stroke = fig(16).toPx()
            fun x(km: Double) = r + (km / maxKm).toFloat().coerceIn(0f, 1f) * w
            drawLine(BoxGray, Offset(x(0.0), y), Offset(x(maxKm), y), stroke, StrokeCap.Round)
            if (distanceKm > 0) drawLine(Blue, Offset(x(0.0), y), Offset(x(distanceKm), y), stroke, StrokeCap.Round)
            violations.forEach {
                val end = min(it.segmentEndKm, distanceKm)
                if (end > it.segmentStartKm) drawLine(Orange, Offset(x(it.segmentStartKm), y), Offset(x(end), y), stroke)
            }
            val car = Offset(x(distanceKm), y)
            drawCircle(Color.White, r, car)
            drawCircle(Blue, r, car, style = Stroke(fig(4).toPx()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..(maxKm / 5).toInt()) Label("${i * 5}km")
        }
    }
}

private val WEEK_DAYS = listOf("월", "화", "수", "목", "금", "토", "일")

/** 이번 주 월~일 막대, 오늘은 강조색 */
@Composable
fun WeekBars(values: List<Double>, todayIndex: Int, modifier: Modifier = Modifier) {
    val maxV = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.width(fig(24)).height(fig(110) * (v / maxV).toFloat().coerceAtLeast(0.04f))
                        .background(if (i == todayIndex) Blue else BoxGray, RoundedCornerShape(fig(8))),
                )
                Label(WEEK_DAYS[i], color = if (i == todayIndex) Blue else TextSub)
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
