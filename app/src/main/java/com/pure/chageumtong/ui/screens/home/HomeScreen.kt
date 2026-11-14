package com.pure.chageumtong.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.pure.chageumtong.R
import com.pure.chageumtong.domain.DriveState
import com.pure.chageumtong.domain.praise
import com.pure.chageumtong.ui.AppViewModel
import com.pure.chageumtong.ui.components.FigButton
import com.pure.chageumtong.ui.components.countUp
import com.pure.chageumtong.ui.components.ProgressRail
import com.pure.chageumtong.ui.components.SemiGauge
import com.pure.chageumtong.ui.components.WeekBars
import com.pure.chageumtong.ui.components.cardKm
import com.pure.chageumtong.ui.components.fmtKm
import com.pure.chageumtong.ui.components.fmtP
import com.pure.chageumtong.ui.theme.AdBadge
import com.pure.chageumtong.ui.theme.D
import com.pure.chageumtong.ui.theme.MIN_TOUCH
import com.pure.chageumtong.ui.theme.OnPrimary
import com.pure.chageumtong.domain.safeKm
import com.pure.chageumtong.ui.theme.Primary
import com.pure.chageumtong.ui.theme.PrimaryDeep
import com.pure.chageumtong.ui.theme.PrimaryHero
import com.pure.chageumtong.ui.theme.PrimaryDark
import com.pure.chageumtong.ui.theme.PrimaryTint
import com.pure.chageumtong.ui.theme.StrokeGray
import com.pure.chageumtong.ui.theme.Surface
import com.pure.chageumtong.ui.theme.SurfaceWarm
import com.pure.chageumtong.ui.theme.T
import com.pure.chageumtong.ui.theme.TextLabel
import com.pure.chageumtong.ui.theme.TextNumber
import com.pure.chageumtong.ui.theme.TextStat
import com.pure.chageumtong.ui.theme.TextTitle
import com.pure.chageumtong.ui.theme.TextUnit
import com.pure.chageumtong.ui.theme.Track
import com.pure.chageumtong.ui.theme.fig
import kotlinx.coroutines.delay
import java.time.LocalDate

/** 홈 (Figma 95:2142). 좌측 메인 카드가 남는/모자란 가로 폭을 흡수한다 (PRD §10). */
@Composable
fun HomeScreen(vm: AppViewModel, onClaimed: (String) -> Unit, onShop: () -> Unit, onHistory: () -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(start = D.ScreenPad, top = D.ScreenPad, end = D.ScreenPad),
        horizontalArrangement = Arrangement.spacedBy(D.ColumnGap),
    ) {
        DriveCard(vm, onClaimed, Modifier.weight(1f).height(D.ContentHeight))
        Column(Modifier.width(D.RightColumn)) {
            TotalCard(vm, onShop, onHistory)
            Spacer(Modifier.height(D.CardGap))
            Row(horizontalArrangement = Arrangement.spacedBy(D.SmallCardGap)) {
                SafeCard(vm, Modifier.weight(1f))
                DistanceCard(vm, Modifier.weight(1f))
            }
            Spacer(Modifier.height(D.SmallCardGap))
            AdCarousel(vm, Modifier.fillMaxWidth().height(fig(279)))
        }
    }
}

private fun Modifier.card(color: Color, radius: Dp = D.RadiusCard) = this
    .clip(RoundedCornerShape(radius))
    .background(color)
    .border(D.StrokeCard, StrokeGray, RoundedCornerShape(radius))

/**
 * 이음매 없는 도시 패턴(왼끝 = 오른끝)을 왼쪽으로 흘려보냄. 보통 속도 = 그림 한 장 폭을 LOOP_MS에 한 바퀴.
 * speed: 0 멈춤, 1 보통, OVERSPEED_SPEED 과속. 바뀔 때 부드럽게 가속·감속
 */
@Composable
private fun LoopingBackground(speed: Float, modifier: Modifier) {
    val bmp = ImageBitmap.imageResource(R.drawable.bg_city_loop)
    val v = animateFloatAsState(speed, tween(800), label = "citySpeed")
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                t = (t + (now - last) / 1_000_000f / LOOP_MS * v.value) % 1f
                last = now
            }
        }
    }
    Canvas(modifier) {
        val h = size.height.roundToInt()
        val w = (size.height * bmp.width / bmp.height).roundToInt()
        var x = -(t * w).roundToInt() // draw 단계에서만 읽어서 매 프레임 재구성 없음
        while (x < size.width) {
            drawImage(bmp, dstOffset = IntOffset(x, 0), dstSize = IntSize(w, h))
            x += w
        }
    }
}

private const val LOOP_MS = 10_000
private const val OVERSPEED_SPEED = 3f // 과속 때 배경 3배 빠르게

private val UNICORN_FRAMES = listOf(
    R.drawable.unicorn_run, R.drawable.unicorn_run_1, R.drawable.unicorn_run_2,
    R.drawable.unicorn_run_3, R.drawable.unicorn_run_4, R.drawable.unicorn_run_5,
)
private const val UNICORN_FRAME_MS = 140L

/** 좌측 메인 카드: 이번 주행 (95:2225) */
@Composable
private fun DriveCard(vm: AppViewModel, onClaimed: (String) -> Unit, modifier: Modifier) {
    val live by vm.live.collectAsState()
    val driving by vm.isDriving.collectAsState()
    val demoRunning by vm.demoRunning.collectAsState()

    val motion by vm.motion.collectAsState()
    val brakeTick by vm.brakeTick.collectAsState()

    // 급정거: 몸이 뒤로 살짝 젖혀졌다(발 기준 회전) 출렁이며 돌아옴
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(brakeTick) {
        if (brakeTick == 0) return@LaunchedEffect
        tilt.animateTo(-12f, tween(120))
        tilt.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
    }

    // 유니콘: 움직일 때 위아래 바운스, 멈추면 정지 (PLAYBOOK 6-B)
    val bounce by rememberInfiniteTransition(label = "unicorn").animateFloat(
        0f, -1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bounce",
    )
    val bouncePx = with(androidx.compose.ui.platform.LocalDensity.current) { fig(12).toPx() }

    Column(modifier.card(SurfaceWarm)) {
        Box(Modifier.fillMaxWidth().height(fig(739))) {
            LoopingBackground(
                when { !motion.moving -> 0f; motion.overspeed -> OVERSPEED_SPEED; else -> 1f },
                Modifier.fillMaxSize(),
            )
            // GIF 6장(장당 140ms)을 넘겨 달리기. 멈추면 첫 장에 서 있음, 과속이면 다리도 빠르게
            val frameMs = if (motion.overspeed) UNICORN_FRAME_MS / 2 else UNICORN_FRAME_MS
            val frame by produceState(0, motion.moving, frameMs) {
                if (!motion.moving) { value = 0; return@produceState }
                while (true) { delay(frameMs); value = (value + 1) % UNICORN_FRAMES.size }
            }
            Image(
                painterResource(UNICORN_FRAMES[frame]), "달리는 유니콘",
                // 새 그림 비율 458:375 → 높이 294 유지, 예전 그림과 가운데·바닥 맞춤
                Modifier.offset(fig(144), fig(422)).size(fig(359), fig(294))
                    .graphicsLayer {
                        translationY = if (motion.moving) bounce * bouncePx else 0f
                        rotationZ = tilt.value
                        transformOrigin = TransformOrigin(0.5f, 1f) // 발 기준
                    },
            )
            Column(Modifier.padding(start = fig(36), top = fig(36))) {
                Text("이번 주행으로 적립될 포인트", style = T.Label, color = TextLabel)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(10))) {
                    Text("${live.livePoints}", style = T.Hero, color = PrimaryHero)
                    Text("P", style = T.Hero, color = Primary.copy(alpha = 0.5f))
                }
            }
            // ▶ 데모 주행: Figma에 없는 요소라 하늘 우상단에 작은 칩으로 (PRD §4)
            if (live.state == DriveState.IDLE && !demoRunning && !driving) {
                FigButton(
                    "▶ 데모 주행", vm::startDemo,
                    Modifier.align(Alignment.TopEnd).padding(fig(36)).width(fig(200)),
                    container = Surface.copy(alpha = 0.92f), content = TextStat, style = T.Button,
                )
            }
        }
        Spacer(Modifier.height(fig(32)))
        ProgressRail(live.distanceKm, live.violations, Modifier.padding(horizontal = fig(36)).fillMaxWidth())
        Spacer(Modifier.height(fig(32)))
        HorizontalDivider(Modifier.padding(horizontal = fig(36)), thickness = fig(1.5), color = Track)
        Spacer(Modifier.height(fig(25)))
        Row(Modifier.padding(start = fig(30), end = fig(36)), verticalAlignment = Alignment.Top) {
            ClaimButton(vm, onClaimed, live.state, live.canClaim, driving)
            Spacer(Modifier.width(fig(47)))
            // Figma 107:114: 칸 간격 214 (248 폭 - 34 겹침)
            Row(Modifier.weight(1f)) {
                Stat("안전운전 거리", fmtKm(safeKm(live.distanceKm, live.violations)), "km", Modifier.width(fig(214)))
                Stat("이동거리", fmtKm(live.distanceKm), "km", Modifier.width(fig(214)))
                Stat("주행시간", "${live.durationSec / 60}", "분", Modifier.wrapContentWidth(Alignment.Start, unbounded = true))
            }
        }
    }
}

@Composable
private fun ClaimButton(vm: AppViewModel, onClaimed: (String) -> Unit, state: DriveState, canClaim: Boolean, driving: Boolean) {
    val m = Modifier.size(fig(364), fig(92))
    when {
        state == DriveState.ENDED && !canClaim -> FigButton("0.3km 미만 · 닫기", vm::dismissDrive, m, style = T.ButtonLarge)
        driving -> FigButton("정차 후 적립할 수 있어요", {}, m, enabled = false, style = T.ButtonLarge)
        else -> FigButton("주행 분석하고 적립하기", { vm.claim(onClaimed) }, m, enabled = canClaim, style = T.ButtonLarge)
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = T.Label, color = TextStat, maxLines = 1, softWrap = false)
        Spacer(Modifier.height(fig(19)))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(4))) {
            Text(value, style = T.Stat, color = TextStat)
            Text(unit, style = T.Label, color = TextStat, modifier = Modifier.padding(bottom = fig(6)))
        }
    }
}

/** 총 적립 포인트 카드 (95:2167) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TotalCard(vm: AppViewModel, onShop: () -> Unit, onHistory: () -> Unit) {
    val data by vm.data.collectAsState()
    val month by vm.month.collectAsState()
    val driving by vm.isDriving.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(driving) { if (driving) confirmReset = false }

    Box(Modifier.fillMaxWidth().height(fig(334)).card(Primary)) {
        Image(painterResource(R.drawable.carrot_big), null, Modifier.offset(fig(259), fig(43)).size(fig(329), fig(336)))
        Box(
            Modifier.offset(fig(248), fig(221)).size(fig(308), fig(113))
                .background(Brush.verticalGradient(listOf(PrimaryDeep.copy(alpha = 0f), PrimaryDeep))),
        )
        // 숨김 메뉴: 제목 길게 누르기 → 데이터 초기화 (PRD §8). 주행 중엔 막음 (PRD §7)
        Text(
            "총 적립 포인트",
            Modifier.offset(fig(35), fig(29)).combinedClickable(enabled = !driving, onClick = {}, onLongClick = { confirmReset = true }),
            style = T.Label, color = OnPrimary,
        )
        Row(Modifier.offset(fig(35), fig(69)), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(6.3))) {
            Text(fmtP(countUp(data.totalPoints)), style = T.TotalNumber, color = OnPrimary)
            Text("P", style = T.TotalNumber, color = OnPrimary.copy(alpha = 0.5f))
        }
        Row(Modifier.offset(fig(35), fig(162)), horizontalArrangement = Arrangement.spacedBy(fig(6))) {
            Text("이번 달", style = T.Month, color = OnPrimary.copy(alpha = 0.7f))
            Text("+${fmtP(month?.earned ?: 0)}P", style = T.MonthBold, color = OnPrimary.copy(alpha = 0.9f))
            Text("적립", style = T.Month, color = OnPrimary.copy(alpha = 0.7f))
        }
        FigButton("포인트 사용", onShop, Modifier.offset(fig(24), fig(221)).size(fig(351), fig(89)), container = Surface, content = TextNumber)
        FigButton("적립 내역 확인", onHistory, Modifier.offset(fig(389), fig(221)).size(fig(199), fig(89)), container = PrimaryDark, content = Color.White)
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("데이터 초기화", style = T.Section) },
            text = { Text("주행·포인트 기록을 처음 상태(시드 데이터)로 되돌릴까요?", style = T.DialogBody) },
            confirmButton = { TextButton({ vm.resetAll(); confirmReset = false }, Modifier.heightIn(min = MIN_TOUCH)) { Text("초기화", style = T.Button) } },
            dismissButton = { TextButton({ confirmReset = false }, Modifier.heightIn(min = MIN_TOUCH)) { Text("취소", style = T.Button) } },
        )
    }
}


@Composable
private fun CardTitle(title: String, value: String, valueColor: Color) {
    Text(title, style = T.Label, color = TextTitle)
    Spacer(Modifier.height(fig(13)))
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(fig(4))) {
        Text(value, style = T.CardNumber, color = valueColor)
        Text("km", style = T.CardNumber, color = TextUnit.copy(alpha = 0.5f))
    }
}

/** 이번 달 안전운전 거리 + 반원 게이지 (107:57). 값은 최근 30일 창 (11/14 승아: 문구만 이번 달) */
@Composable
private fun SafeCard(vm: AppViewModel, modifier: Modifier) {
    val month by vm.month.collectAsState()
    val ratio = month?.safeRatio ?: 0
    Box(modifier.height(fig(365)).card(Surface)) {
        Column(Modifier.padding(start = fig(32), top = fig(35))) {
            CardTitle("이번 달 안전운전 거리", cardKm(month?.safeKm ?: 0.0), TextNumber)
        }
        Column(Modifier.fillMaxWidth().padding(top = fig(178)), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomCenter) {
                SemiGauge(ratio, Modifier.size(fig(229), fig(129)), stroke = fig(28), color = PrimaryDeep)
                Text("$ratio%", style = T.GaugeSmall, color = PrimaryDeep)
            }
            Spacer(Modifier.height(fig(4)))
            Text(
                praise(ratio),
                Modifier.background(PrimaryTint, CircleShape).padding(horizontal = fig(16), vertical = fig(8)),
                style = T.Badge, color = Primary,
            )
        }
    }
}

/** 이번 달 이동거리 + 이번 주 요일 막대 (107:10). 값은 최근 30일 창 */
@Composable
private fun DistanceCard(vm: AppViewModel, modifier: Modifier) {
    val month by vm.month.collectAsState()
    val week by vm.week.collectAsState()
    val today = LocalDate.now(vm.zone).dayOfWeek.value - 1
    Column(modifier.height(fig(365)).card(Surface).padding(start = fig(35), top = fig(35), end = fig(31), bottom = fig(25))) {
        CardTitle("이번 달 이동거리", cardKm(month?.distanceKm ?: 0.0), TextNumber)
        Spacer(Modifier.weight(1f))
        WeekBars(week, today, Modifier.fillMaxWidth())
    }
}

private const val AD_COUNT = 5

/** 광고 배너 캐러셀: 5초 자동 넘김, 주행 중 정지 (PRD §7) */
@Composable
private fun AdCarousel(vm: AppViewModel, modifier: Modifier) {
    val driving by vm.isDriving.collectAsState()
    val pager = rememberPagerState { AD_COUNT }
    LaunchedEffect(driving) {
        while (!driving) {
            delay(5_000)
            pager.animateScrollToPage((pager.currentPage + 1) % AD_COUNT)
        }
    }
    Box(modifier.clip(RoundedCornerShape(D.RadiusCard))) {
        // shortcut: 배너 이미지는 Figma의 1장(시연용, 저작권 현대자동차)뿐이라 5장 모두 같은 이미지, 추가 배너 받으면 교체
        HorizontalPager(pager, Modifier.fillMaxSize()) {
            Image(painterResource(R.drawable.ad_banner_1), "광고", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Text(
            "Ad",
            Modifier.align(Alignment.TopEnd).padding(top = fig(14), end = fig(14))
                .background(AdBadge, CircleShape).padding(horizontal = fig(12), vertical = fig(5)),
            style = T.AdBadge, color = Color.White,
        )
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = fig(10)),
            horizontalArrangement = Arrangement.spacedBy(fig(6)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(AD_COUNT) { i ->
                val on = i == pager.currentPage
                Box(Modifier.size(if (on) fig(10) else fig(6)).background(Color.White.copy(alpha = if (on) 1f else 0.6f), CircleShape))
            }
        }
    }
}
